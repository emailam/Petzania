import {Stack} from "expo-router";
import {TouchableOpacity, View} from "react-native";

export default function ChatLayout() {
    return (
        <Stack>
            <Stack.Screen
                name="index"
                options={{
                    title: 'Chat',
                    headerTitleAlign: 'center',
                    headerTitleStyle: { color: '#000' },
                    headerTintColor: '#9188E5',
                }}
            />
            <Stack.Screen
                name="[chatid]"
                options={{
                    headerShown: false,
                }}
            />
            <Stack.Screen
                name="Image"
                options={{
                    headerShown: false,
                    animation: 'fade_from_bottom',
                }}
            />
            <Stack.Screen
                name="Profile"
                options={{
                    headerShown: false,
                }}
            />
        </Stack>
    );
}