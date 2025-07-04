import { useEffect, useCallback, useRef } from 'react';
import globalMessageService from './globalMessageService';
import { getToken } from '@/storage/tokenStorage';

/**
 * React hook for managing global message service
 * @param {Object} user - Current user object
 * @param {string} token - Authentication token
 * @returns {Object} - Hook interface
 */
export function useGlobalMessageService(user = null, token = null) {
    const isInitializedRef = useRef(false);
    const currentUserRef = useRef(null);

    // Connect to global message service when user logs in
    useEffect(() => {
        const connectToService = async () => {
            // Add additional checks to ensure user is fully authenticated
            if (user && user.userId && !isInitializedRef.current) {
                try {
                    console.log('🌐 Initializing global message service for user:', user.userId);
                    
                    // Get token if not provided
                    let authToken = token;
                    if (!authToken) {
                        authToken = await getToken('accessToken');
                    }
                    
                    if (!authToken) {
                        console.warn('⚠️ No authentication token available for global message service');
                        return;
                    }
                    
                    // Add a small delay to ensure the user context is fully loaded
                    await new Promise(resolve => setTimeout(resolve, 1000));
                    
                    await globalMessageService.connect(user.userId, authToken);
                    isInitializedRef.current = true;
                    currentUserRef.current = user;
                    console.log('✅ Global message service initialized');
                } catch (error) {
                    console.error('❌ Failed to initialize global message service:', error);
                    // Reset initialization flag to allow retry
                    isInitializedRef.current = false;
                }
            }
        };

        // Only connect if user is properly authenticated
        if (user && user.userId) {
            connectToService();
        }
    }, [user?.userId]);

    // Disconnect when user logs out or component unmounts
    useEffect(() => {
        return () => {
            if (isInitializedRef.current) {
                console.log('🔌 Cleaning up global message service');
                globalMessageService.disconnect();
                isInitializedRef.current = false;
                currentUserRef.current = null;
            }
        };
    }, []);

    // Handle user changes (logout/login with different user)
    useEffect(() => {
        const handleUserChange = async () => {
            
            const currentUserId = user?.userId;
            const previousUserId = currentUserRef.current?.userId;
            
            // If user logged out (was logged in, now not)
            if (previousUserId && !currentUserId) {
                console.log('👤 User logged out, disconnecting global message service');
                await globalMessageService.disconnect();
                isInitializedRef.current = false;
                currentUserRef.current = null;
                return;
            }

            // If user changed (different user ID)
            if (previousUserId && currentUserId && previousUserId !== currentUserId) {
                console.log('👤 User changed, disconnecting and reconnecting global message service');
                await globalMessageService.disconnect();
                isInitializedRef.current = false;
                currentUserRef.current = null;
            }

            // If user logged in (wasn't logged in, now is) or user changed
            if (currentUserId && (!previousUserId || previousUserId !== currentUserId)) {
                console.log('👤 User logged in/changed, connecting global message service');
                try {
                    // Get token if not provided
                    let authToken = token;
                    if (!authToken) {
                        authToken = await getToken('accessToken');
                    }
                    
                    if (!authToken) {
                        console.warn('⚠️ No authentication token available for user');
                        return;
                    }
                    
                    await globalMessageService.connect(currentUserId, authToken);
                    isInitializedRef.current = true;
                    currentUserRef.current = user;
                    console.log('✅ Global message service connected for user:', currentUserId);
                } catch (error) {
                    console.error('❌ Failed to connect for user:', error);
                    isInitializedRef.current = false;
                }
            }
        };

        handleUserChange();
    }, [user?.userId]); // Remove token dependency as it causes unnecessary re-runs

    /**
     * Set the currently open chat to prevent unread count increments
     */
    const setCurrentOpenChat = useCallback((chatId) => {
        globalMessageService.setCurrentOpenChat(chatId);
    }, []);

    /**
     * Add a message handler
     */
    const addMessageHandler = useCallback((handler) => {
        globalMessageService.addMessageHandler(handler);
        return () => globalMessageService.removeMessageHandler(handler);
    }, []);

    /**
     * Add a reaction handler
     */
    const addReactionHandler = useCallback((handler) => {
        globalMessageService.addReactionHandler(handler);
        return () => globalMessageService.removeReactionHandler(handler);
    }, []);

    /**
     * Check if the service is connected
     */
    const isConnected = useCallback(() => {
        return globalMessageService.isConnected();
    }, []);

    /**
     * Get connection status details
     */
    const getConnectionStatus = useCallback(() => {
        return globalMessageService.getConnectionStatus();
    }, []);

    /**
     * Force reconnection
     */
    const reconnect = useCallback(async () => {
        if (currentUserRef.current) {
            let authToken = token;
            if (!authToken) {
                authToken = await getToken('accessToken');
            }
            if (authToken) {
                await globalMessageService.connect(currentUserRef.current.userId, authToken);
            }
        }
    }, [token]);

    return {
        setCurrentOpenChat,
        addMessageHandler,
        addReactionHandler,
        isConnected,
        getConnectionStatus,
        reconnect,
        currentUserId: currentUserRef.current?.userId || null
    };
}

export default useGlobalMessageService;
