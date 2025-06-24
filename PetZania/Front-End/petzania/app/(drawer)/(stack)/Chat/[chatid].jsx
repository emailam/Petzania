import React, { useState, useEffect, useContext, useCallback } from 'react';
import { View, ActivityIndicator, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Image } from 'expo-image';

import { GiftedChat, Bubble, InputToolbar, Send } from 'react-native-gifted-chat';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { UserContext } from '@/context/UserContext';
import { getMessagesByChatId, sendMessage, getChatById } from '@/services/chatService';
import stompService from '@/services/stompService';
import Toast from 'react-native-toast-message';
import { getToken } from '@/storage/tokenStorage';

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

    const defaultImage = defaultImage;

    useEffect(() => {
        const connectToStomp = async () => {
            if (currentUser?.userId && chatid) {
                try {
                    const token = await getToken('accessToken');
                    const client = stompService.connect(currentUser.userId, token);
                } catch (error) {
                    console.error('Error connecting to STOMP:', error);
                }
            }
        };
        connectToStomp();
        return () => {
            if (stompService.isClientConnected()) {
                stompService.unsubscribeFromChatTopic(chatid);
            }
        };
    }, [currentUser?.userId, chatid]);

    const handleTopicMessage = useCallback((data) => {

        if (data.eventType === 'SEND' && data.messageDTO) {
            const messageData = data.messageDTO;
            console.log("Received message data:", messageData);

            // Determine avatar
            let avatar;
            if (messageData.senderId === currentUser?.userId) {
                avatar = currentUser?.profilePictureURL || defaultImage;
            } else {
                avatar = otherUser?.profilePictureURL || defaultImage;
            }

            const newMessage = {
                _id: messageData.messageId,
                text: messageData.content,
                createdAt: new Date(messageData.sentAt),
                user: {
                    _id: messageData.senderId,
                    name: messageData.senderName || 'Unknown',
                    avatar: avatar
                }
            };

            console.log("Adding message with avatar:", avatar);
            setMessages(previousMessages => GiftedChat.append(previousMessages, [newMessage]));

            if (messageData.senderId !== currentUser.userId) {
                markMessageAsDelivered(messageData.messageId);
            }
        } else if (data.eventType === 'EDIT' && data.messageDTO) {
            const updatedMessage = data.messageDTO;
            setMessages(previousMessages => 
                previousMessages.map(msg => 
                    msg._id === updatedMessage.messageId 
                        ? { ...msg, text: updatedMessage.content }
                        : msg
                )
            );
        } else if (data.eventType === 'DELETE' && data.messageDTO) {
            const deletedMessage = data.messageDTO;
            setMessages(previousMessages => 
                previousMessages.filter(msg => msg._id !== deletedMessage.messageId)
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
                    text: msg.content,
                    createdAt: new Date(msg.sentAt),
                    user: {
                        _id: msg.senderId,
                        name: msg.senderName,
                        avatar: avatar
                    },
                    ...(msg.file && {
                        image: msg.fileUrl,
                    }),
                };
            });

            setMessages(transformedMessages);
            
            // Now that we have otherUser data, set up STOMP subscription
            setupStompSubscription();
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
            if (stompService.isClientConnected()) {
                stompService.subscribeToChatTopic(chatid, handleTopicMessage);
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
                        text: msg.content,
                        createdAt: new Date(msg.sentAt),
                        user: {
                            _id: msg.senderId,
                            name: msg.senderName,
                            avatar: avatar
                        },
                        ...(msg.file && {
                            image: msg.fileUrl,
                        }),
                    };
                });

                // Prepend earlier messages to existing messages
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
            console.log(`Message ${messageId} marked as delivered`);
        } catch (error) {
            console.error('Error marking message as delivered:', error);
        }
    };

    const onSend = useCallback(async (newMessages = []) => {
        const message = newMessages[0];

        try {
          const response = await sendMessage(chatid, message.text);
          console.log('Message sent:', response);
            if (response && response.messageId) {
                // Message will be added via STOMP subscription if connected
                // If not connected, we'll add it manually as fallback
                if (!stompService.isClientConnected()) {
                    const manualMessage = {
                        _id: response.messageId,
                        text: response.content,
                        createdAt: new Date(response.sentAt),
                        user: {
                            _id: response.senderId,
                            name: currentUser?.name || 'Unknown',
                            avatar: currentUser?.profilePictureURL || defaultImage
                        }
                    };
                    setMessages(previousMessages => GiftedChat.append(previousMessages, [manualMessage]));
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
    }, [chatid, currentUser]);

    const renderBubble = (props) => {
        return (
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
        );
    };

    const renderInputToolbar = (props) => {
        return (
            <InputToolbar
                {...props}
                containerStyle={styles.inputToolbar}
                primaryStyle={styles.primaryInputToolbar}
            />
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
        // This will go to a page where you can view the other user's profile and to delete a chat
        <TouchableOpacity style={styles.header} onPress={() => {}}>
            <TouchableOpacity
                style={styles.backButton}
                onPress={() => router.back()}
            >
                <Ionicons name="arrow-back" size={24} color="#000" />
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
            {renderHeader()}
            <GiftedChat
                messages={messages}
                onSend={onSend}
                onLongPress={(message) => {
                    console.log("Long pressed message:", message);
                }}
                
                user={{
                    _id: currentUser?.userId,
                    name: currentUser?.name,
                    avatar: currentUser?.profilePictureURL || defaultImage
                }}
                renderBubble={renderBubble}
                renderInputToolbar={renderInputToolbar}
                renderSend={renderSend}
                showAvatarForEveryMessage={false}
                showUserAvatar={true}
                scrollToBottom={true}
                scrollToBottomStyle={{ backgroundColor: '#E5E5EA' }}
                scrollToBottomComponent={() => (
                    <Ionicons name="chevron-down" size={24} color="#007AFF" />                )}
                loadEarlier={hasMoreMessages}
                isLoadingEarlier={loadingEarlier}
                onLoadEarlier={loadEarlierMessages}
                loadEarlierLabel={loadingEarlier ? 'Loading...' : 'Load Earlier Messages'}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
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
    backButton: {
        padding: 8,
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
    inputToolbar: {
        borderTopWidth: 1,
        borderTopColor: '#E5E5EA',
        backgroundColor: '#FFFFFF',
    },
    primaryInputToolbar: {
        alignItems: 'center',
    },
    sendContainer: {
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 8,
        marginBottom: 8,
        width: 32,
        height: 32,
    },
});
