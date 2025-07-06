import { Stack } from 'expo-router';
import 'react-native-reanimated';

export default function HelpLayout() {
    return (
        <Stack screenOptions={{ headerShown: true }}>
            <Stack.Screen
                name="index"
                options={{
                    title: 'Help',
                    headerTitleAlign: 'center',
                    headerTitleStyle: { color: '#000' },
                    headerTintColor: '#9188E5',
                }}
            />
        </Stack>
    );
}