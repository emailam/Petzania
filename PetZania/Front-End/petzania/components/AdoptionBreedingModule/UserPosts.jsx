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
  Dimensions,
  Platform,
} from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import PostCard from './PostCard';
import { useUserPostsInfinite, useDeletePost, useUpdatePost, useToggleLike } from '../../services/postService';
import { UserContext } from '@/context/UserContext';
import Ionicons from 'react-native-vector-icons/Ionicons';
const { width: screenWidth, height: screenHeight } = Dimensions.get('window');

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
    <SafeAreaView style={styles.container}>
      {/* Removed LinearGradient - Header now has no background */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>My Posts</Text>
        <Text style={styles.headerSubtitle}>{posts.length} posts</Text>
      </View>

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        refreshControl={
          <RefreshControl 
            refreshing={isRefetching} 
            onRefresh={refetch}
            tintColor="#9188E5"
            colors={['#9188E5']}
          />
        }
      >
        {posts.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyIcon}>📋</Text>
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
                disabled={isFetchingNextPage}
                activeOpacity={0.8}
                style={styles.loadMoreWrapper}
              >
                <LinearGradient
                  colors={['#9188E5', '#7C3AED']}
                  start={{ x: 0, y: 0 }}
                  end={{ x: 1, y: 1 }}
                  style={styles.loadMoreButton}
                >
                  {isFetchingNextPage ? (
                    <ActivityIndicator size="small" color="#FFFFFF" />
                  ) : (
                    <Text style={styles.loadMoreText}>Load More</Text>
                  )}
                </LinearGradient>
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
    backgroundColor: '#F8F7FD', // Light purple-tinted background
  },
  headerGradient: {
    shadowColor: '#7C3AED',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.15,
    shadowRadius: 3.84,
    elevation: 5,
  },
  header: {
    paddingVertical: screenHeight < 700 ? 16 : 20,
    paddingHorizontal: screenWidth < 380 ? 16 : 20,
    paddingTop: Platform.OS === 'android' ? 20 : 16,
    // Removed background - header now transparent
  },
  headerTitle: {
    fontSize: screenWidth < 380 ? 22 : 26,
    fontWeight: '700',
    color: '#4C1D95', // Changed to dark purple since no background
    letterSpacing: 0.5,
  },
  headerSubtitle: {
    fontSize: screenWidth < 380 ? 14 : 16,
    color: '#6B46C1', // Changed to purple since no background
    marginTop: 4,
    fontWeight: '500',
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    // CARD WIDTH ADJUSTMENT - Reduce padding to give cards more width
    // Original: padding: screenWidth < 380 ? 12 : 16,
    padding: screenWidth < 380 ? 3 : 2, // Reduced padding for more card width
    paddingBottom: 32,
  },
  cardContainer: {
    marginBottom: screenWidth < 380 ? 8 : 12,
    // Add horizontal margin if you want some spacing from edges
    // marginHorizontal: 4, // Uncomment this line to add small side margins
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F8F7FD',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#6B46C1',
    fontWeight: '500',
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
  errorIcon: {
    fontSize: 48,
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
    backgroundColor: '#7C3AED',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 12,
    ...Platform.select({
      ios: {
        shadowColor: '#7C3AED',
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
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingTop: screenHeight * 0.15,
    paddingHorizontal: 20,
  },
  emptyIcon: {
    fontSize: 64,
    marginBottom: 16,
  },
  emptyText: {
    fontSize: screenWidth < 380 ? 18 : 20,
    fontWeight: '600',
    color: '#4C1D95',
    marginBottom: 8,
  },
  emptySubtext: {
    fontSize: screenWidth < 380 ? 14 : 16,
    color: '#6B46C1',
    textAlign: 'center',
    maxWidth: screenWidth * 0.8,
    lineHeight: 22,
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
  },
  loadMoreText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: screenWidth < 380 ? 15 : 17,
    letterSpacing: 0.5,
  },
});

export default UserPosts;