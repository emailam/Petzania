import { Stack } from 'expo-router';
import 'react-native-reanimated';

export default function Layout() {
    return (
        <Stack screenOptions={{ headerShown: false }}>
            <Stack.Screen name="(tabs)" />
            <Stack.Screen name="Friends" />
            <Stack.Screen name="Settings" />
            <Stack.Screen name="Help" />
            <Stack.Screen name="Chat" />
            <Stack.Screen name="Search" />
            <Stack.Screen name="PetServices" />
        </Stack>
    );
}