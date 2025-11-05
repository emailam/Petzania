import React, { memo, useState, useCallback, useContext } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  useWindowDimensions,
  Platform,
} from 'react-native';
import { Image } from 'expo-image';
import { Ionicons } from '@expo/vector-icons';
import { getTimeAgo } from '../../services/postService';
import ToggleLike from './ToggleLike';
import PostDetailsModal from './PostDetailsModal';
import EditPostModal from './EditPostModal';
import ReactsModal from './ReactsModal';
import { UserContext } from '@/context/UserContext';

function PostCard({
  post = null,
  showAdvancedFeatures,
  onPostUpdate,
  onPostDelete,
  onPostLikeToggle,
  getUsers,
}) {
  if (!post) {
    return null;
  }

  const { width, height } = useWindowDimensions();
  const { user } = useContext(UserContext);
  
  // Responsive calculations
  const isSmallScreen = width < 380;
  const isMediumScreen = width >= 380 && width < 768;
  const isLargeScreen = width >= 768;
  
  // Dynamic image sizing - adjusted for 2-column layout
  const imageSize = isSmallScreen ? 120 : isMediumScreen ? 160 : 160;
  
  const [detailsModalVisible, setDetailsModalVisible] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [reactsModalVisible, setReactsModalVisible] = useState(false);
  
  const petDTO = post?.petDTO || {};
  const displayTimeAgo = getTimeAgo(post?.createdAt);

  // Handlers
  const handleCardPress = () => {
    if (showAdvancedFeatures) {
      setEditModalVisible(true);
    } else {
      setDetailsModalVisible(true);
    }
  };

  const handleCloseDetailsModal = useCallback(() => {
    setDetailsModalVisible(false);
  }, []);

  const handleCloseEditModal = useCallback(() => {
    setEditModalVisible(false);
  }, []);

  const handleReactsModalPress = useCallback(() => {
    setReactsModalVisible(true);
  }, []);
  
  const handleReactsModalClose = useCallback(() => {
    setReactsModalVisible(false);
  }, []);

  return (
    <>
      <TouchableOpacity
        activeOpacity={0.8}
        accessibilityRole="button"
        accessibilityLabel={`Open ${showAdvancedFeatures ? 'edit' : 'details'} for ${petDTO.name}`}
        onPress={handleCardPress}
        style={[styles.container, isLargeScreen && styles.containerLarge]}
      >
        <View>
          {/* Image Container with Like Button Overlay */}
          <View style={styles.imageContainer}>
            <Image
              source={petDTO.myPicturesURLs[0] ? { uri: petDTO.myPicturesURLs[0] } : require('@/assets/images/Defaults/default-pet.png')}
              style={[styles.image , { width: imageSize, height: imageSize }]}
              contentFit="fill"
            />
            {/* Like Button positioned on top-right of image */}
            <View style={styles.likeOverlay}>
              <ToggleLike
                postId={post.postId}
                onLikeChange={onPostLikeToggle}
                initialLiked={post?.reactedUsersIds?.includes(user.userId)}
                onLongPress={handleReactsModalPress}
              />
            </View>
          </View>

          <View style={styles.contentContainer}>
            {/* Header Section */}
            <View style={styles.headerSection}>
              <View style={styles.titleRow}>
                <View style={styles.nameGenderContainer}>
                  <Text style={[styles.nameText, isSmallScreen && styles.nameTextSmall]} numberOfLines={1}>
                    {petDTO.name}
                  </Text>
                  <Ionicons
                    name={petDTO.gender === 'FEMALE' ? 'female' : 'male'}
                    size={isSmallScreen ? 16 : 18}
                    color={petDTO.gender === 'MALE' ? '#3B82F6' : '#EC4899'}
                    style={styles.genderIcon}
                  />
                </View>
                <Text style={styles.timeText}>{displayTimeAgo}</Text>
              </View>

              {/* Location */}
              <View style={styles.locationContainer}>
                <Ionicons
                  name="location-outline"
                  size={14}
                  color="#6B7280"
                />
                <Text style={styles.locationText} numberOfLines={1}>
                  {post.location}
                </Text>
              </View>
            </View>

            {/* Tags Section */}
            <View style={styles.tagsContainer}>
              {showAdvancedFeatures && (
                <View style={styles.tagAccent}>
                  <Text style={styles.tagTextAccent}>
                    {post?.postType?.charAt(0).toUpperCase() + post?.postType?.slice(1).toLowerCase()}
                  </Text>
                </View>
              )}
            </View>
          </View>
        </View>
      </TouchableOpacity>

      {/* Modals */}
      <ReactsModal
        post={post}
        visible={reactsModalVisible}
        onClose={handleReactsModalClose}
        getUsers={getUsers}
      />

      {!showAdvancedFeatures && (
        <PostDetailsModal
          visible={detailsModalVisible}
          onClose={handleCloseDetailsModal}
          post={post}
        />
      )}

      {showAdvancedFeatures && (
        <EditPostModal
          visible={editModalVisible}
          onClose={handleCloseEditModal}
          post={post}
          onUpdate={onPostUpdate}
          onDelete={onPostDelete}
        />
      )}
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'column',
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    marginVertical: 8,
    padding: 8,
    flex: 1,
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.08,
        shadowRadius: 8,
      },
      android: {
        elevation: 2,
      },
    }),
  },
  containerLarge: {
    alignSelf: 'center',
  },
  imageContainer: {
    position: 'relative',
    marginBottom: 12,
    alignSelf: 'center',
  },
  image: {
    borderRadius: 12,
    backgroundColor: '#F3F4F6',
  },
  likeOverlay: {
    position: 'absolute',
    top: 8,
    right: 8,
    borderRadius: 20,
  },
  contentContainer: {
    flex: 1,
    justifyContent: 'space-between',
  },
  headerSection: {
    marginBottom: 8,
  },
  titleRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 4,
  },
  nameGenderContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    marginRight: 8,
  },
  nameText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1F2937',
    marginRight: 6,
  },
  nameTextSmall: {
    fontSize: 16,
  },
  genderIcon: {
    marginTop: 2,
  },
  timeText: {
    fontSize: 12,
    color: '#9CA3AF',
    fontWeight: '400',
  },
  locationContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  locationText: {
    fontSize: 13,
    color: '#6B7280',
    flex: 1,
  },
  tagsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    marginBottom: 8,
  },
  tagPrimary: {
    backgroundColor: '#EDE9FE',
    borderRadius: 20,
    paddingVertical: 4,
    paddingHorizontal: 12,
  },
  tagSecondary: {
    backgroundColor: '#F3F4F6',
    borderRadius: 20,
    paddingVertical: 4,
    paddingHorizontal: 12,
  },
  tagAccent: {
    backgroundColor: '#DBEAFE',
    borderRadius: 20,
    paddingVertical: 4,
    paddingHorizontal: 12,
  },
  tagTextPrimary: {
    fontSize: 12,
    color: '#7C3AED',
    fontWeight: '500',
  },
  tagTextSecondary: {
    fontSize: 12,
    color: '#4B5563',
    fontWeight: '500',
  },
  tagTextAccent: {
    fontSize: 12,
    color: '#2563EB',
    fontWeight: '500',
  },

  reactCountButton: {
    backgroundColor: '#F9FAFB',
    borderRadius: 16,
    paddingVertical: 4,
    paddingHorizontal: 10,
    borderWidth: 1,
    borderColor: '#E5E7EB',
  },
  reactCountText: {
    fontSize: 13,
    color: '#6B7280',
    fontWeight: '500',
  },
});

// Custom comparison for memo optimization
const areEqual = (prev, next) => {
  return (
    prev.post?.postId === next.post?.postId &&
    prev.post?.reacts === next.post?.reacts &&
    prev.post?.reactedUsersIds?.length === next.post?.reactedUsersIds?.length &&
    prev.post?.location === next.post?.location &&
    prev.post?.createdAt === next.post?.createdAt
  );
};

export default memo(PostCard, areEqual);