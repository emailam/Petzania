import { StyleSheet, Text, View, ScrollView, TouchableOpacity, Alert } from 'react-native'
import { Image } from 'expo-image';
import React, { useState, useEffect } from 'react'
import { Ionicons, MaterialIcons } from '@expo/vector-icons'
import { acceptFriendRequest, cancelFriendRequest, getReceivedFriendRequests } from '@/services/friendsService'

export default function index() {
    const defaultImage = require('@/assets/images/AddPet/Pet Default Pic.png');

    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const fetchFriendRequests = async () => {
        try {
            setLoading(true);
            setError(null);

            const response = await getReceivedFriendRequests(0, 10);

            console.log('Friend requests response:', response);

            if (response && response.content) {
                const friendRequestNotifications = response.content.map((request, index) => ({
                    id: `friend_req_${request.id || Date.now() + index}`,
                    type: 'friend_request',
                    title: 'Friend Request',
                    message: `${request.sender?.username || 'Someone'} wants to be your friend`,
                    avatar: request.sender?.profilePictureURL || null,
                    time: formatTime(request.createdAt || new Date()),
                    isRead: false,
                    actionable: true,
                    requestId: request.requestId,
                    senderData: request.sender
                }));

                setNotifications(friendRequestNotifications);
                console.log(`Found ${friendRequestNotifications.length} friend requests`);
            } else {
                setNotifications([]);
                console.log('No friend requests found');
            }
        } catch (error) {
            console.error('Error fetching friend requests:', error);
            setError('Failed to load friend requests');
        } finally {
            setLoading(false);
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

        fetchFriendRequests();

        const interval = setInterval(() => {
            fetchFriendRequests();
        }, 30000);

        return () => clearInterval(interval);
    }, []);

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
                            // Call API to accept friend request
                            await acceptFriendRequest(notification.requestId);

                            // Remove notification from list
                            setNotifications(prev =>
                                prev.filter(n => n.id !== notification.id)
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
                            await cancelFriendRequest(notification.requestId);

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

    const handleNotificationPress = (notification) => {
        setNotifications(prev => 
            prev.map(n => 
                n.id === notification.id ? { ...n, isRead: true } : n
            )
        );

        // Handle different notification types
        switch (notification.type) {
            case 'message':
                Alert.alert('Open Chat', 'This would open the chat with the user');
                break;
            case 'like':
                Alert.alert('View Pet', 'This would open your pet profile');
                break;
            case 'follow':
                Alert.alert('View Profile', 'This would open the follower\'s profile');
                break;
            case 'adoption':
                Alert.alert('View Adoption', 'This would open adoption details');
                break;
            case 'system':
                Alert.alert('System', 'This would open the relevant system page');
                break;
            default:
                break;
        }
    };

    const getNotificationIcon = (type) => {
        switch (type) {
            case 'friend_request':
                return <Ionicons name="person-add" size={24} color="#007AFF" />;
            case 'message':
                return <Ionicons name="chatbubble" size={24} color="#34C759" />;
            case 'like':
                return <Ionicons name="heart" size={24} color="#FF3B30" />;
            case 'follow':
                return <Ionicons name="person-add-outline" size={24} color="#9188E5" />;
            case 'adoption':
                return <MaterialIcons name="pets" size={24} color="#FF9500" />;
            case 'system':
                return <Ionicons name="information-circle" size={24} color="#5AC8FA" />;
            default:
                return <Ionicons name="notifications" size={24} color="#9188E5" />;
        }
    };

    const unreadCount = notifications.filter(n => !n.isRead).length;

    return (
        <View style={styles.container}>
            {/* Unread count indicator */}
            {unreadCount > 0 && (
                <View style={styles.unreadIndicator}>
                    <Text style={styles.unreadIndicatorText}>{unreadCount} unread notifications</Text>
                </View>
            )}

            {/* Loading indicator */}
            {loading && notifications.length === 0 && (
                <View style={styles.loadingContainer}>
                    <Text style={styles.loadingText}>Loading notifications...</Text>
                </View>
            )}

            {/* Error message */}
            {error && (
                <View style={styles.errorContainer}>
                    <Text style={styles.errorText}>{error}</Text>
                    <TouchableOpacity onPress={fetchFriendRequests} style={styles.retryButton}>
                        <Text style={styles.retryButtonText}>Retry</Text>
                    </TouchableOpacity>
                </View>
            )}

            <ScrollView style={styles.notificationsList} showsVerticalScrollIndicator={false}>
                {notifications.map((notification) => (
                    <TouchableOpacity
                        key={notification.id}
                        style={[
                            styles.notificationItem,
                            !notification.isRead && styles.unreadNotification
                        ]}
                        onPress={() => handleNotificationPress(notification)}
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
                            {notification.actionable && notification.type === 'friend_request' && (
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

                {/* Empty state */}
                {notifications.length === 0 && (
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
    unreadIndicator: {
        backgroundColor: '#e3f2fd',
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderBottomWidth: 1,
        borderBottomColor: '#e0e0e0',
    },
    unreadIndicatorText: {
        fontSize: 14,
        color: '#1976d2',
        fontWeight: '500',
        textAlign: 'center',
    },
    loadingContainer: {
        padding: 20,
        alignItems: 'center',
    },
    loadingText: {
        fontSize: 16,
        color: '#666',
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
        backgroundColor: '#007AFF',
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