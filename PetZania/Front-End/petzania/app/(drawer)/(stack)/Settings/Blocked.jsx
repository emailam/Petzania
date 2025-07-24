import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Alert, ActivityIndicator, RefreshControl } from 'react-native';
import { Image } from 'expo-image';
import { Ionicons } from '@expo/vector-icons';
import Toast from 'react-native-toast-message';
import EmptyState from '@/components/EmptyState';
import UserList from '@/components/UserList';
import { getBlockedUsers, unblockUser } from '@/services/friendsService';
import LottieView from 'lottie-react-native';

const defaultAvatar = require('@/assets/images/Defaults/default-user.png');

export default function Blocked() {
    const [blockedUsers, setBlockedUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [unblockingUsers, setUnblockingUsers] = useState(new Set());

    useEffect(() => {
        loadBlockedUsers();
    }, []);

    const loadBlockedUsers = async () => {
        try {
            setLoading(true);
            const response = await getBlockedUsers(0, 50); // page, size
            
            // Transform blocked users data for UserList compatibility
            const transformedBlockedUsers = (response?.content || []).map(item => ({
                ...item.blocked,
                blockId: item.blockId,
                blockedAt: item.createdAt
            }));
            
            setBlockedUsers(transformedBlockedUsers);
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
                return item.userId !== user.userId;
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

    const handleUserPress = (user) => {
        // Don't navigate to profile for blocked users, just show unblock option
        handleUnblock(user);
    };

    const renderUnblockButton = (user) => {
        const isUnblocking = unblockingUsers.has(user.userId);
        
        return (
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
        );
    };

    const renderEmptyState = () => (
        <EmptyState
            iconName="people-outline"
            title="No Blocked Users"
            subtitle="You haven't blocked anyone yet. When you block someone, they'll appear here."
        />
    );

    if (loading) {
        return (
            <View style={styles.container}>
                <View style={styles.loadingContainer}>
                    <LottieView
                        source={require("@/assets/lottie/loading.json")}
                        autoPlay
                        loop
                        style={styles.lottie}
                    />
                    <Text style={styles.loadingText}>Loading blocked users...</Text>
                    <Text style={styles.loadingSubText}>
                        Getting blocked user information
                    </Text>
                </View>
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <UserList
                users={blockedUsers}
                onUserPress={handleUserPress}
                keyExtractor={(item) => item.blockId || item.userId}
                showChevron={false}
                renderActionButton={renderUnblockButton}
                refreshControl={
                    <RefreshControl
                        refreshing={refreshing}
                        onRefresh={handleRefresh}
                        colors={['#9188E5']}
                        tintColor="#9188E5"
                    />
                }
                ListEmptyComponent={renderEmptyState}
                contentContainerStyle={{ padding: 16 }}
                showsVerticalScrollIndicator={false}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    lottie: {
        width: 80,
        height: 80,
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    loadingText: {
        marginTop: 16,
        fontSize: 18,
        color: '#9188E5',
        fontWeight: '600',
    },
    loadingSubText: {
        marginTop: 8,
        fontSize: 14,
        color: '#666',
        textAlign: 'center',
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
});