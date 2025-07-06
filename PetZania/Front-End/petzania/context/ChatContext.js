import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useGlobalMessage } from '@/context/GlobalMessageContext';
import { UserContext } from '@/context/UserContext';

const ChatContext = createContext();

export const ChatProvider = ({ children }) => {
    const [hasUnreadMessages, setHasUnreadMessages] = useState(false);
    const [chatUnreadIndicators, setChatUnreadIndicators] = useState(new Map());
    const { addMessageHandler } = useGlobalMessage();
    const { user: currentUser } = useContext(UserContext);

    // Note: Removed global message handler from ChatContext to prevent double counting
    // The chat list index.jsx handles unread indicators and calls setChatUnreadIndicator
    // This prevents duplicate increments while maintaining centralized state management

    // Update overall unread indicator when individual chat indicators change
    useEffect(() => {
        const hasAnyUnread = Array.from(chatUnreadIndicators.values()).some(hasUnread => hasUnread);
        console.log('📊 ChatContext overall unread indicator updated:', hasAnyUnread, 'from individual indicators:', Array.from(chatUnreadIndicators.entries()));
        setHasUnreadMessages(hasAnyUnread);
    }, [chatUnreadIndicators]);

    const setChatUnreadIndicator = useCallback((chatId, hasUnread) => {
        setChatUnreadIndicators(prev => {
            const updated = new Map(prev);
            const currentIndicator = updated.get(chatId) || false;
            
            // Only update if the indicator actually changed
            if (currentIndicator !== hasUnread) {
                updated.set(chatId, hasUnread);
                console.log(`📊 Chat ${chatId} unread indicator set: ${currentIndicator} -> ${hasUnread}`);
                return updated;
            }
            
            return prev; // No change, return previous state to prevent unnecessary re-renders
        });
    }, []);

    const resetChatUnreadIndicator = useCallback((chatId) => {
        setChatUnreadIndicator(chatId, false);
    }, [setChatUnreadIndicator]);

    const initializeChatUnreadIndicators = useCallback((unreadIndicatorsMap) => {
        console.log('📊 ChatContext initializing with unread indicators:', unreadIndicatorsMap);
        
        // Log what's being overwritten
        chatUnreadIndicators.forEach((currentIndicator, chatId) => {
            const newIndicator = unreadIndicatorsMap.get(chatId) || false;
            if (currentIndicator !== newIndicator) {
                console.log(`📊 ChatContext overwriting indicator for chat ${chatId}: ${currentIndicator} -> ${newIndicator}`);
            }
        });
        
        setChatUnreadIndicators(new Map(unreadIndicatorsMap));
    }, [chatUnreadIndicators]);

    const getChatUnreadIndicator = useCallback((chatId) => {
        return chatUnreadIndicators.get(chatId) || false;
    }, [chatUnreadIndicators]);

    const contextValue = {
        hasUnreadMessages,
        chatUnreadIndicators,
        setChatUnreadIndicator,
        resetChatUnreadIndicator,
        initializeChatUnreadIndicators,
        getChatUnreadIndicator
    };

    return (
        <ChatContext.Provider value={contextValue}>
            {children}
        </ChatContext.Provider>
    );
};

export const useChat = () => {
    const context = useContext(ChatContext);
    if (!context) {
        throw new Error('useChat must be used within a ChatProvider');
    }
    return context;
};

export default ChatContext;
