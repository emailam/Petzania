import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Animated, Dimensions } from 'react-native';
import { Image } from 'expo-image';
import { Ionicons, MaterialIcons } from '@expo/vector-icons';
import { useNotifications } from '@/context/NotificationContext';
import { useRouter } from 'expo-router';

const { width } = Dimensions.get('window');

const NotificationToast = () => {
    const { recentNotifications } = useNotifications();
    const [currentNotification, setCurrentNotification] = useState(null);
    const [slideAnim] = useState(new Animated.Value(-100));
    const router = useRouter();

    useEffect(() => {
        if (recentNotifications.length > 0) {
            const latestNotification = recentNotifications[0];
            showNotification(latestNotification);
        }
    }, [recentNotifications]);

    const showNotification = (notification) => {
        setCurrentNotification(notification);
        
        // Slide in animation
        Animated.sequence([
            Animated.timing(slideAnim, {
                toValue: 0,
                duration: 300,
                useNativeDriver: true,
            }),
            // Auto hide after 4 seconds
            Animated.delay(4000),
            Animated.timing(slideAnim, {
                toValue: -100,
                duration: 300,
                useNativeDriver: true,
            }),
        ]).start(() => {
            setCurrentNotification(null);
        });
    };

    const hideNotification = () => {
        Animated.timing(slideAnim, {
            toValue: -100,
            duration: 200,
            useNativeDriver: true,
        }).start(() => {
            setCurrentNotification(null);
        });
    };

    const handleNotificationPress = () => {
        hideNotification();
        
        // Navigate based on notification type
        if (currentNotification) {
            switch (currentNotification.type) {
                case 'FRIEND_REQUEST':
                case 'FRIEND_ACCEPTED':
                    router.push('/(drawer)/(stack)/Notifications');
                    break;
                case 'MESSAGE':
                    if (currentNotification.relatedEntityId) {
                        router.push({
                            pathname: '/(drawer)/(stack)/Chat/[chatId]',
                            params: { chatId: currentNotification.relatedEntityId }
                        });
                    }
                    break;
                case 'POST_LIKE':
                case 'POST_COMMENT':
                    if (currentNotification.relatedEntityId) {
                        router.push({
                            pathname: '/(drawer)/(stack)/Posts/[postId]',
                            params: { postId: currentNotification.relatedEntityId }
                        });
                    }
                    break;
                default:
                    router.push('/(drawer)/(stack)/Notifications');
                    break;
            }
        }
    };

    const getNotificationIcon = (type) => {
        switch (type) {
            case 'FRIEND_REQUEST':
                return <Ionicons name="person-add" size={20} color="#007AFF" />;
            case 'FRIEND_ACCEPTED':
                return <Ionicons name="people" size={20} color="#34C759" />;
            case 'MESSAGE':
                return <Ionicons name="chatbubble" size={20} color="#34C759" />;
            case 'POST_LIKE':
                return <Ionicons name="heart" size={20} color="#FF3B30" />;
            case 'POST_COMMENT':
                return <Ionicons name="chatbubble-outline" size={20} color="#007AFF" />;
            case 'PET_ADOPTION':
                return <MaterialIcons name="pets" size={20} color="#FF9500" />;
            case 'SYSTEM':
                return <Ionicons name="information-circle" size={20} color="#5AC8FA" />;
            default:
                return <Ionicons name="notifications" size={20} color="#9188E5" />;
        }
    };

    if (!currentNotification) {
        return null;
    }

    return (
        <Animated.View
            style={[
                styles.container,
                {
                    transform: [{ translateY: slideAnim }],
                }
            ]}
        >
            <TouchableOpacity
                style={styles.notification}
                onPress={handleNotificationPress}
                activeOpacity={0.9}
            >
                <View style={styles.content}>
                    <View style={styles.iconContainer}>
                        {getNotificationIcon(currentNotification.type)}
                    </View>
                    
                    <View style={styles.textContainer}>
                        <Text style={styles.title} numberOfLines={1}>
                            {getNotificationTitle(currentNotification.type)}
                        </Text>
                        <Text style={styles.message} numberOfLines={2}>
                            {currentNotification.message}
                        </Text>
                    </View>
                    
                    <TouchableOpacity
                        style={styles.closeButton}
                        onPress={hideNotification}
                    >
                        <Ionicons name="close" size={18} color="#666" />
                    </TouchableOpacity>
                </View>
            </TouchableOpacity>
        </Animated.View>
    );
};

const getNotificationTitle = (type) => {
    switch (type) {
        case 'FRIEND_REQUEST':
            return 'Friend Request';
        case 'FRIEND_ACCEPTED':
            return 'Friend Accepted';
        case 'MESSAGE':
            return 'New Message';
        case 'POST_LIKE':
            return 'Post Liked';
        case 'POST_COMMENT':
            return 'New Comment';
        case 'PET_ADOPTION':
            return 'Adoption Interest';
        case 'SYSTEM':
            return 'System Notification';
        default:
            return 'Notification';
    }
};

const styles = StyleSheet.create({
    container: {
        position: 'absolute',
        top: 50,
        left: 16,
        right: 16,
        zIndex: 1000,
        elevation: 1000,
    },
    notification: {
        backgroundColor: '#fff',
        borderRadius: 12,
        padding: 16,
        shadowColor: '#000',
        shadowOffset: {
            width: 0,
            height: 4,
        },
        shadowOpacity: 0.15,
        shadowRadius: 8,
        elevation: 8,
        borderLeftWidth: 4,
        borderLeftColor: '#9188E5',
    },
    content: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    iconContainer: {
        marginRight: 12,
        padding: 8,
        backgroundColor: '#f8f9ff',
        borderRadius: 20,
    },
    textContainer: {
        flex: 1,
        marginRight: 8,
    },
    title: {
        fontSize: 14,
        fontWeight: '600',
        color: '#333',
        marginBottom: 2,
    },
    message: {
        fontSize: 13,
        color: '#666',
        lineHeight: 18,
    },
    closeButton: {
        padding: 4,
    },
});

export default NotificationToast;
