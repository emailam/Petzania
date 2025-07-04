import React, { useCallback, useContext } from 'react';
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
import { useUserPostsInfinite, useDeletePost, useUpdatePost, useToggleLike } from '../../services/postService';
import { UserContext } from '@/context/UserContext';

const UserPosts = () => {
  const { user } = useContext(UserContext);
  const deletePostMutation = useDeletePost();
  const updatePostMutation = useUpdatePost();
  const toggleLikeMutation = useToggleLike();
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
  const posts = data?.pages.flatMap((page) => (Array.isArray(page.posts) ? page.posts : [])).filter(Boolean) || [];
  const handlePostUpdate = useCallback(async (postId, updatedData) => {
    try {
      await updatePostMutation.mutateAsync({ 
        postId, 
        newPostData: updatedData 
      })
    // Reset mutation state after success
    } catch (error) {
      console.error('Failed to update post:', error);
    }
  }, [updatePostMutation.mutateAsync]);

const handlePostDelete = useCallback(async (postId) => {
  try {
    await deletePostMutation.mutateAsync(postId);
  } catch (error) {
    console.error('Failed to delete post:', error);
  }
}, [deletePostMutation.mutateAsync]);

const handlePostLike = useCallback(async (postId) => {
  try {
    await toggleLikeMutation.mutateAsync({ postId });
  } catch (error) {
    console.error('Failed to toggle like:', error);
  }
}, [toggleLikeMutation.mutateAsync]);

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#9188E5" />
        <Text style={styles.loadingText}>Loading posts...</Text>
      </View>
    );
  }

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
                  showAdvancedFeatures={true}
                  post={post}
                  onPostUpdate={handlePostUpdate}
                  onPostDelete={handlePostDelete}
                  onPostLikeToggle={handlePostLike}
                  onPostDetails={() => {}} 
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
    </SafeAreaView>
  );
};


const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
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