import { Stack } from 'expo-router';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';

export default function NotificationsLayout() {
    const router = useRouter();

    const CustomHeader = () => (
        <View style={styles.customHeader}>
            <View style={styles.headerRight}>
                <TouchableOpacity
                    onPress={() => {/* Mark all as read */}}
                    style={styles.markAllButton}
                >
                    <Text style={styles.markAllText}>Mark all read</Text>
                </TouchableOpacity>
            </View>
        </View>
    );

    return (
        <Stack>
            <Stack.Screen 
                name="index"
                options={{
                    headerShown: false,
                    header: () => <CustomHeader />,
                }}
            />
        </Stack>
    );
}

const styles = StyleSheet.create({
    customHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingHorizontal: 16,
        paddingVertical: 12,
        paddingTop: 50,
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#e9ecef',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.1,
        shadowRadius: 2,
        elevation: 3,
    },
    headerLeft: {
        flexDirection: 'row',
        alignItems: 'center',
        flex: 1,
    },
    backButton: {
        marginRight: 12,
        padding: 4,
    },
    headerTitle: {
        fontSize: 20,
        fontWeight: 'bold',
    },
    headerRight: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
    },
    markAllButton: {
        paddingHorizontal: 12,
        paddingVertical: 6,
        backgroundColor: '#f0f0f0',
        borderRadius: 16,
    },
    markAllText: {
        fontSize: 12,
        color: '#9188E5',
        fontWeight: '600',
    },
    settingsButton: {
        padding: 4,
    },
});