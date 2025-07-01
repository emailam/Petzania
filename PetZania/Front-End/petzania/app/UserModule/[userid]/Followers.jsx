import React, { useState, useEffect, useContext } from 'react';
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

  const [followers, setFollowers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [followersCount, setFollowersCount] = useState(0);
  const [profileUser, setProfileUser] = useState(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  const isOwnProfile = currentUser?.userId === userid;

  useEffect(() => {
    loadInitialData();
  }, [userid]);

  const {
    data: followersResponse,
    isLoading,
    isError,
    error,
    refetch,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ['followers', userid],
    queryFn: ({ pageParam = 0 }) => 
      getFollowersByUserId(pageParam, 20, 'createdAt', 'desc', userid),
      getNextPageParam: (lastPage, pages) => {
        if (lastPage.last) return undefined;
        return pages.length;
    },
    enabled: !!userid,
  });


  const loadInitialData = async () => {
    try {
      setLoading(true);
      // Load profile user info and followers in parallel
      const [userResponse, countResponse] = await Promise.all([
        getUserById(userid),
        getNumberOfFollowersByUserId(userid)
      ]);

      setProfileUser(userResponse);
      
      // Transform followers data and fetch profile pictures
      const followersData = await Promise.all(
        (followersResponse.content || []).map(async (followItem) => {
          try {
            const response = await getUserProfilePicture(followItem.follower.userId);
            
            return {
              ...followItem.follower,
              profilePictureURL: response.profilePictureURL,
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
      
      setFollowers(followersData);
      setFollowersCount(countResponse);
      setHasMore(followersData.length >= 20 && !followersResponse.last);
      setPage(0);
    } catch (error) {
      console.error('Error loading followers:', error);
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to load followers',
      });
    } finally {
      setLoading(false);
    }
  };
  const loadMoreFollowers = async () => {
    if (loadingMore || !hasMore) return;

    try {
      setLoadingMore(true);
      const nextPage = page + 1;
      const response = await getFollowersByUserId(nextPage, 20, 'createdAt', 'desc', userid);
      
      // Transform new followers data and fetch profile pictures
      const newFollowersData = await Promise.all(
        (response.content || []).map(async (followItem) => {
          try {
            const response = await getUserProfilePicture(followItem.follower.userId);
            return {
              ...followItem.follower,
              profilePictureURL: response.profilePictureURL,
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
      
      setFollowers(prev => [...prev, ...newFollowersData]);
      setPage(nextPage);
      setHasMore(newFollowersData.length >= 20 && !response.last);
    } catch (error) {
      console.error('Error loading more followers:', error);
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to load more followers',
      });
    } finally {
      setLoadingMore(false);
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await loadInitialData();
    setRefreshing(false);
  };

  const handleUserPress = (user) => {
    router.push({
      pathname: `/UserModule/${user.userId}`,
      params: { username: user.username || 'User Profile' }
    });
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

    if (loading) {
        return (
            <View style={styles.container}>
                <View style={styles.loadingContainer}>
                    <ActivityIndicator size="large" color="#9188E5" />
                    <Text style={styles.loadingText}>Loading followers...</Text>
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
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={['#9188E5']}
            tintColor="#9188E5"
          />
        }
        ListEmptyComponent={renderEmptyState}
        ListFooterComponent={
          loadingMore ? (
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
    paddingTop: 50, // Account for status bar
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
