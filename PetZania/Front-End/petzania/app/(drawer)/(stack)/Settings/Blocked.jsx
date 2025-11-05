import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Alert, ActivityIndicator, RefreshControl } from 'react-native';

import { Ionicons } from '@expo/vector-icons';
import Toast from 'react-native-toast-message';
import EmptyState from '@/components/EmptyState';
import UserList from '@/components/UserList';
import { getBlockedUsers, unblockUser } from '@/services/friendsService';

import LoadingIndicator from '@/components/LoadingIndicator';

export default function Blocked() {
    const [blockedUsers, setBlockedUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [unblockingUsers, setUnblockingUsers] = useState(new Set());
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);

    const PAGE_SIZE = 10;

    useEffect(() => {
        loadBlockedUsers(0, true);
    }, []);

    const loadBlockedUsers = async (pageNum = 0, isInitial = false) => {
        try {
            if (isInitial) {
                setLoading(true);
            } else {
                setLoadingMore(true);
            }

            const response = await getBlockedUsers(pageNum, PAGE_SIZE);

            // Transform blocked users data for UserList compatibility
            const transformedBlockedUsers = (response?.content || []).map(item => ({
                ...item.blocked,
                blockId: item.blockId,
                blockedAt: item.createdAt
            }));

            if (isInitial) {
                setBlockedUsers(transformedBlockedUsers);
            } else {
                // Append new users to existing list
                setBlockedUsers(prev => [...prev, ...transformedBlockedUsers]);
            }

            // Check if there are more pages
            setHasMore(transformedBlockedUsers.length === PAGE_SIZE);
            setPage(pageNum);
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
            setLoadingMore(false);
        }
    };

    const handleRefresh = async () => {
        setRefreshing(true);
        setPage(0);
        setHasMore(true);
        await loadBlockedUsers(0, true);
        setRefreshing(false);
    };

    const handleLoadMore = useCallback(() => {
        if (!loadingMore && hasMore && !loading) {
            const nextPage = page + 1;
            loadBlockedUsers(nextPage, false);
        }
    }, [loadingMore, hasMore, loading, page]);

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

            // Optimistically remove user from blocked list
            setBlockedUsers(prev => prev.filter(item => item.userId !== user.userId));

            Toast.show({
                type: 'success',
                text1: 'User unblocked',
                text2: `${user.username || user.firstName} has been unblocked`,
                position: 'top',
                visibilityTime: 2000,
            });
        } catch (error) {
            console.error('Error unblocking user:', error);

            // Reload the list on error to ensure data consistency
            await loadBlockedUsers(0, true);

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

    const renderFooter = () => {
        if (!loadingMore) return null;

        return (
            <View style={styles.footerLoader}>
                <LoadingIndicator />
            </View>
        );
    };

    if (loading) {
        return (
            <View style={[styles.container, styles.centerContent]}>
                <LoadingIndicator text="Loading blocked users..." />
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
                ListFooterComponent={renderFooter}
                contentContainerStyle={{ padding: 16 }}
                showsVerticalScrollIndicator={false}
                onEndReached={handleLoadMore}
                onEndReachedThreshold={0.5}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    centerContent: {
        justifyContent: 'center',
        alignItems: 'center',
    },
    footerLoader: {
        paddingVertical: 20,
        alignItems: 'center',
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