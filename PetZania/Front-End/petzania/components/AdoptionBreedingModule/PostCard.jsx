import React, { useState, useRef } from 'react';
import { 
  View, 
  Text, 
  Image, 
  StyleSheet, 
  Dimensions, 
  TouchableOpacity,
  Modal,
  Animated
} from 'react-native';
import { PanGestureHandler, State, TapGestureHandler } from 'react-native-gesture-handler';
import { Ionicons } from '@expo/vector-icons';
import { getTimeAgo, getPostKey } from '../../services/postService';

const { width } = Dimensions.get('window');
const SWIPE_THRESHOLD = width * 0.5; // Half of screen width

export default function PostCard({
  postId,
  petDTO,
  reacts = 0,
  reactedUsersIds = [],
  location = 'City, Country',
  createdAt,
  isLiked = false,
  likes = 0,
  onLike,
  onPress,
  onReactsPress,
  onInterestChange,
  index = 0,
  showAdvancedFeatures = false, // New prop
  postType = 'General', // New prop for post type
  onDeletePost, // New prop for delete callback
}) {
  const [showMenu, setShowMenu] = useState(false);
  const [selectedInterest, setSelectedInterest] = useState(null);
  const [menuPosition, setMenuPosition] = useState({ x: 0, y: 0 });
  const menuButtonRef = useRef(null);
  const fadeAnim = useRef(new Animated.Value(0)).current;
  
  // Swipe animation values
  const translateX = useRef(new Animated.Value(0)).current;
  const [isDeleteVisible, setIsDeleteVisible] = useState(false);

  // Gesture handler refs
  const panRef = useRef(null);
  const tapRef = useRef(null);

  // Extract data from petDTO
  const petName = petDTO?.name || 'Pet Name';
  const petGender = petDTO?.gender?.toLowerCase() || 'male';
  const petBreed = petDTO?.breed || 'Breed';
  const petAge = petDTO?.age || '2 years';
  const petImage = petDTO?.myPicturesURLs?.[0] || 'https://hips.hearstapps.com/hmg-prod/images/small-fluffy-dog-breeds-maltipoo-66300ad363389.jpg?crop=0.668xw:1.00xh;0.151xw,0&resize=640:*';
  
  const displayTimeAgo = getTimeAgo(createdAt);
  const displayLikes = likes || reacts || 0;
  const postKey = getPostKey({ postId, id: postId }, index);

  const handleCardPress = () => {
    if (onPress && !isDeleteVisible) {
      onPress(postId || postKey);
    }
  };

  const handleLikePress = (event) => {
    event.stopPropagation();
    if (onLike) {
      onLike(postKey);
    }
  };

  const handleReactsPress = (event) => {
    event.stopPropagation();
    if (onReactsPress) {
      onReactsPress(postId || postKey, reactedUsersIds);
    }
  };

  const handleMenuPress = (event) => {
    event.stopPropagation();
    
    if (menuButtonRef.current) {
      menuButtonRef.current.measure((fx, fy, width, height, px, py) => {
        setMenuPosition({
          x: px - 120,
          y: py + height + 5
        });
        setShowMenu(true);
        
        Animated.timing(fadeAnim, {
          toValue: 1,
          duration: 200,
          useNativeDriver: true,
        }).start();
      });
    }
  };

  const hideMenu = () => {
    Animated.timing(fadeAnim, {
      toValue: 0,
      duration: 150,
      useNativeDriver: true,
    }).start(() => {
      setShowMenu(false);
    });
  };

  const handleInterestSelect = (interest) => {
    setSelectedInterest(interest);
    hideMenu();
    if (onInterestChange) {
      onInterestChange(postId || postKey, interest);
    }
  };

  const getInterestIndicator = () => {
    if (selectedInterest === 'interested') return { color: '#4CAF50', icon: 'checkmark-circle' };
    if (selectedInterest === 'not') return { color: '#FF5722', icon: 'close-circle' };
    return null;
  };

  // Gesture handlers for swipe functionality
  const onGestureEvent = Animated.event(
    [{ nativeEvent: { translationX: translateX } }],
    { 
      useNativeDriver: false,
      listener: (event) => {
        // Prevent right swipe by clamping the value
        const { translationX } = event.nativeEvent;
        if (translationX > 0) {
          translateX.setValue(0);
        }
      }
    }
  );

  const onHandlerStateChange = (event) => {
    if (!showAdvancedFeatures) return;

    const { state, translationX } = event.nativeEvent;

    if (state === State.END) {
      const swipeDistance = Math.abs(translationX);
      
      if (translationX < 0 && swipeDistance > SWIPE_THRESHOLD) {
        // Swiped left more than half - show delete button
        setIsDeleteVisible(true);
        Animated.spring(translateX, {
          toValue: -80,
          useNativeDriver: false,
          tension: 100,
          friction: 8,
        }).start();
      } else {
        // Return to normal position
        setIsDeleteVisible(false);
        Animated.spring(translateX, {
          toValue: 0,
          useNativeDriver: false,
          tension: 100,
          friction: 8,
        }).start();
      }
    }
  };

  // Handle tap gesture when advanced features are enabled
  const onTapHandlerStateChange = (event) => {
    if (event.nativeEvent.state === State.END && !isDeleteVisible) {
      handleCardPress();
    }
  };

  const handleDeletePress = () => {
    if (onDeletePost) {
      onDeletePost(postId || postKey);
    }
    // Reset to normal position
    setIsDeleteVisible(false);
    Animated.spring(translateX, {
      toValue: 0,
      useNativeDriver: false,
    }).start();
  };

  const interestIndicator = getInterestIndicator();

  const CardContent = (
    <View style={styles.container}>
      <Image source={{ uri: petImage }} style={styles.image} />
      <View style={styles.details}>
        <View style={styles.headerRow}> 
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Text style={styles.nameText}>{petName}</Text>
            <Ionicons 
              name={petGender === 'female' ? 'female' : 'male'} 
              size={16} 
              color={petGender === 'male' ? '#2196F3' : '#E91E63'} 
              style={styles.genderIcon} 
            />
            {interestIndicator && (
              <Ionicons 
                name={interestIndicator.icon}
                size={16} 
                color={interestIndicator.color} 
                style={styles.interestIndicator} 
              />
            )}
          </View>
          <View style={styles.actionsContainer}>
            <View style={styles.likeSection}>
              <TouchableOpacity 
                style={styles.likeButton}
                onPress={handleLikePress}
                hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                onPressIn={() => tapRef.current?.setNativeProps({enabled: false})}
                onPressOut={() => tapRef.current?.setNativeProps({enabled: true})}
              >
                <Ionicons 
                  name={isLiked ? "heart" : "heart-outline"} 
                  size={20} 
                  color={isLiked ? "#FF3040" : "#666"} 
                />
              </TouchableOpacity>
              <TouchableOpacity 
                style={styles.reactsCounter}
                onPress={handleReactsPress}
                hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                onPressIn={() => tapRef.current?.setNativeProps({enabled: false})}
                onPressOut={() => tapRef.current?.setNativeProps({enabled: true})}
              >
                <Text style={styles.reactsText}>{displayLikes}</Text>
              </TouchableOpacity>
            </View>
            <TouchableOpacity 
              ref={menuButtonRef}
              style={styles.menuButton}
              onPress={handleMenuPress}
              hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
              onPressIn={() => tapRef.current?.setNativeProps({enabled: false})}
              onPressOut={() => tapRef.current?.setNativeProps({enabled: true})}
            >
              <Ionicons name="ellipsis-vertical" size={18} color="#666" />
            </TouchableOpacity>
          </View>
        </View>
        <Text style={styles.timeText}>{displayTimeAgo}</Text>
        <View style={styles.locationRow}>
          <Ionicons name="location-outline" size={14} color="#777" style={{ marginRight: 4}} />
          <Text style={styles.locationText}>{location}</Text>
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
              <Text style={styles.pillText}>{postType.charAt(0).toUpperCase() + postType.slice(1).toLowerCase()}</Text>
            </View>
          )}
        </View>
      </View>

      {/* Modern Dropdown Menu */}
      <Modal
        visible={showMenu}
        transparent={true}
        animationType="none"
        onRequestClose={hideMenu}
      >
        <TouchableOpacity 
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={hideMenu}
        >
          <Animated.View 
            style={[
              styles.dropdownMenu,
              {
                opacity: fadeAnim,
                transform: [{
                  translateY: fadeAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [-10, 0],
                  }),
                }],
                left: menuPosition.x,
                top: menuPosition.y,
              }
            ]}
          >
            <TouchableOpacity 
              style={styles.menuOption}
              onPress={() => handleInterestSelect('interested')}
            >
              <Ionicons name="heart" size={18} color="#4CAF50" />
              <Text style={[styles.menuText, { color: '#4CAF50' }]}>Interested</Text>
            </TouchableOpacity>
            
            <View style={styles.menuSeparator} />
            
            <TouchableOpacity 
              style={styles.menuOption}
              onPress={() => handleInterestSelect('not')}
            >
              <Ionicons name="heart-dislike" size={18} color="#FF5722" />
              <Text style={[styles.menuText, { color: '#FF5722' }]}>Not Interested</Text>
            </TouchableOpacity>
          </Animated.View>
        </TouchableOpacity>
      </Modal>
    </View>
  );

  if (showAdvancedFeatures) {
    return (
      <View style={styles.swipeContainer}>
        <TapGestureHandler
          ref={tapRef}
          onHandlerStateChange={onTapHandlerStateChange}
          shouldCancelWhenOutside={true}
          simultaneousHandlers={panRef}
        >
          <PanGestureHandler
            ref={panRef}
            onGestureEvent={onGestureEvent}
            onHandlerStateChange={onHandlerStateChange}
            activeOffsetX={[-20, 20]} // Require 20px movement to activate
            failOffsetY={[-50, 50]} // Fail if vertical movement is too large
            simultaneousHandlers={tapRef}
          >
            <Animated.View
              style={[
                styles.animatedContainer,
                {
                  transform: [{ translateX }],
                },
              ]}
            >
              {CardContent}
            </Animated.View>
          </PanGestureHandler>
        </TapGestureHandler>
        
        {/* Delete Button */}
        <TouchableOpacity 
          style={styles.deleteButton}
          onPress={handleDeletePress}
          activeOpacity={0.7}
        >
          <Ionicons name="trash-outline" size={20} color="#fff" />
          <Text style={styles.deleteText}>Delete</Text>
        </TouchableOpacity>
      </View>
    );
  }

  // When advanced features are disabled, use regular TouchableOpacity
  return (
    <TouchableOpacity 
      onPress={handleCardPress}
      activeOpacity={0.7}
    >
      {CardContent}
    </TouchableOpacity>
  );
};

