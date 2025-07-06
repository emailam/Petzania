import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet, Alert, ActivityIndicator, RefreshControl } from 'react-native';
import { Image } from 'expo-image';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import Toast from 'react-native-toast-message';
import { getBlockedUsers, unblockUser } from '@/services/friendsService';

const defaultAvatar = require('@/assets/images/Defaults/default-user.png');

export default function Blocked() {
    const { userid } = useLocalSearchParams();
    const router = useRouter();
    const [blockedUsers, setBlockedUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [unblockingUsers, setUnblockingUsers] = useState(new Set());

    useEffect(() => {
        loadBlockedUsers();
    }, [userid]);

    const loadBlockedUsers = async () => {
        try {
            setLoading(true);
            const response = await getBlockedUsers(0, 50); // page, size
            setBlockedUsers(response?.content || []);
        } catch (error) {
            console.error('Error loading blocked users:', error);
            Toast.show({
                type: 'error',
                text1: 'Failed to load blocked users',
                text2: 'Please try again later',
                position: 'top',
                visibilityTime: 3000,
            });
        } finally {
            setLoading(false);
        }
    };

    const handleRefresh = async () => {
        setRefreshing(true);
        await loadBlockedUsers();
        setRefreshing(false);
    };

    const handleUnblock = (user) => {
        Alert.alert(
            "Unblock User",
            `Are you sure you want to unblock ${user.username || user.firstName + ' ' + user.lastName}?`,
            [
                {
                    text: "Cancel",
                    style: "cancel"
                },
                {
                    text: "Unblock",
                    style: "destructive",
                    onPress: () => confirmUnblock(user)
                }
            ]
        );
    };

    const confirmUnblock = async (user) => {
        try {
            setUnblockingUsers(prev => new Set(prev).add(user.userId));

            await unblockUser(user.userId);

            // Remove user from blocked list - fix the filtering logic
            console.log('Unblocking user:', user, "Blocked Users Before:", blockedUsers);
            setBlockedUsers(prev => prev.filter(item => {
                const blockedUser = item.blocked || item;
                return blockedUser.userId !== user.userId;
            }));

            Toast.show({
                type: 'success',
                text1: 'User unblocked',
                text2: `${user.username || user.firstName} has been unblocked`,
                position: 'top',
                visibilityTime: 2000,
            });
        } catch (error) {
            console.error('Error unblocking user:', error);
            Toast.show({
                type: 'error',
                text1: 'Failed to unblock user',
                text2: 'Please try again later',
                position: 'top',
                visibilityTime: 3000,
            });
        } finally {
            setUnblockingUsers(prev => {
                const newSet = new Set(prev);
                newSet.delete(user.userId);
                return newSet;
            });
        }
    };

    const renderUser = ({ item }) => {
        console.log('Rendering blocked user:', item);
        const user = item.blocked;
        const isUnblocking = unblockingUsers.has(user.userId);
        const fullName = `${user.firstName || ''} ${user.lastName || ''}`.trim();
        const displayName = user.username || fullName || 'Unknown User';

        return (
            <TouchableOpacity 
                style={styles.userContainer}
                onPress={() => handleUnblock(user)}
                activeOpacity={0.7}
            >
                <View style={styles.userInfo}>
                    <Image
                        source={defaultAvatar}
                        style={styles.avatar}
                        contentFit="cover"
                    />
                    <View style={styles.userDetails}>
                        <Text style={styles.displayName} numberOfLines={1}>
                            {displayName}
                        </Text>
                        {user.username && fullName && (
                            <Text style={styles.fullName} numberOfLines={1}>
                                {fullName}
                            </Text>
                        )}
                        <Text style={styles.blockedDate}>
                            Blocked
                        </Text>
                    </View>
                </View>

                <TouchableOpacity
                    style={[styles.unblockButton, isUnblocking && styles.unblockButtonDisabled]}
                    onPress={() => handleUnblock(user)}
                    disabled={isUnblocking}
                >
                    {isUnblocking ? (
                        <ActivityIndicator size="small" color="#fff" />
                    ) : (
                        <>
                            <Ionicons name="person-add-outline" size={16} color="#fff" />
                            <Text style={styles.unblockButtonText}>Unblock</Text>
                        </>
                    )}
                </TouchableOpacity>
            </TouchableOpacity>
        );
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'Recently';

        const date = new Date(dateString);
        const now = new Date();
        const diffTime = Math.abs(now - date);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

        if (diffDays === 1) return 'Yesterday';
        if (diffDays < 7) return `${diffDays} days ago`;
        if (diffDays < 30) return `${Math.ceil(diffDays / 7)} weeks ago`;
        if (diffDays < 365) return `${Math.ceil(diffDays / 30)} months ago`;
        return `${Math.ceil(diffDays / 365)} years ago`;
    };

    const renderEmptyState = () => (
        <View style={styles.emptyContainer}>
            <Ionicons name="people-outline" size={80} color="#ccc" />
            <Text style={styles.emptyTitle}>No Blocked Users</Text>
            <Text style={styles.emptySubtitle}>
                You haven't blocked anyone yet. When you block someone, they'll appear here.
            </Text>
        </View>
    );

    if (loading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#9188E5" />
                <Text style={styles.loadingText}>Loading blocked users...</Text>
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <FlatList
                data={blockedUsers}
                renderItem={renderUser}
                keyExtractor={(item) => item.blockId || item.blocked?.userId}
                showsVerticalScrollIndicator={false}
                refreshControl={
                    <RefreshControl
                        refreshing={refreshing}
                        onRefresh={handleRefresh}
                        colors={['#9188E5']}
                        tintColor="#9188E5"
                    />
                }
                ListEmptyComponent={renderEmptyState}
                contentContainerStyle={[
                    styles.listContainer,
                    blockedUsers.length === 0 && styles.emptyListContainer
                ]}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#fff',
    },
    loadingText: {
        marginTop: 10,
        fontSize: 16,
        color: '#666',
    },
    listContainer: {
        padding: 16,
    },
    emptyListContainer: {
        flex: 1,
        justifyContent: 'center',
    },
    userContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingVertical: 12,
        paddingHorizontal: 4,
        marginBottom: 8,
    },
    userInfo: {
        flexDirection: 'row',
        alignItems: 'center',
        flex: 1,
        marginRight: 12,
    },
    avatar: {
        width: 50,
        height: 50,
        borderRadius: 25,
        marginRight: 12,
        borderWidth: 1,
        borderColor: '#9188E5',
        backgroundColor: '#f0f0f0',
    },
    userDetails: {
        flex: 1,
    },
    displayName: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
        marginBottom: 2,
    },
    fullName: {
        fontSize: 14,
        color: '#666',
        marginBottom: 2,
    },
    blockedDate: {
        fontSize: 12,
        color: '#999',
    },
    unblockButton: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#9188E5',
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 20,
        minWidth: 80,
        justifyContent: 'center',
    },
    unblockButtonDisabled: {
        backgroundColor: '#ccc',
    },
    unblockButtonText: {
        color: '#fff',
        fontSize: 14,
        fontWeight: '600',
        marginLeft: 4,
    },
    emptyContainer: {
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 60,
        paddingHorizontal: 40,
    },
    emptyTitle: {
        fontSize: 24,
        fontWeight: '600',
        color: '#333',
        marginTop: 20,
        marginBottom: 10,
        textAlign: 'center',
    },
    emptySubtitle: {
        fontSize: 16,
        color: '#666',
        textAlign: 'center',
        lineHeight: 22,
    },
});