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
import { getFriendsByUserId, getNumberOfFriendsByUserId } from '@/services/friendsService';
import { getUserById, getUserProfilePicture } from '@/services/userService';
import Toast from 'react-native-toast-message';

export default function Friends() {
  const { userid } = useLocalSearchParams();
  const router = useRouter();
  const { user: currentUser } = useContext(UserContext);

  const [friends, setFriends] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [friendsCount, setFriendsCount] = useState(0);
  const [profileUser, setProfileUser] = useState(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  const isOwnProfile = currentUser?.userId === userid;

  useEffect(() => {
    loadInitialData();
  }, [userid]);

  const loadInitialData = async () => {
    try {
      setLoading(true);
      // Load profile user info and friends in parallel
      const [userResponse, friendsResponse, countResponse] = await Promise.all([
        getUserById(userid),
        getFriendsByUserId(0, 20, 'createdAt', 'desc', userid),
        getNumberOfFriendsByUserId ? getNumberOfFriendsByUserId(userid) : Promise.resolve(0)
      ]);

      setProfileUser(userResponse);

      // Transform friends data and fetch profile pictures
      const friendsData = await Promise.all(
        (friendsResponse.content || []).map(async (friendItem) => {
          try {
            const response = await getUserProfilePicture(friendItem.friend.userId);

            return {
              ...friendItem.friend,
              profilePictureURL: response.profilePictureURL,
              friendshipId: friendItem.friendshipId,
              createdAt: friendItem.createdAt
            };
          } catch (profileError) {
            console.warn('Failed to fetch profile picture for friend:', friendItem.friend.userId);
            return {
              ...friendItem.friend,
              friendshipId: friendItem.friendshipId,
              createdAt: friendItem.createdAt
            };
          }
        })
      );
      
      setFriends(friendsData);
      setFriendsCount(countResponse);
      setHasMore(friendsData.length >= 20 && !friendsResponse.last);
      setPage(0);
    } catch (error) {
      console.error('Error loading friends:', error);
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to load friends',
      });
    } finally {
      setLoading(false);
    }
  };
  const loadMoreFriends = async () => {
    if (loadingMore || !hasMore) return;

    try {
      setLoadingMore(true);
      const nextPage = page + 1;
      const response = await getFriendsByUserId(nextPage, 20, 'createdAt', 'desc', userid);
      
      // Transform new friends data and fetch profile pictures
      const newFriendsData = await Promise.all(
        (response.content || []).map(async (friendItem) => {
          try {
            const response = await getUserProfilePicture(friendItem.friend.userId);
            return {
              ...friendItem.friend,
              profilePictureURL: response.profilePictureURL,
              friendshipId: friendItem.friendshipId,
              createdAt: friendItem.createdAt
            };
          } catch (profileError) {
            console.warn('Failed to fetch profile picture for friend:', friendItem.friend.userId);
            return {
              ...friendItem.friend,
              friendshipId: friendItem.friendshipId,
              createdAt: friendItem.createdAt
            };
          }
        })
      );
      
      setFriends(prev => [...prev, ...newFriendsData]);
      setPage(nextPage);
      setHasMore(newFriendsData.length >= 20 && !response.last);
    } catch (error) {
      console.error('Error loading more friends:', error);
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to load more friends',
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
      <Text style={styles.emptyTitle}>No Friends Yet</Text>
      <Text style={styles.emptySubtitle}>
        {isOwnProfile 
          ? "When you make friends, they'll appear here"
          : `${profileUser?.name || 'This user'} doesn't have any friends yet`
        }
      </Text>
    </View>
  );

  if (loading) {
    return (
      <View style={styles.container}>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#9188E5" />
          <Text style={styles.loadingText}>Loading friends...</Text>
        </View>
      </View>
    );
  }
  return (
    <View style={styles.container}>
      <UserList
        users={friends}
        onUserPress={handleUserPress}
        keyExtractor={(item) => item.friendshipId || item.userId}
        onEndReached={loadMoreFriends}
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
