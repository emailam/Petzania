import React, { useState, useEffect, useContext, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { UserContext } from '@/context/UserContext';
import UserList from '@/components/UserList';
import { getFollowersByUserId, getNumberOfFollowersByUserId } from '@/services/friendsService';
import { getUserById, getUserProfilePicture } from '@/services/userService';
import Toast from 'react-native-toast-message';
import { useInfiniteQuery, useQuery } from '@tanstack/react-query';

export default function Followers() {
  const { userid } = useLocalSearchParams();
  const router = useRouter();
  const { user: currentUser } = useContext(UserContext);

  const [profileUser, setProfileUser] = useState(null);
  const isOwnProfile = currentUser?.userId === userid;

  // Get followers count
  const { data: followersCount } = useQuery({
    queryKey: ['followersCount', userid],
    queryFn: () => getNumberOfFollowersByUserId(userid),
    enabled: !!userid,
  });

  // Get profile user info
  useEffect(() => {
    const loadProfileUser = async () => {
      if (userid) {
        try {
          const userResponse = await getUserById(userid);
          setProfileUser(userResponse);
        } catch (error) {
          console.error('Error loading profile user:', error);
        }
      }
    };
    loadProfileUser();
  }, [userid]);

  // Get followers with infinite query
  const {
    data: followersResponse,
    isLoading,
    isError,
    error,
    refetch,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isRefetching,
  } = useInfiniteQuery({
    queryKey: ['followers', userid],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await getFollowersByUserId(pageParam, 20, 'createdAt', 'desc', userid);
      // Transform followers data and fetch profile pictures
      const followersWithPictures = await Promise.all(
        (response.content || []).map(async (followItem) => {
          try {
            const profileResponse = await getUserProfilePicture(followItem.follower.userId);
            return {
              ...followItem.follower,
              profilePictureURL: profileResponse.profilePictureURL,
              followId: followItem.followId,
              createdAt: followItem.createdAt
            };
          } catch (profileError) {
            console.warn('Failed to fetch profile picture for follower:', followItem.follower.userId);
            return {
              ...followItem.follower,
              followId: followItem.followId,
              createdAt: followItem.createdAt
            };
          }
        })
      );

      return {
        ...response,
        content: followersWithPictures
      };
    },
    getNextPageParam: (lastPage, pages) => {
      if (lastPage.last) return undefined;
      return pages.length;
    },
    enabled: !!userid,
  });

  // Flatten the paginated data
  const followers = useMemo(() => {
    return followersResponse?.pages.flatMap(page => page.content) || [];
  }, [followersResponse]);

  const handleUserPress = (user) => {
    router.push({
      pathname: `/UserModule/${user.userId}`,
      params: { username: user.username || 'User Profile' }
    });
  };

  const loadMoreFollowers = () => {
    if (hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  };

  const onRefresh = async () => {
    try {
      await refetch();
    } catch (error) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to refresh followers',
      });
    }
  };

  const renderEmptyState = () => (
    <View style={styles.emptyContainer}>
      <Ionicons name="people-outline" size={60} color="#ccc" />
      <Text style={styles.emptyTitle}>No Followers Yet</Text>
      <Text style={styles.emptySubtitle}>
        {isOwnProfile 
          ? "When people follow you, they'll appear here"
          : `${profileUser?.name || 'This user'} doesn't have any followers yet`
        }
      </Text>
    </View>
  );

  if (isLoading && !followersResponse) {
    return (
      <View style={styles.container}>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#9188E5" />
          <Text style={styles.loadingText}>Loading followers...</Text>
        </View>
      </View>
    );
  }

  if (isError) {
    return (
      <View style={styles.container}>
        <View style={styles.errorContainer}>
          <Ionicons name="alert-circle-outline" size={60} color="#ff4444" />
          <Text style={styles.errorTitle}>Error Loading Followers</Text>
          <Text style={styles.errorSubtitle}>
            {error?.message || 'Something went wrong'}
          </Text>
          <TouchableOpacity style={styles.retryButton} onPress={() => refetch()}>
            <Text style={styles.retryButtonText}>Try Again</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <UserList
        users={followers}
        onUserPress={handleUserPress}
        keyExtractor={(item) => item.userId}
        onEndReached={loadMoreFollowers}
        contentContainerStyle={{ padding: 16 }}
        onEndReachedThreshold={0.1}
        refreshControl={
          <RefreshControl
            refreshing={isRefetching}
            onRefresh={onRefresh}
            colors={['#9188E5']}
            tintColor="#9188E5"
          />
        }
        ListEmptyComponent={renderEmptyState}
        ListFooterComponent={
          followers.length > 0 && isFetchingNextPage ? (
            <View style={styles.footerLoading}>
              <ActivityIndicator size="small" color="#9188E5" />
              <Text style={styles.footerLoadingText}>Loading more...</Text>
            </View>
          ) : null
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
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    paddingTop: 50,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
    backgroundColor: '#fff',
  },
  backButton: {
    padding: 8,
    marginRight: 12,
  },
  headerContent: {
    flex: 1,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  headerSubtitle: {
    fontSize: 14,
    color: '#666',
    marginTop: 2,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 12,
    fontSize: 16,
    color: '#9188E5',
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 40,
  },
  errorTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#ff4444',
    marginTop: 16,
    textAlign: 'center',
  },
  errorSubtitle: {
    fontSize: 14,
    color: '#666',
    marginTop: 8,
    textAlign: 'center',
    lineHeight: 20,
  },
  retryButton: {
    backgroundColor: '#9188E5',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
    marginTop: 16,
  },
  retryButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 40,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#999',
    marginTop: 16,
    textAlign: 'center',
  },
  emptySubtitle: {
    fontSize: 14,
    color: '#999',
    marginTop: 8,
    textAlign: 'center',
    lineHeight: 20,
  },
  footerLoading: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 20,
    gap: 8,
  },
  footerLoadingText: {
    fontSize: 14,
    color: '#666',
  },
});