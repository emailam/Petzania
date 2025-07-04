import { useEffect, useCallback, useRef } from 'react';
import globalMessageService from './globalMessageService';
import { getToken } from '@/storage/tokenStorage';

export function useGlobalMessageService(user = null, token = null) {
    const isInitializedRef = useRef(false);
    const currentUserRef = useRef(null);

    // Single useEffect to handle all user connection logic
    useEffect(() => {
        const handleUserConnection = async () => {
            const currentUserId = user?.userId;
            const previousUserId = currentUserRef.current?.userId;
            
            console.log('🔍 useGlobalMessageService - User state changed');
            console.log('🔍 Current user ID:', currentUserId);
            console.log('🔍 Previous user ID:', previousUserId);
            console.log('� Is initialized:', isInitializedRef.current);
            
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

            // If user logged in (wasn't logged in, now is) or user changed, and not already initialized
            if (currentUserId && (!previousUserId || previousUserId !== currentUserId) && !isInitializedRef.current) {
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
                    
                    // Add a small delay to ensure everything is ready
                    await new Promise(resolve => setTimeout(resolve, 500));
                    
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

        // Only run if we have a user or had a user previously
        if (user?.userId || currentUserRef.current?.userId) {
            handleUserConnection();
        }
    }, [user?.userId]); // Only depend on userId, not the full user object

    // Disconnect when component unmounts
    useEffect(() => {
        return () => {
            if (isInitializedRef.current) {
                console.log('🔌 Cleaning up global message service on unmount');
                globalMessageService.disconnect();
                isInitializedRef.current = false;
                currentUserRef.current = null;
            }
        };
    }, []);

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
