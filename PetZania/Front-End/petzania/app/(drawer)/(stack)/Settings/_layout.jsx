import { Stack } from 'expo-router';
import 'react-native-reanimated';

export default function SettingsLayout() {
    return (
        <Stack screenOptions={{ headerShown: false }}>
            <Stack.Screen name="index"
                options={{
                    headerShown: true,
                    headerTitle: "Settings",
                    headerTitleAlign: "center",
                    headerTintColor: "#9188E5",
                    headerTitleStyle: { color: '#000' }
                }}
            />
            <Stack.Screen name="EmergencyContacts"
                options={{
                    headerShown: true,
                    headerTitle: "Emergency Contacts",
                    headerTitleAlign: "center",
                    headerTintColor: "#9188E5",
                    headerTitleStyle: { color: '#000' }
                }}
            />
            <Stack.Screen name="ContactSupport"
                options={{
                    headerShown: true,
                    headerTitle: "Contact Support",
                    headerTitleAlign: "center",
                    headerTintColor: "#9188E5",
                    headerTitleStyle: { color: '#000' }
                }}
            />
            <Stack.Screen name="ChangePassword"
                options={{
                    headerShown: true,
                    headerTitle: "Change Password",
                    headerTitleAlign: "center",
                    headerTintColor: "#9188E5",
                    headerTitleStyle: { color: '#000' }
                }}
            />
            <Stack.Screen name="About"
                options={{
                    headerShown: true,
                    headerTitle: "About",
                    headerTitleAlign: "center",
                    headerTintColor: "#9188E5",
                    headerTitleStyle: { color: '#000' }
                }}
            />
        </Stack>
    );
}