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

import { useColorScheme } from '@/hooks/useColorScheme';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const colorScheme = useColorScheme();
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
        <UserProvider>
          <PetProvider>
            <SafeAreaView style={{ flex: 1 }}>
              <ThemeProvider value={DefaultTheme}>
                <Stack>
                  <Stack.Screen name="index" options={{ headerShown: false }} />
                  <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
                  <Stack.Screen name="RegisterModule/Onboarding" options={{ headerShown: false }} />
                  <Stack.Screen name="RegisterModule/RegisterScreen" options={{ headerShown: false }} />
                  <Stack.Screen name="RegisterModule/OTPVerificationScreen" options={{ headerShown: false }} />
                  <Stack.Screen name="RegisterModule/ResetPasswordScreen" options={{ headerShown: false }} />
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
                    name="RegisterModule/AddPet1"
                    options={{
                      headerTitle: () => (
                        <View style={{width: screenWidth }}>
                          <CustomHeader title="Add Pet Profile" subtitle="Name" alignment={"center"}/>
                          <Progress.Bar borderRadius={10} progress={(1/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                        </View>
                      ),
                      headerRight: () => (
                        <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                          <Text style={{fontWeight: '500', color:'black'}}>1</Text>/6
                        </Text>
                      ),
                      headerTitleAlign: "center",
                      headerBackTitle: "",
                      headerTintColor: "#9188E5",
                      headerStyle: { backgroundColor: "#FFF" },
                    }}
                  />
                  <Stack.Screen
                    name="RegisterModule/AddPet2"
                    options={{
                      headerTitle: () => (
                        <View style={{width: screenWidth }}>
                          <CustomHeader title="Add Pet Profile" subtitle="Type" alignment={"center"}/>
                          <Progress.Bar borderRadius={10} progress={(2/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                        </View>
                      ),
                      headerRight: () => (
                        <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                          <Text style={{fontWeight: '500', color:'black'}}>2</Text>/6
                        </Text>
                      ),
                      headerTitleAlign: "center",
                      headerBackTitle: "",
                      headerTintColor: "#9188E5",
                      headerStyle: { backgroundColor: "#FFF" },
                    }}
                  />
                  <Stack.Screen
                    name="RegisterModule/AddPet3"
                    options={{
                      headerTitle: () => (
                        <View style={{width: screenWidth }}>
                          <CustomHeader title="Add Pet Profile" subtitle="Breed" alignment={"center"}/>
                          <Progress.Bar borderRadius={10} progress={(3/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                        </View>
                      ),
                      headerRight: () => (
                        <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                          <Text style={{fontWeight: '500', color:'black'}}>3</Text>/6
                        </Text>
                      ),
                      headerTitleAlign: "center",
                      headerBackTitle: "",
                      headerTintColor: "#9188E5",
                      headerStyle: { backgroundColor: "#FFF" },
                    }}
                  />
                  <Stack.Screen
                    name="RegisterModule/AddPet4"
                    options={{
                      headerTitle: () => (
                        <View style={{width: screenWidth }}>
                          <CustomHeader title="Add Pet Profile" subtitle="General Information" alignment={"center"}/>
                          <Progress.Bar borderRadius={10} progress={(4/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                        </View>
                      ),
                      headerRight: () => (
                        <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                          <Text style={{fontWeight: '500', color:'black'}}>4</Text>/6
                        </Text>
                      ),
                      headerTitleAlign: "center",
                      headerBackTitle: "",
                      headerTintColor: "#9188E5",
                      headerStyle: { backgroundColor: "#FFF" },
                    }}
                  />
                  <Stack.Screen
                    name="RegisterModule/AddPet5"
                    options={{
                      headerTitle: () => (
                        <View style={{width: screenWidth }}>
                          <CustomHeader title="Add Pet Profile" subtitle="Health Conditions" alignment={"center"}/>
                          <Progress.Bar borderRadius={10} progress={(5/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                        </View>
                      ),
                      headerRight: () => (
                        <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                          <Text style={{fontWeight: '500', color:'black'}}>5</Text>/6
                        </Text>
                      ),
                      headerTitleAlign: "center",
                      headerBackTitle: "",
                      headerTintColor: "#9188E5",
                      headerStyle: { backgroundColor: "#FFF" },
                    }}
                  />
                  <Stack.Screen
                    name="RegisterModule/AddPet6"
                    options={{
                      headerTitle: () => (
                        <View style={{width: screenWidth }}>
                          <CustomHeader title="Add Pet Profile" subtitle="Finish Pets Profiles" alignment={"center"}/>
                          <Progress.Bar borderRadius={10} progress={(6/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                        </View>
                      ),
                      headerRight: () => (
                        <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                          <Text style={{fontWeight: '500', color:'black'}}>6</Text>/6
                        </Text>
                      ),
                      headerTitleAlign: "center",
                      headerBackTitle: "",
                      headerTintColor: "#9188E5",
                      headerStyle: { backgroundColor: "#FFF" },
                    }}
                  />
                  <Stack.Screen
                    name="RegisterModule/PetDetails"
                    options={{
                      headerTitle: () => (
                        <View style={{width: screenWidth }}>
                          <CustomHeader title="Pet Details" subtitle="Edit Pet Details" alignment={"center"}/>
                          <Progress.Bar borderRadius={10} progress={(6/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                        </View>
                      ),
                      headerRight: () => (
                        <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                          <Text style={{fontWeight: '500', color:'black'}}>6</Text>/6
                        </Text>
                      ),
                      headerTitleAlign: "center",
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
                </Stack>
                <StatusBar style="auto" />
              </ThemeProvider>
            </SafeAreaView>
          </PetProvider>
        </UserProvider>
      </AuthProvider>
    </SafeAreaProvider>
  );
}
