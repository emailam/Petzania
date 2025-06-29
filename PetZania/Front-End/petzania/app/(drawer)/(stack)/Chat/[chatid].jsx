import React, { useState, useEffect, useContext, useCallback } from 'react';
import { View, ActivityIndicator, Text, StyleSheet, TouchableOpacity, TextInput, Alert, Platform } from 'react-native';
import { Image } from 'expo-image';

import * as DocumentPicker from 'expo-document-picker';
import * as ImagePicker from 'expo-image-picker';

import { GiftedChat, Bubble, InputToolbar, Send } from 'react-native-gifted-chat';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { UserContext } from '@/context/UserContext';
import { getMessagesByChatId, sendMessage, getChatById, updateMessageStatus, editMessage, deleteMessage } from '@/services/chatService';
import { uploadFile } from '@/services/uploadService';
import chatStompService from '@/services/chatStompService';
import Toast from 'react-native-toast-message';
import { getToken } from '@/storage/tokenStorage';
import * as Haptics from 'expo-haptics';

export default function ChatDetailScreen() {
    const { chatid } = useLocalSearchParams();
    const router = useRouter();
    const { user: currentUser } = useContext(UserContext);
    const [messages, setMessages] = useState([]);
    const [loading, setLoading] = useState(true);
    const [otherUser, setOtherUser] = useState(null);
    const [loadingEarlier, setLoadingEarlier] = useState(false);
    const [hasMoreMessages, setHasMoreMessages] = useState(true);
    const [currentPage, setCurrentPage] = useState(0);
    const [messagesPerPage] = useState(50);
    const [showMessageActions, setShowMessageActions] = useState(false);
    const [selectedMessage, setSelectedMessage] = useState(null);
    const [showEditInput, setShowEditInput] = useState(false);
    const [editText, setEditText] = useState('');
    const [replyToMessage, setReplyToMessage] = useState(null);

    const [uploadingImages, setUploadingImages] = useState(false);

    const defaultImage = require('@/assets/images/Defaults/default-user.png');

    // Utility functions for file handling
    const isImageFile = (url) => {
        const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg'];
        return imageExtensions.some(ext => url.toLowerCase().includes(ext));
    };

    const getFileNameFromUrl = (url) => {
        try {
            const parts = url.split('/');
            const fileName = parts[parts.length - 1];
            return fileName.split('?')[0] || 'Unknown File';
        } catch (error) {
            return 'Unknown File';
        }
    };

    const getFileTypeFromUrl = (url) => {
        try {
            const fileName = getFileNameFromUrl(url);
            const extension = fileName.split('.').pop();
            return extension ? `.${extension}` : 'Unknown';
        } catch (error) {
            return 'Unknown';
        }
    };

    useEffect(() => {
        const connectToStomp = async () => {
            if (currentUser?.userId && chatid) {
                try {
                    const token = await getToken('accessToken');
                    const client = chatStompService.connect(currentUser.userId, token);
                } catch (error) {
                    console.error('Error connecting to STOMP:', error);
                }
            }
        };
        connectToStomp();
        return () => {
            if (chatStompService.isClientConnected()) {
                chatStompService.unsubscribeFromChatTopic(chatid);
            }
        };
    }, [currentUser?.userId, chatid]);

    const handleTopicMessage = useCallback((data) => {
        if (data.eventType === 'SEND' && data.messageDTO) {
            const messageData = data.messageDTO;

            let avatar;
            if (messageData.senderId === currentUser?.userId) {
                avatar = currentUser?.profilePictureURL || defaultImage;
            } else {
                avatar = otherUser?.profilePictureURL || defaultImage;
            }

            const newMessage = {
                _id: messageData.messageId,
                text: messageData.file ? '' : messageData.content,
                createdAt: new Date(messageData.sentAt),
                user: {
                    _id: messageData.senderId,
                    name: messageData.senderName || 'Unknown',
                    avatar: avatar
                },
                edited: messageData.edited || false,
                status: messageData.status || 'SENT',
                ...(messageData.file && {
                    ...(isImageFile(messageData.content) ? {
                        image: messageData.content,
                    } : {
                        file: {
                            url: messageData.content,
                            name: getFileNameFromUrl(messageData.content),
                            type: getFileTypeFromUrl(messageData.content)
                        }
                    })
                })
            };

            setMessages(previousMessages => GiftedChat.append(previousMessages, [newMessage]));

            if (messageData.senderId !== currentUser.userId) {
                setTimeout(async () => {
                    try {
                        await markMessageAsDelivered(messageData.messageId);
                        setTimeout(async () => {
                            try {
                                await markMessageAsRead(messageData.messageId);
                            } catch (error) {
                                console.error(`Failed to mark new message ${messageData.messageId} as read:`, error);
                            }
                        }, 200);
                    } catch (error) {
                        console.error(`Failed to mark new message ${messageData.messageId} as delivered:`, error);
                    }
                }, 100);
            }
        } else if (data.eventType === 'EDIT' && data.messageDTO) {
            const updatedMessage = data.messageDTO;
            setMessages(previousMessages => 
                previousMessages.map(msg => 
                    msg._id === updatedMessage.messageId 
                        ? { ...msg, text: updatedMessage.content, edited: true }
                        : msg
                )
            );
        } else if (data.eventType === 'DELETE' && data.messageDTO) {
            const deletedMessage = data.messageDTO;
            setMessages(previousMessages => 
                previousMessages.filter(msg => msg._id !== deletedMessage.messageId)
            );
        } else if (data.eventType === 'UPDATE_STATUS' && data.messageDTO) {
            const updatedMessage = data.messageDTO;
            setMessages(previousMessages => 
                previousMessages.map(msg => 
                    msg._id === updatedMessage.messageId 
                        ? { ...msg, status: updatedMessage.status }
                        : msg
                )
            );
        }
    }, [currentUser?.userId, currentUser?.profilePictureURL, otherUser?.profilePictureURL]);

    useEffect(() => {
        if (chatid) {
            loadChatData();
        }
    }, [chatid]);

    const loadChatData = async () => {
        try {
            setLoading(true);

            const [chatData, messagesData] = await Promise.all([
                getChatById(chatid),
                getMessagesByChatId(chatid, 0, messagesPerPage)
            ]);

            const otherUserId = chatData.user1Id === currentUser?.userId ? chatData.user2Id : chatData.user1Id;
            const { getUserById } = await import('@/services/userService');
            const otherUserData = await getUserById(otherUserId);

            // Set otherUser first
            setOtherUser(otherUserData);

            // Handle pagination response
            const messagesArray = Array.isArray(messagesData) ? messagesData : (messagesData.content || []);
            const totalPages = messagesData.totalPages || 1;
            const currentPageNum = messagesData.number || 0;

            // Check if there are more messages to load
            setHasMoreMessages(currentPageNum < totalPages - 1);
            setCurrentPage(currentPageNum);

            // Then transform messages with the otherUser data available
            const transformedMessages = messagesArray.map(msg => {
                let avatar;
                if (msg.senderId === currentUser?.userId) {
                    avatar = currentUser?.profilePictureURL || defaultImage;
                } else {
                    avatar = otherUserData?.profilePictureURL || defaultImage;
                }
                return {
                    _id: msg.messageId,
                    text: msg.file ? '' : msg.content,
                    createdAt: new Date(msg.sentAt),
                    user: {
                        _id: msg.senderId,
                        name: msg.senderName,
                        avatar: avatar
                    },
                    edited: msg.edited || false,
                    status: msg.status || 'SENT',
                    ...(msg.file && {
                        ...(isImageFile(msg.content) ? {
                            image: msg.content,
                        } : {
                            file: {
                                url: msg.content,
                                name: getFileNameFromUrl(msg.content),
                                type: getFileTypeFromUrl(msg.content)
                            }
                        })
                    }),
                };
            });

            setMessages(transformedMessages);

            setupStompSubscription();

            const unreadMessages = transformedMessages.filter(msg => 
                msg.user._id !== currentUser?.userId && 
                (msg.status === 'SENT' || msg.status === 'DELIVERED')
            );

            if (unreadMessages.length > 0) {
                const messagesToProcess = unreadMessages.slice(0, 10);
                messagesToProcess.forEach((message, index) => {
                    setTimeout(async () => {
                        try {
                            if (message.status === 'SENT') {
                                await markMessageAsDelivered(message._id);
                                setTimeout(async () => {
                                    try {
                                        await markMessageAsRead(message._id);
                                    } catch (error) {
                                        console.error(`Failed to mark message ${message._id} as read:`, error);
                                    }
                                }, 200);
                            } else if (message.status === 'DELIVERED') {
                                await markMessageAsRead(message._id);
                            }
                        } catch (error) {
                            console.error(`Failed to update message ${message._id} status:`, error);
                        }
                    }, index * 100);
                });
            }
        } catch (error) {
            console.error('Error loading chat data:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to load chat',
                position: 'top',
                visibilityTime: 3000,
            });
        } finally {
            setLoading(false);
        }
    };

    const setupStompSubscription = () => {
        const checkConnection = () => {
            if (chatStompService.isClientConnected()) {
                chatStompService.subscribeToChatTopic(chatid, handleTopicMessage);
            } else {
                setTimeout(checkConnection, 100);
            }
        };
        setTimeout(checkConnection, 500);
    };

    const loadEarlierMessages = useCallback(async () => {
        if (loadingEarlier || !hasMoreMessages) {
            return;
        }

        try {
            setLoadingEarlier(true);
            const nextPage = currentPage + 1;
            
            const messagesData = await getMessagesByChatId(chatid, nextPage, messagesPerPage);
            
            // Handle pagination response
            const messagesArray = Array.isArray(messagesData) ? messagesData : (messagesData.content || []);
            const totalPages = messagesData.totalPages || 1;
            const currentPageNum = messagesData.number || nextPage;
            
            // Check if there are more messages to load
            setHasMoreMessages(currentPageNum < totalPages - 1);
            setCurrentPage(currentPageNum);

            if (messagesArray.length > 0) {
                // Transform earlier messages
                const transformedMessages = messagesArray.map(msg => {
                    let avatar;
                    if (msg.senderId === currentUser?.userId) {
                        avatar = currentUser?.profilePictureURL || defaultImage;
                    } else {
                        avatar = otherUser?.profilePictureURL || defaultImage;
                    }
                    
                    return {
                        _id: msg.messageId,
                        text: msg.file ? '' : msg.content,
                        createdAt: new Date(msg.sentAt),
                        user: {
                            _id: msg.senderId,
                            name: msg.senderName,
                            avatar: avatar
                        },
                        edited: msg.edited || false,
                        status: msg.status || 'SENT',
                        ...(msg.file && {
                            ...(isImageFile(msg.content) ? {
                                image: msg.content,
                            } : {
                                file: {
                                    url: msg.content,
                                    name: getFileNameFromUrl(msg.content),
                                    type: getFileTypeFromUrl(msg.content)
                                }
                            })
                        }),
                    };
                });

                setMessages(previousMessages => GiftedChat.prepend(previousMessages, transformedMessages));
            }
        } catch (error) {
            console.error('Error loading earlier messages:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to load earlier messages',
                position: 'top',
                visibilityTime: 3000,
            });
        } finally {
            setLoadingEarlier(false);
        }
    }, [chatid, currentPage, hasMoreMessages, loadingEarlier, messagesPerPage, currentUser, otherUser]);

    const markMessageAsDelivered = async (messageId) => {
        try {
            await updateMessageStatus(messageId, 'DELIVERED');
            
            setMessages(previousMessages => 
                previousMessages.map(msg => 
                    msg._id === messageId 
                        ? { ...msg, status: 'DELIVERED' }
                        : msg
                )
            );
        } catch (error) {
            console.error('Error marking message as delivered:', error);
        }
    };

    const markMessageAsRead = async (messageId) => {
        try {
            await updateMessageStatus(messageId, 'READ');
            
            setMessages(previousMessages =>
                previousMessages.map(msg =>
                    msg._id === messageId
                        ? { ...msg, status: 'READ' }
                        : msg
                )
            );
        } catch (error) {
            console.error('Error marking message as read:', error);
        }
    };

    const handleLongPress = useCallback((context, message) => {
        setSelectedMessage(message);
        setShowMessageActions(true);
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);
    }, [currentUser?.userId]);

    const onSend = useCallback(async (newMessages = []) => {
        const message = newMessages[0];

        try {
            const replyToMessageId = replyToMessage ? replyToMessage._id : null;
            const response = await sendMessage(chatid, message.text, replyToMessageId);
            
            if (response && response.messageId) {
                if (!chatStompService.isClientConnected()) {
                    const manualMessage = {
                        _id: response.messageId,
                        text: response.content,
                        createdAt: new Date(response.sentAt),
                        user: {
                            _id: response.senderId,
                            name: currentUser?.name || 'Unknown',
                            avatar: currentUser?.profilePictureURL || defaultImage
                        },
                        ...(response.replyToMessageId && {
                            replyToMessage: replyToMessage
                        })
                    };
                    setMessages(previousMessages => GiftedChat.append(previousMessages, [manualMessage]));
                }
                
                if (replyToMessage) {
                    setReplyToMessage(null);
                }
            }
        } catch (error) {
            console.error('Error sending message:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to send message',
                position: 'top',
                visibilityTime: 3000,
            });
        }
    }, [chatid, currentUser, replyToMessage]);

    const renderBubble = (props) => {
        const { currentMessage } = props;
        const isCurrentUser = currentMessage.user._id === currentUser?.userId;

        return (
            <View>
                <Bubble
                    {...props}
                    wrapperStyle={{
                        right: {
                            backgroundColor: '#007AFF',
                        },
                        left: {
                            backgroundColor: '#E5E5EA',
                        },
                    }}
                    textStyle={{
                        right: {
                            color: '#FFFFFF',
                        },
                        left: {
                            color: '#000000',
                        },
                    }}
                />
                <View style={[
                    styles.messageFooter,
                    isCurrentUser ? styles.messageFooterRight : styles.messageFooterLeft
                ]}>
                    {currentMessage.edited && (
                        <Text style={[
                            styles.editedText,
                            isCurrentUser ? styles.editedTextRight : styles.editedTextLeft
                        ]}>
                            edited
                        </Text>
                    )}
                    {isCurrentUser && (
                        <View style={styles.statusIndicator}>
                            {currentMessage.status === 'SENT' && (
                                <Ionicons name="checkmark" size={12} color="#007AFF" />
                            )}
                            {currentMessage.status === 'DELIVERED' && (
                                <View style={styles.doubleCheck}>
                                    <Ionicons name="checkmark" size={12} color="#007AFF" />
                                    <Ionicons name="checkmark" size={12} color="#007AFF" style={styles.secondCheck} />
                                </View>
                            )}
                            {currentMessage.status === 'READ' && (
                                <View style={styles.doubleCheck}>
                                    <Ionicons name="checkmark" size={12} color="#4CAF50" />
                                    <Ionicons name="checkmark" size={12} color="#4CAF50" style={styles.secondCheck} />
                                </View>
                            )}
                        </View>
                    )}
                </View>
            </View>
        );
    };

    // Custom InputToolbar with clip icon
    const renderInputToolbar = (props) => {
        if (showEditInput) {
            // Show edit mode input
            return (
                <View style={styles.editInputToolbar}>
                    <View style={styles.editHeader}>
                        <Text style={styles.editTitle}>Edit Message</Text>
                        <TouchableOpacity onPress={cancelEdit}>
                            <Ionicons name="close" size={20} color="#666" />
                        </TouchableOpacity>
                    </View>
                    <View style={styles.editInputWrapper}>
                        <TextInput
                            style={styles.editInput}
                            value={editText}
                            onChangeText={setEditText}
                            multiline={true}
                            placeholder="Edit your message..."
                            autoFocus={true}
                        />
                        <TouchableOpacity style={styles.saveButton} onPress={saveEditedMessage}>
                            <Ionicons name="checkmark" size={24} color="#007AFF" />
                        </TouchableOpacity>
                    </View>
                </View>
            );
        }

        // Normal input mode
        return (
            <InputToolbar
                {...props}
                containerStyle={styles.inputToolbar}
                primaryStyle={styles.primaryInputToolbar}
                renderActions={() => (
                    <View style={{ flexDirection: 'row'}}>
                        <TouchableOpacity onPress={handlePickFile} style={styles.clipButton}>
                            <Ionicons name="attach" size={28} color="#918CE5" />
                        </TouchableOpacity>
                        <TouchableOpacity onPress={handleImagePick} style={styles.clipButton}>
                            <Ionicons name="image-outline" size={28} color="#918CE5" />
                        </TouchableOpacity>
                    </View>
                )}
            />
        );
    };

    const renderReplyPreview = () => {
        if (!replyToMessage) return null;

        return (
            <View style={styles.replyPreview}>
                <View style={styles.replyContent}>
                    <Text style={styles.replyLabel}>Replying to {replyToMessage.user.name}</Text>
                    <Text style={styles.replyText} numberOfLines={1}>
                        {replyToMessage.text}
                    </Text>
                </View>
                <TouchableOpacity onPress={cancelReply} style={styles.replyCancel}>
                    <Ionicons name="close" size={20} color="#666" />
                </TouchableOpacity>
            </View>
        );
    };

    const renderSend = (props) => {
        return (
            <Send {...props}>
                <View style={styles.sendContainer}>
                    <Ionicons name="send" size={24} color="#007AFF" />
                </View>
            </Send>
        );
    };

    const renderHeader = () => (
        <TouchableOpacity style={styles.header} onPress={() => {}}>
            <TouchableOpacity
                style={styles.backButton}
                onPress={() => router.back()}
            >
                <Ionicons name="arrow-back" size={24} color="#918CE5" />
            </TouchableOpacity>
            <Image
                source={{ uri: otherUser?.profilePictureURL || defaultImage}}
                style={styles.userAvatar}
            />

            <View style={styles.headerInfo}>
                <Text style={styles.headerTitle}>
                    {otherUser?.name || 'Loading...'}
                </Text>
                <Text style={styles.headerSubtitle}>
                    {otherUser?.online ? 'Online' : 'Offline'}
                </Text>
            </View>
        </TouchableOpacity>
    );

    const handleReplyMessage = () => {
        setReplyToMessage(selectedMessage);
        setShowMessageActions(false);
        setSelectedMessage(null);
    };

    const handleDeleteMessage = async () => {
        if (!selectedMessage) return;

        Alert.alert(
            'Delete Message',
            'Are you sure you want to delete this message?',
            [
                {
                    text: 'Cancel',
                    style: 'cancel',
                },
                {
                    text: 'Delete',
                    style: 'destructive',
                    onPress: async () => {
                        try {
                            await deleteMessage(selectedMessage._id);

                            // Remove message from local state
                            setMessages(previousMessages => 
                                previousMessages.filter(msg => msg._id !== selectedMessage._id)
                            );

                            Toast.show({
                                type: 'success',
                                text1: 'Message deleted',
                                text2: 'The message has been deleted',
                                position: 'top',
                                visibilityTime: 2000,
                            });
                        } catch (error) {
                            console.error('Error deleting message:', error);
                            Toast.show({
                                type: 'error',
                                text1: 'Error',
                                text2: 'Failed to delete message',
                                position: 'top',
                                visibilityTime: 3000,
                            });
                        } finally {
                            setShowMessageActions(false);
                            setSelectedMessage(null);
                        }
                    },
                },
            ]
        );
    };

    const handleEditMessage = () => {
        if (!selectedMessage || !selectedMessage.text) return;
        setEditText(selectedMessage.text);
        setShowEditInput(true);
        setShowMessageActions(false);
    };

    const saveEditedMessage = async () => {
        if (!editText.trim() || !selectedMessage) {
            return;
        }

        try {
            await editMessage(selectedMessage._id, editText.trim());

            setMessages(previousMessages =>
                previousMessages.map(msg =>
                    msg._id === selectedMessage._id
                        ? { ...msg, text: editText.trim(), edited: true }
                        : msg
                )
            );
        } catch (error) {
            console.error('Error editing message:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to edit message',
                position: 'top',
                visibilityTime: 3000,
            });
        } finally {
            setShowEditInput(false);
            setEditText('');
            setSelectedMessage(null);
        }
    };

    const cancelEdit = () => {
        setShowEditInput(false);
        setEditText('');
        setSelectedMessage(null);
    };

    const cancelReply = () => {
        setReplyToMessage(null);
    };

    const renderActionBar = () => (
        <View style={styles.actionsHeader}>
            <View>
                <TouchableOpacity
                    style={styles.backButton}
                    onPress={() => setShowMessageActions(false)}
                >
                    <Ionicons name="arrow-back" size={24} color="#918CE5" />
                </TouchableOpacity>
            </View>
            <View style = {styles.actionsFunctionalities}>
                {((selectedMessage.user._id === currentUser.userId && !selectedMessage.file && !selectedMessage.image ) ? <TouchableOpacity onPress={handleEditMessage} style={styles.actionButton}>
                    <Ionicons name="create-outline" size={28} color="#918CE5" />
                </TouchableOpacity> : null)}
                {(selectedMessage.user._id === currentUser.userId ? <TouchableOpacity onPress={handleDeleteMessage} style={styles.actionButton}>
                    <Ionicons name="trash-outline" size={28} color="#918CE5" />
                </TouchableOpacity> : null)}
                <TouchableOpacity onPress={handleReplyMessage} style={styles.actionButton}>
                    <Ionicons name="arrow-undo-outline" size={28} color="#918CE5" />
                </TouchableOpacity>
            </View>
        </View>
    );

    // Handler for picking a file
    const handlePickFile = async () => {
        try {
            const result = await DocumentPicker.getDocumentAsync({
                type: '*/*',
                copyToCacheDirectory: true,
                multiple: false,
            });
            if (!result.canceled && result.assets && result.assets.length > 0) {
                const file = result.assets[0];
                await uploadAndSendFile({
                    uri: file.uri,
                    name: file.name,
                    type: file.mimeType || 'application/octet-stream',
                });
            }
        } catch (error) {
            console.error('Error picking file:', error);
            Toast.show({
                type: 'error',
                text1: 'File selection error',
                text2: error.message,
                position: 'top',
                visibilityTime: 2000,
            });
        }
    };

    const handleImagePick = async () => {
        try {
            let result = await ImagePicker.launchImageLibraryAsync({
                mediaTypes: ['images'],
                allowsEditing: true,
                aspect: [4, 3],
                quality: 0.7,
            });

            if (!result.canceled && result.assets && result.assets.length > 0) {
                const image = result.assets[0];
                await uploadAndSendFile({
                    uri: image.uri,
                    name: `image_${Date.now()}.jpg`,
                    type: 'image/jpeg',
                });
            }
        } catch (error) {
            console.error('Error picking image:', error);
            Toast.show({
                type: 'error',
                text1: 'Image selection error',
                text2: error.message,
                position: 'top',
                visibilityTime: 2000,
            });
        }
    }

    // Function to upload and send files/images  
    const uploadAndSendFile = async (fileInfo) => {
        try {
            setUploadingImages(true);
            
            Toast.show({
                type: 'info',
                text1: 'Uploading...',
                text2: `Sending ${fileInfo.name}`,
                position: 'top',
                visibilityTime: 2000,
            });

            // First, upload the file to get the URL
            const fileForUpload = {
                uri: fileInfo.uri,
                type: fileInfo.type,
                name: fileInfo.name,
            };

            const uploadResponse = await uploadFile(fileForUpload);

            const fileUrl = uploadResponse;

            const replyToMessageId = replyToMessage ? replyToMessage._id : null;
            const result = await sendMessage(chatid, fileUrl, replyToMessageId, true);

            if (replyToMessage) {
                setReplyToMessage(null);
            }

            if (!chatStompService.isClientConnected()) {
                const manualMessage = {
                    _id: result.messageId,
                    text: result.file ? '' : result.content,
                    createdAt: new Date(result.sentAt),
                    user: {
                        _id: result.senderId,
                        name: currentUser?.name || 'Unknown',
                        avatar: currentUser?.profilePictureURL || defaultImage
                    },
                    ...(result.file && {
                        ...(isImageFile(fileUrl) ? {
                            image: fileUrl,
                        } : {
                            file: {
                                url: fileUrl,
                                name: fileInfo.name,
                                type: fileInfo.type
                            }
                        })
                    }),
                    ...(result.replyToMessageId && {
                        replyToMessage: replyToMessage
                    })
                };
                setMessages(previousMessages => GiftedChat.append(previousMessages, [manualMessage]));
            }

            Toast.show({
                type: 'success',
                text1: 'Success',
                text2: 'File sent successfully',
                position: 'top',
                visibilityTime: 2000,
            });

        } catch (error) {
            console.error('Error uploading and sending file:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to send file',
                position: 'top',
                visibilityTime: 3000,
            });
        } finally {
            setUploadingImages(false);
        }
    };

    const renderCustomView = (props) => {
        const { currentMessage } = props;
        
        if (!currentMessage.file) return null;

        return (
            <TouchableOpacity
                style={styles.fileContainer}
                onPress={() => handleFilePress(currentMessage.file.url)}
                onLongPress={() => handleLongPress(null, currentMessage)}
                activeOpacity={0.7}
            >
                <View style={styles.fileContent}>
                    <Ionicons name="document-outline" size={32} color="#007AFF" />
                    <View style={styles.fileInfo}>
                        <Text style={styles.fileName} numberOfLines={1}>
                            {currentMessage.file.name}
                        </Text>
                        <Text style={styles.fileType}>
                            {currentMessage.file.type} • Tap to download
                        </Text>
                    </View>
                </View>
            </TouchableOpacity>
        );
    };

    // Handle file press to download/open
    const handleFilePress = async (fileUrl) => {
        try {
            const { canOpenURL, openURL } = await import('expo-linking');

            if (await canOpenURL(fileUrl)) {
                await openURL(fileUrl);
            } else {
                Toast.show({
                    type: 'error',
                    text1: 'Cannot open file',
                    text2: 'Unable to open this file type',
                    position: 'top',
                    visibilityTime: 3000,
                });
            }
        } catch (error) {
            console.error('Error opening file:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to open file',
                position: 'top',
                visibilityTime: 3000,
            });
        }
    };

    const renderMessageImage = (props) => {
        const { currentMessage } = props;
        if (!currentMessage.image) return null;

        return (
            <TouchableOpacity
                style={styles.imageContainer}
                onPress={() => {
                    handleImagePress(currentMessage.image);
                }}
                activeOpacity={0.8}
                onLongPress={() => handleLongPress(null, currentMessage)}
            >
                <Image
                    source={{ uri: currentMessage.image }}
                    style={styles.messageImage}
                    contentFit="cover"
                    onError={(error) => {
                        console.error('Image render error:', error);
                    }}
                    onLoad={() => {
                        console.log('Image loaded successfully:', currentMessage.image);
                    }}
                />
            </TouchableOpacity>
        );
    };

    const handleImagePress = (imageuri) => {
        if (!imageuri) {
            console.error('No image URI provided');
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Invalid image',
                position: 'top',
                visibilityTime: 2000,
            });
            return;
        }
        router.push({
            pathname: '/Chat/Image/[imageuri]',
            params: { imageuri: encodeURIComponent(imageuri) }
        });
    };

    if (loading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#007AFF" />
                <Text style={styles.loadingText}>Loading chat...</Text>
            </View>
        );
    }

    return (
        <View style={styles.container}>
            {(!showMessageActions ? renderHeader() : renderActionBar())}
            {renderReplyPreview()}
                <View
                    style={styles.chatWrapper}
                >
                    <GiftedChat
                        messages={messages}
                        onSend={onSend}
                        onLongPress={handleLongPress}
                        user={{
                            _id: currentUser?.userId,
                            name: currentUser?.name,
                            avatar: currentUser?.profilePictureURL || defaultImage
                        }}
                        renderBubble={renderBubble}
                        renderInputToolbar={renderInputToolbar}
                        renderSend={renderSend}
                        renderMessageImage={renderMessageImage}
                        renderCustomView={renderCustomView}
                        showAvatarForEveryMessage={false}
                        showUserAvatar={!showMessageActions}
                        scrollToBottom={true}
                        scrollToBottomStyle={{ backgroundColor: '#E5E5EA' }}
                        scrollToBottomComponent={() => (
                            <Ionicons name="chevron-down" size={24} color="#007AFF" />
                        )}
                        loadEarlier={hasMoreMessages}
                        isLoadingEarlier={loadingEarlier}
                        onLoadEarlier={loadEarlierMessages}
                        loadEarlierLabel={loadingEarlier ? 'Loading...' : 'Load Earlier Messages'}
                        minInputToolbarHeight={44}
                        maxInputLength={500}
                    />
                </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    chatWrapper: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
    },
    loadingText: {
        marginTop: 10,
        color: '#666666',
        fontSize: 16,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderBottomWidth: 1,
        borderBottomColor: '#E5E5EA',
        backgroundColor: '#FFFFFF',
        paddingTop: 30,
    },
    actionsHeader:{
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderBottomColor: '#E5E5EA',
        borderBottomWidth: 1,
        paddingHorizontal: 16,
        paddingVertical: 12,
        paddingTop: 45,
    },
    actionsFunctionalities: {
        flexDirection: 'row',
    },
    actionButton: {
        paddingLeft: 12,
    },
    backButton: {
        marginRight: 8,
    },
    userAvatar: {
        width: 40,
        height: 40,
        borderRadius: 20,
        marginRight: 12,
        borderColor: '#918CE5',
        borderWidth: 1,
    },
    headerInfo: {
        flex: 1,
    },
    headerTitle: {
        fontSize: 18,
        fontWeight: 'bold',
        color: '#000000',
    },
    headerSubtitle: {
        fontSize: 14,
        color: '#666666',
        marginTop: 2,
    },
    headerButton: {
        padding: 8,
        marginLeft: 8,
    },
    sendContainer: {
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 8,
        marginBottom: 8,
        width: 32,
        height: 32,
    },
    editHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 12,
    },
    editTitle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#333',
    },
    editInputWrapper: {
        flexDirection: 'row',
        alignItems: 'flex-end',
    },
    editInput: {
        flex: 1,
        borderWidth: 1,
        borderColor: '#E5E5EA',
        borderRadius: 20,
        paddingHorizontal: 12,
        paddingVertical: 8,
        fontSize: 16,
        maxHeight: 100,
        backgroundColor: '#FFFFFF',
    },
    saveButton: {
        marginLeft: 8,
        padding: 8,
    },
    replyPreview: {
        backgroundColor: '#F0F0F0',
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderBottomWidth: 1,
        borderBottomColor: '#E5E5EA',
    },
    replyContent: {
        flex: 1,
    },
    replyLabel: {
        fontSize: 12,
        color: '#007AFF',
        fontWeight: 'bold',
    },
    replyText: {
        fontSize: 14,
        color: '#666',
        marginTop: 2,
    },
    replyCancel: {
        padding: 4,
    },
    inputToolbar: {
        paddingHorizontal: 8,
    },
    primaryInputToolbar: {
        alignItems: 'center',
        justifyContent: 'center',
    },
    clipButton: {
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 4,
    },
    editInputToolbar: {
        backgroundColor: '#F8F8F8',
        borderTopWidth: 1,
        borderTopColor: '#E5E5EA',
        padding: 12,
        paddingBottom: Platform.OS === 'ios' ? 34 : 16,
    },
    imageContainer: {
        borderRadius: 12,
        overflow: 'hidden',
        marginVertical: 4,
        marginHorizontal: 4,
    },
    messageImage: {
        width: 200,
        height: 200,
        borderRadius: 12,
    },
    fileContainer: {
        backgroundColor: '#FFFFFF',
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#E5E5EA',
        padding: 12,
        marginVertical: 4,
        marginHorizontal: 4,
        flexDirection: 'row',
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: {
            width: 0,
            height: 2,
        },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 2,
    },
    fileContent: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    fileInfo: {
        marginLeft: 8,
        flex: 1,
    },
    fileName: {
        fontSize: 16,
        color: '#333',
        fontWeight: '500',
    },
    fileType: {
        fontSize: 14,
        color: '#666',
        marginTop: 2,
    },
    messageFooter: {
        flexDirection: 'row',
        alignItems: 'center',
        marginTop: 2,
        marginBottom: 4,
    },
    messageFooterRight: {
        justifyContent: 'flex-end',
        marginRight: 8,
    },
    messageFooterLeft: {
        justifyContent: 'flex-start',
        marginLeft: 8,
    },
    statusIndicator: {
        marginLeft: 4,
    },
    doubleCheck: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    secondCheck: {
        marginLeft: -8,
    },
    editedText: {
        fontSize: 11,
        fontStyle: 'italic',
    },
    editedTextRight: {
        color: '#007AFF',
    },
    editedTextLeft: {
        color: '#666',
    },
});
