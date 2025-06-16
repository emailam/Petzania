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
import usePostsData from './usePostsData'; // adjust path
import PostFiltering from './PostFiltering'; // existing component
import PostDetailsModal from './PostDetailsModal'; // existing component
import ReactsModal from './ReactsModal'; // new component
import {
  initializeLikesState,
  togglePostLike,
  getPostKey,
  getDefaultFilters,
  getLikesCount,
  isPostLiked
} from '../services/postService'; // new service

const PostsSection = () => {
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
  const { posts, isLoading, error, filteredCount, totalCount } = usePostsData(filters);

  // Initialize likes state when posts change
  useEffect(() => {
    if (posts.length > 0) {
      const initialLikesState = initializeLikesState(posts);
      setLikesState(initialLikesState);
    }
  }, [posts]);

  // Handle like toggle
  const handleLike = (postKey) => {
    setLikesState(prev => togglePostLike(prev, postKey));
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
      handleLike(selectedPostId);
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
        isLiked={isPostLiked(likesState, postKey)}
        likes={getLikesCount(likesState, postKey, item)}
        onLike={() => handleLike(postKey)}
        onPress={handleCardPress}
        onReactsPress={handleReactsPress}
      />
    );
  };

  const renderHeader = () => (
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
  );

  // Loading and error states
  if (isLoading) return <Text style={styles.center}>Loading...</Text>;
  if (error) return <Text style={styles.center}>Error: {error.message}</Text>;

  return (
    <View style={styles.container}>
      {renderHeader()}

      <FlatList
        data={posts}
        keyExtractor={(item, idx) => getPostKey(item, idx).toString()}
        renderItem={renderPost}
        contentContainerStyle={styles.listContainer}
        showsVerticalScrollIndicator={false}
      />

      {/* Filter Modal */}
      <PostFiltering
        modalVisible={modalVisible}
        setModalVisible={setModalVisible}
        tempFilters={tempFilters}
        setTempFilters={setTempFilters}
        onApplyFilters={applyFilters}
        onResetFilters={resetFilters}
      />

      {/* Reacts Modal */}
      <ReactsModal
        visible={reactsModalVisible}
        onClose={closeReactsModal}
        postId={selectedPostReacts.postId}
        reactedUsersIds={selectedPostReacts.reactedUsersIds}
      />

      {/* Post Details Modal */}
      <PostDetailsModal
        visible={postDetailsModalVisible}
        onClose={closePostDetailsModal}
        postId={selectedPostId}
        isLiked={selectedPostId ? isPostLiked(likesState, selectedPostId) : false}
        onLike={handleLikeFromModal}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: { 
    flex: 1, 
    padding: 16, 
    backgroundColor: '#f9f9f9' 
  },
  center: { 
    flex: 1, 
    textAlign: 'center', 
    marginTop: 40,
    fontSize: 16,
    color: '#666'
  },
  headerRow: { 
    flexDirection: 'row', 
    justifyContent: 'space-between', 
    alignItems: 'center', 
    marginBottom: 16 
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
    borderColor: '#ddd'
  },
  filterText: { 
    marginLeft: 4, 
    fontSize: 14,
    color: '#555'
  },
  listContainer: {
    paddingBottom: 16,
    gap: 24
  },
});

export default PostsSection;