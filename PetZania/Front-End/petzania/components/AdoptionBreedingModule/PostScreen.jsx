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
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import PostCard from './PostCard';
import FilterModal from '@/components/AdoptionBreedingModule/FilterModal';
import SortModal from '@/components/AdoptionBreedingModule/SortModal';
import EmptyState from '@/components/EmptyState';
import LottieView from 'lottie-react-native';
import {
  useFetchPosts,
  useToggleLike,
} from '../../services/postService';
import LandingImages from '@/components/AdoptionBreedingModule/LandingImages';
import { getUserById } from '@/services/userService';
import toast from 'react-native-toast-message';

const { width: screenWidth, height: screenHeight } = Dimensions.get('window');

export default function PostScreen({ postType }) {
  // Responsive calculations
  const isSmallScreen = screenWidth < 380;
  const isLargeScreen = screenWidth >= 768;

  const [filters, setFilters] = useState({
    species: 'ALL',
    breed: 'ALL',
    minAge: 0,
    maxAge: 1000,
    sortBy: 'SCORE',
    sortDesc: true,
  });

  const [isFilterModalVisible, setFilterModalVisible] = useState(false);
  const [isSortModalVisible, setSortModalVisible] = useState(false);
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

  // Ensure even number of items for 2-column layout
  const postsWithPlaceholder = posts.length % 2 === 1 
    ? [...posts, { isPlaceholder: true, postId: 'placeholder' }]
    : posts;
    

  // ALL HOOKS MUST BE CALLED BEFORE ANY CONDITIONAL RETURNS
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

  const handleSortPress = useCallback(() => setSortModalVisible(true), []);
  const handleCloseSort = useCallback(() => setSortModalVisible(false), []);

  const handleApplyFilters = useCallback(newFilters => {
    setFilters(newFilters);
    setFilterModalVisible(false);
    toast.show({ type: 'success', text1: 'Filters applied' });
  }, []);

  const handleApplySort = useCallback(newFilters => {
    console.log('Applying sort:', newFilters);
    setFilters(newFilters);
    setSortModalVisible(false);
    toast.show({ type: 'success', text1: 'Sort applied' });
  }, []);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    await refetch();
    setRefreshing(false);
  }, [refetch]);

  const renderPost = useCallback(({ item }) => {
    // Render empty space for placeholder items
    if (item.isPlaceholder) {
      return <View style={styles.cardWrapper} />;
    }

    return (
      <View style={styles.cardWrapper}>
        <PostCard
          showAdvancedFeatures={false}
          post={item}
          onPostUpdate={() => {}}
          onPostDelete={() => {}}
          onPostLikeToggle={handlePostLike}
          getUsers={handleGetUserById}
        />
      </View>
    );
  }, [handlePostLike, handleGetUserById]);

  // CHANGED BUTTON: solid purple, white text/icon
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
          <ActivityIndicator size="small" color="#FFFFFF" />
        ) : (
          <>
            <Text style={styles.loadMoreText}>Load More</Text>
            <Ionicons name="chevron-down" size={20} color="#FFFFFF" />
          </>
        )}
      </TouchableOpacity>
    );
  }, [fetchNextPage, hasNextPage, isFetchingNextPage]);

  const renderEmpty = useCallback(() => {
    const emptyConfig = {
      ADOPTION: {
        iconName: 'heart-outline',
        title: 'No Adoption Posts',
        subtitle: 'No pets looking for adoption at the moment.\nCheck back later for furry friends seeking their forever homes!'
      },
      BREEDING: {
        iconName: 'git-branch-outline', 
        title: 'No Breeding Posts',
        subtitle: 'No breeding posts available right now.\nCheck back later for breeding opportunities!'
      }
    };

    const config = emptyConfig[postType] || {
      iconName: 'albums-outline',
      title: 'No Posts Available',
      subtitle: 'No posts found at the moment.\nTry refreshing or check back later!'
    };

    return (
      <EmptyState
        iconName={config.iconName}
        title={config.title}
        subtitle={config.subtitle}
        style={styles.emptyStateContainer}
      />
    );
  }, [postType]);

  // Check if filters are active (excluding sort settings)
  const hasActiveFilters = useCallback(() => {
    return (filters.species !== 'ALL') ||
           (filters.breed !== 'ALL') ||
           (filters.minAge !== 0) ||
           (filters.maxAge !== 1000);
  }, [filters]);

  // Check if sort is non-default
  const hasActiveSort = useCallback(() => {
    return (filters.sortBy !== 'SCORE') || (filters.sortDesc !== true);
  }, [filters]);

  const renderHeader = useCallback(() => (
    <View style={[styles.header, isLargeScreen && styles.headerLarge]}>
      <View style={styles.headerContent}>
        <TouchableOpacity
          style={[styles.filterButton, { marginRight: 12 }]}
          onPress={handleFilterPress}
          activeOpacity={0.8}
        >
          <Ionicons name="funnel-outline" size={20} color="#9188E5" />
          <Text style={styles.filterButtonText}>Filter</Text>
          {hasActiveFilters() && <View style={styles.filterBadge} />}
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.filterButton}
          onPress={handleSortPress}
          activeOpacity={0.8}
        >
          <Ionicons name="swap-vertical-outline" size={20} color="#9188E5" />
          <Text style={styles.filterButtonText}>Sort</Text>
          {hasActiveSort() && <View style={styles.filterBadge} />}
        </TouchableOpacity>
      </View>
    </View>
  ), [isLargeScreen, handleFilterPress, handleSortPress, hasActiveFilters, hasActiveSort]);

  // CONDITIONAL RETURNS MUST COME AFTER ALL HOOKS
  // Loading state
  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <LottieView
          source={require("@/assets/lottie/loading.json")}
          autoPlay
          loop
          style={styles.lottie}
        />
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
    <View style={styles.container}>
      <FlatList
        data={postsWithPlaceholder}
        numColumns={2}
        renderItem={renderPost}
        ListHeaderComponent={
          <>
            <LandingImages />
            {renderHeader()}
          </>
        }
        keyExtractor={(item) => item.postId}
        contentContainerStyle={[
          styles.listContent,
          posts.length === 0 && styles.listContentEmpty
        ]}
        ListFooterComponent={renderFooter}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={handleRefresh}
            colors={['#9188E5']}
            tintColor="#9188E5"
          />
        }
        showsVerticalScrollIndicator={false}
        initialNumToRender={6}
        maxToRenderPerBatch={6}
        windowSize={10}
        removeClippedSubviews={true}
        columnWrapperStyle={styles.row}
      />
      {posts.length === 0 && renderEmpty()}

      {/* Filter Modal */}
      <FilterModal
        visible={isFilterModalVisible}
        initialFilters={filters}
        onApply={handleApplyFilters}
        onClose={handleCloseFilters}
      />

      {/* Sort Modal */}
      <SortModal
        visible={isSortModalVisible}
        initialFilters={filters}
        onApply={handleApplySort}
        onClose={handleCloseSort}
      />
    </View>
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
    backgroundColor: '#9188E5',
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
    paddingVertical: 10,
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
    position: 'sticky',
  },
  headerLarge: {
    paddingHorizontal: 24,
  },
  headerContent: {
    flexDirection: 'row',
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
    color: '#9188E5',
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
  listContentEmpty: {
    flex: 1,
  },
  row: {
    justifyContent: 'space-evenly',
    paddingHorizontal: 8,
  },
  cardWrapper: {
    flex: 1,
    marginHorizontal: 6,
    marginBottom: 8,
  },
  emptyStateContainer: {
    flex: 1,
    paddingHorizontal: 32,
    paddingBottom: 100,
  },
  // CHANGED: solid purple, white text/icon for load more (no gradient)
  loadMoreButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#9188E5',
    marginHorizontal: 16,
    marginVertical: 8,
    paddingVertical: 12,
    borderRadius: 12,
    gap: 8,
  },
  loadMoreText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#FFFFFF',
  },
  lottie: {
    width: 100,
    height: 100,
  },
});