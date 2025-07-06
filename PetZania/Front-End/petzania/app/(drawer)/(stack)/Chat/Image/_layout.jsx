import { Stack } from "expo-router";

export default function ImageLayout() {
    return (
        <Stack>
            <Stack.Screen
                name="[imageuri]"
                options={{
                    headerShown: false,
                }}
            />
        </Stack>
    );
}
