import React, { createContext, useContext } from 'react';
import { UserContext } from '@/context/UserContext';
import { useGlobalMessageService } from '../services/useGlobalMessageService';

export const GlobalMessageContext = createContext();

export const GlobalMessageProvider = ({ children }) => {
    const { user } = useContext(UserContext);

    // Initialize global message service with current user
    const globalMessageService = useGlobalMessageService(user);

    return (
        <GlobalMessageContext.Provider value={globalMessageService}>
            {children}
        </GlobalMessageContext.Provider>
    );
};

// Custom hook to use the global message service
export const useGlobalMessage = () => {
    const context = useContext(GlobalMessageContext);
    if (!context) {
        throw new Error('useGlobalMessage must be used within a GlobalMessageProvider');
    }
    return context;
};
