import React, { useState, useEffect, useContext, useCallback, useRef, useMemo } from 'react';
import { View, ActivityIndicator, Text, StyleSheet, TouchableOpacity, TextInput, Alert, Platform, KeyboardAvoidingView, FlatList, Animated } from 'react-native';
import { Image } from 'expo-image';
import * as DocumentPicker from 'expo-document-picker';
import * as ImagePicker from 'expo-image-picker';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { UserContext } from '@/context/UserContext';
import { useGlobalMessage } from '@/context/GlobalMessageContext';
import {
  getMessagesByChatId,
  sendMessage,
  getChatById,
  updateMessageStatus,
  editMessage,
  deleteMessage,
  reactToMessage,
  removeReactionFromMessage,
  getReactionsForMessage
} from '@/services/chatService';
import { uploadFile } from '@/services/uploadService';
import chatStompService from '@/services/chatStompService';
import Toast from 'react-native-toast-message';
import { getToken } from '@/storage/tokenStorage';
import * as Haptics from 'expo-haptics';

export default function ChatDetailScreen() {
  const { chatid } = useLocalSearchParams();
  const router = useRouter();
  const { user: currentUser } = useContext(UserContext);
  const { setCurrentOpenChat } = useGlobalMessage();

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
  
  // Refs and animations
  const flatListRef = useRef(null);
  const inputRef = useRef(null);
  const slideAnim = useRef(new Animated.Value(0)).current;

  // Set current open chat when component mounts and clear on unmount
  useEffect(() => {
    console.log('💬 Setting current open chat to:', chatid);
    setCurrentOpenChat(chatid);
    
    return () => {
      console.log('💬 Clearing current open chat');
      setCurrentOpenChat(null);
    };
  }, [chatid, setCurrentOpenChat]);

  const defaultImage = require('@/assets/images/Defaults/default-user.png');

  const memoizedMessages = useMemo(() => messages, [messages]);

  // Utility functions - memoized for better performance
  const isImageFile = useCallback((url) => {
    const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg'];
    return imageExtensions.some(ext => url.toLowerCase().includes(ext));
  }, []);

  const getFileNameFromUrl = useCallback((url) => {
    try {
      const parts = url.split('/');
      const fileName = parts[parts.length - 1];
      return fileName.split('?')[0] || 'Unknown File';
    } catch (error) {
      return 'Unknown File';
    }
  }, []);

  const getFileTypeFromUrl = useCallback((url) => {
    try {
      const fileName = getFileNameFromUrl(url);
      const extension = fileName.split('.').pop();
      return extension ? `.${extension}` : 'Unknown';
    } catch (error) {
      return 'Unknown';
    }
  }, [getFileNameFromUrl]);

  // STOMP connection setup with improved error handling
  useEffect(() => {
    let connectionAttempts = 0;
    const maxRetries = 3;
    const retryDelay = 2000;
    let retryTimeout = null;

    const connectToStomp = async () => {
      if (!currentUser?.userId || !chatid) {
        console.log('⚠️ Missing userId or chatId for STOMP connection');
        return;
      }

      try {
        connectionAttempts++;
        console.log(`🔄 STOMP connection attempt ${connectionAttempts}/${maxRetries}`);
        
        const token = await getToken('accessToken');
        if (!token) {
          console.log('❌ No access token available for STOMP connection');
          return;
        }

        console.log('🔌 Connecting to STOMP with userId:', currentUser.userId);

        // Connect to STOMP (now returns a Promise)
        await chatStompService.connect(currentUser.userId, token);
        console.log('✅ STOMP connection successful');

        // Subscribe to chat topic after successful connection
        try {
          await chatStompService.subscribeToChatTopic(chatid, handleTopicMessage);
          console.log('✅ Successfully subscribed to chat topic:', chatid);
          connectionAttempts = 0; // Reset attempts on success
        } catch (subscriptionError) {
          console.error('❌ Failed to subscribe to chat topic:', subscriptionError);
          throw subscriptionError;
        }

      } catch (error) {
        console.error(`❌ STOMP connection attempt ${connectionAttempts} failed:`, error.message);

        // Retry logic
        if (connectionAttempts < maxRetries) {
          console.log(`⏳ Retrying STOMP connection in ${retryDelay/1000} seconds...`);
          retryTimeout = setTimeout(connectToStomp, retryDelay);
        } else {
          console.error('❌ STOMP connection failed after maximum retries');
          showToast('error', 'Connection Error', 'Failed to connect to chat service. Messages may not update in real-time.');
        }
      }
    };

    connectToStomp();

    return () => {
      // Clear any pending retry timeout
      if (retryTimeout) {
        clearTimeout(retryTimeout);
      }
      
      try {
        if (chatStompService.isClientConnected()) {
          console.log('🔌 Disconnecting STOMP and unsubscribing from chat:', chatid);
          chatStompService.unsubscribeFromChatTopic(chatid);
        }
      } catch (error) {
        console.error('Error during STOMP cleanup:', error);
      }
    };
  }, [currentUser?.userId, chatid]);

  // Re-subscribe to chat topic when message handler changes
  useEffect(() => {
    if (!chatid || !chatStompService.isClientConnected()) {
      return;
    }

    const resubscribe = async () => {
      try {
        console.log('🔄 Re-subscribing to chat topic with updated handler:', chatid);
        // Unsubscribe first
        chatStompService.unsubscribeFromChatTopic(chatid);
        // Subscribe with the updated handler
        await chatStompService.subscribeToChatTopic(chatid, handleTopicMessage);
        console.log('✅ Successfully re-subscribed to chat topic:', chatid);
      } catch (error) {
        console.error('❌ Failed to re-subscribe to chat topic:', error);
      }
    };

    resubscribe();
  }, [chatid, handleTopicMessage]);

  // Handle incoming STOMP messages
  const handleTopicMessage = useCallback((data) => {
    console.log('📡 Received STOMP message:', data);
    
    if (!data || !data.eventType || !data.messageDTO) {
      console.warn('⚠️ Invalid STOMP message format:', data);
      return;
    }

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
      default:
        console.log('⚠️ Unknown event type:', eventType);
    }
  }, [handleNewMessage, handleMessageEdit, handleMessageDelete, handleStatusUpdate]);

  const handleNewMessage = useCallback((messageData) => {
    console.log('📨 Handling new message from STOMP:', messageData);
    
    if (!messageData || !messageData.messageId) {
      console.warn('⚠️ Invalid message data received:', messageData);
      return;
    }

    // Use a function to access current state values
    setMessages(prev => {
      // Get the current user from context
      const currentUserFromContext = currentUser;
      
      // Check if message already exists to prevent duplicates
      const messageExists = prev.some(msg => msg._id === messageData.messageId);
      if (messageExists) {
        console.log('📝 Message already exists, skipping duplicate:', messageData.messageId);
        return prev;
      }

      // Get other user data from the existing messages or use defaults
      let otherUserData = null;
      const existingMessage = prev.find(msg => msg.user._id !== currentUserFromContext?.userId);
      if (existingMessage) {
        otherUserData = {
          name: existingMessage.user.name,
          profilePictureURL: existingMessage.user.avatar === defaultImage ? null : existingMessage.user.avatar
        };
      }

      const avatar = messageData.senderId === currentUserFromContext?.userId 
        ? currentUserFromContext?.profilePictureURL || defaultImage
        : otherUserData?.profilePictureURL || defaultImage;

      const newMessage = {
        _id: messageData.messageId,
        text: messageData.file ? '' : messageData.content,
        createdAt: new Date(messageData.sentAt),
        user: {
          _id: messageData.senderId,
          name: messageData.senderId === currentUserFromContext?.userId 
            ? currentUserFromContext?.name || 'You'
            : otherUserData?.name || 'Unknown',
          avatar: avatar
        },
        edited: messageData.edited || false,
        status: messageData.status || 'SENT',
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

      console.log('✅ Adding new message to state:', messageData.messageId);
      return [newMessage, ...prev];
    });

    // Auto-mark messages as read if not from current user
    if (messageData.senderId !== currentUser?.userId) {
      console.log('🔔 Auto-marking message as delivered/read for other user message:', messageData.messageId);
      setTimeout(() => markMessageAsDelivered(messageData.messageId), 100);
      setTimeout(() => markMessageAsRead(messageData.messageId), 300);
    } else {
      console.log('👤 Message from current user, not auto-marking as read:', messageData.messageId);
    }
  }, [currentUser, isImageFile, getFileNameFromUrl, getFileTypeFromUrl, defaultImage]);

  const handleMessageEdit = useCallback((updatedMessage) => {
    setMessages(prev => 
      prev.map(msg => 
        msg._id === updatedMessage.messageId 
          ? { ...msg, text: updatedMessage.content, edited: true }
          : msg
      )
    );
  }, []);

  const handleMessageDelete = useCallback((deletedMessage) => {
    setMessages(prev => prev.filter(msg => msg._id !== deletedMessage.messageId));
  }, []);

  const handleStatusUpdate = useCallback((updatedMessage) => {
    setMessages(prev => 
      prev.map(msg => 
        msg._id === updatedMessage.messageId 
          ? { ...msg, status: updatedMessage.status }
          : msg
      )
    );
  }, []);

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
          name: msg.senderId === currentUser?.userId 
            ? currentUser?.name || 'You'
            : otherUserData?.name || 'Unknown',
          avatar: msg.senderId === currentUser?.userId 
            ? currentUser?.profilePictureURL || defaultImage
            : otherUserData?.profilePictureURL || defaultImage
        },
        edited: msg.edited || false,
        status: msg.status || 'SENT',
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

  // Message status updates
  const markMessageAsDelivered = useCallback(async (messageId) => {
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
  }, []);

  const markMessageAsRead = useCallback(async (messageId) => {
    try {
      await updateMessageStatus(messageId, 'READ');
      setMessages(prev =>
        prev.map(msg =>
          msg._id === messageId ? { ...msg, status: 'READ' } : msg
        )
      );
    } catch (error) {
      console.error('Error marking message as read:', error);
    }
  }, []);

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
            name: msg.senderId === currentUser?.userId 
              ? currentUser?.name || 'You'
              : otherUser?.name || 'Unknown',
            avatar: msg.senderId === currentUser?.userId 
              ? currentUser?.profilePictureURL || defaultImage
              : otherUser?.profilePictureURL || defaultImage
          },
          edited: msg.edited || false,
          status: msg.status || 'SENT',
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

        setMessages(prev => [...prev, ...transformedMessages]);
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
    setSelectedMessage(message);
    setShowMessageActions(true);
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);
  }, []);

  // Message sending
  const onSend = useCallback(async () => {
    if (!inputText.trim()) return;

    const messageText = inputText.trim();
    setInputText('');

    try {
      const replyToMessageId = replyToMessage ? replyToMessage._id : null;
      const response = await sendMessage(chatid, messageText, replyToMessageId);
      
      if (response && response.messageId) {
        console.log('💬 Message sent successfully:', response);
        
        // Always add the message locally for immediate feedback
        const optimisticMessage = {
          _id: response.messageId,
          text: response.content,
          createdAt: new Date(response.sentAt),
          user: {
            _id: response.senderId,
            name: currentUser?.name || 'Unknown',
            avatar: currentUser?.profilePictureURL || defaultImage
          },
          replyToMessage: replyToMessage,
          status: 'SENT'
        };
        
        console.log('🚀 Adding optimistic message to local state:', optimisticMessage);
        
        // Add message to local state immediately
        setMessages(prev => {
          // Check if message already exists to prevent duplicates
          const messageExists = prev.some(msg => msg._id === response.messageId);
          if (messageExists) {
            console.log('📝 Optimistic message already exists, skipping:', response.messageId);
            return prev;
          }
          console.log('✅ Adding optimistic message to state');
          return [optimisticMessage, ...prev];
        });
        
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
    setSelectedMessage(null);
  };

  const handleEditMessage = () => {
    if (!selectedMessage || !selectedMessage.text) return;
    setEditText(selectedMessage.text);
    setShowEditInput(true);
    // Don't clear selectedMessage here - we need it for saving
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
              setSelectedMessage(null);
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
    console.log('Saving edited message:', editText, selectedMessage);
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

      if (result && result.messageId) {
        console.log('File message sent:', result);
        
        // Always add the file message locally for immediate feedback
        const optimisticFileMessage = {
          _id: result.messageId,
          text: result.file ? '' : result.content,
          createdAt: new Date(result.sentAt),
          user: {
            _id: result.senderId,
            name: currentUser?.name || 'Unknown',
            avatar: currentUser?.profilePictureURL || defaultImage
          },
          status: 'SENT',
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
          ...(replyToMessage && {
            replyToMessage: replyToMessage
          })
        };
        
        // Add file message to local state immediately
        setMessages(prev => {
          // Check if message already exists to prevent duplicates
          const messageExists = prev.some(msg => msg._id === result.messageId);
          if (messageExists) {
            return prev;
          }
          return [optimisticFileMessage, ...prev];
        });
        
        if (replyToMessage) {
          setReplyToMessage(null);
        }
        
        showToast('success', 'Success', 'File sent successfully');
      }
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

  // Memoized components for better performance
  const FooterComponent = useMemo(() => {
    if (loadingEarlier) {
      return (
        <View style={styles.loadEarlierContainer}>
          <ActivityIndicator size="small" color="#918CE5" />
          <Text style={styles.loadEarlierText}>Loading earlier messages...</Text>
        </View>
      );
    }
    
    if (hasMoreMessages) {
      return (
        <TouchableOpacity style={styles.loadEarlierButton} onPress={loadEarlierMessages}>
          <Text style={styles.loadEarlierButtonText}>Load Earlier Messages</Text>
        </TouchableOpacity>
      );
    }
    
    return null;
  }, [loadingEarlier, hasMoreMessages, loadEarlierMessages]);

  // Render functions
  const renderMessage = useCallback(({ item: message }) => {
    const isCurrentUser = message.user._id === currentUser?.userId;
    const isSelected = selectedMessage?._id === message._id;

    return (
      <View style={[
        styles.messageContainer, 
        isCurrentUser ? styles.messageContainerRight : styles.messageContainerLeft,
        isSelected && styles.messageContainerSelected
      ]}>
        {!isCurrentUser && (
          <Image
            source={otherUser?.profilePictureURL ? { uri: otherUser.profilePictureURL } : defaultImage}
            style={styles.avatar}
          />
        )}

        <View style={[styles.messageWrapper, isCurrentUser ? styles.messageWrapperRight : styles.messageWrapperLeft]}>
          {/* Reply Preview */}
          {message.replyToMessage && (
            <View style={styles.replyMessageContainer}>
              <View style={styles.replyLine} />
              <View style={styles.replyContent}>
                <Text style={styles.replyAuthor}>{message.replyToMessage.user.name}</Text>
                <Text style={styles.replyText} numberOfLines={1}>
                  {message.replyToMessage.text ||
                   (message.replyToMessage.image ? '📷 Photo' :
                    message.replyToMessage.file ? `📄 ${message.replyToMessage.file.name}` : 'Media')}
                </Text>
              </View>
            </View>
          )}

            {/* Message Content */}
            <TouchableOpacity
                style={[
                  styles.messageBubble, 
                  isCurrentUser ? styles.messageBubbleRight : styles.messageBubbleLeft,
                  isSelected && styles.messageBubbleSelected
                ]}
                onLongPress={(event) => handleLongPress(message, event)}
                activeOpacity={0.8}
            >
            {/* Image Message */}
            {message.image && (
                <TouchableOpacity
                    onPress={() => handleImagePress(message.image)}
                    activeOpacity={0.8}
                    onLongPress={(event) => handleLongPress(message, event)}
                >
                    <Image source={{ uri: message.image }} style={styles.messageImage} />
                </TouchableOpacity>
            )}

            {/* File Message */}
            {message.file && (
              <TouchableOpacity
                style={styles.fileContainer}
                onPress={() => handleFilePress(message.file.url)}
                onLongPress={(event) => handleLongPress(message, event)}
                activeOpacity={0.7}
              >
                <Ionicons name="document-outline" size={16} color="#918CE5" />
                <View style={styles.fileInfo}>
                  <Text style={styles.fileName} numberOfLines={1}>
                    {message.file.name}
                  </Text>
                  <Text style={styles.fileType}>
                    {message.file.type} • Tap to open
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
                  <Ionicons name="checkmark" size={12} color="#918CE5" />
                )}
                {message.status === 'DELIVERED' && (
                  <View style={styles.doubleCheck}>
                    <Ionicons name="checkmark" size={12} color="#918CE5" />
                    <Ionicons name="checkmark" size={12} color="#918CE5" style={styles.secondCheck} />
                  </View>
                )}
                {message.status === 'READ' && (
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
  }, [currentUser?.userId, selectedMessage?._id, otherUser?.profilePictureURL, handleLongPress, handleImagePress, handleFilePress]);

  const renderHeader = () => (
    <View style={styles.header}>
      <TouchableOpacity style={styles.backButton} onPress={() => router.back()}>
        <Ionicons name="arrow-back" size={24} color="#918CE5" />
      </TouchableOpacity>

      {selectedMessage ? (
        // Action Header when message is selected
        <View style={styles.actionHeader}>
          <View style={styles.actionButtons}>
            <TouchableOpacity style={styles.headerActionButton} onPress={handleReplyMessage}>
              <Ionicons name="arrow-undo" size={24} color="#918CE5" />
            </TouchableOpacity>

            {selectedMessage?.user._id === currentUser?.userId && (
              <>
                <TouchableOpacity style={styles.headerActionButton} onPress={handleEditMessage}>
                  <Ionicons name="create-outline" size={24} color="#918CE5" />
                </TouchableOpacity>

                <TouchableOpacity style={styles.headerActionButton} onPress={handleDeleteMessage}>
                  <Ionicons name="trash-outline" size={24} color="#FF3B30" />
                </TouchableOpacity>
              </>
            )}

            <TouchableOpacity
              style={styles.headerActionButton}
              onPress={() => {
                setSelectedMessage(null);
                setShowMessageActions(false);
              }}
            >
                <Ionicons name="close" size={24} color="#666" />
            </TouchableOpacity>
          </View>
        </View>
      ) : (
        // Normal Header
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
      )}
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
              <Ionicons name="checkmark" size={24} color="#918CE5" />
            </TouchableOpacity>
          </View>
        </View>
      );
    }

    return (
      <View style={styles.inputContainer}>
        <View style={styles.inputToolbar}>
          {/* Left Action Buttons */}
          <TouchableOpacity onPress={handlePickFile} style={styles.actionButton}>
            <Ionicons name="attach" size={24} color="#918CE5" />
          </TouchableOpacity>
          <TouchableOpacity onPress={handleImagePick} style={styles.actionButton}>
            <Ionicons name="camera" size={24} color="#918CE5" />
          </TouchableOpacity>
          
          {/* Message Input Container */}
          <View style={styles.messageInputContainer}>
            <TextInput
              ref={inputRef}
              style={styles.messageInput}
              value={inputText}
              onChangeText={setInputText}
              placeholder="Message"
              placeholderTextColor="#999"
              multiline
              maxLength={500}
              textAlignVertical="center"
              onFocus={() => {
                setTimeout(() => {
                  flatListRef.current?.scrollToOffset({ offset: 0, animated: true });
                }, 100);
              }}
            />
          </View>
          
          {/* Send Button */}
          <TouchableOpacity
            onPress={onSend}
            style={[
              styles.sendButton,
              {
                opacity: inputText.trim() ? 1 : 0.5,
                backgroundColor: inputText.trim() ? '#918CE5' : '#CCC'
              }
            ]}
            disabled={!inputText.trim() || uploadingFiles}
            activeOpacity={0.8}
          >
            <Ionicons name="send" size={20} color="#FFFFFF" />
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
            {replyToMessage.text || 
             (replyToMessage.image ? '📷 Photo' : 
              replyToMessage.file ? `📄 ${replyToMessage.file.name}` : 'Media')}
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
        <ActivityIndicator size="large" color="#918CE5" />
        <Text style={styles.loadingText}>Loading chat...</Text>
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={100}
    >
      {renderHeader()}
      {renderReplyPreview()}
        <View style={styles.chatContent}>
            <FlatList
                ref={flatListRef}
                data={memoizedMessages}
                keyExtractor={(item, index) => `${item._id}_${index}`}
                renderItem={renderMessage}
                inverted
                showsVerticalScrollIndicator={false}
                onEndReached={loadEarlierMessages}
                onEndReachedThreshold={0.1}
                ListFooterComponent={FooterComponent}
                contentContainerStyle={styles.messagesList}
                keyboardShouldPersistTaps="handled"
            />
        </View>

      {renderInputToolbar()}

      {/* Upload Progress Indicator */}
      {uploadingFiles && (
        <View style={styles.uploadIndicator}>
            <ActivityIndicator size="small" color="#918CE5" />
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
    color: '#918CE5',
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
  messageContainerSelected: {
    backgroundColor: 'hsla(243, 63.10%, 72.40%, 0.10)',
    borderRadius: 24,
    padding: 4,
    marginHorizontal: -4,
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
  },
  messageBubbleRight: {
    backgroundColor: '#918CE5',
    borderBottomRightRadius: 4,
  },
  messageBubbleLeft: {
    backgroundColor: '#E5E5EA',
    borderBottomLeftRadius: 4,
  },
  messageBubbleSelected: {
    shadowColor: '#918CE5',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 4,
    borderWidth: 2,
    borderColor: 'rgba(145, 140, 229, 0.3)',
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
    backgroundColor: '#918CE5',
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
    color: '#918CE5',
  },
  replyText: {
    fontSize: 14,
    color: '#666',
    marginTop: 2,
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
    backgroundColor: '#918CE5',
    justifyContent: 'center',
    alignItems: 'center',
  },
  clipButton: {
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 4,
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
    color: '#918CE5',
    fontWeight: 'bold',
  },
  replyCancel: {
    padding: 4,
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
    paddingTop: Platform.OS === 'ios' ? 50 : 12,
  },
  backButton: {
    marginRight: 8,
  },
  actionHeader: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
  },
  actionButtons: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'flex-end',
  },
  headerActionButton: {
    padding: 8,
    marginLeft: 8,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
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
    backgroundColor: '#F8F8F8',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#E5E5EA',
    padding: 6,
    flexDirection: 'row',
    alignItems: 'center',
    width: 200,
    maxHeight: 100,
  },
  fileInfo: {
    marginLeft: 6,
    flex: 1,
  },
  fileName: {
    fontSize: 12,
    color: '#333',
    fontWeight: '500',
  },
  fileType: {
    fontSize: 11,
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
    color: '#918CE5',
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
  // WhatsApp-style Input Styles
  inputContainer: {
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderTopWidth: 1,
    borderTopColor: '#E5E5EA',
    paddingBottom: Platform.OS === 'ios' ? 24 : 12,
  },
  inputToolbar: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    paddingHorizontal: 4,
    paddingVertical: 4,
  },
  actionButton: {
    padding: 10,
    marginRight: 8,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
    width: 44,
    height: 44,
  },
  messageInputContainer: {
    flex: 1,
    marginHorizontal: 4,
    backgroundColor: '#F8F8F8',
    borderRadius: 22,
    paddingHorizontal: 16,
    paddingVertical: 10,
    maxHeight: 120,
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: '#E5E5EA',
  },
  messageInput: {
    fontSize: 16,
    lineHeight: 20,
    color: '#000',
    textAlignVertical: 'center',
    minHeight: 20,
    paddingVertical: 0,
  },
  // Edit Input Styles
  editInputToolbar: {
    backgroundColor: '#F8F8F8',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderTopWidth: 1,
    borderTopColor: '#E5E5EA',
  },
  editHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  editTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#918CE5',
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
});