const CARD_WIDTH = width;
const IMAGE_WIDTH = CARD_WIDTH * 0.3;
const DETAILS_WIDTH = CARD_WIDTH * 0.7;

const styles = StyleSheet.create({
  swipeContainer: {
    position: 'relative',
    overflow: 'hidden',
  },
  animatedContainer: {
    zIndex: 1,
  },
  container: {
    flexDirection: 'row',
    width: "auto",
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
    width: IMAGE_WIDTH,
    height: IMAGE_WIDTH,
    borderRadius: 8,
  },
  details: {
    width: DETAILS_WIDTH - 16,
    marginLeft: 8,
  },
  headerRow: {
    maxWidth: "80%",
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent:"space-between",
  },
  nameText: {
    fontSize: 16,
    fontWeight: '500',
    color: '#333',
  },
  genderIcon: {
    marginLeft: 8,
  },
  interestIndicator: {
    marginLeft: 6,
  },
  actionsContainer: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8,
  },
  likeSection: {
    flexDirection: 'column',
    alignItems: 'center',
    gap: 4,
  },
  likeButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#f8f8f8',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  menuButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#f8f8f8',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
    marginTop: 2,
  },
  reactsCounter: {
    padding: 4,
    backgroundColor: '#f0f0f0',
    borderRadius: 12,
    minWidth: 30,
    alignItems: 'center',
  },
  reactsText: {
    fontSize: 12,
    color: '#666',
    fontWeight: '500',
  },
  timeText: {
    fontSize: 12,
    color: '#888',
    marginTop: 2,
  },
  locationRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 3,
  },
  locationText: {
    fontSize: 12,
    color: '#555',
  },
  pillsRow: {
    flexDirection: 'row',
    marginTop: 6,
    flexWrap: 'wrap',
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
    backgroundColor: '#9188E5',
  },
  pillText: {
    fontSize: 12,
    color: 'white',
  },
  deleteButton: {
    position: 'absolute',
    right: 0,
    top: 0,
    bottom: 0,
    width: 80,
    borderTopRightRadius: 12,
    borderBottomRightRadius: 12,
    backgroundColor: '#FF3040',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 0,
  },
  deleteText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '500',
    marginTop: 2,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  dropdownMenu: {
    position: 'absolute',
    backgroundColor: 'white',
    borderRadius: 12,
    paddingVertical: 8,
    minWidth: 140,
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 8,
  },
  menuOption: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  menuText: {
    fontSize: 14,
    fontWeight: '500',
    marginLeft: 12,
  },
  menuSeparator: {
    height: 1,
    backgroundColor: '#f0f0f0',
    marginHorizontal: 12,
  },
});