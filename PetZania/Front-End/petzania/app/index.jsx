import React from 'react'
import { Redirect } from 'expo-router';
import { useFonts } from 'expo-font';
import { ActivityIndicator, View } from 'react-native';
import { getToken } from '@/storage/tokenStorage';

export default function App() {
    const [fontsLoaded] = useFonts({
        'Inter-Bold': require('@/assets/fonts/Inter-Bold.ttf'),
    });
    if (!fontsLoaded) {
        return (
            <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
                <ActivityIndicator size="large" color="#9188E5" />
            </View>
        );
    }
    // return <Redirect href="/RegisterModule/RegisterScreen" />;
    return <Redirect href="/Home" />;
}