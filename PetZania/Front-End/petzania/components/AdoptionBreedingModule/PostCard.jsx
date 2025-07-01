import React, { memo, useState, useCallback } from 'react';
import {
  View,
  Text,
  Image,
  StyleSheet,
  TouchableOpacity,
  useWindowDimensions,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { getTimeAgo } from '../../services/postService';
import ToggleLike from './ToggleLike';
import ToggleInterest from './ToggleInterest';
import PostDetailsModal from './PostDetailsModal';
import EditPostModal from './EditPostModal';

function PostCard({
  post = null,
  showAdvancedFeatures = false,
  onPostUpdate,
  onPostDelete,
  onLikeChange,
  onToggleInterest = () => {},
  onPostDetails = () => {},
}) {
  const { width } = useWindowDimensions();
  const imageSize = width * 0.30;
  const detailsMaxWidth = width * 0.70 - 16;
  
  // Fix: Check if current user liked the post (you'll need currentUserId)
  // const initialLikedState = post?.reactedUsersIds?.includes(currentUserId) || false;
  const initialLikedState = post?.reactedUsersIds?.includes(post?.ownerId) || false; // Temporary fix
  
  // Local state
  const [detailsModalVisible, setDetailsModalVisible] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [isLiked, setIsLiked] = useState(initialLikedState);

  // Null checks
  if (!post) {
    console.warn('PostCard: Missing required props', { post });
    return null;
  }

  // Pet details with null safety
  const petName = post?.petDTO?.name || 'Unknown Pet';
  const petGender = post?.petDTO?.gender?.toLowerCase() || 'male';
  const petBreed = post?.petDTO?.breed || '—';
  const petAge = post?.petDTO?.age || '—';
  const petImage = post?.petDTO?.myPicturesURLs?.[0]
    ? { uri: post?.petDTO.myPicturesURLs[0] }
    : require('@/assets/images/Defaults/default-pet.png');

  const displayTimeAgo = getTimeAgo(post?.createdAt);

  // Handlers
  const handlePress = useCallback(() => {
    if (showAdvancedFeatures) {
      setEditModalVisible(true);
    } else {
      setDetailsModalVisible(true);
    }
  }, [showAdvancedFeatures]);

  const handleCloseDetailsModal = useCallback(() => {
    setDetailsModalVisible(false);
  }, []);

  const handleCloseEditModal = useCallback(() => {
    setEditModalVisible(false);
  }, []);

  const handleLikeChange = useCallback((newLikedState) => {
    setIsLiked(newLikedState);
    onLikeChange?.(newLikedState);
  }, [onLikeChange]);

  const handlePostUpdate = useCallback((updatedPost) => {
    onPostUpdate?.(updatedPost);
    setEditModalVisible(false);
  }, [onPostUpdate]);

  const handlePostDelete = useCallback(() => {
    onPostDelete?.(post?.postId);
    setEditModalVisible(false);
  }, [post?.postId, onPostDelete]);

  return (
    <>
      <TouchableOpacity
        activeOpacity={0.7}
        accessibilityRole="button"
        accessibilityLabel={`Open ${showAdvancedFeatures ? 'edit' : 'details'} for ${petName}`}
        onPress={handlePress}
      >
        <View style={styles.container}>
          <Image
            source={petImage}
            style={[styles.image, { width: imageSize, height: imageSize }]}
            defaultSource={require('@/assets/images/Defaults/default-pet.png')}
          />

          <View style={[styles.details, { maxWidth: detailsMaxWidth }]}>
            <View style={styles.headerRow}>
              <View style={styles.nameGenderRow}>
                <Text style={styles.nameText} numberOfLines={1}>
                  {petName}
                </Text>
                <Ionicons
                  name={petGender === 'female' ? 'female' : 'male'}
                  size={16}
                  color={petGender === 'male' ? '#2196F3' : '#E91E63'}
                  style={styles.genderIcon}
                />
              </View>

              <View style={styles.actionsContainer}>
                <ToggleLike
                  post={post}
                  onLikeChange={handleLikeChange}
                />
                <ToggleInterest postId={post?.postId} />
              </View>
            </View>

            <Text style={styles.timeText}>{displayTimeAgo}</Text>

            <View style={styles.locationRow}>
              <Ionicons
                name="location-outline"
                size={14}
                color="#777"
                style={{ marginRight: 4 }}
              />
              <Text style={styles.locationText}>{post?.location}</Text>
            </View>

            <View style={styles.pillsRow}>
              <View style={styles.pill}>
                <Text style={styles.pillText}>{petBreed}</Text>
              </View>
              <View style={styles.pill}>
                <Text style={styles.pillText}>{petAge}</Text>
              </View>
              {showAdvancedFeatures && (
                <View style={[styles.pill, styles.postTypePill]}>
                  <Text style={styles.pillText}>
                    {post?.postType?.charAt(0).toUpperCase() + post?.postType?.slice(1).toLowerCase()}
                  </Text>
                </View>
              )}
            </View>
          </View>
        </View>
      </TouchableOpacity>

      {/* Post Details Modal - for regular users */}
      {!showAdvancedFeatures && (
        <PostDetailsModal
          visible={detailsModalVisible}
          onClose={handleCloseDetailsModal}
          post={post}
        />
      )}

      {/* Edit Post Modal - for advanced features/post owners */}
      {showAdvancedFeatures && (
        <EditPostModal
          visible={editModalVisible}
          onClose={handleCloseEditModal}
          post={post}
          onUpdate={handlePostUpdate}
          onDelete={handlePostDelete}
        />
      )}
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderRadius: 12,
    borderColor: '#ddd',
    borderWidth: 1,
    overflow: 'hidden',
    elevation: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.2,
    shadowRadius: 2,
    padding: 8,
    alignItems: 'center',
    gap: 8,
  },
  image: {
    borderRadius: 8
  },
  details: {
    marginLeft: 8
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  nameGenderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    maxWidth: '75%'
  },
  nameText: {
    fontSize: 16,
    fontWeight: '500',
    color: '#333',
    flexShrink: 1
  },
  genderIcon: {
    marginLeft: 8
  },
  actionsContainer: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8
  },
  timeText: {
    fontSize: 12,
    color: '#888',
    marginTop: 2
  },
  locationRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 3
  },
  locationText: {
    fontSize: 12,
    color: '#555'
  },
  pillsRow: {
    flexDirection: 'row',
    marginTop: 6,
    flexWrap: 'wrap'
  },
  pill: {
    backgroundColor: '#9188E5',
    borderRadius: 16,
    paddingVertical: 4,
    paddingHorizontal: 12,
    marginRight: 8,
    marginBottom: 4,
  },
  postTypePill: {
    backgroundColor: '#9188E5'
  },
  pillText: {
    fontSize: 12,
    color: 'white'
  },
});

// Custom comparison for memo optimization
const areEqual = (prev, next) => {
  return (
    prev.post?.postId === next.post?.postId &&
    prev.post?.reacts === next.post?.reacts &&
    prev.post?.reactedUsersIds?.length === next.post?.reactedUsersIds?.length
  );
};

export default memo(PostCard, areEqual);