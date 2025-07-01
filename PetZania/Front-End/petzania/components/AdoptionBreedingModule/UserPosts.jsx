import React, { useState, useContext } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  SafeAreaView,
  RefreshControl,
  ActivityIndicator,
  TouchableOpacity,

} from 'react-native';
import PostCard from './PostCard';
import EditPostModal from './EditPostModal';
import { useUserPostsInfinite } from '../../services/postService';
import { UserContext } from '@/context/UserContext';

const UserPosts = () => {
  const [selectedPost, setSelectedPost] = useState(null);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const { user } = useContext(UserContext);
  const {
    data,
    isLoading,
    isFetchingNextPage,
    fetchNextPage,
    hasNextPage,
    refetch,
    isRefetching,
    error,
  } = useUserPostsInfinite(user.userId);
  // Flatten pages and ensure only valid posts are mapped
  const posts =
    data?.pages.flatMap((page) => (Array.isArray(page.posts) ? page.posts : []))
      .filter(Boolean) || [];

  const handlePostPress = (post) => {
    setSelectedPost(post);
    setEditModalVisible(true);
  };

  const handlePostUpdate = () => {
    setEditModalVisible(false);
    setSelectedPost(null);
  };

  const handlePostDelete = () => {
    setEditModalVisible(false);
    setSelectedPost(null);
  };


  if (error) {
    return (
      <View style={styles.loadingContainer}>
        <Text style={styles.loadingText}>Failed to load posts.</Text>
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
          <RefreshControl refreshing={isRefetching} onRefresh={refetch} />
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
          <>
            {posts.map((post) => (
              <View key={post.postId} style={styles.cardContainer}>
                <PostCard
                  onPress={() => handlePostPress(post)}
                  showAdvancedFeatures={true}
                  post = {post}
                />
              </View>
            ))}

            {hasNextPage && (
              <TouchableOpacity
                onPress={fetchNextPage}
                style={styles.loadMoreButton}
                disabled={isFetchingNextPage}
              >
                <Text style={styles.loadMoreText}>
                  {isFetchingNextPage ? 'Loading more...' : 'Load More'}
                </Text>
              </TouchableOpacity>
            )}
          </>
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
        onDelete={handlePostDelete}
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
  loadMoreButton: {
    backgroundColor: '#9188E5',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 16,
  },
  loadMoreText: {
    color: '#fff',
    fontWeight: '600',
    fontSize: 16,
  },
});

export default UserPosts;