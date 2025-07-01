import { Stack } from "expo-router";

export default function ImageLayout() {
    return (
        <Stack>
            <Stack.Screen
                name="[userId]"
                options={{
                    headerShown: true,
                    title: 'Profile',
                    headerTitleStyle: { color: '#000' },
                    headerTintColor: '#9188E5',
                }}
            />
        </Stack>
    );
}
