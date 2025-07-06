import { Stack, useRouter } from 'expo-router';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Header } from "@react-navigation/elements";

export default function UserLayout() {
    const router = useRouter();

    const CustomHeader = ({ title, onBack }) => (
        <View style={styles.customHeader}>
            <TouchableOpacity onPress={onBack} style={styles.headerBackButton}>
                <Ionicons name="arrow-back" size={24} color="#9188E5" />
            </TouchableOpacity>
            <Text style={styles.headerTitle}>{title}</Text>
        </View>
    );

    return (
        <Stack
            screenOptions={{
                headerBackTitleVisible: false,
                headerTintColor: '#9188E5',
                headerStyle: { backgroundColor: '#FFF' },
                header: ({ options, route, navigation }) => (
                    <CustomHeader
                        title={route.params?.username || options.headerTitle || 'User Profile'}
                        onBack={() => {
                            if (router.canGoBack()) {
                                router.back();
                            } else {
                                router.push('/Home');
                            }
                        }}
                    />
                ),
            }}
        >
            <Stack.Screen name="index" />
            <Stack.Screen name="Followers" />
            <Stack.Screen name="Following" />
            <Stack.Screen name="Friends" />
            <Stack.Screen
                name="Blocked"
                options={{
                    headerShown: true,
                    headerTitle: 'Blocked Users',
                }}
            />
        </Stack>
    );
}

const styles = StyleSheet.create({
    customHeader: {
        height: 50,
        backgroundColor: '#FFF',
        flexDirection: 'row',
        alignItems: 'flex-end',
        paddingBottom: 12,
        paddingHorizontal: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#f0f0f0',
    },
    headerBackButton: {
        paddingRight: 12,
    },
    headerTitle: {
        fontSize: 20,
        fontWeight: '600',
        color: '#000',
        marginLeft: 8,
    },
});