import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { Text, View, Dimensions } from 'react-native';
import 'react-native-reanimated';
import * as Progress from 'react-native-progress';

import CustomHeader from '@/components/CustomHeader';

import { PetProvider } from '@/context/PetContext';
import { UserProvider } from '@/context/UserContext';
import { AuthProvider } from '@/context/AuthContext';
import { FlowProvider } from '@/context/FlowContext';

import { ActionSheetProvider } from '@expo/react-native-action-sheet';

import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import Toast from 'react-native-toast-message';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const [loaded] = useFonts({
    SpaceMono: require('../assets/fonts/SpaceMono-Regular.ttf'),
  });

  const screenWidth = Dimensions.get("window").width; // Get screen width

  useEffect(() => {
    if (loaded) {
      SplashScreen.hideAsync();
    }
  }, [loaded]);

  if (!loaded) {
    return null;
  }

  return (
    <SafeAreaProvider>
      <AuthProvider>
        <FlowProvider>
          <UserProvider>
            <PetProvider>
              <ActionSheetProvider>
                <SafeAreaView style={{ flex: 1 }}>
                  <ThemeProvider value={DefaultTheme}>
                    <Stack>
                      <Stack.Screen name="index" options={{ headerShown: false }} />
                      <Stack.Screen name="(drawer)" options={{ headerShown: false }} />
                      <Stack.Screen name="RegisterModule/Onboarding" options={{ headerShown: false }} />
                      <Stack.Screen name="RegisterModule/RegisterScreen" options={{ headerShown: false }} />
                      <Stack.Screen name="RegisterModule/OTPVerificationScreen" options={{ headerShown: false }} />
                      <Stack.Screen name="RegisterModule/ResetPasswordScreen" options={{ headerShown: false }} />
                      <Stack.Screen name="RegisterModule/ForgotPasswordScreen" options={{ headerShown: false }} />
                      <Stack.Screen name="RegisterModule/LoginScreen" options={{ headerShown: false }} />
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
                        name="PetModule"
                        options={{ headerShown: false }}
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
                      <Stack.Screen
                        name="UserModule"
                        options={{headerShown: false}}
                      />
                    </Stack>
                    <StatusBar style="auto" />
                  </ThemeProvider>
                </SafeAreaView>
              </ActionSheetProvider>
            </PetProvider>
          </UserProvider>
        </FlowProvider>
      </AuthProvider>
      <Toast />
    </SafeAreaProvider>
  );
}
