import { Stack } from 'expo-router';
import 'react-native-reanimated';

export default function SettingsLayout() {
    return (
        <Stack screenOptions={{ headerShown: false }}>
            <Stack.Screen name="index" options={{ headerShown: true, headerTitle: "Settings" }}/>
            <Stack.Screen name="EmergencyContacts" options={{ headerShown: true, headerTitle: "Emergency Contacts"}}/>
            <Stack.Screen name="ContactSupport" options={{ headerShown: true, headerTitle: "Contact Support"}}/>
            <Stack.Screen name="ChangePassword" options={{ headerShown: true, headerTitle: "Change Password"}}/>
        </Stack>
    );
}