import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  SafeAreaView,
  Alert,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import PostCard from './PostCard';
import EditPostModal from './EditPostModal';
import { fetchUserPosts, deletePost } from '../../services/postService'; // Adjust the import path as necessary
import Posts from '../../pets.json'
const UserPosts = ({ userId }) => {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedPost, setSelectedPost] = useState(null);
  const [editModalVisible, setEditModalVisible] = useState(false);

  useEffect(() => {
    loadUserPosts();
  }, [userId]);

  const loadUserPosts = async () => {
    try {
      setLoading(true);
      const userPosts = Posts;
      setPosts(userPosts);
    } catch (error) {
      console.error('Error loading user posts:', error);
      Alert.alert('Error', 'Failed to load your posts');
    } finally {
      setLoading(false);
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await loadUserPosts();
    setRefreshing(false);
  };

  const handlePostPress = (post) => {
    setSelectedPost(post);
    setEditModalVisible(true);
  };

  const handleDeletePost = async (postId) => {
    Alert.alert(
      'Delete Post',
      'Are you sure you want to delete this post?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              await deletePost(postId);
              setPosts(posts.filter(post => post.id !== postId));
              Alert.alert('Success', 'Post deleted successfully');
            } catch (error) {
              Alert.alert('Error', 'Failed to delete post');
            }
          }
        }
      ]
    );
  };

  const handlePostUpdate = (updatedPost) => {
    setPosts(posts.map(post => 
      post.id === updatedPost.id ? updatedPost : post
    ));
    setEditModalVisible(false);
    setSelectedPost(null);
  };

  const handleInterestResponse = (postId, response) => {
    console.log(`Post ${postId} marked as ${response}`);
    // Implement your interest response logic here
    Alert.alert('Response Recorded', `You marked this post as ${response}`);
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#9188E5" />
        <Text style={styles.loadingText}>Loading your posts...</Text>
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>My Posts</Text>
        <Text style={styles.headerSubtitle}>{posts.length} posts</Text>
      </View>

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
      >
        {posts.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>No posts yet</Text>
            <Text style={styles.emptySubtext}>
              Start sharing your pets to find them loving homes!
            </Text>
          </View>
        ) : (
          posts.map((post, index) => (
            <View key={post.id || index} style={styles.cardContainer}>
              <PostCard
                postId={post.id}
                petDTO={post.petDTO}
                reacts={post.reacts}
                reactedUsersIds={post.reactedUsersIds}
                location={post.location}
                createdAt={post.createdAt}
                isLiked={post.isLiked}
                likes={post.likes}
                postType={post.postType} // This will be shown since we're in userPosts
                postStatus={post.status}
                isUserPost={true} // Flag to show this is user's own post
                onPress={() => handlePostPress(post)}
                onDelete={() => handleDeletePost(post.id)}
                index={index}
                showAdvancedFeatures={true} // Show advanced features for user posts
              />
            </View>
          ))
        )}
      </ScrollView>

      <EditPostModal
        visible={editModalVisible}
        onClose={() => {
          setEditModalVisible(false);
          setSelectedPost(null);
        }}
        post={selectedPost}
        onUpdate={handlePostUpdate}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: '#fff',
    paddingVertical: 20,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: '600',
    color: '#333',
  },
  headerSubtitle: {
    fontSize: 14,
    color: '#666',
    marginTop: 4,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: 16,
  },
  cardContainer: {
    marginBottom: 16,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingTop: 100,
  },
  emptyText: {
    fontSize: 18,
    fontWeight: '500',
    color: '#666',
    marginBottom: 8,
  },
  emptySubtext: {
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
    maxWidth: 250,
  },
});

export default UserPosts;