import { StyleSheet, Text, View, ScrollView, TouchableOpacity, Alert, RefreshControl } from 'react-native'
import { Image } from 'expo-image';
import React, { useState, useEffect, useCallback, useRef } from 'react'

import { Ionicons } from '@expo/vector-icons';
import Entypo from '@expo/vector-icons/Entypo';

import { useRouter } from 'expo-router'
import { getAllNotifications, markNotificationAsRead, markAllNotificationsAsRead, deleteNotification } from '@/services/notificationService'
import { acceptFriendRequest, cancelFriendRequest } from '@/services/friendsService'
import { useNotifications } from '@/context/NotificationContext'
import { getUserById } from '@/services/userService';
import EmptyState from '@/components/EmptyState';
import BottomSheet from '@/components/BottomSheet';
import { useInfiniteQuery } from '@tanstack/react-query';

import LoadingIndicator from '@/components/LoadingIndicator';

export default function index() {
    const defaultImage = require('@/assets/images/Defaults/default-user.png');
    const router = useRouter();
    const { decrementUnreadCount, resetUnreadCount } = useNotifications();

    const [selectedNotification, setSelectedNotification] = useState(null);

    // Bottom sheet ref
    const bottomSheetRef = useRef(null);

    // useInfiniteQuery for notifications
    const {
        data,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
        isLoading,
        isError,
        error: fetchError,
        refetch,
    } = useInfiniteQuery({
        queryKey: ['notifications'],
        queryFn: async ({ pageParam = 0 }) => {
            const response = await getAllNotifications(pageParam, 10);

            if (response && response?.content) {
                // Collect user IDs we need to fetch
                const userIdsToFetch = new Set();

                response.content.forEach(notification => {
                    if (notification.type === 'FRIEND_REQUEST_ACCEPTED') {
                        if (notification.recipientId) {
                            userIdsToFetch.add(notification.recipientId);
                        }
                    } else {
                        if (notification.initiatorId) {
                            userIdsToFetch.add(notification.initiatorId);
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
                        relevantUserId = notification.recipientId;
                        senderUser = relevantUserId ? userMap[relevantUserId] : null;
                    } else {
                        relevantUserId = notification.initiatorId;
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
                        requestId: notification.entityId,
                        senderData: senderUser,
                    };
                });

                return {
                    content: formattedNotifications,
                    currentPage: pageParam,
                    hasMore: response.content.length === 10,
                };
            }

            return {
                content: [],
                currentPage: pageParam,
                hasMore: false,
            };
        },
        getNextPageParam: (lastPage) => {
            return lastPage.hasMore ? lastPage.currentPage + 1 : undefined;
        },
        staleTime: 30000, // 30 seconds
    });

    // Flatten all notifications from pages
    const notifications = data?.pages?.flatMap(page => page.content) || [];

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

    // Handle refresh
    const onRefresh = useCallback(async () => {
        await refetch();
    }, [refetch]);

    // Handle load more
    const loadMore = useCallback(() => {
        if (hasNextPage && !isFetchingNextPage) {
            fetchNextPage();
        }
    }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

    const markAsRead = async (notification) => {
        if (notification.isRead) return;

        try {
            await markNotificationAsRead(notification.id);
            // Optimistically update the cache
            refetch();
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
                            refetch();
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

    const handleNotificationLongPress = async (notification) => {
        setSelectedNotification(notification);
        bottomSheetRef.current?.present();
    };

    const handleMarkAsRead = async () => {
        if (!selectedNotification) return;

        try {
            await markNotificationAsRead(selectedNotification.id);
            refetch();
            decrementUnreadCount(1);
            bottomSheetRef.current?.dismiss();
            setSelectedNotification(null);
        } catch (error) {
            console.error('Error marking notification as read:', error);
            Alert.alert('Error', 'Failed to mark notification as read');
        }
    };

    const handleDeleteNotification = async () => {
        if (!selectedNotification) return;

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
                            await deleteNotification(selectedNotification.id);
                            refetch();
                            if (!selectedNotification.isRead) {
                                decrementUnreadCount(1);
                            }
                            bottomSheetRef.current?.dismiss();
                            setSelectedNotification(null);
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
                            await acceptFriendRequest(notification.requestId);
                            await markNotificationAsRead(notification.id);
                            refetch();
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
                            await cancelFriendRequest(notification.requestId);
                            await markNotificationAsRead(notification.id);
                            refetch();
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
                console.log('Post ID:', notification.postId);
                
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

    // Show loading state
    if (isLoading) {
        return (
            <View style={[styles.container, { justifyContent: 'center', alignItems: 'center' }]}>
                <LoadingIndicator text="Loading notifications..." />
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <ScrollView
                style={styles.notificationsList}
                showsVerticalScrollIndicator={false}
                refreshControl={
                    <RefreshControl
                        refreshing={false}
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
                <View style={styles.headerSpacer} />

                {/* Unread count indicator */}
                {localUnreadCount > 0 && (
                    <View style={styles.unreadIndicator}>
                        <View style={styles.unreadIndicatorContent}>
                            <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                                <Ionicons name="notifications" size={16} color="#9188E5" />
                                <Text style={styles.unreadIndicatorText}>
                                    {localUnreadCount} unread notification{localUnreadCount > 1 ? 's' : ''}
                                </Text>
                            </View>
                            <TouchableOpacity onPress={markAllAsRead} style={styles.markAllButton}>
                                <Ionicons name="checkmark-done" size={16} color="#fff" />
                                <Text style={styles.markAllButtonText}>Mark all read</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                )}

                {/* Error message */}
                {isError && (
                    <View style={styles.errorContainer}>
                        <Text style={styles.errorText}>{fetchError?.message || 'Failed to load notifications'}</Text>
                        <TouchableOpacity onPress={() => refetch()} style={styles.retryButton}>
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
                        onLongPress={() => handleNotificationLongPress(notification)}
                    >
                        <View style={styles.notificationContent} >
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

                            {/* Three dots button */}
                            {!notification.actionable && <View style={styles.moreOptionsContainer}>
                                <TouchableOpacity
                                    onPress={() => handleNotificationLongPress(notification)}
                                    style={styles.moreOptionsButton}
                                    activeOpacity={0.7}
                                >
                                    <Entypo name="dots-three-horizontal" size={20} color="#9188E5" />
                                </TouchableOpacity>
                            </View>
                            }

                            {/* Read indicator */}
                            {!notification.isRead && (
                                <View style={styles.unreadDot} />
                            )}
                        </View>
                    </TouchableOpacity>
                ))}

                {/* Loading more indicator */}
                {isFetchingNextPage && notifications.length > 0 && (
                    <View style={styles.loadingMore}>
                        <LoadingIndicator text="Loading more..." />
                    </View>
                )}

                {/* Empty state */}
                {notifications.length === 0 && !isLoading && (
                    <View style={styles.emptyState}>
                        <EmptyState
                            iconName="notifications-off"
                            title="No notifications"
                            subtitle="You're all caught up!"
                            iconColor='#ccc'
                        />
                    </View>
                )}
            </ScrollView>

            {/* Bottom Sheet Modal for notification actions */}
            <BottomSheet
                ref={bottomSheetRef}
                index={0}
                snapPoints={['30%']}
            >
                <View style={styles.bottomSheetHeader}>
                    <Text style={styles.bottomSheetTitle}>Notification Actions</Text>
                </View>

                <View style={styles.bottomSheetActions}>
                    {selectedNotification && !selectedNotification.isRead && (
                        <TouchableOpacity
                            style={styles.bottomSheetAction}
                            onPress={handleMarkAsRead}
                        >
                            <View style={styles.actionIconContainer}>
                                <Ionicons name="checkmark-circle" size={24} color="#9188E5" />
                            </View>
                            <View style={styles.actionTextContainer}>
                                <Text style={styles.actionText}>Mark as Read</Text>
                            </View>
                        </TouchableOpacity>
                    )}

                    <TouchableOpacity
                        style={styles.bottomSheetAction}
                        onPress={handleDeleteNotification}
                    >
                        <View style={styles.actionIconContainer}>
                            <Ionicons name="trash-outline" size={24} color="#FF3B30" />
                        </View>
                        <View style={styles.actionTextContainer}>
                            <Text style={[styles.actionText, styles.removeText]}>Remove Notification</Text>
                        </View>
                    </TouchableOpacity>
                </View>
            </BottomSheet>
        </View>
    )
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#F9FAFB',
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
        justifyContent: 'center',
        alignItems: 'flex-end',
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#f0f0f0',
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
        justifyContent: 'space-between',
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
        flex: 1,
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#f0f0f0',
    },
    unreadNotification: {
        backgroundColor: '#f8f9ff',
    },
    notificationContent: {
        flex: 1,
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
        marginRight: 50, // Increased to make room for absolutely positioned three dots button
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
        right: 10,
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
        paddingVertical: 20,
    },
    // Bottom Sheet Styles
    bottomSheetContent: {
        flex: 1,
        paddingHorizontal: 20,
    },
    bottomSheetHeader: {
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#f0f0f0',
        marginBottom: 8,
    },
    bottomSheetTitle: {
        fontSize: 18,
        fontWeight: '600',
        color: '#333',
        textAlign: 'center',
    },
    bottomSheetActions: {
        flex: 1,
    },
    bottomSheetAction: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 8,
        borderRadius: 12,
        marginBottom: 8,
    },
    actionIconContainer: {
        width: 40,
        height: 40,
        borderRadius: 20,
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    actionTextContainer: {
        flex: 1,
    },
    actionText: {
        fontSize: 16,
        fontWeight: '500',
        color: '#333',
    },
    removeText: {
        color: '#FF3B30',
    },
    moreOptionsButton: {
        padding: 8,
        borderRadius: 20,
        justifyContent: 'center',
        alignItems: 'center',
    },
})