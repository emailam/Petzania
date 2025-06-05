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
                }}
            />
            <Stack.Screen
                name="ChatDetails"
                options={{
                    title: 'Chat Details',
                    headerTitleAlign: 'center',
                }}
            />
        </Stack>
    );
}