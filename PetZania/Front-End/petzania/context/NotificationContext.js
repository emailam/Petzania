import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { getUnreadNotificationCount } from '@/services/notificationService';
import notificationStompService from '@/services/notificationStompService';
import { UserContext } from '@/context/UserContext';
import { getToken } from '@/storage/tokenStorage';

const NotificationContext = createContext();

export const useNotifications = () => {
    const context = useContext(NotificationContext);
    if (!context) {
        throw new Error('useNotifications must be used within a NotificationProvider');
    }
    return context;
};

export const NotificationProvider = ({ children }) => {
    const [unreadCount, setUnreadCount] = useState(0);
    const [loading, setLoading] = useState(false);
    const [connected, setConnected] = useState(false);
    const [recentNotifications, setRecentNotifications] = useState([]);
    const { user } = useContext(UserContext);

    const fetchUnreadCount = useCallback(async () => {
        if (!user?.name) {
            return ;
        }
        try {
            setLoading(true);
            const count = await getUnreadNotificationCount();
            setUnreadCount(count || 0);
        } catch (error) {
            console.error('Error fetching unread count:', error);
            setUnreadCount(0);
        } finally {
            setLoading(false);
        }
    }, [user?.name]);

    const decrementUnreadCount = useCallback((amount = 1) => {
        setUnreadCount(prev => Math.max(0, prev - amount));
    }, []);

    const incrementUnreadCount = useCallback((amount = 1) => {
        setUnreadCount(prev => prev + amount);
    }, []);

    const resetUnreadCount = useCallback(() => {
        setUnreadCount(0);
    }, []);

    const addRecentNotification = useCallback((notification) => {
        setRecentNotifications(prev => [notification, ...prev.slice(0, 9)]); // Keep last 10
    }, []);

    const clearRecentNotifications = useCallback(() => {
        setRecentNotifications([]);
    }, []);

    // Initialize real-time notifications when user is available
    useEffect(() => {
        const initializeNotifications = async () => {
            if (user?.userId) {
                try {
                    // Get the access token from storage
                    const accessToken = await getToken('accessToken');

                    if (accessToken) {
                        // Initial fetch
                        fetchUnreadCount();

                        // Connect to notification service with authentication token
                        notificationStompService.connect(user.userId, accessToken);

                        // Set up real-time listeners
                        const unsubscribeNotification = notificationStompService.onNotification((notification) => {
                            console.log('New notification received:', notification);
                            addRecentNotification(notification);
                            if (!notification.isRead) {
                                incrementUnreadCount(1);
                            }
                        });

                        const unsubscribeCount = notificationStompService.onCountUpdate((count) => {
                            console.log('Count updated to:', count);
                            setUnreadCount(count);
                        });

                        const unsubscribeConnection = notificationStompService.onConnectionChange((isConnected) => {
                            console.log('Notification service connection:', isConnected);
                            setConnected(isConnected);
                            if (isConnected) {
                                // Refresh count when reconnected
                                fetchUnreadCount();
                            }
                        });

                        return () => {
                            unsubscribeNotification();
                            unsubscribeCount();
                            unsubscribeConnection();
                            notificationStompService.disconnect();
                        };
                    }
                } catch (error) {
                    console.error('Error initializing notifications:', error);
                }
            }
        };

        const cleanup = initializeNotifications();
        return () => {
            if (cleanup && typeof cleanup.then === 'function') {
                cleanup.then(cleanupFn => cleanupFn && cleanupFn());
            }
        };
    }, [user?.userId, fetchUnreadCount, incrementUnreadCount, addRecentNotification]);

    // Poll for unread count as fallback (every 2 minutes)
    useEffect(() => {
        if (!connected && user?.userId) {
            const interval = setInterval(async () => {
                try {
                    fetchUnreadCount();
                    // Also check if we can reconnect
                    if (!notificationStompService.isClientConnected()) {
                        const accessToken = await getToken('accessToken');
                        if (accessToken) {
                            notificationStompService.connect(user.userId, accessToken);
                        }
                    }
                } catch (error) {
                    console.error('Error in polling fallback:', error);
                }
            }, 120000);
            return () => clearInterval(interval);
        }
    }, [connected, user?.userId, fetchUnreadCount]);

    const value = {
        unreadCount,
        loading,
        connected,
        recentNotifications,
        fetchUnreadCount,
        decrementUnreadCount,
        incrementUnreadCount,
        resetUnreadCount,
        addRecentNotification,
        clearRecentNotifications,
    };

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
};
