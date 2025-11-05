import { StyleSheet, Text, View, ActivityIndicator, RefreshControl } from 'react-native'
import LottieView from 'lottie-react-native'
import React, { useContext, useCallback, useState} from 'react'
import { useInfiniteQuery } from '@tanstack/react-query';
import { getFriendsByUserId } from '@/services/friendsService';
import { getUserProfilePicture } from '@/services/userService';
import { UserContext } from '@/context/UserContext';
import UserList from '@/components/UserList';
import EmptyState from '@/components/EmptyState';

export default function FriendsScreen() {
  const { user } = useContext(UserContext);
  const [refreshing, setRefreshing] = useState(false);

  const {
    data,
    error,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError,
    refetch
  } = useInfiniteQuery({
    queryKey: ['friends', user?.userId],
    queryFn: async ({ pageParam = 0 }) => {
      if (!user?.userId) {
        throw new Error('User ID is required');
      }
      const response = await getFriendsByUserId(pageParam, 20, 'createdAt', 'desc', user.userId);
      // Transform friendship data to user data and fetch profile pictures
      const friendsData = await Promise.all(
        (response.content || []).map(async (friendship) => {
          try {
            const profileResponse = await getUserProfilePicture(friendship.friend.userId);
            return {
              ...friendship.friend,
              profilePictureURL: profileResponse.profilePictureURL,
              friendshipId: friendship.friendshipId,
              createdAt: friendship.createdAt
            };
          } catch (profileError) {
            console.warn('Failed to fetch profile picture for user:', friendship.friend.userId);
            return {
              ...friendship.friend,
              friendshipId: friendship.friendshipId,
              createdAt: friendship.createdAt
            };
          }
        })
      );

      return {
        content: friendsData,
        last: response.last,
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        currentPage: pageParam
      };
    },
    getNextPageParam: (lastPage) => {
      return lastPage.last ? undefined : lastPage.currentPage + 1;
    },
    enabled: !!user?.userId,
    staleTime: 5 * 60 * 1000, // 5 minutes
    cacheTime: 10 * 60 * 1000, // 10 minutes
  });

  // Flatten the paginated data
  const friends = data?.pages?.flatMap(page => page.content) || [];

  const handleEndReached = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const EmptyComponent = () => (
    <EmptyState
      iconName="people-outline"
      title="No Friends Yet"
      subtitle="Start connecting with other pet lovers in your area. Your friends will appear here once you make connections!"
      style={styles.emptyState}
    />
  );

  const FooterComponent = () => {
    if (!isFetchingNextPage || friends.length === 0) return null;

    return (
      <View style={styles.footerLoader}>
        <ActivityIndicator size="small" color="#9188E5" />
        <Text style={styles.footerText}>Loading more friends...</Text>
      </View>
    );
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await refetch();
    setRefreshing(false);
  };


  if (isLoading) {
    return (
      <View style={styles.centered}>
        <LottieView
          source={require("@/assets/lottie/loading.json")}
          autoPlay
          loop
          style={styles.lottie}
        />
        <Text style={styles.loadingText}>Loading friends...</Text>
      </View>
    );
  }

  if (isError) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>{error?.message || 'An error occurred'}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <UserList
        users={friends}
        keyExtractor={(item) => item.friendshipId || item.userId}
        EmptyComponent={<EmptyComponent />}
        FooterComponent={<FooterComponent />}
        onEndReached={handleEndReached}
        onEndReachedThreshold={0.1}
        contentContainerStyle={{ padding: 12 }}
        itemStyle={styles.friendItem}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={['#9188E5']}
            tintColor="#9188E5"
          />
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  emptyState: {
    flex: 1,
    paddingHorizontal: 20,
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#9188E5',
  },
  errorText: {
    color: '#f44336',
    fontSize: 16,
    marginTop: 8,
  },
  friendItem: {
    borderRadius: 8,
    marginBottom: 4,
  },
  footerLoader: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 20,
    paddingHorizontal: 16,
  },
  footerText: {
    marginLeft: 8,
    fontSize: 14,
    color: '#9188E5',
  },
  lottie: {
    width: 80,
    height: 80,
  },
});