import React, { useState, useEffect, useContext } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ActivityIndicator,
  RefreshControl,
  TouchableOpacity,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { UserContext } from '@/context/UserContext';
import UserList from '@/components/UserList';
import { getFollowingByUserId, getNumberOfFollowingByUserId } from '@/services/friendsService';
import { getUserById, getUserProfilePicture } from '@/services/userService';
import Toast from 'react-native-toast-message';

export default function Following() {
  const { userid } = useLocalSearchParams();
  const router = useRouter();

  const { user: currentUser } = useContext(UserContext);

  const [following, setFollowing] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [page, setPage] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);
  const [profileUser, setProfileUser] = useState(null);

  const isOwnProfile = currentUser?.userId === userid;
  useEffect(() => {
    loadInitialData();
  }, [userid]);

  const loadInitialData = async () => {
    try {
      setLoading(true);
      // Load profile user info and following in parallel
      const [userResponse, followingResponse, countResponse] = await Promise.all([
        getUserById(userid),
        getFollowingByUserId(0, 20, 'createdAt', 'desc', userid),
        getNumberOfFollowingByUserId(userid)
      ]);

      setProfileUser(userResponse);
        // Transform following data and fetch profile pictures
      const followingData = await Promise.all(
        (followingResponse.content || []).map(async (followItem) => {
          try {
            console.log(followItem);
            const response = await getUserProfilePicture(followItem.followed.userId);
            return {
              ...followItem.followed,
              profilePictureURL: response.profilePictureURL,
              followId: followItem.followId,
              createdAt: followItem.createdAt
            };
          } catch (profileError) {
            console.warn('Failed to fetch profile picture for followed user:', followItem.followed.userId);
            return {
              ...followItem.followed,
              followId: followItem.followId,
              createdAt: followItem.createdAt
            };
          }
        })
      );

      setFollowing(followingData);
      setFollowingCount(countResponse);
      setHasMore(followingData.length >= 20 && !followingResponse.last);
      setPage(0);
    } catch (error) {
      console.error('Error loading following:', error);
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to load following',
      });
    } finally {
      setLoading(false);
    }
  };

  const loadMoreFollowing = async () => {
    if (loadingMore || !hasMore) return;

    try {
      setLoadingMore(true);
      const nextPage = page + 1;
      const response = await getFollowingByUserId(nextPage, 20, 'createdAt', 'desc', userid);
        // Transform new following data and fetch profile pictures
      const newFollowingData = await Promise.all(
        (response.content || []).map(async (followItem) => {
          try {
            const response = await getUserProfilePicture(followItem.followed.userId);
            return {
              ...followItem.followed,
              profilePictureURL: response.profilePictureURL,
              followId: followItem.followId,
              createdAt: followItem.createdAt
            };
          } catch (profileError) {
            console.warn('Failed to fetch profile picture for followed user:', followItem.followed.userId);
            return {
              ...followItem.followed,
              followId: followItem.followId,
              createdAt: followItem.createdAt
            };
          }
        })
      );
      
      setFollowing(prev => [...prev, ...newFollowingData]);
      setPage(nextPage);
      setHasMore(newFollowingData.length >= 20 && !response.last);
    } catch (error) {
      console.error('Error loading more following:', error);
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to load more following',
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
      <Text style={styles.emptyTitle}>No Following</Text>
      <Text style={styles.emptySubtitle}>
        {isOwnProfile 
          ? "When you follow people, they'll appear here"
          : `${profileUser?.name || 'This user'} isn't following anyone yet`
        }
      </Text>
    </View>
  );

  if (loading) {
    return (
      <View style={styles.container}>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#9188E5" />
          <Text style={styles.loadingText}>Loading following...</Text>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <UserList
        users={following}
        onUserPress={handleUserPress}
        keyExtractor={(item) => item.userId}
        onEndReached={loadMoreFollowing}
        onEndReachedThreshold={0.1}
        contentContainerStyle={{ padding: 12 }}
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
