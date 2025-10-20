import React, { useCallback, useContext, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  RefreshControl,
  ActivityIndicator,
  TouchableOpacity,
  Dimensions,
  Platform,
} from 'react-native';
import PostCard from './PostCard';
import { useUserPostsInfinite, useDeletePost, useUpdatePost, useToggleLike } from '../../services/postService';
import { UserContext } from '@/context/UserContext';
import Ionicons from 'react-native-vector-icons/Ionicons';
import LottieView from 'lottie-react-native';
import EmptyState from '../EmptyState';

const { width: screenWidth, height: screenHeight } = Dimensions.get('window');

const UserPosts = ({ userId }) => {
  const { user } = useContext(UserContext);
  const deletePostMutation = useDeletePost();
  const updatePostMutation = useUpdatePost();
  const toggleLikeMutation = useToggleLike();
  
  // Use the passed userId or fall back to current user's ID
  const targetUserId = userId || user?.userId;
  const isOwnProfile = user?.userId === targetUserId;
  
  const {
    data,
    isLoading,
    isFetchingNextPage,
    fetchNextPage,
    hasNextPage,
    refetch,
    isRefetching,
    error,
  } = useUserPostsInfinite(targetUserId);

  // Flatten pages and ensure only valid posts are mapped
  const posts = data?.pages.flatMap((page) => (Array.isArray(page.posts) ? page.posts : [])).filter(Boolean) || [];
  
  // Create 2-column layout using useMemo for performance
  const postsInColumns = useMemo(() => {
    const columns = [];
    for (let i = 0; i < posts.length; i += 2) {
      columns.push({
        left: posts[i] || null,
        right: posts[i + 1] || null,
        key: `row-${i}`
      });
    }
    return columns;
  }, [posts]);
  
  const handlePostUpdate = useCallback(async (postId, updatedData) => {
    try {
      await updatePostMutation.mutateAsync({ 
        postId, 
        newPostData: updatedData 
      })
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
        <LottieView
          source={require("@/assets/lottie/loading.json")}
          autoPlay
          loop
          style={styles.lottie}
        />
        <Text style={styles.loadingText}>Loading posts...</Text>
        <Text style={styles.loadingSubText}>
          Getting posts from the user
        </Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.errorContainer}>
        <View style={styles.errorIconContainer}>
          <Ionicons name="alert-circle-outline" size={48} color="#EF4444" />
        </View>
        <Text style={styles.errorText}>Something went wrong</Text>
        <Text style={styles.errorSubtext}>Unable to load posts</Text>
        <TouchableOpacity
          style={styles.retryButton}
          onPress={refetch}
          activeOpacity={0.8}
        >
          <Text style={styles.retryButtonText}>Try Again</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {posts.length === 0 ? (
        <EmptyState
          iconName="document-text-outline"
          title="No posts yet"
          subtitle={isOwnProfile 
            ? 'Start sharing your pets to find them loving homes!'
            : "This user hasn't shared any posts yet."
          }
          style={styles.emptyStateContainer}
        />
      ) : (
        <>
          {/* 2-Column Posts Grid */}
          <View>
            {postsInColumns.map((row) => (
              <View key={row.key} style={styles.postRow}>
                <View style={styles.postColumn}>
                  {row.left && (
                    <PostCard
                      post={row.left}
                      showAdvancedFeatures={isOwnProfile}
                      onPostUpdate={isOwnProfile ? handlePostUpdate : undefined}
                      onPostDelete={isOwnProfile ? handlePostDelete : undefined}
                      onPostLikeToggle={handlePostLike}
                      onPostDetails={() => {}}
                    />
                  )}
                </View>
                <View style={styles.postColumn}>
                  {row.right && (
                    <PostCard
                      post={row.right}
                      showAdvancedFeatures={isOwnProfile}
                      onPostUpdate={isOwnProfile ? handlePostUpdate : undefined}
                      onPostDelete={isOwnProfile ? handlePostDelete : undefined}
                      onPostLikeToggle={handlePostLike}
                      onPostDetails={() => {}}
                    />
                  )}
                </View>
              </View>
            ))}
          </View>

          {hasNextPage && (
            <TouchableOpacity
              onPress={fetchNextPage}
              disabled={isFetchingNextPage}
              activeOpacity={0.8}
              style={styles.loadMoreWrapper}
            >
              <View style={styles.loadMoreButton}>
                {isFetchingNextPage ? (
                  <ActivityIndicator size="small" color="#FFFFFF" />
                ) : (
                  <Text style={styles.loadMoreText}>Load More</Text>
                )}
              </View>
            </TouchableOpacity>
          )}
        </>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  postRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  postColumn: {
    flex: 1,
    marginHorizontal: 4,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F9FAFB',
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
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F9FAFB',
    paddingHorizontal: 32,
  },
  errorIconContainer: {
    marginBottom: 16,
  },
  errorText: {
    fontSize: 20,
    fontWeight: '600',
    color: '#1F2937',
    marginBottom: 8,
  },
  errorSubtext: {
    fontSize: 16,
    color: '#6B7280',
    marginBottom: 24,
    textAlign: 'center',
  },
  retryButton: {
    backgroundColor: '#9188E5',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 12,
    ...Platform.select({
      ios: {
        shadowColor: '#9188E5',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.2,
        shadowRadius: 4,
      },
      android: {
        elevation: 4,
      },
    }),
  },
  retryButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  emptyStateContainer: {
    paddingVertical: 40,
    paddingHorizontal: 20,
  },
  loadMoreWrapper: {
    marginTop: 16,
    marginBottom: 8,
    borderRadius: 12,
    overflow: 'hidden',
  },
  loadMoreButton: {
    paddingVertical: screenWidth < 380 ? 14 : 16,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 52,
    backgroundColor: '#9188E5', // Solid color replaces gradient
  },
  loadMoreText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: screenWidth < 380 ? 15 : 17,
    letterSpacing: 0.5,
  },
  lottie: {
    width: 80,
    height: 80,
  },
});

export default UserPosts;