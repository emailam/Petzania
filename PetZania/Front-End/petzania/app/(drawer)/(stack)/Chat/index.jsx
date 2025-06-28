import React, { useState, useEffect, useContext } from 'react';
import {
    View,
    Text,
    StyleSheet,
    FlatList,
    TouchableOpacity,
    ActivityIndicator,
    RefreshControl,
} from 'react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { UserContext } from '@/context/UserContext';
import { getAllChats, getMessagesByChatId, updateMessageStatus } from '@/services/chatService';
import { getUserById } from '@/services/userService';
import { getFriendsByUserId } from '@/services/friendsService';
import Toast from 'react-native-toast-message';


export default function ChatIndex() {
    const router = useRouter();
    const { user: currentUser } = useContext(UserContext);
    const [chats, setChats] = useState([]);
    const [onlineUsers, setOnlineUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);

    useEffect(() => {
        loadInitialChats();
        loadOnlineUsers();
    }, []);

    const loadInitialChats = async () => {
        try {
            setLoading(true);
            const response = await getAllChats();

            // The response should be an array of chats
            const chatsData = Array.isArray(response) ? response : [];
            // Enrich each chat with user details and last message
            const enrichedChats = await Promise.all(
                chatsData.map(async (chat) => {
                    try {
                        // Get the other user's ID
                        const otherUserId = chat.user1Id === currentUser?.userId ? chat.user2Id : chat.user1Id;

                        // Fetch the other user's details and last message in parallel
                        const [otherUser, messagesResponse] = await Promise.all([
                            getUserById(otherUserId),
                            getMessagesByChatId(chat.chatId, 0, 10)
                        ]);

                        // Extract the messages and check for unread ones
                        const messages = Array.isArray(messagesResponse) ? messagesResponse : (messagesResponse?.content || []);
                        const lastMessage = messages.length > 0 ? messages[0] : null;

                        // Check if there are unread messages from the other user
                        const unreadMessages = messages.filter(msg =>
                            msg.senderId !== currentUser?.userId && // Message from other user
                            msg.status === 'SENT' // Message status is SENT (unread)
                        );
                        const hasUnreadMessages = unreadMessages.length > 0;

                        return {
                            ...chat,
                            otherUser: otherUser,
                            lastMessage: lastMessage,
                            hasUnreadMessages: hasUnreadMessages,
                            unreadCount: unreadMessages.length
                        };
                    } catch (error) {
                        console.warn('Failed to fetch user details for chat:', chat.chatId);
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
            setChats(enrichedChats);
        } catch (error) {
            console.error('Error loading chats:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to load chats',
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
    };    const handleChatPress = async (chat) => {
        try {
            // Mark unread messages as read before navigating
            if (chat.hasUnreadMessages) {
                // Get the last 10 messages to mark as read
                const messagesResponse = await getMessagesByChatId(chat.chatId, 0, 10);
                const messages = Array.isArray(messagesResponse) ? messagesResponse : (messagesResponse?.content || []);
                
                // Filter messages that are not from current user and are unread (SENT status)
                const unreadMessages = messages.filter(msg => 
                    msg.senderId !== currentUser?.userId && 
                    msg.status === 'SENT'
                );

                // Mark each unread message as READ
                const markAsReadPromises = unreadMessages.map(async (message) => {
                    try {
                        await updateMessageStatus(message.messageId, 'READ');
                        console.log(`Marked message ${message.messageId} as READ`);
                    } catch (error) {
                        console.warn(`Failed to mark message ${message.messageId} as read:`, error);
                    }
                });

                // Wait for all messages to be marked as read
                await Promise.all(markAsReadPromises);

                // Update the chat state to reflect the changes
                setChats(prevChats => 
                    prevChats.map(c => 
                        c.chatId === chat.chatId 
                            ? { ...c, hasUnreadMessages: false, unreadCount: 0 }
                            : c
                    )
                );
            }
        } catch (error) {
            console.error('Error marking messages as read:', error);
            // Continue to navigation even if marking as read fails
        }

        // Navigate to the chat
        router.push(`/Chat/${chat.chatId}`);
    };

    const handleOnlineUserPress = async (user) => {
        try {
            // Check if a chat already exists with this user
            const existingChat = chats.find(chat => 
                chat.otherUser?.userId === user.userId
            );

            if (existingChat) {
                router.push(`/Chat/${existingChat.chatId}`);
            } else {
                // Create new chat or navigate to user profile
                router.push(`/UserModule/${user.userId}`);
            }
        } catch (error) {
            console.error('Error handling online user press:', error);
        }
    };

    const renderChatItem = ({ item }) => {
        const otherUser = item.otherUser;
        const lastMessage = item.lastMessage;

        return (
            <TouchableOpacity
                style={[
                    styles.chatItem,
                    item.hasUnreadMessages && styles.chatItemUnread
                ]}
                onPress={() => handleChatPress(item)}
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
                    {item.hasUnreadMessages && (
                        <View style={styles.unreadDot} />
                    )}
                </View>

                <View style={styles.chatContent}>
                    <View style={styles.chatHeader}>
                        <Text style={styles.userName} numberOfLines={1}>
                            {otherUser?.name || 'Unknown User'}
                        </Text>
                        <Text style={styles.timestamp}>
                            {lastMessage ? formatTime(lastMessage.sentAt) : formatTime(item.createdAt)}
                        </Text>
                    </View>

                    <View style={styles.messagePreview}>
                        <Text style={styles.lastMessage} numberOfLines={1}>
                            {lastMessage ? 
                                (lastMessage.senderId === currentUser?.userId ? 
                                    `You: ${lastMessage.content}` : 
                                    lastMessage.content
                                ) : 
                                'Start a conversation'
                            }
                        </Text>
                        {item.hasUnreadMessages && item.unreadCount > 0 && (
                            <View style={styles.unreadBadge}>
                                <Text style={styles.unreadCount}>
                                    {item.unreadCount > 9 ? '10+' : item.unreadCount}
                                </Text>
                            </View>
                        )}
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
            <Ionicons name="chatbubbles-outline" size={60} color="#ccc" />
            <Text style={styles.emptyTitle}>No Conversations Yet</Text>
            <Text style={styles.emptySubtitle}>
                Start a conversation by visiting someone's profile and tapping Message
            </Text>
        </View>
    );

    if (loading) {
        return (
            <View style={styles.container}>
                <View style={styles.loadingContainer}>
                    <ActivityIndicator size="large" color="#9188E5" />
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
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: 40,
        paddingTop: 100,
    },
    emptyTitle: {
        fontSize: 18,
        fontWeight: '600',
        color: '#999',
        marginTop: 16,
        textAlign: 'center',
    },
    emptySubtitle: {
        fontSize: 14,
        color: '#999',
        marginTop: 8,
        textAlign: 'center',
        lineHeight: 20,
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
