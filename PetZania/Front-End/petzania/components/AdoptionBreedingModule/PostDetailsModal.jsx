import React, { useState, useEffect, useCallback, memo, useContext, useRef, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Dimensions,
  ActivityIndicator,
  ScrollView,
  BackHandler
} from 'react-native';
import { Image } from 'expo-image';
import {
  BottomSheetModal,
  BottomSheetScrollView,
  BottomSheetBackdrop,
} from '@gorhom/bottom-sheet';
import { createChat } from '@/services/chatService';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import Toast from 'react-native-toast-message';
import { getUserById } from '@/services/userService';
import { UserContext } from '@/context/UserContext';
import LottieView from 'lottie-react-native';
import ImageViewing from 'react-native-image-viewing';

const { width, height } = Dimensions.get('window');

const PostDetailsModal = memo(({
  visible = false,
  onClose,
  post
}) => {
  const bottomSheetModalRef = useRef(null);
  const snapPoints = useMemo(() => ['90%'], []);

  const [ownerDetails, setOwnerDetails] = useState(null);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [loading, setLoading] = useState(false);
  const [chatLoading, setChatLoading] = useState(false);
  const [imageViewerVisible, setImageViewerVisible] = useState(false);
  const [imageViewerIndex, setImageViewerIndex] = useState(0);

  const router = useRouter();
  const { user } = useContext(UserContext);

  const isOwner = user?.userId === post?.ownerId;

  // Handle modal visibility
  useEffect(() => {
    if (visible) {
      bottomSheetModalRef.current?.present();
    } else {
      bottomSheetModalRef.current?.dismiss();
    }
  }, [visible]);

  // Handle Android back button
  useEffect(() => {
    const backAction = () => {
      if (visible) {
        onClose?.();
        return true;
      }
      return false;
    };

    const backHandler = BackHandler.addEventListener('hardwareBackPress', backAction);
    return () => backHandler.remove();
  }, [visible, onClose]);

  // Handle modal dismiss
  const handleSheetChanges = useCallback((index) => {
    if (index === -1) {
      onClose?.();
    }
  }, [onClose]);

  // Backdrop component
  const renderBackdrop = useCallback(
    (props) => (
      <BottomSheetBackdrop
        {...props}
        disappearsOnIndex={-1}
        appearsOnIndex={0}
        opacity={0.5}
        pressBehavior="close"
      />
    ),
    []
  );

  const goToPetProfile = () => {
    bottomSheetModalRef.current?.dismiss();
    router.push({ pathname: `/PetModule/${post.petDTO.petId}` });
  };

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

  const goToOwnerProfile = () => {
    if (ownerDetails?.userId) {
      onClose();
      router.push({
          pathname: `/UserModule/${ownerDetails.userId}`,
          params: { username: ownerDetails.username }
      })
    } else {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Owner information not available',
        position: 'top',
        visibilityTime: 3000,
      });
    }
  };

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

  const handleImagePress = useCallback((index) => {
    setImageViewerIndex(index);
    setImageViewerVisible(true);
  }, []);

  const handleChatPress = useCallback(async () => {
    if (!post?.ownerId) {
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Owner information not available',
        position: 'top',
        visibilityTime: 3000,
      });
      return;
    }

    try {
      setChatLoading(true);
      console.log('Creating chat with pet owner:', post.ownerId);
      const chat = await createChat(post.ownerId);

      // Close the modal first
      onClose();

      // Navigate to chat
      router.push(`/Chat/${chat.chatId}`);
    } catch (error) {
      console.error('Error creating chat:', error);
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: 'Failed to start conversation. Please try again.',
        position: 'top',
        visibilityTime: 3000,
      });
    } finally {
      setChatLoading(false);
    }
  }, [post?.ownerId, onClose, router]);

  if (!visible || !post) return null;

  const petData = post.petDTO;
  const images = petData?.myPicturesURLs || [];
  const hasImages = images.length > 0;

  // Prepare images for ImageViewing component
  const imageViewerImages = images.map(uri => ({ uri }));

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
    <>
      <BottomSheetModal
        ref={bottomSheetModalRef}
        snapPoints={snapPoints}
        onChange={handleSheetChanges}
        backdropComponent={renderBackdrop}
        enablePanDownToClose={true}
        enableDismissOnClose={true}
        handleComponent={null}
        backgroundStyle={styles.bottomSheetBackground}
      >
        <BottomSheetScrollView
          style={styles.scrollContent}
          contentContainerStyle={styles.scrollContentContainer}
          showsVerticalScrollIndicator={false}
        >
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
                      <TouchableOpacity
                        key={index}
                        activeOpacity={0.9}
                        onPress={() => handleImagePress(index)}
                      >
                        <Image
                          source={{ uri: imageUrl }}
                          style={styles.petImage}
                          resizeMode="cover"
                        />
                      </TouchableOpacity>
                    ))}
                  </ScrollView>
                  {renderImagePagination()}
                </>
              ) : (
                <TouchableOpacity
                  activeOpacity={0.9}
                  onPress={() => handleImagePress(0)}
                >
                  <Image
                    source={require('@/assets/images/Defaults/default-pet.png')}
                    style={styles.petImage}
                    resizeMode="cover"
                  />
                </TouchableOpacity>
              )}
            </View>

            <View style={styles.detailsSection}>
              {/* Pet Name and Gender */}
              <View style={styles.petNameRow}>
                <View style={styles.petNameContainer}>
                  <TouchableOpacity onPress={goToPetProfile} style={{ flexDirection: 'row', alignItems: 'center' }}>
                    <Image
                      source={{ uri: images[0] }}
                      style={styles.ownerAvatar}
                      resizeMode="cover"
                    />
                    <Text style={styles.petName}>{petData?.name || 'Unknown Pet'}</Text>
                  </TouchableOpacity>
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
                  <TouchableOpacity style={styles.ownerInfo} onPress={goToOwnerProfile}>
                    <Image
                      source={
                        ownerDetails.profilePictureURL 
                          ? { uri: ownerDetails.profilePictureURL }
                          : require('@/assets/images/Defaults/default-user.png')
                      }
                      style={styles.ownerAvatar}
                    />
                    <Text style={styles.ownerName}>
                      {ownerDetails.firstName && ownerDetails.lastName 
                        ? `${ownerDetails.firstName} ${ownerDetails.lastName}`
                        : ownerDetails.username || 'Pet Owner'
                      }
                    </Text>
                  </TouchableOpacity>
                </View>
              ) : null}

              {/* Description Section */}
              <Text style={styles.sectionLabel}>Description</Text>
              <Text style={styles.description}>
                { post.description || 'No description available.'}
              </Text>

              {/* Chat Button - Only show if not owner */}
              {!isOwner && (
                <TouchableOpacity 
                  style={[styles.adoptButton, chatLoading && styles.adoptButtonLoading]}
                  onPress={handleChatPress}
                  activeOpacity={0.8}
                  disabled={chatLoading}
                >
                  {chatLoading ? (
                    <View style={styles.adoptButtonContent}>
                      <LottieView
                        source={require("@/assets/lottie/loading.json")}
                        autoPlay
                        loop
                        style={styles.lottieLoader}
                      />
                      <Text style={styles.adoptButtonText}>Starting chat...</Text>
                    </View>
                  ) : (
                    <View style={styles.adoptButtonContent}>
                      <Ionicons name="chatbubble-outline" size={20} color="white" style={styles.adoptButtonLoader} />
                      <Text style={styles.adoptButtonText}>
                        Chat with Owner
                      </Text>
                    </View>
                  )}
                </TouchableOpacity>
              )}
            </View>
          </BottomSheetScrollView>
        </BottomSheetModal>

        {/* Image Viewer Modal */}
        <ImageViewing
          images={hasImages ? imageViewerImages : [{ uri: Image.resolveAssetSource(require('@/assets/images/Defaults/default-pet.png')).uri }]}
          imageIndex={imageViewerIndex}
          visible={imageViewerVisible}
          onRequestClose={() => setImageViewerVisible(false)}
          animationType="fade"
          backgroundColor="rgba(0, 0, 0, 0.9)"
          presentationStyle="overFullScreen"
        />
      </>
  );
});

const styles = StyleSheet.create({
  sectionLabel: {
  fontSize: 14,
  color: '#666', // gray-700
  fontWeight: '500',
  marginBottom: 8,
},
  bottomSheetBackground: {
    backgroundColor: 'white',
  },
  scrollContent: {
    flex: 1,
  },
  scrollContentContainer: {
    paddingBottom: 20,
  },
  header: {
    alignItems: 'flex-end',
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 8,
  },
  closeButton: {
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
    borderWidth: 1,
    borderColor: '#9188E5',
    backgroundColor: '#f0f0f0',
  },
  ownerName: {
    fontSize: 16,
    fontWeight: '500',
    color: '#333',
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
  adoptButtonLoading: {
    opacity: 0.7,
  },
  adoptButtonContent: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  adoptButtonLoader: {
    marginRight: 8,
  },
  lottieLoader: {
    width: 24,
    height: 24,
    marginRight: 8,
  },
  adoptButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: 'white',
  },
});

PostDetailsModal.displayName = 'PostDetailsModal';

export default PostDetailsModal;