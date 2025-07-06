import React, { useState, useEffect, useContext, useCallback, useRef, useMemo } from 'react';
import { View, ActivityIndicator, Text, StyleSheet, TouchableOpacity, TextInput, Alert, Platform, KeyboardAvoidingView, FlatList, Animated, Dimensions } from 'react-native';
import { Image } from 'expo-image';
import * as DocumentPicker from 'expo-document-picker';
import * as ImagePicker from 'expo-image-picker';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { UserContext } from '@/context/UserContext';
import { useGlobalMessage } from '@/context/GlobalMessageContext';
import LottieView from 'lottie-react-native';
import {
  getMessagesByChatId,
  sendMessage,
  getChatById,
  updateMessageStatus,
  editMessage,
  deleteMessage,
  reactToMessage,
  removeReactionFromMessage,
  getReactionsForMessage,
  getMessageById
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
  const [editingMessage, setEditingMessage] = useState(null);
  const [replyToMessage, setReplyToMessage] = useState(null);
  const [inputText, setInputText] = useState('');
  const [uploadingFiles, setUploadingFiles] = useState(false);

  // Reaction state
  const [showReactionPicker, setShowReactionPicker] = useState(false);
  const [reactionMessage, setReactionMessage] = useState(null);
  const [reactionPickerPosition, setReactionPickerPosition] = useState({ x: 0, y: 0 });

  // Refs and animations
  const flatListRef = useRef(null);
  const inputRef = useRef(null);
  const slideAnim = useRef(new Animated.Value(0)).current;

  // Reaction types with emojis
  const reactionTypes = [
    { type: 'LIKE', emoji: '👍', name: 'Like' },
    { type: 'LOVE', emoji: '❤️', name: 'Love' },
    { type: 'HAHA', emoji: '😂', name: 'Haha' },
    { type: 'SAD', emoji: '😢', name: 'Sad' },
    { type: 'ANGRY', emoji: '😡', name: 'Angry' }
  ];

  // Set current open chat when component mounts and clear on unmount
  useEffect(() => {
    setCurrentOpenChat(chatid);

    return () => {
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

        // Subscribe to user-specific message updates (as per backend implementation)
        try {
          await chatStompService.subscribeToUserMessages(currentUser.userId, handleTopicMessage);
          console.log('✅ Successfully subscribed to user messages:', currentUser.userId);

          // Also subscribe to user-specific reaction updates
          await chatStompService.subscribeToUserReactions(currentUser.userId, handleReactionMessage);
          console.log('✅ Successfully subscribed to user reactions:', currentUser.userId);

          connectionAttempts = 0; // Reset attempts on success
        } catch (subscriptionError) {
          console.error('❌ Failed to subscribe to user topics:', subscriptionError);
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
          console.log('🔌 Disconnecting STOMP and unsubscribing from user topics');
          chatStompService.unsubscribeFromUserMessages(currentUser.userId);
          chatStompService.unsubscribeFromUserReactions(currentUser.userId);
        }
      } catch (error) {
        console.error('Error during STOMP cleanup:', error);
      }
    };
  }, [currentUser?.userId, chatid]);

  // Handle incoming STOMP messages
  const handleTopicMessage = useCallback((data) => {
    console.log('📡 Received STOMP message:', data);
    
    if (!data || !data.eventType || !data.messageDTO) {
      console.warn('⚠️ Invalid STOMP message format:', data);
      return;
    }

    const { eventType, messageDTO } = data;
    
    // Only process messages for the current chat
    if (messageDTO.chatId !== chatid) {
      console.log('💬 Message not for current chat, ignoring:', messageDTO.chatId, 'vs', chatid);
      return;
    }
    
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
  }, [handleNewMessage, handleMessageEdit, handleMessageDelete, handleStatusUpdate, chatid]);

  // Handle incoming reaction messages
  const handleReactionMessage = useCallback((data) => {
    console.log('📡 Received STOMP reaction message:', data);
    
    if (!data || !data.eventType || !data.messageReactionDTO) {
      console.warn('⚠️ Invalid STOMP reaction message format:', data);
      return;
    }

    const { eventType, messageReactionDTO } = data;
    
    switch (eventType) {
      case 'REACT':
        console.log('👍 Message reaction added:', messageReactionDTO);
        setMessages(prev => 
          prev.map(msg => {
            if (msg._id === messageReactionDTO.messageId) {
              const existingReactions = msg.reactions || [];
              const existingReactionIndex = existingReactions.findIndex(r => r.userId === messageReactionDTO.userId);
              
              let updatedReactions;
              if (existingReactionIndex >= 0) {
                // Update existing reaction
                updatedReactions = existingReactions.map((r, index) =>
                  index === existingReactionIndex 
                    ? { ...r, reactionType: messageReactionDTO.reactionType }
                    : r
                );
              } else {
                // Add new reaction
                updatedReactions = [
                  ...existingReactions,
                  {
                    userId: messageReactionDTO.userId,
                    reactionType: messageReactionDTO.reactionType,
                    userName: 'User' // We might need to fetch this from user context
                  }
                ];
              }
              
              return { ...msg, reactions: updatedReactions };
            }
            return msg;
          })
        );
        break;
        
      case 'REMOVE_REACT':
        console.log('👎 Message reaction removed:', messageReactionDTO);
        setMessages(prev => 
          prev.map(msg => {
            if (msg._id === messageReactionDTO.messageId) {
              const updatedReactions = (msg.reactions || []).filter(r => r.userId !== messageReactionDTO.userId);
              return { ...msg, reactions: updatedReactions };
            }
            return msg;
          })
        );
        break;
        
      default:
        console.log('⚠️ Unknown reaction event type:', eventType);
    }
  }, [chatid]);

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
        reactions: messageData.reactions || [],
        replyToMessage: messageData.replyToMessageId ? {
          _id: messageData.replyToMessageId,
          // We'd need to fetch the reply message details or have them in the response
          text: 'Reply message content would go here',
          user: { name: 'Reply User' }
        } : null,
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
        reactions: msg.reactions || [],
        replyToMessage: msg.replyToMessageId ? {
          _id: msg.replyToMessageId,
          // Would need to fetch actual reply message content
          text: 'Reply message',
          user: { name: 'Reply User' }
        } : null,
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
          reactions: msg.reactions || [],
          replyToMessage: msg.replyToMessageId ? {
            _id: msg.replyToMessageId,
            text: 'Reply message',
            user: { name: 'Reply User' }
          } : null,
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
    console.log('🔔 Long press detected on message:', message._id);
    // Show both reactions picker and message actions
    handleReactToMessage(message, event);
    setSelectedMessage(message);
    setShowMessageActions(true);
    console.log('✅ Action bar should now be visible for message:', message._id);
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Rigid);
  }, [handleReactToMessage]);

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
          status: 'SENT',
          reactions: response.reactions || [],
          edited: false
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
    console.log('📝 Reply button pressed, selectedMessage:', selectedMessage?._id);
    setReplyToMessage(selectedMessage);
    // Clear action bar and selection for reply since it's a one-time action
    setSelectedMessage(null);
    setShowMessageActions(false);
    setShowReactionPicker(false);
    setReactionMessage(null);
    setReactionPickerPosition({ x: 0, y: 0 });
  };

  const handleEditMessage = () => {
    console.log('✏️ Edit button pressed, selectedMessage:', selectedMessage?._id);
    if (!selectedMessage || !selectedMessage.text) {
      console.log('❌ Cannot edit: no selected message or no text');
      return;
    }
    
    // Store the message to edit in separate state
    setEditingMessage(selectedMessage);
    setEditText(selectedMessage.text);
    setShowEditInput(true);
    
    // Hide both the reaction picker and action modal when starting edit
    setShowReactionPicker(false);
    setReactionMessage(null);
    setReactionPickerPosition({ x: 0, y: 0 });
    setSelectedMessage(null);
    setShowMessageActions(false);
    console.log('✅ Edit mode activated, modal and reactions hidden');
  };

  const handleDeleteMessage = async () => {
    console.log('🗑️ Delete button pressed, selectedMessage:', selectedMessage?._id);
    if (!selectedMessage) return;

    // Hide the reaction picker and action modal immediately
    setShowReactionPicker(false);
    setReactionMessage(null);
    setReactionPickerPosition({ x: 0, y: 0 });
    setShowMessageActions(false);
    setSelectedMessage(null);

    Alert.alert(
      'Delete Message',
      'Are you sure you want to delete this message?',
      [
        { 
          text: 'Cancel', 
          style: 'cancel',
          onPress: () => {
            console.log('❌ Delete cancelled');
            // Don't restore action bar, keep it hidden
          }
        },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              console.log('🗑️ Deleting message:', selectedMessage._id);
              await deleteMessage(selectedMessage._id);
              setMessages(prev => prev.filter(msg => msg._id !== selectedMessage._id));
              console.log('✅ Message deleted successfully');
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
    console.log('Saving edited message:', editText, editingMessage);
    if (!editText.trim() || !editingMessage) return;

    try {
      await editMessage(editingMessage._id, editText.trim());
      setMessages(prev => 
        prev.map(msg => 
          msg._id === editingMessage._id 
            ? { ...msg, text: editText.trim(), edited: true }
            : msg
        )
      );
      // Clear edit mode and all related states after successful save
      setShowEditInput(false);
      setEditText('');
      setEditingMessage(null);
      setSelectedMessage(null);
      setShowMessageActions(false);
      setShowReactionPicker(false);
      setReactionMessage(null);
      setReactionPickerPosition({ x: 0, y: 0 });
      console.log('✅ Message edited successfully');
    } catch (error) {
      console.error('Error editing message:', error);
      showToast('error', 'Error', 'Failed to edit message');
      // Keep edit mode visible on error
    }
  };

  const cancelEdit = () => {
    // Clear edit mode and all related states
    setShowEditInput(false);
    setEditText('');
    setEditingMessage(null);
    setSelectedMessage(null);
    setShowMessageActions(false);
    setShowReactionPicker(false);
    setReactionMessage(null);
    setReactionPickerPosition({ x: 0, y: 0 });
    console.log('❌ Edit cancelled, all states cleared');
  };

  const cancelReply = () => {
    setReplyToMessage(null);
  };

  // Reaction functions
  const handleReactToMessage = (message, event = null) => {
    setReactionMessage(message);
    
    // Calculate position for floating picker with better positioning logic
    if (event && event.nativeEvent) {
      const { pageX, pageY } = event.nativeEvent;
      const { width: screenWidth } = Dimensions.get('window');
      const pickerWidth = 250; // Approximate picker width (5 reactions * 40px + padding)
      
      // Calculate X position - ensure picker doesn't go off screen
      let xPosition = pageX - (pickerWidth / 2); // Center picker on touch point
      
      // Adjust if too far left
      if (xPosition < 20) {
        xPosition = 20;
      }
      // Adjust if too far right
      if (xPosition + pickerWidth > screenWidth - 20) {
        xPosition = screenWidth - pickerWidth - 20;
      }
      
      setReactionPickerPosition({
        x: xPosition,
        y: Math.max(100, pageY - 80) // Position above the touch point with more space
      });
    } else {
      // Default position if no event (e.g., from header button)
      const { width: screenWidth } = Dimensions.get('window');
      const pickerWidth = 250;
      setReactionPickerPosition({ 
        x: (screenWidth - pickerWidth) / 2, // Center on screen
        y: 200 
      });
    }
    
    setShowReactionPicker(true);
    // Don't close message actions here - let them show simultaneously
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
  };

  const addReaction = async (reactionType) => {
    if (!reactionMessage) return;

    try {
      console.log('Adding reaction:', reactionType, 'to message:', reactionMessage._id);
      
      // Optimistic update
      setMessages(prev => 
        prev.map(msg => {
          if (msg._id === reactionMessage._id) {
            const existingReaction = msg.reactions?.find(r => r.userId === currentUser?.userId);
            let updatedReactions = msg.reactions || [];
            
            if (existingReaction) {
              // Update existing reaction
              updatedReactions = updatedReactions.map(r =>
                r.userId === currentUser?.userId 
                  ? { ...r, reactionType: reactionType }
                  : r
              );
            } else {
              // Add new reaction
              updatedReactions = [
                ...updatedReactions,
                {
                  userId: currentUser?.userId,
                  userName: currentUser?.name,
                  reactionType: reactionType
                }
              ];
            }
            
            return { ...msg, reactions: updatedReactions };
          }
          return msg;
        })
      );

      // API call
      await reactToMessage(reactionMessage._id, reactionType);
      
      // Hide both reaction picker and action buttons
      setShowReactionPicker(false);
      setReactionMessage(null);
      setReactionPickerPosition({ x: 0, y: 0 });
      setSelectedMessage(null);
      setShowMessageActions(false);
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
      
    } catch (error) {
      console.error('Error adding reaction:', error);
      showToast('error', 'Error', 'Failed to add reaction');
      
      // Revert optimistic update on error
      const messageData = await getMessagesByChatId(chatid, 0, messagesPerPage);
      if (messageData) {
        // Re-fetch and update the specific message
        const messagesArray = Array.isArray(messageData) ? messageData : (messageData.content || []);
        const updatedMessage = messagesArray.find(msg => msg.messageId === reactionMessage._id);
        if (updatedMessage) {
          setMessages(prev => 
            prev.map(msg => 
              msg._id === reactionMessage._id 
                ? { ...msg, reactions: updatedMessage.reactions || [] }
                : msg
            )
          );
        }
      }
    }
  };

  const removeReaction = async (message) => {
    try {
      console.log('Removing reaction from message:', message._id);
      
      // Optimistic update
      setMessages(prev => 
        prev.map(msg => {
          if (msg._id === message._id) {
            const updatedReactions = (msg.reactions || []).filter(r => r.userId !== currentUser?.userId);
            return { ...msg, reactions: updatedReactions };
          }
          return msg;
        })
      );

      // API call
      await removeReactionFromMessage(message._id);
      
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      
    } catch (error) {
      console.error('Error removing reaction:', error);
      showToast('error', 'Error', 'Failed to remove reaction');
      
      // Revert optimistic update on error
      const messageData = await getMessagesByChatId(chatid, 0, messagesPerPage);
      if (messageData) {
        const messagesArray = Array.isArray(messageData) ? messageData : (messageData.content || []);
        const updatedMessage = messagesArray.find(msg => msg.messageId === message._id);
        if (updatedMessage) {
          setMessages(prev => 
            prev.map(msg => 
              msg._id === message._id 
                ? { ...msg, reactions: updatedMessage.reactions || [] }
                : msg
            )
          );
        }
      }
    }
  };

  const getUserReaction = (message) => {
    if (!message.reactions || !currentUser?.userId) return null;
    return message.reactions.find(r => r.userId === currentUser.userId);
  };

  const getReactionSummary = (message) => {
    if (!message.reactions || message.reactions.length === 0) return null;
    
    const reactionCounts = {};
    message.reactions.forEach(reaction => {
      reactionCounts[reaction.reactionType] = (reactionCounts[reaction.reactionType] || 0) + 1;
    });
    
    return reactionCounts;
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
          reactions: result.reactions || [],
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

          {/* Reactions */}
          {message.reactions && message.reactions.length > 0 && (
            <View style={[styles.reactionsContainer, isCurrentUser ? styles.reactionsContainerRight : styles.reactionsContainerLeft]}>
              {Object.entries(getReactionSummary(message) || {}).map(([reactionType, count]) => {
                const reactionEmoji = reactionTypes.find(r => r.type === reactionType)?.emoji || '👍';
                const userReaction = getUserReaction(message);
                const isUserReaction = userReaction?.reactionType === reactionType;
                
                return (
                  <TouchableOpacity
                    key={reactionType}
                    style={[
                      styles.reactionBubble,
                      isUserReaction && styles.reactionBubbleActive
                    ]}
                    onPress={() => {
                      if (isUserReaction) {
                        removeReaction(message);
                      } else {
                        // If user already has a different reaction, this will update it
                        addReaction(reactionType);
                        setReactionMessage(message);
                      }
                    }}
                  >
                    <Text style={styles.reactionEmoji}>{reactionEmoji}</Text>
                    {count > 1 && <Text style={styles.reactionCount}>{count}</Text>}
                  </TouchableOpacity>
                );
              })}
            </View>
          )}
        </View>
      </View>
    );
  }, [currentUser?.userId, selectedMessage?._id, otherUser?.profilePictureURL, handleLongPress, handleImagePress, handleFilePress]);

  const renderHeader = () => {
    return (
      <View style={styles.header}>
        <TouchableOpacity style={styles.backButton} onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color="#918CE5" />
        </TouchableOpacity>

        {/* Always show normal header */}
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
  };

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
        <LottieView
          source={require("@/assets/lottie/loading.json")}
          autoPlay
          loop
          style={styles.lottie}
        />
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

      {/* Floating Reaction Picker */}
      {showReactionPicker && (
        <>
          {/* Transparent overlay to close picker */}
          <TouchableOpacity 
            style={styles.reactionOverlay}
            activeOpacity={1}
            onPress={() => {
              setShowReactionPicker(false);
              setReactionMessage(null);
              setReactionPickerPosition({ x: 0, y: 0 });
              setSelectedMessage(null);
              setShowMessageActions(false);
            }}
          />
          
          {/* Floating reaction picker */}
          <View 
            style={[
              styles.floatingReactionPicker,
              {
                left: reactionPickerPosition.x,
                top: reactionPickerPosition.y,
              }
            ]}
          >
            {reactionTypes.map((reaction) => (
              <TouchableOpacity
                key={reaction.type}
                style={styles.floatingReactionButton}
                onPress={() => addReaction(reaction.type)}
              >
                <Text style={styles.floatingReactionEmoji}>{reaction.emoji}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </>
      )}

      {/* Messenger-style Bottom Action Modal */}
      {showMessageActions && selectedMessage && (
        <>
          {/* Semi-transparent overlay */}
          <TouchableOpacity 
            style={styles.actionModalOverlay}
            activeOpacity={1}
            onPress={() => {
              console.log('❌ Action modal overlay pressed, clearing all states');
              setSelectedMessage(null);
              setShowMessageActions(false);
              setShowReactionPicker(false);
              setReactionMessage(null);
              setReactionPickerPosition({ x: 0, y: 0 });
            }}
          />
          
          {/* Bottom action modal */}
          <View style={styles.bottomActionModal}>
            <View style={styles.actionModalHeader}>
              <Text style={styles.actionModalTitle}>Message Actions</Text>
              <TouchableOpacity
                onPress={() => {
                  console.log('❌ Close button pressed, clearing all states');
                  setSelectedMessage(null);
                  setShowMessageActions(false);
                  setShowReactionPicker(false);
                  setReactionMessage(null);
                  setReactionPickerPosition({ x: 0, y: 0 });
                }}
              >
                <Ionicons name="close" size={24} color="#666" />
              </TouchableOpacity>
            </View>
            
            <View style={styles.actionsList}>
              {/* Reply Action */}
              {/* <TouchableOpacity style={styles.actionItem} onPress={handleReplyMessage}>
                <View style={styles.actionIconContainer}>
                  <Ionicons name="arrow-undo" size={24} color="#918CE5" />
                </View>
                <Text style={styles.actionText}>Reply</Text>
              </TouchableOpacity> */}

              {/* Edit and Delete actions only for current user's messages */}
              {selectedMessage?.user._id === currentUser?.userId && (
                <>
                  {/* Hide Edit button for image and file messages */}
                  {!selectedMessage?.image && !selectedMessage?.file && (
                    <TouchableOpacity style={styles.actionItem} onPress={handleEditMessage}>
                      <View style={styles.actionIconContainer}>
                        <Ionicons name="create-outline" size={24} color="#918CE5" />
                      </View>
                      <Text style={styles.actionText}>Edit</Text>
                    </TouchableOpacity>
                  )}

                  <TouchableOpacity style={styles.actionItem} onPress={handleDeleteMessage}>
                    <View style={styles.actionIconContainer}>
                      <Ionicons name="trash-outline" size={24} color="#FF3B30" />
                    </View>
                    <Text style={[styles.actionText, { color: '#FF3B30' }]}>Delete</Text>
                  </TouchableOpacity>
                </>
              )}
            </View>
          </View>
        </>
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
  // Header Styles
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E5E5EA',
  },
  backButton: {
    marginRight: 12,
    padding: 8,
  },
  headerInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  userAvatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    marginRight: 12,
  },
  headerTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#000',
  },
  headerSubtitle: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
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
    paddingHorizontal: 6,
    paddingVertical: 8,
    borderTopWidth: 1,
    borderTopColor: '#E5E5EA',
    paddingBottom: Platform.OS === 'ios' ? 24 : 12,
  },
  inputToolbar: {
    flexDirection: 'row',
    alignItems: 'flex-end',
  },
  actionButton: {
    padding: 10,
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
  // Reaction Styles
  reactionsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 4,
    marginHorizontal: 8,
  },
  reactionsContainerRight: {
    justifyContent: 'flex-end',
  },
  reactionsContainerLeft: {
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
    marginBottom: 4,
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  reactionBubbleActive: {
    backgroundColor: '#918CE5',
    borderColor: '#918CE5',
  },
  reactionEmoji: {
    fontSize: 14,
    marginRight: 2,
  },
  reactionCount: {
    fontSize: 12,
    color: '#666',
    fontWeight: '500',
  },
  // Floating Reaction Picker Styles
  reactionOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'transparent',
    zIndex: 999,
  },
  floatingReactionPicker: {
    position: 'absolute',
    flexDirection: 'row',
    backgroundColor: '#FFFFFF',
    borderRadius: 25,
    paddingHorizontal: 8,
    paddingVertical: 6,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 8,
    elevation: 8,
    zIndex: 1000,
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  floatingReactionButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
    marginHorizontal: 2,
  },
  floatingReactionEmoji: {
    fontSize: 24,
  },
  // Bottom Action Modal Styles (Messenger-style)
  actionModalOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 998,
  },
  bottomActionModal: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: '#FFFFFF',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    paddingBottom: Platform.OS === 'ios' ? 34 : 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: -2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 8,
    elevation: 8,
    zIndex: 999,
  },
  actionModalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#E5E5EA',
  },
  actionModalTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#000',
  },
  actionsList: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  actionItem: {
    flexDirection: 'column',
    alignItems: 'center',
    borderRadius: 12,
  },
  actionIconContainer: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#FFF',
    justifyContent: 'center',
    alignItems: 'center',
  },
  actionText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#000',
    textAlign: 'center',
  },
  lottie: {
    width: 80,
    height: 80,
  },
});