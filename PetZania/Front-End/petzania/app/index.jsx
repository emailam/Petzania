import React, { useEffect, useState, useContext } from 'react';
import { Redirect } from 'expo-router';
import { useFonts } from 'expo-font';
import { ActivityIndicator, View } from 'react-native';
import { getToken, clearAllTokens } from '@/storage/tokenStorage';
import { getUserById } from '@/services/userService';
import { getUserId } from '@/storage/userStorage';

import { UserContext } from '@/context/UserContext';
import { PetContext } from '@/context/PetContext';
import { Ionicons } from '@expo/vector-icons';
import { FontAwesome } from '@expo/vector-icons';

import 'react-native-gesture-handler';

export default function App() {
    const [fontsLoaded] = useFonts({
        ...Ionicons.font,
        ...FontAwesome.font,
        'Inter-Bold': require('@/assets/fonts/Inter-Bold.ttf'),
    });

    const [isReady, setIsReady] = useState(false);
    const [redirectPath, setRedirectPath] = useState(null);

    const { setUser } = useContext(UserContext);
    const { setPets } = useContext(PetContext);

    useEffect(() => {
        const checkAuth = async () => {
            try {
                const accessToken = await getToken('accessToken');
                const refreshToken = await getToken('refreshToken');
                const userId = await getUserId('userId');

                if (!accessToken || !refreshToken || !userId) {
                    setRedirectPath('/RegisterModule/Onboarding');
                    return;
                }

                const userData = await getUserById(userId);
                setUser(userData);
                setPets(userData.myPets);

                if (userData?.name === null) {
                    setRedirectPath('/RegisterModule/ProfileSetUp1');
                } else {
                    setRedirectPath('/Home');
                }
            } catch (error) {
                console.error('Auth check failed:', error.message);
                await clearAllTokens();
                setRedirectPath('/RegisterModule/LoginScreen');
            } finally {
                setIsReady(true);
            }
        };

        checkAuth();
    }, []);

    if (!fontsLoaded || !isReady) {
        return (
            <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
                <ActivityIndicator size="large" color="#9188E5" />
            </View>
        );
    }

    return <Redirect href={redirectPath} />;
}