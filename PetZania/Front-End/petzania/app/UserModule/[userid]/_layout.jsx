import { Stack, useRouter } from 'expo-router';
import { TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

export default function UserLayout() {
    const router = useRouter();
    const CustomBackButton = () => (
        <TouchableOpacity
            onPress={() => {
                if (router.canGoBack()) {
                    router.back();
                } else {
                    router.push('/Home');
                }
            }}
            style={{ paddingRight: 12 }}
        >
            <Ionicons name="arrow-back" size={24} color="#9188E5" />
        </TouchableOpacity>
    );

    return (
        <Stack
            screenOptions={{
                headerBackTitleVisible: false,
                headerTintColor: '#9188E5',
                headerStyle: { backgroundColor: '#FFF' },
            }}
        >
            <Stack.Screen
                name="index"
                options={({ route }) => ({
                    headerShown: true,
                    headerTitle: route.params?.username || 'User Profile',
                    headerTitleStyle: { color: '#000', fontWeight: '600' },
                    headerLeft: () => <CustomBackButton />,
                })}
            />
            <Stack.Screen
                name="Followers"
                options={({ route }) => ({
                    headerShown: true,
                    headerTitle: route.params?.username || 'User Profile',
                    headerTitleStyle: { color: '#000', fontWeight: '600' },
                    headerLeft: () => <CustomBackButton />,
                })}
            />
            <Stack.Screen
                name="Following"
                options={({ route }) => ({
                    headerShown: true,
                    headerTitle: route.params?.username || 'User Profile',
                    headerTitleStyle: { color: '#000', fontWeight: '600' },
                    headerLeft: () => <CustomBackButton />,
                })}
            />
            <Stack.Screen
                name="Friends"
                options={({ route }) => ({
                    headerShown: true,
                    headerTitle: route.params?.username || 'User Profile',
                    headerTitleStyle: { color: '#000', fontWeight: '600' },
                    headerLeft: () => <CustomBackButton />,
                })}
            />
            <Stack.Screen
                name="Blocked"
                options={() => ({
                    headerShown: true,
                    headerTitle: 'Blocked Users',
                    headerTitleStyle: { color: '#000', fontWeight: '600' },
                    headerLeft: () => <CustomBackButton />,
                })}
            />
        </Stack>
    );
}