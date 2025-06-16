import React from 'react';
import { View, Text, Image, StyleSheet, Dimensions, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

const { width } = Dimensions.get('window');

// Helper function to calculate time ago from createdAt
const getTimeAgo = (createdAt) => {
  const now = new Date();
  const created = new Date(createdAt);
  const diffInMs = now - created;
  const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
  const diffInDays = Math.floor(diffInHours / 24);
  
  if (diffInHours < 1) return 'Just now';
  if (diffInHours < 24) return `${diffInHours} hours ago`;
  if (diffInDays < 7) return `${diffInDays} days ago`;
  return `${Math.floor(diffInDays / 7)} weeks ago`;
};

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
  onReactsPress, // New prop for handling reacts counter press
}) {
  // Extract data from petDTO
  const petName = petDTO?.name || 'Pet Name';
  const petGender = petDTO?.gender?.toLowerCase() || 'male';
  const petBreed = petDTO?.breed || 'Breed';
  const petAge = petDTO?.age || '2 years';
  const petImage = petDTO?.myPicturesURLs?.[0] || 'https://hips.hearstapps.com/hmg-prod/images/small-fluffy-dog-breeds-maltipoo-66300ad363389.jpg?crop=0.668xw:1.00xh;0.151xw,0&resize=640:*';
  const displayTimeAgo = createdAt ? getTimeAgo(createdAt) : '2 hours ago';
  const displayLikes = likes || reacts || 0;

  const handleCardPress = () => {
    if (onPress) {
      onPress(postId);
    }
  };

  const handleLikePress = (event) => {
    // Prevent the card press event from firing when like button is pressed
    event.stopPropagation();
    if (onLike) {
      onLike();
    }
  };

  const handleReactsPress = (event) => {
    // Prevent the card press event from firing when reacts counter is pressed
    event.stopPropagation();
    if (onReactsPress) {
      onReactsPress(postId, reactedUsersIds);
    }
  };

  return (
    <TouchableOpacity 
      style={styles.container} 
      onPress={handleCardPress}
      activeOpacity={0.7}
    >
      <Image source={{ uri: petImage }} style={styles.image} />
      <View style={styles.details}>
        <View style={styles.headerRow}> 
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Text style={styles.nameText}>{petName}</Text>
            <Ionicons 
              name={petGender === 'female' ? 'female' : 'male'} 
              size={16} 
              color={petGender === 'male' ? 'blue' : 'pink'} 
              style={styles.genderIcon} 
            />
          </View>
          <View style={styles.actionsContainer}>
            <TouchableOpacity 
              style={styles.likeButton}
              onPress={handleLikePress}
              hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
            >
              <Ionicons 
                name={isLiked ? "heart" : "heart-outline"} 
                size={24} 
                color={isLiked ? "#FF3040" : "black"} 
              />
            </TouchableOpacity>
            <TouchableOpacity 
              style={styles.reactsCounter}
              onPress={handleReactsPress}
              hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
            >
              <Text style={styles.reactsText}>{displayLikes}</Text>
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
        </View>
      </View>
    </TouchableOpacity>
  );
};

const CARD_WIDTH = width;
const IMAGE_WIDTH = CARD_WIDTH * 0.3;
const DETAILS_WIDTH = CARD_WIDTH * 0.7;

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    width: "auto",
    backgroundColor: '#fff',
    borderRadius: 12,
    borderColor: '#ddd',
    borderWidth: 1,
    overflow: 'hidden',
    elevation: 3,  // Android shadow
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.2,
    shadowRadius: 2,
    padding: 8,
    alignItems: 'center',
    gap: 8, // space between image and details
  },
  image: {
    width: IMAGE_WIDTH,
    height: IMAGE_WIDTH,
    borderRadius: 8,
  },
  details: {
    width: DETAILS_WIDTH - 16, // account for padding
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
  actionsContainer: {
    flexDirection: 'column',
    alignItems: 'center',
    gap: 4,
  },
  likeButton: {
    padding: 4,
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
    marginTop: 4,
  },
  locationRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 6,
  },
  locationText: {
    fontSize: 12,
    color: '#555',
  },
  pillsRow: {
    flexDirection: 'row',
    marginTop: 8,
  },
  pill: {
    backgroundColor: '#9188E5',
    borderRadius: 16,
    paddingVertical: 4,
    paddingHorizontal: 12,
    marginRight: 8,
  },
  pillText: {
    fontSize: 12,
    color: 'white',
  },
});