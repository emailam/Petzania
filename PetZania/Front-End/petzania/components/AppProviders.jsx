import React from 'react';
import { ActionSheetProvider } from '@expo/react-native-action-sheet';
import { PetProvider } from '@/context/PetContext';
import { UserProvider } from '@/context/UserContext';
import { FlowProvider } from '@/context/FlowContext';
import { FriendsProvider } from '@/context/FriendsContext';
import { NotificationProvider } from '@/context/NotificationContext';

// Combined Providers Component to reduce nesting
export default function AppProviders({ children }) {
    return (
        <FlowProvider>
            <UserProvider>
                <NotificationProvider>
                    <FriendsProvider>
                        <PetProvider>
                            <ActionSheetProvider>
                                {children}
                            </ActionSheetProvider>
                        </PetProvider>
                    </FriendsProvider>
                </NotificationProvider>
            </UserProvider>
        </FlowProvider>
    );
}
