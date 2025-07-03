import React, { useState, useEffect, useCallback, memo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal,
  TouchableOpacity,
  Image,
  ScrollView,
  Dimensions,
  ActivityIndicator,
  Alert
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { getUserById } from '@/services/userService';

const { width, height } = Dimensions.get('window');

const PostDetailsModal = memo(({
  visible = false,
  onClose,
  post
}) => {
  const [ownerDetails, setOwnerDetails] = useState(null);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [loading, setLoading] = useState(false);

  // Load owner details when modal opens
  useEffect(() => {
    if (visible && post?.ownerId) {
      loadOwnerDetails();
    }
    // Reset state when modal closes
    return () => {
      if (!visible) {
        setCurrentImageIndex(0);
        setOwnerDetails(null);
      }
    };
  }, [visible, post?.ownerId]);

  const loadOwnerDetails = async () => {
    try {
      setLoading(true);
      const owner = await getUserById(post.ownerId);
      setOwnerDetails(owner);
    } catch (error) {
      console.error('Error loading owner details:', error);
      // Don't show error to user since owner section is optional
    } finally {
      setLoading(false);
    }
  };

  const handleImageScroll = useCallback((event) => {
    const slideSize = event.nativeEvent.layoutMeasurement.width;
    const index = Math.floor(event.nativeEvent.contentOffset.x / slideSize);
    setCurrentImageIndex(index);
  }, []);

  const handleChatPress = useCallback(() => {
    // TODO: Implement chat functionality
    Alert.alert('Chat', 'Chat functionality will be implemented soon');
  }, []);

  const handleAdopt = useCallback(() => {
    // TODO: Implement adopt functionality
    Alert.alert('Adopt', 'Adoption process will be implemented soon');
  }, []);

  if (!visible || !post) return null;

  const petData = post.petDTO;
  const images = petData?.myPicturesURLs || [];
  const hasImages = images.length > 0;
  const renderImagePagination = () => {
    if (images.length <= 1) return null;

    return (
      <View style={styles.paginationContainer}>
        {images.map((_, index) => (
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

  return (
    <Modal 
      animationType="slide" 
      transparent={true} 
      visible={visible} 
      onRequestClose={onClose}
    >
      <View style={styles.modalOverlay}>
        <View style={styles.modalContainer}>
          <TouchableOpacity
            style={styles.closeButton}
            onPress={onClose}
            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
          >
            <Ionicons name="close" size={24} color="#666" />
          </TouchableOpacity>

          <ScrollView showsVerticalScrollIndicator={false}>
            {/* Images Section */}
            <View style={styles.imageSection}>
                  {hasImages ? (
                    <>
                      <ScrollView
                        horizontal
                        pagingEnabled
                        showsHorizontalScrollIndicator={false}
                        onMomentumScrollEnd={handleImageScroll}
                        style={styles.imageScrollView}
                      >
                        {images.map((imageUrl, index) => (
                          <Image
                            key={index}
                            source={{ uri: imageUrl }}
                            style={styles.petImage}
                            resizeMode="cover"
                          />
                        ))}
                      </ScrollView>
                      {renderImagePagination()}
                    </>
                  ) : (
                    <Image
                      source={require('@/assets/images/Defaults/default-pet.png')}
                      style={styles.petImage}
                      resizeMode="cover"
                    />
                  )}
            </View>

            <View style={styles.detailsSection}>
              {/* Pet Name and Gender */}
              <View style={styles.petNameRow}>
                <View style={styles.petNameContainer}>
                  <Text style={styles.petName}>{petData?.name || 'Unknown Pet'}</Text>
                  <Ionicons 
                    name={petData?.gender?.toLowerCase() === 'female' ? 'female' : 'male'} 
                    size={20} 
                    color={petData?.gender?.toLowerCase() === 'male' ? '#2196F3' : '#E91E63'} 
                    style={styles.genderIcon} 
                  />
                </View>
              </View>

              {/* Location */}
              <View style={styles.locationRow}>
                <Ionicons name="location-outline" size={16} color="#666" />
                <Text style={styles.locationText}>
                  {post.location || 'Location not specified'}
                </Text>
              </View>

              {/* Pet Details Pills */}
              {/* Pet Details Pills */}
<View style={{marginBottom: 20}}>
  <Text style={styles.sectionLabel}>Pet Details</Text>
  <View style={styles.pillsContainer}>
    {petData?.breed && (
      <View style={styles.pill}>
        <Text style={styles.pillText}>{petData.breed}</Text>
      </View>
    )}
    {petData?.age && (
      <View style={styles.pill}>
        <Text style={styles.pillText}>{petData.age}</Text>
      </View>
    )}
    {petData?.vaccinated && (
      <View style={styles.pill}>
        <Text style={styles.pillText}>Vaccinated</Text>
      </View>
    )}
  </View>
</View>

{/* Owner Section */}
{(loading || ownerDetails) && (
  <Text style={styles.sectionLabel}>Owner</Text>
)}
{loading ? (
  <View style={styles.ownerLoadingContainer}>
    <ActivityIndicator size="small" color="#9188E5" />
    <Text style={styles.loadingText}>Loading owner info...</Text>
  </View>
) : ownerDetails ? (
  <View style={styles.ownerSection}>
    {/* ...owner info... */}
  </View>
) : null}

{/* Description Section */}
<Text style={styles.sectionLabel}>Description</Text>
<Text style={styles.description}>
  {petData?.description || post.description || 'No description available.'}
</Text>

              {/* Adopt Button */}
              <TouchableOpacity 
                style={styles.adoptButton} 
                onPress={handleAdopt}
                activeOpacity={0.8}
              >
                <Text style={styles.adoptButtonText}>
                  Adopt {petData?.name || 'this pet'}
                </Text>
              </TouchableOpacity>
            </View>
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
});

const styles = StyleSheet.create({
  sectionLabel: {
  fontSize: 14,
  color: '#374151', // gray-700
  fontWeight: '500',
  marginBottom: 8,
},
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  modalContainer: {
    backgroundColor: 'white',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    maxHeight: height * 0.9,
    minHeight: height * 0.5,
  },
  closeButton: {
    position: 'absolute',
    top: 16,
    right: 16,
    zIndex: 10,
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    borderRadius: 20,
    padding: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  ownerLoadingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: '#f0f0f0',
    marginBottom: 16,
  },
  loadingText: {
    marginLeft: 8,
    fontSize: 14,
    color: '#666',
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
    backgroundColor: '#f5f5f5',
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
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  detailsSection: {
    padding: 20,
    paddingBottom: 30,
  },
  petNameRow: {
    flexDirection: 'row',
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
  locationRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  locationText: {
    fontSize: 14,
    color: '#666',
    marginLeft: 4,
    flex: 1,
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
    paddingVertical: 12,
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: '#f0f0f0',
  },
  ownerInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  ownerAvatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    marginRight: 12,
    backgroundColor: '#f0f0f0',
  },
  ownerName: {
    fontSize: 16,
    fontWeight: '500',
    color: '#333',
  },
  chatButton: {
    padding: 8,
    backgroundColor: '#f8f6ff',
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#e8e5ff',
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
    shadowColor: '#9188E5',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 5,
  },
  adoptButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: 'white',
  },
});

PostDetailsModal.displayName = 'PostDetailsModal';

export default PostDetailsModal;