import React, { useEffect, useState, useContext } from 'react';
import { Redirect } from 'expo-router';
import { useFonts } from 'expo-font';
import { ActivityIndicator, View } from 'react-native';
import { getToken, clearAllTokens } from '@/storage/tokenStorage';
import { getUserById } from '@/services/userService';
import { getUserId } from '@/storage/userStorage';

import { UserContext } from '@/context/UserContext';
import { PetContext } from '@/context/PetContext';
import { AntDesign, FontAwesome5, Ionicons, Feather, FontAwesome } from '@expo/vector-icons';

import { Asset } from 'expo-asset';
import { PETS } from '@/constants/PETS';

import 'react-native-gesture-handler';

export default function App() {
    const [fontsLoaded] = useFonts({
        ...Ionicons.font,
        ...FontAwesome.font,
        ...FontAwesome5.font,
        ...AntDesign.font,
        ...Feather.font,
        'Inter-Bold': require('@/assets/fonts/Inter-Bold.ttf'),
    });

    const [isReady, setIsReady] = useState(false);
    const [redirectPath, setRedirectPath] = useState(null);

    const { user, setUser } = useContext(UserContext);
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
        // preloadPetImages();
    }, []);

    // Add this effect to handle when user context is lost
    useEffect(() => {
        if (isReady && !user) {
            setRedirectPath('/RegisterModule/LoginScreen');
        }
    }, [isReady, user]);

    const preloadPetImages = async () => {
        await Promise.all(PETS.map(pet => Asset.loadAsync(pet.image)));
    };

    if (!fontsLoaded || !isReady) {
        return (
            <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
                <ActivityIndicator size="large" color="#9188E5" />
            </View>
        );
    }

    return <Redirect href={redirectPath} />;
}