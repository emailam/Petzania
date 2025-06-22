import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import PostCard from './PostCard'; // adjust path
import PostFiltering from './PostFiltering'; // existing component
import PostDetailsModal from './PostDetailsModal'; // existing component
import ReactsModal from './ReactsModal'; // new component
import usePostsHook from './usePostsHook'; // custom hook for fetching posts
import {
  initializeLikesState,
  togglePostLike,
  getPostKey,
  getDefaultFilters,
  getLikesCount,
  isPostLiked
} from '../../services/postService'; // updated service

// Renamed from usePosts to PostsList (proper component name)
export default function PostsList({ showHeader = true, postType = "" }) {
  // Filter states
  const [filters, setFilters] = useState(getDefaultFilters());
  const [likesState, setLikesState] = useState({});

  // Modal states
  const [modalVisible, setModalVisible] = useState(false);
  const [reactsModalVisible, setReactsModalVisible] = useState(false);
  const [postDetailsModalVisible, setPostDetailsModalVisible] = useState(false);

  // Selected data states
  const [selectedPostReacts, setSelectedPostReacts] = useState({
    postId: null,
    reactedUsersIds: []
  });
  const [selectedPostId, setSelectedPostId] = useState(null);

  // Temp filter states for modal
  const [tempFilters, setTempFilters] = useState(filters);

  // Use the custom hook to get filtered data
  const { posts, isLoading, error, filteredCount, totalCount } = usePostsHook(filters, postType);

  // Initialize likes state when posts change
  useEffect(() => {
    if (posts.length > 0) {
      const initialLikesState = initializeLikesState(posts);
      setLikesState(initialLikesState);
    }
  }, [posts]);

  // Universal handle like function with API integration option
  const handleLike = (postKey, enableApiCall = false) => {
    const apiOptions = enableApiCall ? {
      apiCall: true,
      onApiSuccess: (result) => {
        console.log('Like API success:', result);
        // Optionally sync with server response
      },
      onApiError: (error) => {
        console.error('Like API error:', error);
        // Optionally revert the like state on API failure
        setLikesState(prev => togglePostLike(prev, postKey));
      }
    } : {};

    setLikesState(prev => togglePostLike(prev, postKey, apiOptions));
  };

  // Handle card press to open post details
  const handleCardPress = (postId) => {
    console.log('Post pressed with ID:', postId);
    setSelectedPostId(postId);
    setPostDetailsModalVisible(true);
  };

  // Handle like from PostDetailsModal
  const handleLikeFromModal = () => {
    if (selectedPostId) {
      handleLike(selectedPostId, true); // Enable API call for modal interactions
    }
  };

  // Handle reacts counter press
  const handleReactsPress = (postId, reactedUsersIds) => {
    console.log('Reacts pressed for post:', postId);
    console.log('Reacted users IDs:', reactedUsersIds);
    setSelectedPostReacts({ postId, reactedUsersIds });
    setReactsModalVisible(true);
  };

  // Modal control functions
  const openFilter = () => {
    setTempFilters({ ...filters });
    setModalVisible(true);
  };

  const applyFilters = () => {
    setFilters({ ...tempFilters });
    setModalVisible(false);
  };

  const resetFilters = () => {
    setTempFilters(getDefaultFilters());
  };

  const closeReactsModal = () => {
    setReactsModalVisible(false);
    setSelectedPostReacts({ postId: null, reactedUsersIds: [] });
  };

  const closePostDetailsModal = () => {
    setPostDetailsModalVisible(false);
    setSelectedPostId(null);
  };

  // Render functions
  const renderPost = ({ item, index }) => {
    const postKey = getPostKey(item, index);

    return (
      <PostCard
        {...item}
        index={index} // Pass index for consistent key generation
        isLiked={isPostLiked(likesState, postKey)}
        likes={getLikesCount(likesState, postKey, item)}
        onLike={(key) => handleLike(key, false)} // Disable API call for card interactions (faster UX)
        onPress={handleCardPress}
        onReactsPress={handleReactsPress}
     />
    );
  };

  const renderInlineHeader = () => (
    <View style={styles.inlineHeader}>
      <View style={styles.headerRow}>
        <View>
          <Text style={styles.title}>Posts</Text>
          <Text style={styles.subtitle}>
            Showing {filteredCount} of {totalCount} pets
          </Text>
        </View>
        <TouchableOpacity
          style={styles.filterButton}
          onPress={openFilter}
          activeOpacity={0.7}
        >
          <Ionicons name="filter-outline" size={20} color="#9188E5" />
          <Text style={styles.filterText}>Filter by</Text>
        </TouchableOpacity>
      </View>
    </View>
  );

  // Loading and error states
  if (isLoading) return <Text style={styles.center}>Loading...</Text>;
  if (error) return <Text style={styles.center}>Error: {error.message}</Text>;

  return (
    <View style={styles.container}>
      {/* Conditional Inline Header */}
      {showHeader && renderInlineHeader()}

      {/* Posts List */}
      <FlatList
        data={posts}
        keyExtractor={(item, idx) => getPostKey(item, idx).toString()}
        renderItem={renderPost}
        contentContainerStyle={styles.listContainer}
        showsVerticalScrollIndicator={false}
        scrollEnabled={false} // Disable internal scroll since parent handles it
        nestedScrollEnabled={true} // Enable nested scrolling
      />

      {/* Filter Modal */}
      <PostFiltering
        modalVisible={modalVisible}
        setModalVisible={setModalVisible}
        tempFilters={tempFilters}
        setTempFilters={setTempFilters}
        onApplyFilters={applyFilters}
        onResetFilters={resetFilters} />

      {/* Reacts Modal */}
      <ReactsModal
        visible={reactsModalVisible}
        onClose={closeReactsModal}
        postId={selectedPostReacts.postId}
        reactedUsersIds={selectedPostReacts.reactedUsersIds} />

      {/* Post Details Modal */}
      <PostDetailsModal
        visible={postDetailsModalVisible}
        onClose={closePostDetailsModal}
        postId={selectedPostId}
        isLiked={selectedPostId ? isPostLiked(likesState, selectedPostId) : false}
        onLike={handleLikeFromModal} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { 
    flex: 1, 
    backgroundColor: '#f9f9f9' 
  },
  center: { 
    flex: 1, 
    textAlign: 'center', 
    marginTop: 40,
    fontSize: 16,
    color: '#666',
    backgroundColor: '#f9f9f9',
  },
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
  },
  title: { 
    fontSize: 24, 
    fontWeight: '600',
    color: '#333'
  },
  subtitle: {
    fontSize: 14,
    color: '#666',
    marginTop: 2
  },
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
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  filterText: { 
    marginLeft: 4, 
    fontSize: 14,
    color: '#555'
  },
  listContainer: {
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 40,
    gap: 16,
  },
});