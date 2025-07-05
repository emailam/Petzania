import { StyleSheet, Text, View, ScrollView, TouchableOpacity, Alert, RefreshControl } from 'react-native'
import { Image } from 'expo-image';
import React, { useState, useEffect, useCallback } from 'react'
import { Ionicons } from '@expo/vector-icons'
import { useRouter } from 'expo-router'
import { getAllNotifications, markNotificationAsRead, markAllNotificationsAsRead, deleteNotification } from '@/services/notificationService'
import { acceptFriendRequest, cancelFriendRequest } from '@/services/friendsService'
import { useNotifications } from '@/context/NotificationContext'
import { getUserById } from '@/services/userService';

export default function index() {
    const defaultImage = require('@/assets/images/Defaults/default-user.png');
    const router = useRouter();
    const { decrementUnreadCount, resetUnreadCount, recentNotifications } = useNotifications();

    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(false);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState(null);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

    const fetchNotifications = async (pageNum = 0, isRefresh = false) => {
        try {
            if (isRefresh) {
                setRefreshing(true);
            } else {
                setLoading(true);
            }
            setError(null);

            const response = await getAllNotifications(pageNum, 10);

            if (response && response?.content) {
                // Collect user IDs we need to fetch
                const userIdsToFetch = new Set();

                response.content.forEach(notification => {
                    if (notification.type === 'FRIEND_REQUEST_ACCEPTED') {
                        // For friend request accepted, we want the accepter's info (receiverId)
                        if (notification.attributes?.receiverId) {
                            userIdsToFetch.add(notification.attributes.receiverId);
                        }
                    } else {
                        // For other notifications, use senderId
                        if (notification.attributes?.senderId) {
                            userIdsToFetch.add(notification.attributes.senderId);
                        }
                    }
                });

                const uniqueUserIds = [...userIdsToFetch];
                const userResults = await Promise.all(uniqueUserIds.map(id => getUserById(id)));
                const userMap = {};
                uniqueUserIds.forEach((id, idx) => {
                    userMap[id] = userResults[idx];
                });

                const formattedNotifications = response.content.map((notification) => {
                    let relevantUserId;
                    let senderUser;

                    if (notification.type === 'FRIEND_REQUEST_ACCEPTED') {
                        // Show the person who accepted the request (receiverId)
                        relevantUserId = notification.attributes?.receiverId;
                        senderUser = relevantUserId ? userMap[relevantUserId] : null;
                    } else {
                        // For other notifications, show the sender
                        relevantUserId = notification.attributes?.senderId;
                        senderUser = relevantUserId ? userMap[relevantUserId] : null;
                    }

                    return {
                        id: notification.notificationId,
                        type: notification.type,
                        title: getNotificationTitle(notification.type),
                        message: notification.message,
                        avatar: senderUser?.profilePictureURL || null,
                        time: formatTime(notification.createdAt),
                        isRead: notification.status === 'READ',
                        actionable: notification.type === 'FRIEND_REQUEST_RECEIVED' && notification.status === 'UNREAD',
                        requestId: notification.attributes?.friendRequestId,
                        senderData: senderUser,
                        attributes: notification.attributes || {}
                    };
                });

                if (isRefresh || pageNum === 0) {
                    setNotifications(formattedNotifications);
                } else {
                    setNotifications(prev => [...prev, ...formattedNotifications]);
                }

                setHasMore(response.content.length === 10);
                setPage(pageNum);
            } else {
                if (isRefresh || pageNum === 0) {
                    setNotifications([]);
                }
                setHasMore(false);
            }
        } catch (error) {
            console.error('Error fetching notifications:', error);
            setError('Failed to load notifications');
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    const getNotificationTitle = (type) => {
        switch (type) {
            case 'FRIEND_REQUEST_RECEIVED':
                return 'Friend Request';
            case 'FRIEND_REQUEST_ACCEPTED':
                return 'Friend Accepted';
            case 'NEW_FOLLOWER':
                return 'New Follower';
            case 'PET_POST_LIKED':
                return 'Post Liked';
            default:
                return 'Notification';
        }
    };

    // Format time helper function
    const formatTime = (dateString) => {
        if (!dateString) return 'Just now';

        try {
            const date = new Date(dateString);
            const now = new Date();
            const diffInMinutes = Math.floor((now - date) / (1000 * 60));

            if (diffInMinutes < 1) return 'Just now';
            if (diffInMinutes < 60) return `${diffInMinutes} minute${diffInMinutes > 1 ? 's' : ''} ago`;

            const diffInHours = Math.floor(diffInMinutes / 60);
            if (diffInHours < 24) return `${diffInHours} hour${diffInHours > 1 ? 's' : ''} ago`;

            const diffInDays = Math.floor(diffInHours / 24);
            return `${diffInDays} day${diffInDays > 1 ? 's' : ''} ago`;
        } catch (error) {
            return 'Recently';
        }
    };


    useEffect(() => {
        fetchNotifications(0, false);
    }, []);

    // Listen for real-time notifications and prepend to the list
    useEffect(() => {
        if (!recentNotifications || recentNotifications.length === 0) return;
        // Only handle the most recent notification
        const newNotification = recentNotifications[0];
        if (!newNotification) return;
        // Prevent duplicates
        if (notifications.some(n => n.id === newNotification.notificationId)) return;
        (async () => {
            let senderUser = null;
            let relevantUserId;

            if (newNotification.type === 'FRIEND_REQUEST_ACCEPTED') {
                // For friend request accepted, get the accepter's info (receiverId)
                relevantUserId = newNotification.attributes?.receiverId;
            } else {
                // For other notifications, get the sender's info
                relevantUserId = newNotification.attributes?.senderId;
            }

            if (relevantUserId) {
                try {
                    senderUser = await getUserById(relevantUserId);
                } catch (e) {
                    senderUser = null;
                }
            }
            const formatted = {
                id: newNotification.notificationId,
                type: newNotification.type,
                title: getNotificationTitle(newNotification.type),
                message: newNotification.message,
                avatar: senderUser?.profilePictureURL || null,
                time: formatTime(newNotification.createdAt),
                isRead: newNotification.status === 'READ',
                actionable: newNotification.type === 'FRIEND_REQUEST_RECEIVED' && newNotification.status === 'UNREAD',
                requestId: newNotification.attributes?.friendRequestId,
                senderData: senderUser,
                attributes: newNotification.attributes || {}
            };
            setNotifications(prev => [formatted, ...prev]);
        })();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [recentNotifications]);

    const onRefresh = useCallback(() => {
        fetchNotifications(0, true);
    }, []);

    const loadMore = () => {
        if (!loading && hasMore) {
            fetchNotifications(page + 1, false);
        }
    };

    const markAsRead = async (notification) => {
        if (notification.isRead) return;

        try {
            await markNotificationAsRead(notification.id);
            setNotifications(prev => 
                prev.map(n => 
                    n.id === notification.id ? { ...n, isRead: true } : n
                )
            );
            decrementUnreadCount(1);
        } catch (error) {
            console.error('Error marking notification as read:', error);
        }
    };

    const markAllAsRead = async () => {
        Alert.alert(
            'Mark All as Read',
            `Mark all ${localUnreadCount} notification${localUnreadCount > 1 ? 's' : ''} as read?`,
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Mark All Read',
                    onPress: async () => {
                        try {
                            await markAllNotificationsAsRead();
                            setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
                            resetUnreadCount();
                        } catch (error) {
                            console.error('Error marking all notifications as read:', error);
                            Alert.alert('Error', 'Failed to mark all notifications as read');
                        }
                    }
                }
            ]
        );
    };

    const deleteNotificationItem = async (notificationId) => {
        Alert.alert(
            'Delete Notification',
            'Are you sure you want to delete this notification?',
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Delete',
                    style: 'destructive',
                    onPress: async () => {
                        try {
                            await deleteNotification(notificationId);
                            const deletedNotification = notifications.find(n => n.id === notificationId);
                            setNotifications(prev => prev.filter(n => n.id !== notificationId));
                            
                            // Decrement unread count if deleted notification was unread
                            if (deletedNotification && !deletedNotification.isRead) {
                                decrementUnreadCount(1);
                            }
                        } catch (error) {
                            console.error('Error deleting notification:', error);
                            Alert.alert('Error', 'Failed to delete notification');
                        }
                    }
                }
            ]
        );
    };

    const handleAcceptFriend = async (notification) => {
        Alert.alert(
            'Accept Friend Request',
            `Are you sure you want to accept ${notification.senderData?.username || 'this user'}'s friend request?`,
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Accept',
                    onPress: async () => {
                        try {
                            await acceptFriendRequest(notification.attributes?.requestId);
                            
                            // Mark notification as read and remove actionable state
                            await markNotificationAsRead(notification.id);
                            setNotifications(prev =>
                                prev.map(n => 
                                    n.id === notification.id 
                                        ? { ...n, isRead: true, actionable: false, title: 'Friend Accepted', message: `You and ${notification.senderData?.username || 'this user'} are now friends!` }
                                        : n
                                )
                            );

                            Alert.alert('Success', 'Friend request accepted!');
                        } catch (error) {
                            console.error('Error accepting friend request:', error);
                            Alert.alert('Error', 'Failed to accept friend request. Please try again.');
                        }
                    }
                }
            ]
        );
    };

    const handleRejectFriend = async (notification) => {
        Alert.alert(
            'Reject Friend Request',
            `Are you sure you want to reject ${notification.senderData?.username || 'this user'}'s friend request?`,
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Reject',
                    style: 'destructive',
                    onPress: async () => {
                        try {
                            await cancelFriendRequest(notification.attributes?.requestId);

                            await markNotificationAsRead(notification.id);

                            // Remove the notification entirely for rejected requests
                            setNotifications(prev => 
                                prev.filter(n => n.id !== notification.id)
                            );

                            Alert.alert('Rejected', 'Friend request rejected');
                        } catch (error) {
                            console.error('Error rejecting friend request:', error);
                            Alert.alert('Error', 'Failed to reject friend request. Please try again.');
                        }
                    }
                }
            ]
        );
    };

    const handleNotificationPress = async (notification) => {
        // Mark as read
        await markAsRead(notification);
        console.log('Notification type:', notification);

        // Handle different notification types with navigation
        switch (notification.type) {
            case 'FRIEND_REQUEST_RECEIVED':
                router.push({
                    pathname: `/UserModule/${notification.senderData?.userId}`,
                    params: { username: notification.senderData.username }
                });
                break;
            case 'FRIEND_REQUEST_ACCEPTED':
                // Navigate to friends list or user profile
                router.push(`/(drawer)/(stack)/Friends`);
                break;
            case 'NEW_FOLLOWER':
                router.push({
                    pathname: `/UserModule/${notification.senderData?.userId}`,
                    params: { username: notification.senderData.username }
                });
                break;
            case 'PET_POST_LIKED':
                console.log('Post ID:', notification.attributes?.postId);
                
            default:
                // Generic fallback
                Alert.alert(notification.title, notification.message);
                break;
        }
    };

    const getNotificationIcon = (type) => {
        switch (type) {
            case 'FRIEND_REQUEST_RECEIVED':
                return <Ionicons name="person-add" size={24} color="#007AFF" />;
            case 'FRIEND_REQUEST_ACCEPTED':
                return <Ionicons name="people" size={24} color="#34C759" />;
            case 'NEW_FOLLOWER':
                return <Ionicons name="person-add" size={24} color="#34C759" />;
            case 'PET_POST_LIKED':
                return <Ionicons name="heart" size={24} color="#FF3B30" />;
            default:
                return <Ionicons name="notifications" size={24} color="#9188E5" />;
        }
    };

    const localUnreadCount = notifications.filter(n => !n.isRead).length;

    return (
        <View style={styles.container}>
            <ScrollView
                style={styles.notificationsList}
                showsVerticalScrollIndicator={false}
                refreshControl={
                    <RefreshControl
                        refreshing={refreshing}
                        onRefresh={onRefresh}
                        colors={['#9188E5']}
                        tintColor="#9188E5"
                    />
                }
                onScroll={({ nativeEvent }) => {
                    const { layoutMeasurement, contentOffset, contentSize } = nativeEvent;
                    const paddingToBottom = 20;
                    if (layoutMeasurement.height + contentOffset.y >= 
                        contentSize.height - paddingToBottom) {
                        loadMore();
                    }
                }}
                scrollEventThrottle={400}
            >
                {/* Content padding for floating header */}
                {/* Floating Header */}
                <View style={styles.floatingHeader}>
                    <View style={styles.headerContent}>
                        <Text style={styles.headerTitle}>Notifications</Text>
                        {localUnreadCount > 0 && (
                            <TouchableOpacity onPress={markAllAsRead} style={styles.markAllButton}>
                                <Ionicons name="checkmark-done" size={16} color="#fff" />
                                <Text style={styles.markAllButtonText}>Mark all read</Text>
                            </TouchableOpacity>
                        )}
                    </View>
                </View>
                <View style={styles.headerSpacer} />

                {/* Unread count indicator */}
                {localUnreadCount > 0 && (
                    <View style={styles.unreadIndicator}>
                        <View style={styles.unreadIndicatorContent}>
                            <Ionicons name="notifications" size={16} color="#9188E5" />
                            <Text style={styles.unreadIndicatorText}>
                                {localUnreadCount} unread notification{localUnreadCount > 1 ? 's' : ''}
                            </Text>
                        </View>
                    </View>
                )}

                {/* Error message */}
                {error && (
                    <View style={styles.errorContainer}>
                        <Text style={styles.errorText}>{error}</Text>
                        <TouchableOpacity onPress={() => fetchNotifications(0, true)} style={styles.retryButton}>
                            <Text style={styles.retryButtonText}>Retry</Text>
                        </TouchableOpacity>
                    </View>
                )}

                {notifications.map((notification) => (
                    <TouchableOpacity
                        key={notification.id}
                        style={[
                            styles.notificationItem,
                            !notification.isRead && styles.unreadNotification
                        ]}
                        onPress={() => handleNotificationPress(notification)}
                        onLongPress={() => deleteNotificationItem(notification.id)}
                    >
                        <View style={styles.notificationContent}>
                            {/* Avatar */}
                            <View style={styles.avatarContainer}>
                                <Image
                                    source={notification.avatar || defaultImage}
                                    style={styles.avatar}
                                />
                                <View style={styles.iconBadge}>
                                    {getNotificationIcon(notification.type)}
                                </View>
                            </View>

                            {/* Content */}
                            <View style={styles.textContent}>
                                <Text style={styles.notificationTitle}>{notification.title}</Text>
                                <Text style={styles.notificationMessage}>{notification.message}</Text>
                                <Text style={styles.notificationTime}>{notification.time}</Text>
                            </View>

                            {/* Actions for friend requests */}
                            {notification.actionable && notification.type === 'FRIEND_REQUEST_RECEIVED' && (
                                <View style={styles.actionButtons}>
                                    <TouchableOpacity
                                        style={styles.acceptButton}
                                        onPress={() => handleAcceptFriend(notification)}
                                    >
                                        <Ionicons name="checkmark" size={16} color="#fff" />
                                    </TouchableOpacity>
                                    <TouchableOpacity
                                        style={styles.rejectButton}
                                        onPress={() => handleRejectFriend(notification)}
                                    >
                                        <Ionicons name="close" size={16} color="#fff" />
                                    </TouchableOpacity>
                                </View>
                            )}

                            {/* Read indicator */}
                            {!notification.isRead && (
                                <View style={styles.unreadDot} />
                            )}
                        </View>
                    </TouchableOpacity>
                ))}

                {/* Loading more indicator */}
                {loading && notifications.length > 0 && (
                    <View style={styles.loadingMore}>
                        <Text style={styles.loadingMoreText}>Loading more...</Text>
                    </View>
                )}

                {/* Empty state */}
                {notifications.length === 0 && !loading && (
                    <View style={styles.emptyState}>
                        <Ionicons name="notifications-off" size={64} color="#ccc" />
                        <Text style={styles.emptyTitle}>No notifications</Text>
                        <Text style={styles.emptyMessage}>You're all caught up!</Text>
                    </View>
                )}
            </ScrollView>
        </View>
    )
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    header: {
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#f0f0f0',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.1,
        shadowRadius: 2,
        elevation: 2,
    },
    floatingHeader: {
        flex: 1,
        justifyContent: 'flex-end',
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#f0f0f0',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 5,
        zIndex: 10,
    },
    headerContent: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingVertical: 16,
        paddingHorizontal: 20,
    },
    headerTitle: {
        fontSize: 24,
        fontWeight: '700',
        color: '#333',
    },
    markAllButton: {
        backgroundColor: '#9188E5',
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 20,
        shadowColor: '#9188E5',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.3,
        shadowRadius: 4,
        elevation: 4,
    },
    markAllButtonText: {
        color: '#fff',
        fontSize: 13,
        fontWeight: '600',
        marginLeft: 6,
    },
    unreadIndicator: {
        backgroundColor: 'rgba(145, 136, 229, 0.1)',
        borderBottomWidth: 1,
        borderBottomColor: '#e0e0e0',
    },
    unreadIndicatorContent: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 16,
        paddingVertical: 12,
    },
    unreadIndicatorText: {
        fontSize: 14,
        color: '#9188E5',
        fontWeight: '500',
        marginLeft: 8,
    },
    errorContainer: {
        padding: 20,
        alignItems: 'center',
        backgroundColor: '#ffebee',
        marginHorizontal: 16,
        marginVertical: 8,
        borderRadius: 8,
    },
    errorText: {
        fontSize: 14,
        color: '#d32f2f',
        marginBottom: 8,
        textAlign: 'center',
    },
    retryButton: {
        backgroundColor: '#9188E5',
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 6,
    },
    retryButtonText: {
        color: '#fff',
        fontSize: 14,
        fontWeight: '600',
    },
    notificationsList: {
        flex: 1,
    },
    notificationItem: {
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#f0f0f0',
    },
    unreadNotification: {
        backgroundColor: '#f8f9ff',
    },
    notificationContent: {
        flexDirection: 'row',
        padding: 16,
        alignItems: 'flex-start',
        position: 'relative',
    },
    avatarContainer: {
        position: 'relative',
        marginRight: 12,
    },
    avatar: {
        width: 50,
        height: 50,
        borderRadius: 25,
        borderWidth: 1,
        borderColor: '#9188E5',
        backgroundColor: '#f0f0f0',
    },
    iconBadge: {
        position: 'absolute',
        bottom: -5,
        right: -5,
        backgroundColor: '#fff',
        borderRadius: 15,
        padding: 3,
        borderWidth: 2,
        borderColor: '#fff',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.2,
        shadowRadius: 2,
        elevation: 3,
    },
    textContent: {
        flex: 1,
        marginRight: 10,
    },
    notificationTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
        marginBottom: 4,
    },
    notificationMessage: {
        fontSize: 14,
        color: '#666',
        lineHeight: 20,
        marginBottom: 6,
    },
    notificationTime: {
        fontSize: 12,
        color: '#999',
    },
    actionButtons: {
        flexDirection: 'row',
        gap: 8,
    },
    acceptButton: {
        backgroundColor: '#34C759',
        width: 32,
        height: 32,
        borderRadius: 16,
        justifyContent: 'center',
        alignItems: 'center',
    },
    rejectButton: {
        backgroundColor: '#FF3B30',
        width: 32,
        height: 32,
        borderRadius: 16,
        justifyContent: 'center',
        alignItems: 'center',
    },
    unreadDot: {
        position: 'absolute',
        top: 16,
        right: 16,
        width: 8,
        height: 8,
        borderRadius: 4,
        backgroundColor: '#9188E5',
    },
    loadingMore: {
        padding: 16,
        alignItems: 'center',
    },
    loadingMoreText: {
        fontSize: 14,
        color: '#666',
    },
    emptyState: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        paddingTop: 100,
    },
    emptyTitle: {
        fontSize: 18,
        fontWeight: '600',
        color: '#333',
        marginTop: 16,
        marginBottom: 8,
    },
    emptyMessage: {
        fontSize: 14,
        color: '#666',
        textAlign: 'center',
    },
})