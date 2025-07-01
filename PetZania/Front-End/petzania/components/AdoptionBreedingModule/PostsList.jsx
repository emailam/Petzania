import React, { useState, useCallback, useMemo, forwardRef, useImperativeHandle } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
  StyleSheet,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import FilterModal from './FilterModal';
import PostCard from './PostCard';
import { useFetchPosts } from '../../services/postService';

const PostsList = forwardRef(({ postType = 'ADOPTION', showHeader = true }, ref) => {
  // State management - include all filter fields
  const [filterModalVisible, setFilterModalVisible] = useState(false);
  const [filters, setFilters] = useState({
    species: 'ALL',
    breed: 'ALL',
    minAge: 0,
    maxAge: 1000,
    sortBy: 'CREATED_AT',
    sortDesc: true
  });

  // Expose openFilter to parent
  useImperativeHandle(ref, () => ({
    openFilter: () => setFilterModalVisible(true),
  }));

  // ---- (rest of your code unchanged, except for possible showHeader default) ----

  const queryFilters = useMemo(() => ({
    petPostType: postType,
    ...filters
  }), [postType, filters]);

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError,
    refetch,
    isRefetching
  } = useFetchPosts(queryFilters);

  const posts = useMemo(() => {
    return data?.pages?.flatMap(page => page.items) || [];
  }, [data]);

  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (filters.species !== 'ALL') count++;
    if (filters.breed !== 'ALL') count++;
    if (filters.minAge !== 0) count++;
    if (filters.maxAge !== 1000) count++;
    return count;
  }, [filters]);

  const postTypeDisplay = useMemo(() => {
    const types = {
      'ADOPTION': 'Pets for Adoption',
      'BREEDING': 'Pets for Breeding',
    };
    return types[postType] || 'Posts';
  }, [postType]);

  const handleApplyFilters = useCallback((newFilters) => {
    setFilters(newFilters);
  }, []);

  const handleOpenFilters = useCallback(() => {
    setFilterModalVisible(true);
  }, []);

  const handleCloseFilters = useCallback(() => {
    setFilterModalVisible(false);
  }, []);


  const renderItem = useCallback(({ item }) => (
    <PostCard
      post={item}
      showAdvancedFeatures={false}
    />
  ), []);

  const handleLoadMore = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const renderFooter = useCallback(() => {
    if (!isFetchingNextPage) return null;
    return (
      <View style={styles.footerLoader}>
        <ActivityIndicator size="small" color="#9188E5" />
      </View>
    );
  }, [isFetchingNextPage]);

  const renderEmpty = useCallback(() => {
    if (isLoading) return null;
    return (
      <View style={styles.centerContainer}>
        <Ionicons name="paw" size={48} color="#ccc" />
        <Text style={styles.centerTitle}>No pets found</Text>
        <Text style={styles.centerSub}>
          Try adjusting your filters to see more results
        </Text>
      </View>
    );
  }, [isLoading]);

  if (isLoading && posts.length === 0) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#9188E5" />
      </View>
    );
  }

  if (isError) {
    return (
      <View style={styles.centerContainer}>
        <Ionicons name="alert-circle" size={48} color="#ff6b6b" />
        <Text style={styles.centerTitle}>Something went wrong</Text>
        <Text style={styles.centerSub}>Pull to refresh and try again</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header */}
      {showHeader && (
        <View style={styles.inlineHeader}>
          <View style={styles.headerRow}>
            <View>
              <Text style={styles.title}>{postTypeDisplay}</Text>
              <Text style={styles.subtitle}>
                {posts.length > 0 ? `${data?.pages?.[0]?.totalCount || 0} pets available` : 'No pets available'}
              </Text>
            </View>
            <TouchableOpacity
              style={styles.filterButton}
              onPress={handleOpenFilters}
              activeOpacity={0.7}
            >
              <Ionicons name="filter" size={20} color="#555" />
              <Text style={styles.filterText}>
                Filters {activeFilterCount > 0 && `(${activeFilterCount})`}
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      )}

      <FlatList
        data={posts}
        renderItem={renderItem}
        keyExtractor={(item) => item.postId.toString()}
        contentContainerStyle={[
          styles.listContainer,
          posts.length === 0 && styles.emptyListContainer
        ]}
        refreshControl={
          <RefreshControl
            refreshing={isRefetching}
            onRefresh={refetch}
            colors={['#9188E5']}
            tintColor="#9188E5"
          />
        }
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.5}
        ListFooterComponent={renderFooter}
        ListEmptyComponent={renderEmpty}
        showsVerticalScrollIndicator={false}
        removeClippedSubviews={true}
        maxToRenderPerBatch={10}
        windowSize={10}
      />

      <FilterModal
        visible={filterModalVisible}
        onClose={handleCloseFilters}
        onApply={handleApplyFilters}
        initialFilters={filters}
      />
    </View>
  );
});

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f9f9f9' },
  inlineHeader: {
    backgroundColor: '#f9f9f9',
    paddingBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  headerRow: {
    flexDirection: 'row', 
    justifyContent: 'space-between',
    alignItems: 'center', 
    paddingHorizontal: 16,
    paddingTop: 16,
  },
  title: { fontSize: 24, fontWeight: '600', color: '#333' },
  subtitle: { fontSize: 14, color: '#666', marginTop: 2 },
  filterButton: {
    flexDirection: 'row', 
    alignItems: 'center',
    backgroundColor: '#fff', 
    paddingHorizontal: 12, 
    paddingVertical: 8,
    borderRadius: 20, 
    borderWidth: 1, 
    borderColor: '#ddd',
    shadowColor: '#000', 
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1, 
    shadowRadius: 2, 
    elevation: 2,
  },
  filterText: { marginLeft: 4, fontSize: 14, color: '#555' },
  listContainer: { paddingHorizontal: 16, paddingTop: 16, paddingBottom: 40, gap: 16 },
  emptyListContainer: { flex: 1 },
  footerLoader: { paddingVertical: 20, alignItems: 'center' },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', paddingHorizontal: 20 },
  centerTitle: { marginTop: 10, fontSize: 18, fontWeight: '600', color: '#333' },
  centerSub: { marginTop: 5, fontSize: 14, color: '#666', textAlign: 'center' },
});

export default PostsList;