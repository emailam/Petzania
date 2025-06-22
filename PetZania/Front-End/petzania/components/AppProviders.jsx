import React from 'react';
import { ActionSheetProvider } from '@expo/react-native-action-sheet';
import { PetProvider } from '@/context/PetContext';
import { UserProvider } from '@/context/UserContext';
import { AuthProvider } from '@/context/AuthContext';
import { FlowProvider } from '@/context/FlowContext';
import { FriendsProvider } from '@/context/FriendsContext';
import { ChatProvider } from '@/context/ChatContext';

// Combined Providers Component to reduce nesting
export default function AppProviders({ children }) {
    return (
        <AuthProvider>
            <FlowProvider>
                <UserProvider>
                    <FriendsProvider>
                        <PetProvider>
                            <ActionSheetProvider>
                                {children}
                            </ActionSheetProvider>
                        </PetProvider>
                    </FriendsProvider>
                </UserProvider>
            </FlowProvider>
        </AuthProvider>
    );
}
