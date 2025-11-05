import React, { useEffect, useState, useContext, useCallback } from 'react';
import { Redirect } from 'expo-router';
import { useFonts } from 'expo-font';
import { View, Text } from 'react-native';
import { Image } from 'expo-image';
import { Asset } from 'expo-asset';

import { getUserById } from '@/services/userService';

import { getUserId } from '@/storage/userStorage';
import { getToken, clearAllTokens } from '@/storage/tokenStorage';
import { getOnboardingStatus } from '@/storage/onboardingStorage';

import LottieView from 'lottie-react-native';

import { UserContext } from '@/context/UserContext';
import { PetContext } from '@/context/PetContext';

import { AntDesign, FontAwesome5, Ionicons, Feather, FontAwesome } from '@expo/vector-icons';

import { PETS } from '@/constants/PETS';

import 'react-native-gesture-handler';
import * as SplashScreen from 'expo-splash-screen';

SplashScreen.preventAutoHideAsync();

export default function App() {
    const [fontsLoaded] = useFonts({
        ...Ionicons.font,
        ...FontAwesome.font,
        ...FontAwesome5.font,
        ...AntDesign.font,
        ...Feather.font,
        'Inter-Bold': require('@/assets/fonts/Inter-Bold.ttf'),
    });
    const [appIsReady, setAppIsReady] = useState(false);
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
                const hasSeenOnboarding = await getOnboardingStatus();

                if(!hasSeenOnboarding) {
                    setRedirectPath('/RegisterModule/Onboarding');
                    return;
                }

                if (!accessToken || !refreshToken || !userId) {
                    setRedirectPath('/RegisterModule/LoginScreen');
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

    // New effect to handle app ready state and splash screen with 3-second delay
    useEffect(() => {
        if (fontsLoaded && isReady) {
            // Add 3-second delay before setting app as ready
            const timer = setTimeout(() => {
                setAppIsReady(true);
            }, 3000);

            return () => clearTimeout(timer);
        }
    }, [fontsLoaded, isReady]);

    const preloadPetImages = async () => {
        await Promise.all(PETS.map(pet => Asset.loadAsync(pet.image)));
    };

    const onLayoutRootView = useCallback(async () => {
        if (appIsReady) {
            // Hide the splash screen once the app is ready
            await SplashScreen.hideAsync();
        }
    }, [appIsReady]);

    if (!appIsReady) {
        return (
            <View style={{
                flex: 1,
                justifyContent: 'center',
                alignItems: 'center',
                backgroundColor: '#9a90f5' // Match your app's primary color
            }}>
                {/* App Logo */}
                <Image
                    source={require('@/assets/images/splash.png')}
                    style={{
                        width: 300,
                        height: 300,
                        resizeMode: 'contain'
                    }}
                />

                {/* Loading Animation */}
                <View style={{ marginTop: 40 }}>
                    <LottieView
                        source={require('@/assets/lottie/loading.json')}
                        style={{
                            width: 100,
                            height: 100,
                        }}
                        autoPlay
                        loop
                        speed={1.0}
                    />
                </View>

                {/* Loading Text */}
                <Text style={{
                    color: 'white',
                    fontSize: 16,
                    fontWeight: '600',
                    marginTop: 20,
                    textAlign: 'center'
                }}>
                    Loading PetZania...
                </Text>
            </View>
        );
    }

    return (
        <View style={{ flex: 1 }} onLayout={onLayoutRootView}>
            <Redirect href={redirectPath} />
        </View>
    );
}