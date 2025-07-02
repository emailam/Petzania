import React, { useState, useCallback, useRef } from 'react';
import {
  View,
  FlatList,
  Dimensions,
  StyleSheet,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
  Platform,
  SafeAreaView,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import PostCard from './PostCard';
import FilterModal from '@/components/AdoptionBreedingModule/FilterModal';
import {
  useFetchPosts,
  useToggleLike,
} from '../../services/postService';
import { getUserById } from '@/services/userService';
import toast from 'react-native-toast-message';

const { width: screenWidth, height: screenHeight } = Dimensions.get('window');

export default function PostScreen({ postType }) {
  // Responsive calculations
  const isSmallScreen = screenWidth < 380;
  const isMediumScreen = screenWidth >= 380 && screenWidth < 768;
  const isLargeScreen = screenWidth >= 768;

  const [filters, setFilters] = useState({
    species: 'ALL',
    breed: 'ALL',
    minAge: 0,
    maxAge: 1000,
    sortBy: 'CREATED_DATE',
    sortDesc: true,
  });

  const [isFilterModalVisible, setFilterModalVisible] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  const toggleLikeMutation = useToggleLike();

  const titleMap = {
    ADOPTION: 'Pets for Adoption',
    BREEDING: 'Pets for Breeding',
  };
  const title = titleMap[postType] || 'Posts';

  const {
    data,
    isLoading,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    refetch,
  } = useFetchPosts({ petPostType: postType, ...filters });

  const posts = data?.pages.flatMap(page => (Array.isArray(page.posts) ? page.posts : [])) || [];
  // Handlers
  const handlePostLike = useCallback(
    async postId => {
      try {
        await toggleLikeMutation.mutateAsync({ postId });
      } catch (err) {
        toast.show({ type: 'error', text1: 'Failed to toggle like' });
      }
    },
    [toggleLikeMutation]
  );

  const handleGetUserById = useCallback(async userId => {
    try {
      return await getUserById(userId);
    } catch {
      return null;
    }
  }, []);

  const handleFilterPress = useCallback(() => setFilterModalVisible(true), []);
  const handleCloseFilters = useCallback(() => setFilterModalVisible(false), []);
  const handleApplyFilters = useCallback(newFilters => {
    setFilters(newFilters);
    setFilterModalVisible(false);
    toast.show({ type: 'success', text1: 'Filters applied' });
  }, []);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    await refetch();
    setRefreshing(false);
  }, [refetch]);

  const renderPost = useCallback(({ item }) => (
    <View style={[styles.cardWrapper, isLargeScreen && styles.cardWrapperLarge]}>
      <PostCard
        showAdvancedFeatures={false}
        post={item}
        onPostUpdate={() => {}}
        onPostDelete={() => {}}
        onPostLikeToggle={handlePostLike}
        getUsers={handleGetUserById}
      />
    </View>
  ), [handlePostLike, handleGetUserById, isLargeScreen]);

  const renderFooter = useCallback(() => {
    if (!hasNextPage) return null;
    
    return (
      <TouchableOpacity
        onPress={fetchNextPage}
        style={styles.loadMoreButton}
        disabled={isFetchingNextPage}
        activeOpacity={0.8}
      >
        {isFetchingNextPage ? (
          <ActivityIndicator size="small" color="#7C3AED" />
        ) : (
          <>
            <Text style={styles.loadMoreText}>Load More</Text>
            <Ionicons name="chevron-down" size={20} color="#7C3AED" />
          </>
        )}
      </TouchableOpacity>
    );
  }, [fetchNextPage, hasNextPage, isFetchingNextPage]);

  const renderEmpty = useCallback(() => (
    <View style={styles.emptyContainer}>
      <View style={styles.emptyIconContainer}>
        <Ionicons 
          name={postType === 'ADOPTION' ? 'heart-outline' : 'git-branch-outline'} 
          size={64} 
          color="#E5E7EB" 
        />
      </View>
      <Text style={styles.emptyText}>No {title} Found</Text>
      <Text style={styles.emptySubtext}>
        {postType === 'ADOPTION' 
          ? 'Check back later for pets looking for their forever homes'
          : 'No breeding posts available at the moment'}
      </Text>
      <TouchableOpacity 
        style={styles.refreshButton} 
        onPress={handleRefresh}
        activeOpacity={0.8}
      >
        <Ionicons name="refresh" size={20} color="#7C3AED" />
        <Text style={styles.refreshButtonText}>Refresh</Text>
      </TouchableOpacity>
    </View>
  ), [postType, title, handleRefresh]);

  // Loading state
  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#7C3AED" />
        <Text style={styles.loadingText}>Loading {title}...</Text>
      </View>
    );
  }

  // Error state
  if (isError) {
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
      {/* Header */}
      <View style={[styles.header, isLargeScreen && styles.headerLarge]}>
        <View style={styles.headerContent}>
          <Text style={[styles.headerTitle, isSmallScreen && styles.headerTitleSmall]}>
            {title}
          </Text>
          <TouchableOpacity
            style={styles.filterButton}
            onPress={handleFilterPress}
            activeOpacity={0.8}
          >
            <Ionicons name="filter" size={20} color="#7C3AED" />
            <Text style={styles.filterButtonText}>Filter</Text>
            {Object.keys(filters).some(key => 
              (key === 'species' && filters[key] !== 'ALL') ||
              (key === 'breed' && filters[key] !== 'ALL') ||
              (key === 'minAge' && filters[key] !== 0) ||
              (key === 'maxAge' && filters[key] !== 1000)
            ) && <View style={styles.filterBadge} />}
          </TouchableOpacity>
        </View>
        {posts.length > 0 && (
          <Text style={styles.postCount}>
            {posts.length} {posts.length === 1 ? 'pet' : 'pets'} available
          </Text>
        )}
      </View>

      {/* Posts List */}
      <FlatList
        data={posts}
        renderItem={renderPost}
        keyExtractor={item => item.postId}
        contentContainerStyle={[
          styles.listContent,
          posts.length === 0 && styles.listContentEmpty
        ]}
        ListEmptyComponent={renderEmpty}
        ListFooterComponent={renderFooter}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={handleRefresh}
            colors={['#7C3AED']}
            tintColor="#7C3AED"
          />
        }
        showsVerticalScrollIndicator={false}
        initialNumToRender={5}
        maxToRenderPerBatch={5}
        windowSize={10}
        removeClippedSubviews={true}
      />

      {/* Filter Modal */}
      <FilterModal
        visible={isFilterModalVisible}
        initialFilters={{ ...filters, petPostType: postType }}
        onApply={handleApplyFilters}
        onClose={handleCloseFilters}
      />
    </SafeAreaView>
    
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F9FAFB',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F9FAFB',
  },
  loadingText: {
    marginTop: 12,
    fontSize: 16,
    color: '#6B7280',
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
  header: {
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 16,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#E5E7EB',
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.05,
        shadowRadius: 2,
      },
      android: {
        elevation: 2,
      },
    }),
  },
  headerLarge: {
    paddingHorizontal: 24,
  },
  headerContent: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: '700',
    color: '#1F2937',
  },
  headerTitleSmall: {
    fontSize: 20,
  },
  filterButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F3F4F6',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    gap: 6,
    position: 'relative',
  },
  filterButtonText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#7C3AED',
  },
  filterBadge: {
    position: 'absolute',
    top: -2,
    right: -2,
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#EF4444',
  },
  postCount: {
    fontSize: 14,
    color: '#6B7280',
    marginTop: 8,
  },
  listContent: {
    paddingVertical: 16,
  },
  listContentEmpty: {
    flex: 1,
  },
  cardWrapper: {
    marginBottom: 16,
  },
  cardWrapperLarge: {
    alignItems: 'center',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 32,
    paddingBottom: 100,
  },
  emptyIconContainer: {
    marginBottom: 24,
  },
  emptyText: {
    fontSize: 20,
    fontWeight: '600',
    color: '#1F2937',
    marginBottom: 8,
  },
  emptySubtext: {
    fontSize: 16,
    color: '#6B7280',
    textAlign: 'center',
    marginBottom: 24,
    lineHeight: 22,
  },
  refreshButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#EDE9FE',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 20,
    gap: 8,
  },
  refreshButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#7C3AED',
  },
  loadMoreButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#FFFFFF',
    marginHorizontal: 16,
    marginVertical: 8,
    paddingVertical: 12,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E5E7EB',
    gap: 8,
  },
  loadMoreText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#7C3AED',
  },
});