import React, { useState, useEffect, useContext, useCallback } from 'react';
import EmptyState from "@/components/EmptyState";
import {
    View,
    Text,
    StyleSheet,
    FlatList,
    TouchableOpacity,
    RefreshControl,
    Alert,
    Vibration,
} from 'react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import LottieView from 'lottie-react-native';
import { UserContext } from '@/context/UserContext';
import { useGlobalMessage } from '@/context/GlobalMessageContext';
import { useChat } from '@/context/ChatContext';

import {
    getAllChats,
    getMessagesByChatId,
    getUserChatByChatId,
    partialUpdateUserChat,
} from '@/services/chatService';
import { getUserById } from '@/services/userService';
import { getFriendsByUserId } from '@/services/friendsService';
import Toast from 'react-native-toast-message';


export default function ChatIndex() {
    const router = useRouter();
    const { user: currentUser } = useContext(UserContext);
    const { addMessageHandler, addReactionHandler, isConnected, getConnectionStatus } = useGlobalMessage();
    const {
        setChatUnreadIndicator,
        resetChatUnreadIndicator,
        initializeChatUnreadIndicators,
        getChatUnreadIndicator
    } = useChat();
    const [chats, setChats] = useState([]);
    const [userChats, setUserChats] = useState(new Map());
    const [onlineUsers, setOnlineUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);

    useEffect(() => {
        loadInitialChats();
        loadOnlineUsers();
        setupGlobalMessageHandlers();
        
        // Debug: Test setting an indicator manually after 5 seconds
        const testIndicator = setTimeout(() => {
            if (chats.length > 0) {
                const firstChatId = chats[0].chatId;
                console.log('🧪 TEST: Manually setting unread indicator for chat:', firstChatId);
                setChatUnreadIndicator(firstChatId, true);
            }
        }, 5000);
        
        // Debug: Log connection status periodically
        const connectionStatusInterval = setInterval(() => {
            const status = getConnectionStatus();
            console.log('🔍 Global message service status:', status);
        }, 10000); // Every 10 seconds

        // Periodically refresh unread counts as a fallback (but preserve real-time updates)
        const unreadCountRefreshInterval = setInterval(() => {
            console.log('🔄 Refreshing chats as fallback (preserving real-time unread counts)...');
            loadInitialChats(true); // Pass flag to preserve real-time counts
        }, 30000); // Every 30 seconds

        return () => {
            clearTimeout(testIndicator);
            clearInterval(connectionStatusInterval);
            clearInterval(unreadCountRefreshInterval);
        };
    }, [currentUser]);

    // Set up global message handlers for real-time updates
    const setupGlobalMessageHandlers = useCallback(() => {
        // Handle incoming messages
        const removeMessageHandler = addMessageHandler((eventData) => {
            const { messageDTO, eventType, isFromCurrentChat } = eventData;
            console.log('📨 Chat list received message event:', eventType, messageDTO);
            
            switch (eventType) {
                case 'SEND':
                    // Update the chat list with new message (backend handles sorting)
                    setChats(prevChats => {
                        return prevChats.map(chat => {
                            if (chat.chatId === messageDTO.chatId) {
                                return {
                                    ...chat,
                                    lastMessage: messageDTO,
                                };
                            }
                            return chat;
                        });
                    });

                    // Update unread indicator if message is not from current chat and not from current user
                    console.log('🔍 Message evaluation for unread indicator:', {
                        chatId: messageDTO.chatId,
                        senderId: messageDTO.senderId,
                        currentUserId: currentUser?.userId,
                        isFromCurrentChat,
                        messageId: messageDTO.messageId,
                        shouldSetIndicator: !isFromCurrentChat && messageDTO.senderId !== currentUser?.userId
                    });
                    
                    if (!isFromCurrentChat && messageDTO.senderId !== currentUser?.userId) {
                        console.log('📈 Chat list setting unread indicator for chat:', messageDTO.chatId);
                        // Update the backend unread count and set indicator
                        updateUnreadIndicator(messageDTO.chatId, true);
                    } else {
                        console.log('📝 Not setting unread indicator:', {
                            chatId: messageDTO.chatId,
                            senderId: messageDTO.senderId,
                            currentUserId: currentUser?.userId,
                            isFromCurrentChat,
                            reason: isFromCurrentChat ? 'from current chat' : 'from current user'
                        });
                    }
                    break;

                case 'DELETE':
                    // Handle message deletion - might need to update last message if it was the deleted one
                    setChats(prevChats => {
                        return prevChats.map(chat => {
                            if (chat.chatId === messageDTO.chatId && 
                                chat.lastMessage && 
                                chat.lastMessage.messageId === messageDTO.messageId) {
                                // The last message was deleted, we might need to fetch the new last message
                                // For now, we'll keep the old one and let the next refresh handle it
                                return {
                                    ...chat,
                                    lastMessage: null,
                                };
                            }
                            return chat;
                        });
                    });
                    break;

                case 'EDIT':
                    // Update the last message if it's the edited one
                    setChats(prevChats => {
                        return prevChats.map(chat => {
                            if (chat.chatId === messageDTO.chatId && 
                                chat.lastMessage && 
                                chat.lastMessage.messageId === messageDTO.messageId) {
                                return {
                                    ...chat,
                                    lastMessage: {
                                        ...chat.lastMessage,
                                        content: messageDTO.content,
                                        edited: true
                                    }
                                };
                            }
                            return chat;
                        });
                    });
                    break;

                case 'UPDATE_STATUS':
                    // Update message status if it's the last message
                    setChats(prevChats => {
                        return prevChats.map(chat => {
                            if (chat.chatId === messageDTO.chatId && 
                                chat.lastMessage && 
                                chat.lastMessage.messageId === messageDTO.messageId) {
                                return {
                                    ...chat,
                                    lastMessage: {
                                        ...chat.lastMessage,
                                        status: messageDTO.status
                                    }
                                };
                            }
                            return chat;
                        });
                    });
                    break;

                default:
                    console.log('🚫 Unknown message event type:', eventType);
            }
        });

        // Handle reactions
        const removeReactionHandler = addReactionHandler((eventData) => {
            const { messageReactionDTO, eventType } = eventData;

            switch (eventType) {
                case 'REACT':
                case 'REMOVE_REACT':
                    // Update message with reaction if it's the last message in any chat
                    setChats(prevChats => {
                        return prevChats.map(chat => {
                            if (chat.lastMessage &&
                                chat.lastMessage.messageId === messageReactionDTO.messageId) {
                                return {
                                    ...chat,
                                    lastMessage: {
                                        ...chat.lastMessage,
                                        messageReact: eventType === 'REACT' ? messageReactionDTO : null
                                    }
                                };
                            }
                            return chat;
                        });
                    });
                    break;

                default:
                    console.log('🚫 Unknown reaction event type:', eventType);
            }
        });

        // Cleanup function
        return () => {
            removeMessageHandler();
            removeReactionHandler();
        };
    }, [addMessageHandler, addReactionHandler, currentUser?.userId]);

    // Update unread indicator helper
    const updateUnreadIndicator = async (chatId, hasUnread) => {
        console.log(`🔄 updateUnreadIndicator called for chat ${chatId} with hasUnread: ${hasUnread}`);

        try {
            const userChat = userChats.get(chatId);
            if (!userChat) {
                console.warn(`⚠️ UserChat not found for chatId: ${chatId}`);
                return;
            }

            console.log(`🔄 Updating unread indicator for chat ${chatId}:`, {
                hasUnread,
                source: 'real-time message',
                currentUserChatUnread: userChat.unread || 0
            });

            // Update ChatContext immediately for real-time UI updates
            console.log(`📊 Setting ChatContext indicator for chat ${chatId} to: ${hasUnread}`);
            setChatUnreadIndicator(chatId, hasUnread);
            
            // Update backend - increment count by 1 if setting to true, reset to 0 if setting to false
            const newUnreadCount = hasUnread ? Math.max(1, (userChat.unread || 0) + 1) : 0;
            await partialUpdateUserChat(chatId, {
                unread: newUnreadCount
            });
            console.log(`✅ Backend updated for chat ${chatId} with unread count: ${newUnreadCount}`);

            // Update local state
            setUserChats(prev => {
                const updated = new Map(prev);
                updated.set(chatId, {
                    ...userChat,
                    unread: newUnreadCount
                });
                return updated;
            });
            console.log(`✅ Local state updated for chat ${chatId} with unread indicator: ${hasUnread}`);
        } catch (error) {
            console.error('❌ Error updating unread indicator:', error);
        }
    };

    const loadInitialChats = async (preserveRealTimeUnreadCounts = false) => {
        try {
            setLoading(true);
            
            // Fetch all chats first
            const chatsResponse = await getAllChats();
            const chatsData = Array.isArray(chatsResponse) ? chatsResponse : [];
            
            // Create a map to store userChat data
            const userChatsMap = new Map();

            // Enrich each chat with user details, last message, and userChat data
            const enrichedChats = await Promise.all(
                chatsData.map(async (chat) => {
                    try {
                        // Get the other user's ID
                        const otherUserId = chat.user1Id === currentUser?.userId ? chat.user2Id : chat.user1Id;

                        // Fetch user details, last message, and userChat data in parallel
                        const [otherUser, messagesResponse, userChatResponse] = await Promise.all([
                            getUserById(otherUserId),
                            getMessagesByChatId(chat.chatId, 0, 10),
                            getUserChatByChatId(chat.chatId).catch(() => null) // Handle case where userChat doesn't exist
                        ]);

                        // Extract the messages
                        const messages = Array.isArray(messagesResponse) ? messagesResponse : (messagesResponse?.content || []);
                        const lastMessage = messages.length > 0 ? messages[0] : null;

                        // Store userChat data in map
                        if (userChatResponse) {
                            userChatsMap.set(chat.chatId, userChatResponse);
                        } else {
                            // Create default userChat entry if it doesn't exist
                            userChatsMap.set(chat.chatId, {
                                chatId: chat.chatId,
                                userId: currentUser?.userId,
                                unread: 0,
                                pinned: false,
                                muted: false
                            });
                        }

                        // Get unread count from userChat data
                        const userChat = userChatsMap.get(chat.chatId);
                        console.log("SUIT", userChat);
                        const unreadCount = userChat?.unread || 0;

                        return {
                            ...chat,
                            otherUser: otherUser,
                            lastMessage: lastMessage,
                            hasUnreadMessages: unreadCount > 0,
                            unreadCount: unreadCount
                        };
                    } catch (error) {
                        console.warn('Failed to fetch details for chat:', chat.chatId, error);
                        // Return a basic chat object with minimal data
                        return {
                            ...chat,
                            otherUser: null,
                            lastMessage: null,
                            hasUnreadMessages: false,
                            unreadCount: 0
                        };
                    }
                })
            );
            
            // No need to sort - backend already returns chats sorted
            setUserChats(userChatsMap);
            setChats(enrichedChats);

            // Initialize ChatContext with unread indicators (preserve real-time updates if needed)
            const unreadIndicatorsForContext = new Map();
            userChatsMap.forEach((userChat, chatId) => {
                const backendHasUnread = (userChat.unread || 0) > 0;
                
                if (preserveRealTimeUnreadCounts) {
                    // Keep current context indicator or set based on backend count
                    const currentContextIndicator = getChatUnreadIndicator(chatId);
                    const finalIndicator = currentContextIndicator || backendHasUnread;
                    unreadIndicatorsForContext.set(chatId, finalIndicator);
                    console.log(`📊 Preserving real-time indicator for chat ${chatId}: context=${currentContextIndicator}, backend=${backendHasUnread}, final=${finalIndicator}`);
                } else {
                    // Use backend value as is (for initial load)
                    unreadIndicatorsForContext.set(chatId, backendHasUnread);
                    console.log(`📊 Setting initial indicator for chat ${chatId}: backend=${backendHasUnread}, unreadCount=${userChat.unread}`);
                }
            });
            console.log('📊 Initializing ChatContext with unread indicators:', unreadIndicatorsForContext);
            initializeChatUnreadIndicators(unreadIndicatorsForContext);
        } catch (error) {
            console.error('Error loading chats:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to load chats. Please try again.',
                position: 'top',
                visibilityTime: 3000,
            });
        } finally {
            setLoading(false);
        }
    };

    const loadOnlineUsers = async () => {
        try {
            const friendsResponse = await getFriendsByUserId(0, 50, 'createdAt', 'desc', currentUser?.userId);
            const friends = friendsResponse.content;
            if(!friends) return ;

            // Get detailed user info for each friend to check online status
            const friendsWithOnlineStatus = await Promise.all(
                friends.map(async (user) => {
                    try {
                        const userDetails = await getUserById(user.friend.userId);
                        return userDetails;
                    } catch (error) {
                        console.warn('Failed to fetch user details for friend:', friend.userId);
                        return null;
                    }
                })
            );

            // Filter out null results and only keep online users
            const onlineFriends = friendsWithOnlineStatus
                .filter(user => user && user.online === true);

            setOnlineUsers(onlineFriends);
        } catch (error) {
            console.error('Error loading online users:', error);
            setOnlineUsers([]);
        }
    };

    const onRefresh = async () => {
        setRefreshing(true);
        await Promise.all([
            loadInitialChats(),
            loadOnlineUsers()
        ]);
        setRefreshing(false);
    };

    const formatTime = (timestamp) => {
        if (!timestamp) return '';

        const date = new Date(timestamp);
        const now = new Date();
        const diffInHours = (now - date) / (1000 * 60 * 60);

        if (diffInHours < 24) {
            return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        } else if (diffInHours < 48) {
            return 'Yesterday';
        } else {
            return date.toLocaleDateString();
        }
    };
    
    const handleChatPress = async (chat) => {
        try {
            // Reset unread count in userChat
            const userChat = userChats.get(chat.chatId);
            if (userChat && userChat.unread > 0) {
                await partialUpdateUserChat(chat.chatId, {
                    unread: 0
                });

                // Update local state
                setUserChats(prev => {
                    const updated = new Map(prev);
                    updated.set(chat.chatId, {
                        ...userChat,
                        unread: 0
                    });
                    return updated;
                });

                // Update chat state to reflect no unread messages
                setChats(prevChats => 
                    prevChats.map(c => 
                        c.chatId === chat.chatId 
                            ? { ...c, hasUnreadMessages: false, unreadCount: 0 }
                            : c
                    )
                );

                // Reset ChatContext unread indicator
                resetChatUnreadIndicator(chat.chatId);
            }
        } catch (error) {
            console.error('Error resetting unread count:', error);
            // Continue to navigation even if reset fails
        }

        // Navigate to the chat
        router.push(`/Chat/${chat.chatId}`);
    };

    const handleChatLongPress = async (chat) => {
        // Add haptic feedback for better UX
        Vibration.vibrate(50);
        
        const userChat = userChats.get(chat.chatId);
        const isPinned = userChat?.pinned || false;
        const otherUserName = chat.otherUser?.name || 'Unknown User';

        Alert.alert(
            isPinned ? 'Unpin Chat' : 'Pin Chat',
            isPinned 
                ? `Unpin chat with ${otherUserName}?` 
                : `Pin chat with ${otherUserName} to the top?`,
            [
                {
                    text: 'Cancel',
                    style: 'cancel',
                },
                {
                    text: isPinned ? 'Unpin' : 'Pin',
                    onPress: async () => {
                        try {
                            const newPinnedStatus = !isPinned;
                            
                            // Update backend
                            await partialUpdateUserChat(chat.chatId, {
                                pinned: newPinnedStatus
                            });

                            // Update local userChats state
                            setUserChats(prev => {
                                const updated = new Map(prev);
                                const currentUserChat = updated.get(chat.chatId) || {
                                    chatId: chat.chatId,
                                    userId: currentUser?.userId,
                                    unread: 0,
                                    pinned: false,
                                    muted: false
                                };
                                updated.set(chat.chatId, {
                                    ...currentUserChat,
                                    pinned: newPinnedStatus
                                });
                                return updated;
                            });

                            // Refresh chat list to get updated order from backend
                            loadInitialChats();

                            Toast.show({
                                type: 'success',
                                text1: newPinnedStatus ? 'Chat Pinned' : 'Chat Unpinned',
                                text2: newPinnedStatus 
                                    ? `Chat with ${otherUserName} has been pinned to the top`
                                    : `Chat with ${otherUserName} has been unpinned`,
                                position: 'top',
                                visibilityTime: 2000,
                            });

                        } catch (error) {
                            console.error('Error updating pin status:', error);
                            Toast.show({
                                type: 'error',
                                text1: 'Error',
                                text2: 'Failed to update pin status. Please try again.',
                                position: 'top',
                                visibilityTime: 3000,
                            });
                        }
                    },
                },
            ]
        );
    };

    const handleOnlineUserPress = async (user) => {
        try {
            // Check if a chat already exists with this user
            const existingChat = chats.find(chat => 
                chat.otherUser?.userId === user.userId
            );

            if (existingChat) {
                handleChatPress(existingChat);
            } else {
                router.push({
                    pathname: `/UserModule/${user.userId}`,
                    params: { username: user.username }
                });
            }
        } catch (error) {
            console.error('Error handling online user press:', error);
        }
    };

    const renderLastMessageContent = (lastMessage, isFromCurrentUser) => {
        if (!lastMessage) return 'Start a conversation';

        const content = lastMessage.content || '';
        const prefix = isFromCurrentUser ? 'You: ' : '';

        // Check if the content is a URL (image or file)
        if (content.startsWith('http') && (content.includes('.jpg') || content.includes('.jpeg') || 
            content.includes('.png') || content.includes('.gif') || content.includes('.webp'))) {
            return (
                <View style={styles.mediaMessagePreview}>
                    <Ionicons name="image" size={14} color="#9188E5" style={styles.mediaIcon} />
                    <Text style={styles.mediaText}>{prefix}Image</Text>
                </View>
            );
        }
        
        // Check if it's a file URL (anything else that starts with http)
        if (content.startsWith('http')) {
            return (
                <View style={styles.mediaMessagePreview}>
                    <Ionicons name="document" size={14} color="#9188E5" style={styles.mediaIcon} />
                    <Text style={styles.mediaText}>{prefix}File</Text>
                </View>
            );
        }

        // Regular text message
        return `${prefix}${content}`;
    };

    const renderChatItem = ({ item }) => {
        const otherUser = item.otherUser;
        const lastMessage = item.lastMessage;

        // Get real-time unread indicator and pinned status from ChatContext and userChats map
        const userChat = userChats.get(item.chatId);
        const contextUnreadIndicator = getChatUnreadIndicator(item.chatId);
        const userChatHasUnread = (userChat?.unread || 0) > 0;
        const realTimeHasUnread = contextUnreadIndicator || userChatHasUnread;
        const isPinned = userChat?.pinned || false;

        // Debug logging for unread indicators - always log for debugging
        console.log(`📊 Chat ${item.chatId} (${item.otherUser?.name}) unread indicators:`, {
            contextUnreadIndicator,
            userChatHasUnread,
            userChatUnreadCount: userChat?.unread || 0,
            realTimeHasUnread,
            isPinned
        });

        const isFromCurrentUser = lastMessage?.senderId === currentUser?.userId;
        const lastMessageContent = renderLastMessageContent(lastMessage, isFromCurrentUser);

        return (
            <TouchableOpacity
                style={[
                    styles.chatItem,
                    realTimeHasUnread && styles.chatItemUnread,
                ]}
                onPress={() => handleChatPress(item)}
                onLongPress={() => handleChatLongPress(item)}
                delayLongPress={500}
            >
                <View style={styles.avatarContainer}>
                    {otherUser?.profilePictureURL ? (
                        <Image
                            source={{ uri: otherUser.profilePictureURL }}
                            style={styles.avatar}
                        />
                    ) : (
                        <View style={styles.defaultAvatar}>
                            <Ionicons name="person" size={24} color="#9188E5" />
                        </View>
                    )}
                    {realTimeHasUnread && (
                        <View style={styles.unreadDot} />
                    )}
                </View>

                <View style={styles.chatContent}>
                    <View style={styles.chatHeader}>
                        <View style={styles.userNameContainer}>
                            {isPinned && (
                                <Ionicons 
                                    name="pin" 
                                    size={14} 
                                    color="#9188E5" 
                                    style={styles.pinnedIcon} 
                                />
                            )}
                            <Text style={styles.userName} numberOfLines={1}>
                                {otherUser?.name || 'Unknown User'}
                            </Text>
                        </View>
                        <Text style={styles.timestamp}>
                            {lastMessage ? formatTime(lastMessage.sentAt) : formatTime(item.createdAt)}
                        </Text>
                    </View>

                    <View style={styles.messagePreview}>
                        {typeof lastMessageContent === 'string' ? (
                            <Text style={styles.lastMessage} numberOfLines={1}>
                                {lastMessageContent}
                            </Text>
                        ) : (
                            lastMessageContent
                        )}
                        {/* Removed unread badge - now only using dot indicator */}
                    </View>
                </View>
            </TouchableOpacity>
        );
    };

    const renderOnlineUser = ({ item }) => (
        <TouchableOpacity
            style={styles.onlineUserItem}
            onPress={() => handleOnlineUserPress(item)}
        >
            <View style={styles.onlineAvatarContainer}>
                {item.profilePictureURL ? (
                    <Image
                        source={{ uri: item.profilePictureURL }}
                        style={styles.onlineAvatar}
                    />
                ) : (
                    <View style={styles.defaultOnlineAvatar}>
                        <Ionicons name="person" size={20} color="#9188E5" />
                    </View>
                )}
                <View style={styles.onlineIndicator} />
            </View>
            <Text style={styles.onlineUserName} numberOfLines={1}>
                {item.name || 'User'}
            </Text>
        </TouchableOpacity>
    );

    const renderOnlineUsersSection = () => {
        if (onlineUsers.length === 0) return null;

        return (
            <View style={styles.onlineSection}>
                <FlatList
                    data={onlineUsers}
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    keyExtractor={(item) => item.userId}
                    renderItem={renderOnlineUser}
                    contentContainerStyle={styles.onlineUsersList}
                />
            </View>
        );
    };

    const renderEmptyState = () => (
        <View style={styles.emptyContainer}>
            <EmptyState
                iconName="chatbubbles-outline"
                title="No Conversations Yet"
                subtitle="Start a conversation by visiting someone's profile and tapping Message"
            />
        </View>
    );

    if (loading) {
        return (
            <View style={styles.container}>
                <View style={styles.loadingContainer}>
                    <LottieView
                        source={require("@/assets/lottie/loading.json")}
                        autoPlay
                        loop
                        style={styles.lottie}
                    />
                    <Text style={styles.loadingText}>Loading chats...</Text>
                </View>
            </View>
        );
    }
    return (
        <View style={styles.container}>
            {renderOnlineUsersSection()}
            <FlatList
                data={chats}
                keyExtractor={(item) => item.chatId.toString()}
                renderItem={renderChatItem}
                refreshControl={
                    <RefreshControl
                        refreshing={refreshing}
                        onRefresh={onRefresh}
                        colors={['#9188E5']}
                        tintColor="#9188E5"
                    />
                }
                ItemSeparatorComponent={() => <View style={styles.separator} />}
                ListEmptyComponent={renderEmptyState}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    lottie: {
        width: 80,
        height: 80,
    },
    loadingText: {
        marginTop: 12,
        fontSize: 16,
        color: '#9188E5',
    },
    chatItem: {
        flexDirection: 'row',
        backgroundColor: '#fff',
        paddingVertical: 12,
        paddingHorizontal: 16,
    },
    chatItemUnread: {
        backgroundColor: 'rgba(145, 140, 229, 0.1)',
        marginHorizontal: 0,
    },
    avatarContainer: {
        marginRight: 12,
        position: 'relative',
    },
    avatar: {
        width: 50,
        height: 50,
        borderRadius: 25,
    },
    defaultAvatar: {
        width: 50,
        height: 50,
        borderRadius: 25,
        backgroundColor: '#f0f0f0',
        justifyContent: 'center',
        alignItems: 'center',
    },
    unreadDot: {
        position: 'absolute',
        top: 2,
        right: 2,
        width: 12,
        height: 12,
        borderRadius: 6,
        backgroundColor: '#918CE5',
        borderWidth: 2,
        borderColor: '#fff',
    },
    chatContent: {
        flex: 1,
        justifyContent: 'center',
    },
    chatHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 4,
    },
    userNameContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        flex: 1,
    },
    pinnedIcon: {
        marginRight: 4,
    },
    userName: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
        flex: 1,
    },
    timestamp: {
        fontSize: 12,
        color: '#666',
    },
    messagePreview: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    lastMessage: {
        fontSize: 14,
        color: '#666',
        flex: 1,
    },
    mediaMessagePreview: {
        flexDirection: 'row',
        alignItems: 'center',
        flex: 1,
    },
    mediaIcon: {
        marginRight: 4,
    },
    mediaText: {
        fontSize: 14,
        color: '#666',
        flex: 1,
    },
    unreadBadge: {
        backgroundColor: '#918CE5',
        borderRadius: 10,
        minWidth: 20,
        height: 20,
        justifyContent: 'center',
        alignItems: 'center',
        marginLeft: 8,
        paddingHorizontal: 6,
    },
    unreadCount: {
        color: '#fff',
        fontSize: 12,
        fontWeight: '600',
    },
    separator: {
        height: 1,
        backgroundColor: '#f0f0f0',
        marginLeft: 78,
    },
    emptyContainer: {
        paddingVertical: 12
    },
    footerLoading: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        paddingVertical: 20,
        gap: 8,
    },
    footerLoadingText: {
        fontSize: 14,
        color: '#666',
    },
    // Online users styles
    onlineSection: {
        backgroundColor: '#fff',
        paddingVertical: 16,
    },
    onlineUsersList: {
        paddingHorizontal: 16,
    },
    onlineUserItem: {
        alignItems: 'center',
        marginRight: 16,
        width: 60,
    },
    onlineAvatarContainer: {
        position: 'relative',
        marginBottom: 8,
    },
    onlineAvatar: {
        width: 70,
        height: 70,
        borderRadius: 35,
    },
    defaultOnlineAvatar: {
        width: 50,
        height: 50,
        borderRadius: 25,
        backgroundColor: '#f0f0f0',
        justifyContent: 'center',
        alignItems: 'center',
    },
    onlineIndicator: {
        position: 'absolute',
        bottom: 2,
        right: 2,
        width: 16,
        height: 16,
        borderRadius: 8,
        backgroundColor: '#4CAF50',
        borderWidth: 2,
        borderColor: '#fff',
    },
    onlineUserName: {
        fontSize: 12,
        color: '#666',
        textAlign: 'center',
    },
});
