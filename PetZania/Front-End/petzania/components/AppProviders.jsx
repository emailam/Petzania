import React from 'react';
import { ActionSheetProvider } from '@expo/react-native-action-sheet';
import { PetProvider } from '@/context/PetContext';
import { UserProvider } from '@/context/UserContext';
import { AuthProvider } from '@/context/AuthContext';
import { FlowProvider } from '@/context/FlowContext';

// Combined Providers Component to reduce nesting
export default function AppProviders({ children }) {
    return (
        <AuthProvider>
            <FlowProvider>
                <UserProvider>
                    <PetProvider>
                        <ActionSheetProvider>
                            {children}
                        </ActionSheetProvider>
                    </PetProvider>
                </UserProvider>
            </FlowProvider>
        </AuthProvider>
    );
}
