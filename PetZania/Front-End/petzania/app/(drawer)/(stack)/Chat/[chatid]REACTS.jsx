import React, { useState, useEffect, useContext, useCallback, useRef } from 'react';
import { 
  View, 
  ActivityIndicator, 
  Text, 
  StyleSheet, 
  TouchableOpacity, 
  TextInput, 
  Alert, 
  Platform, 
  KeyboardAvoidingView, 
  FlatList, 
  Modal, 
  Animated,
  Dimensions 
} from 'react-native';
import { Image } from 'expo-image';
import * as DocumentPicker from 'expo-document-picker';
import * as ImagePicker from 'expo-image-picker';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { UserContext } from '@/context/UserContext';
import {
  getMessagesByChatId,
  sendMessage,
  getChatById,
  updateMessageStatus,
  editMessage,
  deleteMessage,
  reactToMessage
} from '@/services/chatService';
import { uploadFile } from '@/services/uploadService';
import chatStompService from '@/services/chatStompService';
import Toast from 'react-native-toast-message';
import { getToken } from '@/storage/tokenStorage';
import * as Haptics from 'expo-haptics';

const { width: screenWidth } = Dimensions.get('window');

export default function ChatDetailScreen() {
  const { chatid } = useLocalSearchParams();
  const router = useRouter();
  const { user: currentUser } = useContext(UserContext);

  // State management
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [otherUser, setOtherUser] = useState(null);
  const [loadingEarlier, setLoadingEarlier] = useState(false);
  const [hasMoreMessages, setHasMoreMessages] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [messagesPerPage] = useState(50);
  
  // UI state
  const [showMessageActions, setShowMessageActions] = useState(false);
  const [selectedMessage, setSelectedMessage] = useState(null);
  const [showEditInput, setShowEditInput] = useState(false);
  const [editText, setEditText] = useState('');
  const [replyToMessage, setReplyToMessage] = useState(null);
  const [inputText, setInputText] = useState('');
  const [uploadingFiles, setUploadingFiles] = useState(false);
  
  // Reaction modal state
  const [showReactionModal, setShowReactionModal] = useState(false);
  const [reactionModalPosition, setReactionModalPosition] = useState({ x: 0, y: 0 });
  
  // Refs and animations
  const flatListRef = useRef(null);
  const inputRef = useRef(null);
  const scaleAnim = useRef(new Animated.Value(0)).current;
  const slideAnim = useRef(new Animated.Value(0)).current;
  
  // Constants
  const defaultImage = require('@/assets/images/Defaults/default-user.png');
  const reactions = {'❤️': 'LOVE', '😂': 'HAHA', '😢': 'SAD', '😡': 'ANGRY', '👍': 'LIKE'};

  // Utility functions
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

  // STOMP connection setup
  useEffect(() => {
    const connectToStomp = async () => {
      if (currentUser?.userId && chatid) {
        try {
          const token = await getToken('accessToken');
          chatStompService.connect(currentUser.userId, token);
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

  // Handle incoming STOMP messages
  const handleTopicMessage = useCallback((data) => {
    const { eventType, messageDTO } = data;
    switch (eventType) {
      case 'SEND':
        handleNewMessage(messageDTO);
        break;
      case 'EDIT':
        handleMessageEdit(messageDTO);
        break;
      case 'DELETE':
        handleMessageDelete(messageDTO);
        break;
      case 'UPDATE_STATUS':
        handleStatusUpdate(messageDTO);
        break;
      case 'REACTION':
        handleReactionUpdate(messageDTO);
        break;
      default:
        console.log('Unknown event type:', eventType);
    }
  }, [currentUser?.userId, otherUser]);

  const handleNewMessage = (messageData) => {
    const avatar = messageData.senderId === currentUser?.userId 
      ? currentUser?.profilePictureURL || defaultImage
      : otherUser?.profilePictureURL || defaultImage;

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
      reactions: messageData.reactions || {},
      replyToMessage: messageData.replyToMessage || null,
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

    setMessages(prev => [newMessage, ...prev]);

    // Auto-mark messages as read if not from current user
    if (messageData.senderId !== currentUser.userId) {
      setTimeout(() => markMessageAsDelivered(messageData.messageId), 100);
      setTimeout(() => markMessageAsRead(messageData.messageId), 300);
    }
  };

  const handleMessageEdit = (updatedMessage) => {
    setMessages(prev => 
      prev.map(msg => 
        msg._id === updatedMessage.messageId 
          ? { ...msg, text: updatedMessage.content, edited: true }
          : msg
      )
    );
  };

  const handleMessageDelete = (deletedMessage) => {
    setMessages(prev => prev.filter(msg => msg._id !== deletedMessage.messageId));
  };

  const handleStatusUpdate = (updatedMessage) => {
    setMessages(prev => 
      prev.map(msg => 
        msg._id === updatedMessage.messageId 
          ? { ...msg, status: updatedMessage.status }
          : msg
      )
    );
  };

  const handleReactionUpdate = (reactionData) => {
    setMessages(prev => 
      prev.map(msg => 
        msg._id === reactionData.messageId 
          ? { ...msg, reactions: reactionData.reactions }
          : msg
      )
    );
  };

  // Load chat data
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
      
      setOtherUser(otherUserData);

      const messagesArray = Array.isArray(messagesData) ? messagesData : (messagesData.content || []);
      console.log('Loaded messages:', messagesArray);
      const totalPages = messagesData.totalPages || 1;
      const currentPageNum = messagesData.number || 0;

      setHasMoreMessages(currentPageNum < totalPages - 1);
      setCurrentPage(currentPageNum);

      const transformedMessages = messagesArray.map(msg => ({
        _id: msg.messageId,
        text: msg.file ? '' : msg.content,
        createdAt: new Date(msg.sentAt),
        user: {
          _id: msg.senderId,
          name: msg.senderName,
          avatar: msg.senderId === currentUser?.userId 
            ? currentUser?.profilePictureURL || defaultImage
            : otherUserData?.profilePictureURL || defaultImage
        },
        edited: msg.edited || false,
        status: msg.status || 'SENT',
        reactions: msg.reactions || {},
        replyToMessage: msg.replyToMessage || null,
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
      }));

      setMessages(transformedMessages);
      setupStompSubscription();
      
      // Mark unread messages as read
      const unreadMessages = transformedMessages.filter(msg => 
        msg.user._id !== currentUser?.userId && 
        (msg.status === 'SENT' || msg.status === 'DELIVERED')
      );

      unreadMessages.slice(0, 10).forEach((message, index) => {
        setTimeout(async () => {
          try {
            if (message.status === 'SENT') {
              await markMessageAsDelivered(message._id);
              setTimeout(() => markMessageAsRead(message._id), 200);
            } else if (message.status === 'DELIVERED') {
              await markMessageAsRead(message._id);
            }
          } catch (error) {
            console.error(`Failed to update message ${message._id} status:`, error);
          }
        }, index * 100);
      });

    } catch (error) {
      console.error('Error loading chat data:', error);
      showToast('error', 'Error', 'Failed to load chat');
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

  // Message status updates
  const markMessageAsDelivered = async (messageId) => {
    try {
      await updateMessageStatus(messageId, 'DELIVERED');
      setMessages(prev => 
        prev.map(msg => 
          msg._id === messageId ? { ...msg, status: 'DELIVERED' } : msg
        )
      );
    } catch (error) {
      console.error('Error marking message as delivered:', error);
    }
  };

  const markMessageAsRead = async (messageId) => {
    try {
      await updateMessageStatus(messageId, 'READ');
      setMessages(prev =>
        prev.map(msg =>
          msg._id === messageId ? { ...msg, status: 'read' } : msg
        )
      );
    } catch (error) {
      console.error('Error marking message as read:', error);
    }
  };

  // Load earlier messages
  const loadEarlierMessages = useCallback(async () => {
    if (loadingEarlier || !hasMoreMessages) return;

    try {
      setLoadingEarlier(true);
      const nextPage = currentPage + 1;
      const messagesData = await getMessagesByChatId(chatid, nextPage, messagesPerPage);
      
      const messagesArray = Array.isArray(messagesData) ? messagesData : (messagesData.content || []);
      const totalPages = messagesData.totalPages || 1;
      const currentPageNum = messagesData.number || nextPage;
      
      setHasMoreMessages(currentPageNum < totalPages - 1);
      setCurrentPage(currentPageNum);

      if (messagesArray.length > 0) {
        const transformedMessages = messagesArray.map(msg => ({
          _id: msg.messageId,
          text: msg.file ? '' : msg.content,
          createdAt: new Date(msg.sentAt),
          user: {
            _id: msg.senderId,
            name: msg.senderName,
            avatar: msg.senderId === currentUser?.userId 
              ? currentUser?.profilePictureURL || defaultImage
              : otherUser?.profilePictureURL || defaultImage
          },
          edited: msg.edited || false,
          status: msg.status || 'SENT',
          reactions: msg.reactions || {},
          replyToMessage: msg.replyToMessage || null,
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
        }));

        setMessages(prev => [...transformedMessages, ...prev]);
      }
    } catch (error) {
      console.error('Error loading earlier messages:', error);
      showToast('error', 'Error', 'Failed to load earlier messages');
    } finally {
      setLoadingEarlier(false);
    }
  }, [chatid, currentPage, hasMoreMessages, loadingEarlier, messagesPerPage, currentUser, otherUser]);

  // Message interactions
  const handleLongPress = useCallback((message, event) => {
    const { pageX, pageY } = event.nativeEvent;
    setSelectedMessage(message);
    setReactionModalPosition({ 
      x: Math.max(10, Math.min(pageX - 150, screenWidth - 320)), 
      y: Math.max(100, pageY - 60) 
    });
    setShowReactionModal(true);
    
    Animated.spring(scaleAnim, {
      toValue: 1,
      useNativeDriver: true,
    }).start();
    
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);
  }, []);

  const handleReaction = useCallback(async (reaction) => {
    if (!selectedMessage) return;

    // Get the string value for the reaction
    const reactionString = reactions[reaction];
    if (!reactionString) return;

    try {
      setMessages(prev => 
        prev.map(msg => {
          if (msg._id === selectedMessage._id) {
            const msgReactions = { ...msg.reactions };
            const currentUserId = currentUser.userId.toString();
            
            if (msgReactions[reaction]) {
              if (msgReactions[reaction].includes(currentUserId)) {
                msgReactions[reaction] = msgReactions[reaction].filter(id => id !== currentUserId);
                if (msgReactions[reaction].length === 0) {
                  delete msgReactions[reaction];
                }
              } else {
                msgReactions[reaction].push(currentUserId);
              }
            } else {
              msgReactions[reaction] = [currentUserId];
            }
            
            return { ...msg, reactions: msgReactions };
          }
          return msg;
        })
      );

      await reactToMessage(selectedMessage._id, reactionString);
    } catch (error) {
      console.error('Error adding reaction:', error);
      showToast('error', 'Error', 'Failed to add reaction');
      
      // Revert the optimistic update on error
      setMessages(prev => 
        prev.map(msg => {
          if (msg._id === selectedMessage._id) {
            return selectedMessage; // Revert to original message
          }
          return msg;
        })
      );
    } finally {
      closeReactionModal();
    }
  }, [selectedMessage, currentUser]);

  const closeReactionModal = () => {
    Animated.spring(scaleAnim, {
      toValue: 0,
      useNativeDriver: true,
    }).start(() => {
      setShowReactionModal(false);
      setSelectedMessage(null);
    });
  };

  // Message sending
  const onSend = useCallback(async () => {
    if (!inputText.trim()) return;

    const messageText = inputText.trim();
    setInputText('');

    try {
      const replyToMessageId = replyToMessage ? replyToMessage._id : null;
      const response = await sendMessage(chatid, messageText, replyToMessageId);
      
      if (response && response.messageId) {
        console.log('Message sent:', response);
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
            reactions: {},
            replyToMessage: replyToMessage,
            status: 'SENT'
          };
          setMessages(prev => [manualMessage, ...prev]);
        }
        
        if (replyToMessage) {
          setReplyToMessage(null);
        }
      }
    } catch (error) {
      console.error('Error sending message:', error);
      showToast('error', 'Error', 'Failed to send message');
    }
  }, [chatid, currentUser, replyToMessage, inputText]);

  // Message actions
  const handleReplyMessage = () => {
    setReplyToMessage(selectedMessage);
    setShowMessageActions(false);
    setSelectedMessage(null);
    closeReactionModal();
  };

  const handleEditMessage = () => {
    if (!selectedMessage || !selectedMessage.text) return;
    setEditText(selectedMessage.text);
    setShowEditInput(true);
    setShowMessageActions(false);
    closeReactionModal();
  };

  const handleDeleteMessage = async () => {
    if (!selectedMessage) return;

    Alert.alert(
      'Delete Message',
      'Are you sure you want to delete this message?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              await deleteMessage(selectedMessage._id);
              setMessages(prev => prev.filter(msg => msg._id !== selectedMessage._id));
              closeReactionModal();
            } catch (error) {
              console.error('Error deleting message:', error);
              showToast('error', 'Error', 'Failed to delete message');
            }
          },
        },
      ]
    );
  };

  const saveEditedMessage = async () => {
    if (!editText.trim() || !selectedMessage) return;

    try {
      await editMessage(selectedMessage._id, editText.trim());
      setMessages(prev => 
        prev.map(msg => 
          msg._id === selectedMessage._id 
            ? { ...msg, text: editText.trim(), edited: true }
            : msg
        )
      );
      setShowEditInput(false);
      setEditText('');
      setSelectedMessage(null);
    } catch (error) {
      console.error('Error editing message:', error);
      showToast('error', 'Error', 'Failed to edit message');
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

  // File handling
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
      showToast('error', 'File selection error', error.message);
    }
  };

  const handleImagePick = async () => {
    try {
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        allowsEditing: true,
        aspect: [1, 1],
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
      showToast('error', 'Image selection error', error.message);
    }
  };

  const uploadAndSendFile = async (fileInfo) => {
    try {
      setUploadingFiles(true);
      showToast('info', 'Uploading...', `Sending ${fileInfo.name}`);

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
        setMessages(prev => [manualMessage, ...prev]);
      }

      showToast('success', 'Success', 'File sent successfully');
    } catch (error) {
      console.error('Error uploading and sending file:', error);
      showToast('error', 'Error', 'Failed to send file');
    } finally {
      setUploadingFiles(false);
    }
  };

  // Utility functions
  const showToast = (type, text1, text2) => {
    Toast.show({
      type,
      text1,
      text2,
      position: 'top',
      visibilityTime: type === 'error' ? 3000 : 2000,
    });
  };

  const handleImagePress = (imageUri) => {
    if (!imageUri) {
      showToast('error', 'Error', 'Invalid image');
      return;
    }
    router.push({
      pathname: '/Chat/Image/[imageuri]',
      params: { imageuri: encodeURIComponent(imageUri) }
    });
  };

  const handleFilePress = async (fileUrl) => {
    try {
      const { canOpenURL, openURL } = await import('expo-linking');
      if (await canOpenURL(fileUrl)) {
        await openURL(fileUrl);
      } else {
        showToast('error', 'Cannot open file', 'Unable to open this file type');
      }
    } catch (error) {
      console.error('Error opening file:', error);
      showToast('error', 'Error', 'Failed to open file');
    }
  };

  // Render functions
  const renderMessage = ({ item: message }) => {
    const isCurrentUser = message.user._id === currentUser?.userId;
    const hasReactions = Object.keys(message.reactions || {}).length > 0;

    return (
      <View style={[styles.messageContainer, isCurrentUser ? styles.messageContainerRight : styles.messageContainerLeft]}>
        {!isCurrentUser && (
          <Image source={{ uri: message.user.avatar }} style={styles.avatar} />
        )}
        
        <View style={[styles.messageWrapper, isCurrentUser ? styles.messageWrapperRight : styles.messageWrapperLeft]}>
          {/* Reply Preview */}
          {message.replyToMessage && (
            <View style={styles.replyMessageContainer}>
              <View style={styles.replyLine} />
              <View style={styles.replyContent}>
                <Text style={styles.replyAuthor}>{message.replyToMessage.user.name}</Text>
                <Text style={styles.replyText} numberOfLines={1}>
                  {message.replyToMessage.text || 'File'}
                </Text>
              </View>
            </View>
          )}

          {/* Message Content */}
          <TouchableOpacity
            style={[styles.messageBubble, isCurrentUser ? styles.messageBubbleRight : styles.messageBubbleLeft]}
            onLongPress={(event) => handleLongPress(message, event)}
            activeOpacity={0.8}
          >
            {/* Image Message */}
            {message.image && (
              <TouchableOpacity onPress={() => handleImagePress(message.image)} activeOpacity={0.8}>
                <Image source={{ uri: message.image }} style={styles.messageImage} />
              </TouchableOpacity>
            )}

            {/* File Message */}
            {message.file && (
              <TouchableOpacity 
                style={styles.fileContainer}
                onPress={() => handleFilePress(message.file.url)}
                activeOpacity={0.7}
              >
                <Ionicons name="document-outline" size={32} color="#007AFF" />
                <View style={styles.fileInfo}>
                  <Text style={styles.fileName} numberOfLines={1}>
                    {message.file.name}
                  </Text>
                  <Text style={styles.fileType}>
                    {message.file.type} • Tap to download
                  </Text>
                </View>
              </TouchableOpacity>
            )}

            {/* Text Message */}
            {message.text && (
              <Text style={[styles.messageText, isCurrentUser ? styles.messageTextRight : styles.messageTextLeft]}>
                {message.text}
              </Text>
            )}
          </TouchableOpacity>

          {/* Reactions */}
          {hasReactions && (
            <View style={[styles.reactionsContainer, isCurrentUser ? styles.reactionsRight : styles.reactionsLeft]}>
              {Object.entries(message.reactions).map(([reaction, userIds]) => (
                <TouchableOpacity
                  key={reaction}
                  style={[
                    styles.reactionBubble,
                    userIds.includes(currentUser.userId.toString()) && styles.reactionBubbleActive
                  ]}
                  onPress={() => handleReaction(reaction)}
                >
                  <Text style={styles.reactionEmoji}>{reaction}</Text>
                  <Text style={styles.reactionCount}>{userIds.length}</Text>
                </TouchableOpacity>
              ))}
            </View>
          )}

          {/* Message Footer */}
          <View style={[styles.messageFooter, isCurrentUser ? styles.messageFooterRight : styles.messageFooterLeft]}>
            <Text style={styles.messageTime}>
              {new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </Text>
            
            {message.edited && (
              <Text style={[styles.editedText, isCurrentUser ? styles.editedTextRight : styles.editedTextLeft]}>
                edited
              </Text>
            )}
            
            {isCurrentUser && (
              <View style={styles.statusIndicator}>
                {message.status === 'SENT' && (
                  <Ionicons name="checkmark" size={12} color="#007AFF" />
                )}
                {message.status === 'DELIVERED' && (
                  <View style={styles.doubleCheck}>
                    <Ionicons name="checkmark" size={12} color="#007AFF" />
                    <Ionicons name="checkmark" size={12} color="#007AFF" style={styles.secondCheck} />
                  </View>
                )}
                {message.status === 'read' && (
                  <View style={styles.doubleCheck}>
                    <Ionicons name="checkmark" size={12} color="#4CAF50" />
                    <Ionicons name="checkmark" size={12} color="#4CAF50" style={styles.secondCheck} />
                  </View>
                )}
              </View>
            )}
          </View>
        </View>
      </View>
    );
  };

  const renderHeader = () => (
    <View style={styles.header}>
      <TouchableOpacity style={styles.backButton} onPress={() => router.back()}>
        <Ionicons name="arrow-back" size={24} color="#918CE5" />
      </TouchableOpacity>
      <TouchableOpacity 
        style={styles.headerInfo} 
        onPress={() => router.push(`/Chat/Profile/${otherUser?.userId}?chatId=${chatid}`)}
      >
        <Image
          source={otherUser?.profilePictureURL ? { uri: otherUser.profilePictureURL } : defaultImage}
          style={styles.userAvatar}
        />
        <View>
          <Text style={styles.headerTitle}>
            {otherUser?.name || 'Loading...'}
          </Text>
          <Text style={styles.headerSubtitle}>
            {otherUser?.online ? 'Online' : 'Offline'}
          </Text>
        </View>
      </TouchableOpacity>
    </View>
  );

  const renderInputToolbar = () => {
    if (showEditInput) {
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
              placeholderTextColor="#999"
              autoFocus={true}
            />
            <TouchableOpacity style={styles.saveButton} onPress={saveEditedMessage}>
              <Ionicons name="checkmark" size={24} color="#007AFF" />
            </TouchableOpacity>
          </View>
        </View>
      );
    }

    return (
      <View style={styles.inputToolbar}>
        <View style={styles.inputRow}>
          <TouchableOpacity onPress={handlePickFile} style={styles.clipButton}>
            <Ionicons name="attach" size={28} color="#918CE5" />
          </TouchableOpacity>
          <TouchableOpacity onPress={handleImagePick} style={styles.clipButton}>
            <Ionicons name="image-outline" size={28} color="#918CE5" />
          </TouchableOpacity>
          
          <TextInput
            ref={inputRef}
            style={styles.textInput}
            value={inputText}
            onChangeText={setInputText}
            placeholder="Type a message..."
            placeholderTextColor="#999"
            multiline
            maxLength={500}
            onFocus={() => {
              setTimeout(() => {
                flatListRef.current?.scrollToOffset({ offset: 0, animated: true });
              }, 100);
            }}
          />
          
          <TouchableOpacity 
            onPress={onSend} 
            style={[styles.sendButton, { opacity: inputText.trim() ? 1 : 0.5 }]} 
            disabled={!inputText.trim() || uploadingFiles}
          >
            <Ionicons name="send" size={24} color="#FFFFFF" />
          </TouchableOpacity>
        </View>
      </View>
    );
  };

  const renderReplyPreview = () => {
    if (!replyToMessage) return null;

    return (
      <Animated.View style={[styles.replyPreview, { transform: [{ translateY: slideAnim }] }]}>
        <View style={styles.replyContent}>
          <Text style={styles.replyLabel}>Replying to {replyToMessage.user.name}</Text>
          <Text style={styles.replyText} numberOfLines={1}>
            {replyToMessage.text || 'File'}
          </Text>
        </View>
        <TouchableOpacity onPress={cancelReply} style={styles.replyCancel}>
          <Ionicons name="close" size={20} color="#666" />
        </TouchableOpacity>
      </Animated.View>
    );
  };

  // Loading state
  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#007AFF" />
        <Text style={styles.loadingText}>Loading chat...</Text>
      </View>
    );
  }

  return (
    <KeyboardAvoidingView 
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
    >
      {renderHeader()}
      {renderReplyPreview()}
      
      <View style={styles.chatContent}>
        <FlatList
          ref={flatListRef}
          data={messages}
          keyExtractor={(item) => item._id.toString()}
          renderItem={renderMessage}
          inverted
          showsVerticalScrollIndicator={false}
          onEndReached={loadEarlierMessages}
          onEndReachedThreshold={0.1}
          ListFooterComponent={
            loadingEarlier ? (
              <View style={styles.loadEarlierContainer}>
                <ActivityIndicator size="small" color="#007AFF" />
                <Text style={styles.loadEarlierText}>Loading earlier messages...</Text>
              </View>
            ) : hasMoreMessages ? (
              <TouchableOpacity style={styles.loadEarlierButton} onPress={loadEarlierMessages}>
                <Text style={styles.loadEarlierButtonText}>Load Earlier Messages</Text>
              </TouchableOpacity>
            ) : null
          }
          contentContainerStyle={styles.messagesList}
          keyboardShouldPersistTaps="handled"
        />
      </View>
      
      {renderInputToolbar()}
      
      {/* Enhanced Reaction Modal */}
      <Modal
        visible={showReactionModal}
        transparent
        animationType="none"
        onRequestClose={closeReactionModal}
      >
        <TouchableOpacity 
          style={styles.reactionModalOverlay}
          activeOpacity={1}
          onPress={closeReactionModal}
        >
          <Animated.View
            style={[
              styles.reactionModal,
              {
                transform: [{ scale: scaleAnim }],
                left: reactionModalPosition.x,
                top: reactionModalPosition.y,
              }
            ]}
          >
            <View style={styles.reactionRow}>
              {Object.keys(reactions).map((reaction) => (
                <TouchableOpacity
                  key={reaction}
                  style={styles.reactionButton}
                  onPress={() => handleReaction(reaction)}
                >
                  <Text style={styles.reactionEmoji}>{reaction}</Text>
                </TouchableOpacity>
              ))}
            </View>
            
            <View style={styles.actionRow}>
              <TouchableOpacity 
                style={styles.actionRowButton}
                onPress={() => {
                  closeReactionModal();
                  handleReplyMessage();
                }}
              >
                <Ionicons name="arrow-undo-outline" size={24} color="#007AFF" />
                <Text style={styles.actionRowText}>Reply</Text>
              </TouchableOpacity>
              
              {selectedMessage?.user._id === currentUser?.userId && !selectedMessage?.file && !selectedMessage?.image && (
                <TouchableOpacity 
                  style={styles.actionRowButton}
                  onPress={() => {
                    closeReactionModal();
                    handleEditMessage();
                  }}
                >
                  <Ionicons name="create-outline" size={24} color="#007AFF" />
                  <Text style={styles.actionRowText}>Edit</Text>
                </TouchableOpacity>
              )}
              
              {selectedMessage?.user._id === currentUser?.userId && (
                <TouchableOpacity 
                  style={styles.actionRowButton}
                  onPress={() => {
                    closeReactionModal();
                    handleDeleteMessage();
                  }}
                >
                  <Ionicons name="trash-outline" size={24} color="#FF3B30" />
                  <Text style={[styles.actionRowText, {color: '#FF3B30'}]}>Delete</Text>
                </TouchableOpacity>
              )}
            </View>
          </Animated.View>
        </TouchableOpacity>
      </Modal>

      {/* Upload Progress Indicator */}
      {uploadingFiles && (
        <View style={styles.uploadIndicator}>
          <ActivityIndicator size="small" color="#007AFF" />
          <Text style={styles.uploadText}>Uploading file...</Text>
        </View>
      )}
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  chatContent: {
    flex: 1,
  },
  messagesList: {
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  loadEarlierContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 16,
  },
  loadEarlierText: {
    marginLeft: 8,
    color: '#666',
    fontSize: 14,
  },
  loadEarlierButton: {
    alignItems: 'center',
    paddingVertical: 12,
  },
  loadEarlierButtonText: {
    color: '#007AFF',
    fontSize: 14,
    fontWeight: '500',
  },
  messageContainer: {
    flexDirection: 'row',
    marginVertical: 4,
    alignItems: 'flex-end',
  },
  messageContainerRight: {
    justifyContent: 'flex-end',
  },
  messageContainerLeft: {
    justifyContent: 'flex-start',
  },
  avatar: {
    width: 32,
    height: 32,
    borderRadius: 16,
    marginRight: 8,
    marginBottom: 4,
  },
  messageWrapper: {
    maxWidth: '75%',
    minWidth: 60,
  },
  messageWrapperRight: {
    alignItems: 'flex-end',
  },
  messageWrapperLeft: {
    alignItems: 'flex-start',
  },
  messageBubble: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 18,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 1,
  },
  messageBubbleRight: {
    backgroundColor: '#007AFF',
    borderBottomRightRadius: 4,
  },
  messageBubbleLeft: {
    backgroundColor: '#E5E5EA',
    borderBottomLeftRadius: 4,
  },
  messageText: {
    fontSize: 16,
    lineHeight: 20,
  },
  messageTextRight: {
    color: '#FFFFFF',
  },
  messageTextLeft: {
    color: '#000000',
  },
  messageTime: {
    fontSize: 11,
    color: '#666',
    marginHorizontal: 4,
  },
  replyMessageContainer: {
    flexDirection: 'row',
    marginBottom: 4,
    paddingLeft: 8,
  },
  replyLine: {
    width: 3,
    backgroundColor: '#007AFF',
    borderRadius: 1.5,
    marginRight: 8,
  },
  replyContent: {
    flex: 1,
    paddingVertical: 2,
  },
  replyAuthor: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#007AFF',
  },
  replyText: {
    fontSize: 14,
    color: '#666',
    marginTop: 2,
  },
  reactionsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 4,
  },
  reactionsRight: {
    justifyContent: 'flex-end',
  },
  reactionsLeft: {
    justifyContent: 'flex-start',
  },
  reactionBubble: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F0F0F0',
    borderRadius: 12,
    paddingHorizontal: 8,
    paddingVertical: 4,
    marginRight: 4,
    marginTop: 2,
  },
  reactionBubbleActive: {
    backgroundColor: '#007AFF',
  },
  reactionEmoji: {
    fontSize: 16,
  },
  reactionCount: {
    fontSize: 12,
    marginLeft: 4,
    color: '#666',
    fontWeight: '500',
  },
  inputToolbar: {
    backgroundColor: '#FFFFFF',
    borderTopWidth: 1,
    borderTopColor: '#E5E5EA',
    paddingHorizontal: 12,
    paddingVertical: 8,
    paddingBottom: Platform.OS === 'ios' ? 34 : 16,
  },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    minHeight: 44,
  },
  textInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#E5E5EA',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 10,
    marginHorizontal: 8,
    fontSize: 16,
    maxHeight: 100,
    backgroundColor: '#F8F8F8',
  },
  sendButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#007AFF',
    justifyContent: 'center',
    alignItems: 'center',
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
  replyLabel: {
    fontSize: 12,
    color: '#007AFF',
    fontWeight: 'bold',
  },
  replyCancel: {
    padding: 4,
  },
  reactionModalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
  },
  reactionModal: {
    position: 'absolute',
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
    minWidth: 300,
  },
  reactionRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#E5E5EA',
  },
  reactionButton: {
    padding: 8,
    borderRadius: 20,
    backgroundColor: '#F0F0F0',
  },
  actionRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingTop: 8,
  },
  actionRowButton: {
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  actionRowText: {
    fontSize: 12,
    marginTop: 4,
    color: '#007AFF',
    fontWeight: '500',
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
    paddingTop: 50,
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
    flexDirection: 'row',
    alignItems: 'center',
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
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 2,
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
  uploadIndicator: {
    position: 'absolute',
    top: 100,
    left: 20,
    right: 20,
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    borderRadius: 8,
    padding: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  uploadText: {
    color: '#FFFFFF',
    marginLeft: 8,
    fontSize: 14,
  },
});