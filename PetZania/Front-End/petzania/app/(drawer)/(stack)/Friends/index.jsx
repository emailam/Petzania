import { StyleSheet, Text, View, ActivityIndicator } from 'react-native'
import React, { useEffect, useState, useContext } from 'react'
import { getFriendsByUserId } from '@/services/friendsService';
import { getUserProfilePicture } from '@/services/userService';
import { UserContext } from '@/context/UserContext';
import UserList from '@/components/UserList';

export default function FriendsScreen() {
  const [friends, setFriends] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);
  const [hasMore, setHasMore] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const { user } = useContext(UserContext);


  useEffect(() => {
    const fetchFriends = async () => {
      try {
        setLoading(true);
        setError(null);
        setCurrentPage(0);
        setHasMore(true);

        if (!user?.userId) {
          setError('User not found. Please log in again.');
          return;
        }
        const response = await getFriendsByUserId(0, 20, 'createdAt', 'desc', user.userId);

        // Transform friendship data to user data for the UserList component
        const friendsData = await Promise.all(
          (response.content || []).map(async (friendship) => {
            try {
              const response = await getUserProfilePicture(friendship.friend.userId);
              return {
                ...friendship.friend,
                profilePictureURL: response.profilePictureURL,
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

        setFriends(friendsData);
        setCurrentPage(0);

        // Check if there are more pages
        setHasMore(response.content && response.content.length === 20 && !response.last);

      } catch (err) {
        console.error('Error fetching friends:', err);
        setError('Failed to load friends.');
      } finally {
        setLoading(false);
      }
    };
    fetchFriends();
  }, [user]);

  const loadMoreFriends = async () => {
    if (loadingMore || !hasMore || !user?.userId) return;

    try {
      setLoadingMore(true);
      const nextPage = currentPage + 1;
      const response = await getFriendsByUserId(nextPage, 20, 'createdAt', 'desc', user.userId);

      const newFriendsData = await Promise.all(
        (response.content || []).map(async (friendship) => {
          try {
            const profilePictureURL = await getUserProfilePicture(friendship.friend.userId);
            return {
              ...friendship.friend,
              profilePictureURL: profilePictureURL,
              friendshipId: friendship.friendshipId,
              createdAt: friendship.createdAt
            };
          } catch (profileError) {
            // If profile picture fetch fails, continue without it
            console.warn('Failed to fetch profile picture for user:', friendship.friend.userId);
            return {
              ...friendship.friend,
              friendshipId: friendship.friendshipId,
              createdAt: friendship.createdAt
            };
          }
        })
      );

      setFriends(prevFriends => [...prevFriends, ...newFriendsData]);
      setCurrentPage(nextPage);

      setHasMore(response.content && response.content.length === 20 && !response.last);

    } catch (err) {
      console.error('Error loading more friends:', err);
    } finally {
      setLoadingMore(false);
    }
  };

  const EmptyComponent = () => (
    <View style={styles.centered}>
      <Text style={styles.emptyText}>You have no friends yet.</Text>
    </View>
  );

  const FooterComponent = () => {
    if (!loadingMore) return null;

    return (
      <View style={styles.footerLoader}>
        <ActivityIndicator size="small" color="#9188E5" />
        <Text style={styles.footerText}>Loading more friends...</Text>
      </View>
    );
  };

  const handleEndReached = () => {
    if (hasMore && !loadingMore) {
      loadMoreFriends();
    }
  };

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#9188E5" />
        <Text style={styles.loadingText}>Loading friends...</Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>{error}</Text>
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
        contentContainerStyle={{ padding: 16 }}
        itemStyle={styles.friendItem}
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
  emptyText: {
    color: '#888',
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
});