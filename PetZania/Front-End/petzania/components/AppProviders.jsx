import React from 'react';
import { ActionSheetProvider } from '@expo/react-native-action-sheet';
import { PetProvider } from '@/context/PetContext';
import { UserProvider } from '@/context/UserContext';
import { FlowProvider } from '@/context/FlowContext';
import { FriendsProvider } from '@/context/FriendsContext';
import { NotificationProvider } from '@/context/NotificationContext';
import { GlobalMessageProvider } from '@/context/GlobalMessageContext';
import { ChatProvider } from '@/context/ChatContext';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';

const queryClient = new QueryClient();

// Combined Providers Component to reduce nesting
export default function AppProviders({ children }) {
    // Create a QueryClient instance

    return (
        <QueryClientProvider client={queryClient}>
            <FlowProvider>
                <UserProvider>
                    <GlobalMessageProvider>
                        <ChatProvider>
                            <NotificationProvider>
                                <FriendsProvider>
                                    <PetProvider>
                                        <ActionSheetProvider>
                                            {children}
                                        </ActionSheetProvider>
                                    </PetProvider>
                                </FriendsProvider>
                            </NotificationProvider>
                        </ChatProvider>
                    </GlobalMessageProvider>
                </UserProvider>
            </FlowProvider>
        </QueryClientProvider>
    );
}
