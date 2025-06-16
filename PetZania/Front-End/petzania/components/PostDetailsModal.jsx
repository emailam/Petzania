import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal,
  TouchableOpacity,
  Image,
  ScrollView,
  Dimensions,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';

const { width, height } = Dimensions.get('window');

// Mock function to fetch post details - replace with your actual API call
const fetchPostDetails = async (postId) => {
  // Replace this with your actual API call
  return {
    "postId": postId,
    "ownerId": "8e2a7ad4-4a2f-4b3d-b5f1-1a2c3e4d5f6a",
    "petDTO": {
      "petId": "1c2d3e4f-5a6b-7c8d-9e0f-1234567890ab",
      "name": "Bella",
      "description": "A friendly Maltipoo looking for a loving home.",
      "gender": "FEMALE",
      "dateOfBirth": "2023-03-15",
      "age": "1 year",
      "breed": "Maltipoo",
      "species": "DOG",
      "myVaccinesURLs": [],
      "myPicturesURLs": [
        "https://hips.hearstapps.com/hmg-prod/images/small-fluffy-dog-breeds-maltipoo-66300ad363389.jpg?crop=0.668xw:1.00xh;0.151xw,0&resize=640:*",
        "https://example.com/image2.jpg",
        "https://example.com/image3.jpg"
      ]
    },
    "postStatus": "PENDING",
    "reactedUsersIds": [
      "11111111-2222-3333-4444-555555555555",
      "66666666-7777-8888-9999-000000000000"
    ]
  };
};

// Mock function to fetch owner details - replace with your actual API call
const fetchOwnerDetails = async (ownerId) => {
  // Replace this with your actual API call
  return {
    "name": "Shawky Ibrahim",
    "profilePictureURL":"https://hips.hearstapps.com/hmg-prod/images/small-fluffy-dog-breeds-maltipoo-66300ad363389.jpg?crop=0.668xw:1.00xh;0.151xw,0&resize=640:*",

  };
};

// Mock function to update post status - replace with your actual API call
const updatePostStatus = async (postId, status) => {
  // Replace this with your actual API call
  console.log(`Updating post ${postId} to status: ${status}`);
  return { success: true };
};

