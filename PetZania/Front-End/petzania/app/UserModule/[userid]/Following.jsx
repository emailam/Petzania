import React, { useState, useEffect, useContext } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ActivityIndicator,
  RefreshControl,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { UserContext } from '@/context/UserContext';
import UserList from '@/components/UserList';
import EmptyState from '@/components/EmptyState';
import { getFollowingByUserId, getNumberOfFollowingByUserId, unfollowUser } from '@/services/friendsService';
import { getUserById, getUserProfilePicture } from '@/services/userService';
import Toast from 'react-native-toast-message';
import LottieView from 'lottie-react-native';

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
  const [unfollowingUsers, setUnfollowingUsers] = useState(new Set());

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

  const handleUnfollow = (followedUser) => {
    Alert.alert(
      "Unfollow User",
      `Are you sure you want to unfollow ${followedUser.username || followedUser.firstName + ' ' + followedUser.lastName}?`,
      [
        {
          text: "Cancel",
          style: "cancel"
        },
        {
          text: "Unfollow",
          style: "destructive",
          onPress: () => confirmUnfollow(followedUser)
        }
      ]
    );
  };

  const confirmUnfollow = async (followedUser) => {
    try {
      setUnfollowingUsers(prev => new Set(prev).add(followedUser.userId));

      await unfollowUser(followedUser.userId);

      // Remove user from following list
      setFollowing(prev => prev.filter(item => item.userId !== followedUser.userId));

      Toast.show({
        type: 'success',
        text1: 'User Unfollowed',
        text2: `You are no longer following ${followedUser.username || followedUser.firstName}`,
        position: 'top',
        visibilityTime: 2000,
      });
    } catch (error) {
      console.error('Error unfollowing user:', error);
      Toast.show({
        type: 'error',
        text1: 'Failed to unfollow user',
        text2: 'Please try again later',
        position: 'top',
        visibilityTime: 3000,
      });
    } finally {
      setUnfollowingUsers(prev => {
        const newSet = new Set(prev);
        newSet.delete(followedUser.userId);
        return newSet;
      });
    }
  };

  const renderUnfollowButton = (followedUser) => {
    if (!isOwnProfile) return null; // Only show unfollow button on own profile
    
    const isUnfollowing = unfollowingUsers.has(followedUser.userId);
    
    return (
      <TouchableOpacity
        style={[styles.unfollowButton, isUnfollowing && styles.unfollowButtonDisabled]}
        onPress={() => handleUnfollow(followedUser)}
        disabled={isUnfollowing}
      >
        {isUnfollowing ? (
          <ActivityIndicator size="small" color="#fff" />
        ) : (
          <>
            <Ionicons name="person-remove-outline" size={16} color="#fff" />
            <Text style={styles.unfollowButtonText}>Unfollow</Text>
          </>
        )}
      </TouchableOpacity>
    );
  };

  const renderEmptyState = () => (
    <EmptyState
      iconName="people-outline"
      title="No Following"
      subtitle={
        isOwnProfile 
          ? "When you follow people, they'll appear here"
          : `${profileUser?.name || 'This user'} isn't following anyone yet`
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
          <Text style={styles.loadingText}>Loading following...</Text>
          <Text style={styles.loadingSubText}>
            Getting following information
          </Text>
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
        showChevron={!isOwnProfile}
        renderActionButton={isOwnProfile ? renderUnfollowButton : null}
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
  unfollowButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ff6b6b',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    minWidth: 90,
    justifyContent: 'center',
  },
  unfollowButtonDisabled: {
    backgroundColor: '#ccc',
  },
  unfollowButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
    marginLeft: 4,
  },
});
