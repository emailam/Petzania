import { DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { Text, View, ActivityIndicator } from 'react-native';
import 'react-native-reanimated';

import CustomHeader from '@/components/CustomHeader';
import ErrorBoundary from '@/components/ErrorBoundary';
import AppProviders from '@/components/AppProviders';
import NotificationToast from '@/components/NotificationToast';

import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import Toast from 'react-native-toast-message';

SplashScreen.preventAutoHideAsync();

// Loading Component
const LoadingScreen = () => (
  <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#fff' }}>
    <ActivityIndicator size="large" color="#9188E5" />
    <Text style={{ marginTop: 16, fontSize: 16, color: '#666' }}>Loading Petzania...</Text>
  </View>
);

export default function RootLayout() {
  const [loaded, error] = useFonts({
    SpaceMono: require('../assets/fonts/SpaceMono-Regular.ttf'),
  });

  useEffect(() => {
    if (loaded || error) {
      SplashScreen.hideAsync();
    }
  }, [loaded, error]);

  // Show loading screen while fonts are loading
  if (!loaded && !error) {
    return <LoadingScreen />;
  }

  // Show error if font loading failed
  if (error) {
    console.error('Font loading error:', error);
  }
  return (
    <ErrorBoundary>
      <SafeAreaProvider>
        <AppProviders>
          <SafeAreaView style={{ flex: 1 }}>
            <ThemeProvider value={DefaultTheme}>
              <Stack>
                <Stack.Screen name="index" options={{ headerShown: false }} />
                <Stack.Screen name="(drawer)" options={{ headerShown: false }} />
                {/* Registration Module Screens */}
                <Stack.Screen name="RegisterModule/Onboarding" options={{ headerShown: false }} />
                <Stack.Screen name="RegisterModule/RegisterScreen" options={{ headerShown: false }} />
                <Stack.Screen name="RegisterModule/OTPVerificationScreen" options={{ headerShown: false }} />
                <Stack.Screen name="RegisterModule/ResetPasswordScreen" options={{ headerShown: false }} />
                <Stack.Screen name="RegisterModule/ForgotPasswordScreen" options={{ headerShown: false }} />
                <Stack.Screen name="RegisterModule/LoginScreen" options={{ headerShown: false }} />
                {/* Profile Setup Screens */}
                <Stack.Screen
                  name="RegisterModule/ProfileSetUp1"
                  options={{
                    headerTitle: () => (
                      <View>
                        <CustomHeader title="Profile Set Up" subtitle="Add your details 1 of 3"/>
                      </View>
                    ),
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                  }}
                />
                <Stack.Screen
                  name="RegisterModule/ProfileSetUp2"
                  options={{
                    headerTitle: () => (
                      <View>
                        <CustomHeader title="Profile Set Up" subtitle="Add your pets 2 of 3"/>
                      </View>
                    ),
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                  }}
                />
                <Stack.Screen
                  name="RegisterModule/ProfileSetUp3"
                  options={{
                    headerTitle: () => (
                      <View>
                        <CustomHeader title="Profile Set Up" subtitle="Finalize your profile 3 of 3"/>
                      </View>
                    ),
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                  }}
                />

                {/* Module Screens */}
                <Stack.Screen name="PetModule" options={{ headerShown: false }} />
                <Stack.Screen name="UserModule" options={{ headerShown: false }} />
              </Stack>
              <StatusBar style="auto" />
              <NotificationToast />
            </ThemeProvider>
          </SafeAreaView>
        </AppProviders>
        <Toast />
      </SafeAreaProvider>
    </ErrorBoundary>
  );
}