const PostDetailsModal = ({ 
  visible, 
  onClose, 
  postId,
  isLiked = false,
  onLike
}) => {
  const [postDetails, setPostDetails] = useState(null);
  const [ownerDetails, setOwnerDetails] = useState(null);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (visible && postId) {
      loadPostDetails();
    }
  }, [visible, postId]);

  const loadPostDetails = async () => {
    setLoading(true);
    try {
      const post = await fetchPostDetails(postId);
      const owner = await fetchOwnerDetails(post.ownerId);
      setPostDetails(post);
      setOwnerDetails(owner);
    } catch (error) {
      console.error('Error loading post details:', error);
      Alert.alert('Error', 'Failed to load post details');
    } finally {
      setLoading(false);
    }
  };

  const handleAdopt = async () => {
    if (!postDetails) return;
    
    Alert.alert(
      'Confirm Adoption',
      `Are you sure you want to adopt ${postDetails.petDTO.name}?`,
      [
        { text: 'Cancel', style: 'cancel' },
        { 
          text: 'Adopt', 
          onPress: async () => {
            try {
              await updatePostStatus(postId, 'COMPLETED');
              Alert.alert('Success', 'Adoption request submitted!');
              onClose();
            } catch (error) {
              Alert.alert('Error', 'Failed to submit adoption request');
            }
          }
        }
      ]
    );
  };

  const handleChatPress = () => {
    // Empty functionality as requested - will be implemented later
    console.log('Chat button pressed');
  };

  const renderImagePagination = () => {
    if (!postDetails?.petDTO?.myPicturesURLs || postDetails.petDTO.myPicturesURLs.length <= 1) {
      return null;
    }

    return (
      <View style={styles.paginationContainer}>
        {postDetails.petDTO.myPicturesURLs.map((_, index) => (
          <View
            key={index}
            style={[
              styles.paginationDot,
              index === currentImageIndex && styles.paginationDotActive
            ]}
          />
        ))}
      </View>
    );
  };

  if (!visible) return null;

  return (
    <Modal
      animationType="slide"
      transparent={true}
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.modalOverlay}>
        <View style={styles.modalContainer}>
          {/* Close Button */}
          <TouchableOpacity style={styles.closeButton} onPress={onClose}>
            <Ionicons name="close" size={24} color="#666" />
          </TouchableOpacity>

          {loading ? (
            <View style={styles.loadingContainer}>
              <Text>Loading...</Text>
            </View>
          ) : postDetails ? (
            <ScrollView showsVerticalScrollIndicator={false}>
              {/* Pet Images Section */}
              <View style={styles.imageSection}>
                <ScrollView
                  horizontal
                  pagingEnabled
                  showsHorizontalScrollIndicator={false}
                  onMomentumScrollEnd={(event) => {
                    const index = Math.round(event.nativeEvent.contentOffset.x / width);
                    setCurrentImageIndex(index);
                  }}
                  style={styles.imageScrollView}
                >
                  {postDetails.petDTO.myPicturesURLs.map((imageUrl, index) => (
                    <Image
                      key={index}
                      source={{ uri: imageUrl }}
                      style={styles.petImage}
                      resizeMode="cover"
                    />
                  ))}
                </ScrollView>
                {renderImagePagination()}
              </View>

              {/* Details Section */}
              <View style={styles.detailsSection}>
                {/* Pet Name and Heart */}
                <View style={styles.petNameRow}>
                  <View style={styles.petNameContainer}>
                    <Text style={styles.petName}>{postDetails.petDTO.name}</Text>
                    <Ionicons 
                      name={postDetails.petDTO.gender?.toLowerCase() === 'female' ? 'female' : 'male'} 
                      size={20} 
                      color={postDetails.petDTO.gender?.toLowerCase() === 'male' ? 'blue' : 'pink'} 
                      style={styles.genderIcon} 
                    />
                  </View>
                  <TouchableOpacity onPress={onLike} style={styles.heartButton}>
                    <Ionicons 
                      name={isLiked ? "heart" : "heart-outline"} 
                      size={28} 
                      color={isLiked ? "#FF3040" : "#666"} 
                    />
                  </TouchableOpacity>
                </View>

                {/* Location */}
                <View style={styles.locationRow}>
                  <Ionicons name="location-outline" size={16} color="#666" />
                  <Text style={styles.locationText}>New Cairo, 5th District (50 km)</Text>
                </View>

                {/* Pet Details Pills */}
                <View style={styles.pillsContainer}>
                  <View style={styles.pill}>
                    <Text style={styles.pillText}>{postDetails.petDTO.breed}</Text>
                  </View>
                  <View style={styles.pill}>
                    <Text style={styles.pillText}>{postDetails.petDTO.age}</Text>
                  </View>
                  <View style={styles.pill}>
                    <Text style={styles.pillText}>Vaccinated</Text>
                  </View>
                </View>

                {/* Owner Section */}
                <View style={styles.ownerSection}>
                  <View style={styles.ownerInfo}>
                    <Image
                      source={{ 
                        uri: ownerDetails?.profilePictureURL || 'https://via.placeholder.com/40x40' 
                      }}
                      style={styles.ownerAvatar}
                    />
                    <Text style={styles.ownerName}>{ownerDetails?.name || 'Owner Name'}</Text>
                  </View>
                  <TouchableOpacity style={styles.chatButton} onPress={handleChatPress}>
                    <Ionicons name="chatbubble-outline" size={20} color="#9188E5" />
                  </TouchableOpacity>
                </View>

                {/* Description */}
                <Text style={styles.description}>
                  {postDetails.petDTO.description || 'No description available.'}
                </Text>

                {/* Adopt Button */}
                <TouchableOpacity style={styles.adoptButton} onPress={handleAdopt}>
                  <Text style={styles.adoptButtonText}>
                    Adopt {postDetails.petDTO.name}
                  </Text>
                </TouchableOpacity>
              </View>
            </ScrollView>
          ) : (
            <View style={styles.errorContainer}>
              <Text>Error loading post details</Text>
            </View>
          )}
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  modalContainer: {
    backgroundColor: 'white',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    maxHeight: height * 0.8,
    position: 'relative',
  },
  closeButton: {
    position: 'absolute',
    top: 16,
    right: 16,
    zIndex: 10,
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    borderRadius: 20,
    padding: 8,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: 200,
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: 200,
  },
  imageSection: {
    position: 'relative',
  },
  imageScrollView: {
    height: height * 0.4,
  },
  petImage: {
    width: width,
    height: height * 0.4,
  },
  paginationContainer: {
    position: 'absolute',
    bottom: 16,
    left: 0,
    right: 0,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
  },
  paginationDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: 'rgba(255, 255, 255, 0.5)',
    marginHorizontal: 4,
  },
  paginationDotActive: {
    backgroundColor: '#9188E5',
  },
  detailsSection: {
    padding: 20,
  },
  petNameRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  petNameContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  petName: {
    fontSize: 24,
    fontWeight: '600',
    color: '#333',
  },
  genderIcon: {
    marginLeft: 8,
  },
  heartButton: {
    padding: 8,
  },
  locationRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  locationText: {
    fontSize: 14,
    color: '#666',
    marginLeft: 4,
  },
  pillsContainer: {
    flexDirection: 'row',
    marginBottom: 20,
    flexWrap: 'wrap',
  },
  pill: {
    backgroundColor: '#9188E5',
    borderRadius: 16,
    paddingVertical: 6,
    paddingHorizontal: 16,
    marginRight: 8,
    marginBottom: 8,
  },
  pillText: {
    fontSize: 12,
    color: 'white',
    fontWeight: '500',
  },
  ownerSection: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  ownerInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  ownerAvatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    marginRight: 12,
  },
  ownerName: {
    fontSize: 16,
    fontWeight: '500',
    color: '#9188E5',
  },
  chatButton: {
    padding: 8,
    backgroundColor: '#f0f0f0',
    borderRadius: 20,
  },
  description: {
    fontSize: 14,
    color: '#666',
    lineHeight: 20,
    marginBottom: 24,
  },
  adoptButton: {
    backgroundColor: '#9188E5',
    borderRadius: 25,
    paddingVertical: 16,
    alignItems: 'center',
    marginBottom: 20,
  },
  adoptButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: 'white',
  },
});

export default PostDetailsModal;