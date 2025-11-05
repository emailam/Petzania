import React, { useState, useEffect, useContext } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
  Alert,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { UserContext } from '@/context/UserContext';
import UserList from '@/components/UserList';
import EmptyState from '@/components/EmptyState';
import { getFriendsByUserId, getNumberOfFriendsByUserId, removeFriend } from '@/services/friendsService';
import { getUserById, getUserProfilePicture } from '@/services/userService';
import Toast from 'react-native-toast-message';
import LottieView from 'lottie-react-native';

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
  const [unfriendingUsers, setUnfriendingUsers] = useState(new Set());

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

  const handleUnfriend = (friend) => {
    Alert.alert(
      "Remove Friend",
      `Are you sure you want to remove ${friend.username || friend.firstName + ' ' + friend.lastName} from your friends?`,
      [
        {
          text: "Cancel",
          style: "cancel"
        },
        {
          text: "Remove",
          style: "destructive",
          onPress: () => confirmUnfriend(friend)
        }
      ]
    );
  };

  const confirmUnfriend = async (friend) => {
    try {
      setUnfriendingUsers(prev => new Set(prev).add(friend.userId));

      await removeFriend(friend.userId);

      // Remove friend from list
      setFriends(prev => prev.filter(item => item.userId !== friend.userId));

      Toast.show({
        type: 'success',
        text1: 'Friend Removed',
        text2: `${friend.username || friend.firstName} has been removed from your friends`,
        position: 'top',
        visibilityTime: 2000,
      });
    } catch (error) {
      console.error('Error removing friend:', error);
      Toast.show({
        type: 'error',
        text1: 'Failed to remove friend',
        text2: 'Please try again later',
        position: 'top',
        visibilityTime: 3000,
      });
    } finally {
      setUnfriendingUsers(prev => {
        const newSet = new Set(prev);
        newSet.delete(friend.userId);
        return newSet;
      });
    }
  };

  const renderUnfriendButton = (friend) => {
    if (!isOwnProfile) return null; // Only show unfriend button on own profile
    
    const isUnfriending = unfriendingUsers.has(friend.userId);
    
    return (
      <TouchableOpacity
        style={[styles.unfriendButton, isUnfriending && styles.unfriendButtonDisabled]}
        onPress={() => handleUnfriend(friend)}
        disabled={isUnfriending}
      >
        {isUnfriending ? (
          <ActivityIndicator size="small" color="#fff" />
        ) : (
          <>
            <Ionicons name="person-remove-outline" size={16} color="#fff" />
            <Text style={styles.unfriendButtonText}>Remove</Text>
          </>
        )}
      </TouchableOpacity>
    );
  };

  const renderEmptyState = () => (
    <EmptyState
      iconName="people-outline"
      title="No Friends Yet"
      subtitle={
        isOwnProfile 
          ? "When you make friends, they'll appear here"
          : `${profileUser?.name || 'This user'} doesn't have any friends yet`
      }
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
          <Text style={styles.loadingText}>Loading friends...</Text>
          <Text style={styles.loadingSubText}>
            Getting friend information
          </Text>
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
        showChevron={!isOwnProfile}
        renderActionButton={isOwnProfile ? renderUnfriendButton : null}
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
  unfriendButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ff4444',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    minWidth: 80,
    justifyContent: 'center',
  },
  unfriendButtonDisabled: {
    backgroundColor: '#ccc',
  },
  unfriendButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
    marginLeft: 4,
  },
});
