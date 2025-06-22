import { StyleSheet, Text, View, ActivityIndicator } from 'react-native'
import React, { useEffect, useState, useContext } from 'react'
import { getFriends } from '@/services/friendsService';
import { UserContext } from '@/context/UserContext';
import UserList from '@/components/UserList';

export default function FriendsScreen() {
  const [friends, setFriends] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { user: currentUser } = useContext(UserContext);

  useEffect(() => {
    const fetchFriends = async () => {
      try {
        setLoading(true);
        setError(null);

        if (!currentUser?.userId) {
          setError('User not found. Please log in again.');
          return;
        }

        const response = await getFriends(0, 50);
        // Transform friendship data to user data for the UserList component
        const friendsData = (response.content || []).map(friendship => {
          // Determine which user is the friend (not the current user)
          const friend = friendship.user1.userId === currentUser.userId ? friendship.user2 : friendship.user1;
          return {
            ...friend,
            friendshipId: friendship.friendshipId // Keep track of friendship ID
          };
        });
        setFriends(friendsData);
      } catch (err) {
        console.error('Error fetching friends:', err);
        setError('Failed to load friends.');
      } finally {
        setLoading(false);
      }
    };
    fetchFriends();
  }, [currentUser]);
  const EmptyComponent = () => (
    <View style={styles.centered}>
      <Text style={styles.emptyText}>You have no friends yet.</Text>
    </View>
  );

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
});