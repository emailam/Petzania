import { Stack } from 'expo-router';

export default function AdoptionLayout() {
    return (
        <Stack>
            <Stack.Screen name="index" options={{ headerShown: false }} />
        </Stack>
    );
}