import React, { useState, useEffect } from 'react'
import { View, Text, StyleSheet, Dimensions, Platform, ScrollView, TouchableOpacity, Image } from 'react-native'
import { MaterialIcons, MaterialCommunityIcons } from '@expo/vector-icons'

const { width, height } = Dimensions.get('window')

// Responsive breakpoints
const isSmallScreen = width < 350
const isMediumScreen = width >= 350 && width < 450
const isLargeScreen = width >= 450

// Responsive scaling functions
const scaleFont = (size) => {
  if (isSmallScreen) return size * 0.8
  if (isMediumScreen) return size * 0.9
  return size
}

const scalePadding = (size) => {
  if (isSmallScreen) return size * 0.7
  if (isMediumScreen) return size * 0.85
  return size
}

const scaleSize = (size) => {
  if (isSmallScreen) return size * 0.8
  if (isMediumScreen) return size * 0.9
  return size
}

// Sample posts data with photos
const allPosts = [
  {
    id: 1,
    author: "luna_pawsome",
    time: "2h",
    content: "Perfect day at the park! 🌳 Can't get enough of this sunshine ☀️",
    likes: 1247,
    comments: 89,
    isLiked: false,
    location: "Central Park",
    avatar: "https://images.unsplash.com/photo-1517849845537-4d257902454a?w=100&h=100&fit=crop&crop=face",
    image: "https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=400&h=400&fit=crop"
  },
  {
    id: 2,
    author: "whiskers_daily",
    time: "4h",
    content: "My favorite spot by the window 🪟 The birds better watch out! 😼",
    likes: 892,
    comments: 45,
    isLiked: true,
    location: "Home Sweet Home",
    avatar: "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=100&h=100&fit=crop&crop=face",
    image: "https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=400&h=400&fit=crop"
  },
  {
    id: 3,
    author: "golden_adventures",
    time: "6h",
    content: "Beach day with my humans! 🏖️ Sand between my paws never felt so good",
    likes: 2156,
    comments: 178,
    isLiked: false,
    location: "Sunset Beach",
    avatar: "https://images.unsplash.com/photo-1552053831-71594a27632d?w=100&h=100&fit=crop&crop=face",
    image: "https://images.unsplash.com/photo-1551717743-49959800b1f6?w=400&h=400&fit=crop"
  },
  {
    id: 4,
    author: "fluffy_mcwhiskers",
    time: "8h",
    content: "Caught a sunbeam and I'm never letting it go ✨ This is my life now",
    likes: 743,
    comments: 62,
    isLiked: true,
    location: "Living Room Kingdom",
    avatar: "https://images.unsplash.com/photo-1596854407944-bf87f6fdd49e?w=100&h=100&fit=crop&crop=face",
    image: "https://images.unsplash.com/photo-1571566882372-1598d88abd90?w=400&h=400&fit=crop"
  },
  {
    id: 5,
    author: "zoomie_champion",
    time: "12h",
    content: "3 AM energy is unmatched! 🏃‍♂️💨 Sorry humans, but the zoomies wait for no one",
    likes: 1534,
    comments: 203,
    isLiked: false,
    location: "The Living Room Track",
    avatar: "https://images.unsplash.com/photo-1583337130417-3346a1be7dee?w=100&h=100&fit=crop&crop=face",
    image: "https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=400&h=400&fit=crop"
  },
  {
    id: 6,
    author: "purrfect_life_cat",
    time: "1d",
    content: "Knocked exactly 4 items off the counter today ✅ Mission accomplished",
    likes: 978,
    comments: 134,
    isLiked: true,
    location: "Kitchen Counter",
    avatar: "https://images.unsplash.com/photo-1606214174585-fe31582cd532?w=100&h=100&fit=crop&crop=face",
    image: "https://images.unsplash.com/photo-1561948955-570b270e7c36?w=400&h=400&fit=crop"
  },
  {
    id: 7,
    author: "mountain_dog_adventures",
    time: "1d",
    content: "Hiking with my pack 🥾 The view is nice but these smells are incredible!",
    likes: 1876,
    comments: 156,
    isLiked: false,
    location: "Rocky Mountain Trail",
    avatar: "https://images.unsplash.com/photo-1588943211346-0908a1fb0b01?w=100&h=100&fit=crop&crop=face",
    image: "https://images.unsplash.com/photo-1605568427561-40dd23c2acea?w=400&h=400&fit=crop"
  },
  {
    id: 8,
    author: "sleepy_whiskers",
    time: "2d",
    content: "Achieved 18 hours of sleep today 😴 New personal record! Training starts tomorrow",
    likes: 2341,
    comments: 267,
    isLiked: true,
    location: "The Ultimate Nap Spot",
    avatar: "https://images.unsplash.com/photo-1472491235688-bdc81a63246e?w=100&h=100&fit=crop&crop=face",
    image: "https://images.unsplash.com/photo-1574144611937-0df059b5ef3e?w=400&h=400&fit=crop"
  }
]

// Function to get random posts, but always with luna_pawsome first and golden_adventures second
const getRandomPosts = () => {
  // Find the required posts
  const luna = allPosts.find(p => p.author === "luna_pawsome")
  const golden = allPosts.find(p => p.author === "golden_adventures")
  // Filter out those two from the rest
  const rest = allPosts.filter(p => p.author !== "luna_pawsome" && p.author !== "golden_adventures")
  // Shuffle the rest
  const shuffledRest = rest.sort(() => 0.5 - Math.random())
  // Pick a random number of additional posts (1-4)
  const numExtra = Math.floor(Math.random() * 4) + 1
  // Compose the final array
  return [luna, golden, ...shuffledRest.slice(0, numExtra)].filter(Boolean)
}

const PostCard = ({ post, onLike }) => (
  <View style={styles.postCard}>
    {/* Header */}
    <View style={styles.postHeader}>
      <Image source={{ uri: post.avatar }} style={styles.profilePic} />
      <View style={styles.userInfo}>
        <Text style={styles.username}>{post.author}</Text>
        {post.location && <Text style={styles.location}>{post.location}</Text>}
      </View>
      <TouchableOpacity style={styles.moreButton}>
        <MaterialIcons name="more-horiz" size={scaleSize(24)} color="#262626" />
      </TouchableOpacity>
    </View>
    
    {/* Image */}
    <Image source={{ uri: post.image }} style={styles.postImage} />
    
    {/* Action buttons */}
    <View style={styles.actionButtons}>
      <View style={styles.leftActions}>
        <TouchableOpacity onPress={() => onLike(post.id)} style={styles.actionButton}>
          <MaterialIcons 
            name={post.isLiked ? "favorite" : "favorite-border"} 
            size={scaleSize(28)} 
            color={post.isLiked ? "#ed4956" : "#262626"} 
          />
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionButton}>
          <MaterialCommunityIcons name="comment-outline" size={scaleSize(28)} color="#262626" />
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionButton}>
          <MaterialCommunityIcons name="send-outline" size={scaleSize(28)} color="#262626" />
        </TouchableOpacity>
      </View>
      <TouchableOpacity style={styles.bookmarkButton}>
        <MaterialCommunityIcons name="bookmark-outline" size={scaleSize(28)} color="#262626" />
      </TouchableOpacity>
    </View>
    
    {/* Likes count */}
    <TouchableOpacity style={styles.likesContainer}>
      <Text style={styles.likesText}>{post.likes.toLocaleString()} likes</Text>
    </TouchableOpacity>
    
    {/* Caption */}
    <View style={styles.captionContainer}>
      <Text style={styles.caption}>
        <Text style={styles.username}>{post.author}</Text>
        <Text style={styles.captionText}> {post.content}</Text>
      </Text>
    </View>
    
    {/* Comments */}
    {post.comments > 0 && (
      <TouchableOpacity style={styles.commentsButton}>
        <Text style={styles.commentsText}>
          View all {post.comments} comments
        </Text>
      </TouchableOpacity>
    )}
    
    {/* Time */}
    <Text style={styles.timeText}>{post.time} ago</Text>
  </View>
)

export default function index() {
  const [posts, setPosts] = useState([])

  useEffect(() => {
    setPosts(getRandomPosts())
  }, [])

  const refreshPosts = () => {
    setPosts(getRandomPosts())
  }

  const handleLike = (postId) => {
    setPosts(prevPosts => 
      prevPosts.map(post => {
        if (post.id === postId) {
          return {
            ...post,
            isLiked: !post.isLiked,
            likes: post.isLiked ? post.likes - 1 : post.likes + 1
          }
        }
        return post
      })
    )
  }

  return (
    <View style={styles.container}>
      {/* Posts Feed */}
      <ScrollView 
        style={styles.feedContainer}
        contentContainerStyle={styles.feedContent}
        showsVerticalScrollIndicator={false}
      >
        {posts.map((post) => (
          <PostCard key={post.id} post={post} onLike={handleLike} />
        ))}
      </ScrollView>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    paddingTop: Platform.OS === 'ios' ? 50 : 30,
    paddingBottom: scalePadding(10),
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#dbdbdb',
  },
  headerContent: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: scalePadding(16),
  },
  headerTitle: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    justifyContent: 'center',
  },
  appTitle: {
    fontSize: scaleFont(24),
    fontWeight: 'bold',
    color: '#262626',
    marginLeft: scalePadding(8),
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  headerButton: {
    marginLeft: scalePadding(16),
  },
  storiesContainer: {
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#dbdbdb',
    paddingVertical: scalePadding(10),
  },
  storiesScroll: {
    paddingHorizontal: scalePadding(8),
  },
  storyItem: {
    alignItems: 'center',
    marginHorizontal: scalePadding(8),
    width: scaleSize(70),
  },
  storyImageContainer: {
    width: scaleSize(56),
    height: scaleSize(56),
    borderRadius: scaleSize(28),
    borderWidth: 2,
    borderColor: '#9188E5',
    padding: 2,
  },
  storyImage: {
    width: '100%',
    height: '100%',
    borderRadius: scaleSize(26),
  },
  storyUsername: {
    fontSize: scaleFont(10),
    color: '#262626',
    marginTop: scalePadding(4),
    textAlign: 'center',
  },
  feedContainer: {
    flex: 1,
  },
  feedContent: {
    paddingBottom: scalePadding(20),
  },
  postCard: {
    backgroundColor: '#fff',
    marginBottom: scalePadding(0),
  },
  postHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: scalePadding(16),
    paddingVertical: scalePadding(12),
  },
  profilePic: {
    width: scaleSize(32),
    height: scaleSize(32),
    borderRadius: scaleSize(16),
    marginRight: scalePadding(12),
  },
  userInfo: {
    flex: 1,
  },
  username: {
    fontSize: scaleFont(14),
    fontWeight: '600',
    color: '#262626',
  },
  location: {
    fontSize: scaleFont(11),
    color: '#8e8e8e',
    marginTop: 1,
  },
  moreButton: {
    padding: scalePadding(8),
  },
  postImage: {
    width: width,
    height: width,
    resizeMode: 'cover',
  },
  actionButtons: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: scalePadding(16),
    paddingTop: scalePadding(12),
  },
  leftActions: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  actionButton: {
    marginRight: scalePadding(16),
  },
  bookmarkButton: {
    marginLeft: 'auto',
  },
  likesContainer: {
    paddingHorizontal: scalePadding(16),
    paddingTop: scalePadding(8),
  },
  likesText: {
    fontSize: scaleFont(14),
    fontWeight: '600',
    color: '#262626',
  },
  captionContainer: {
    paddingHorizontal: scalePadding(16),
    paddingTop: scalePadding(8),
  },
  caption: {
    fontSize: scaleFont(14),
    lineHeight: scaleFont(18),
  },
  captionText: {
    color: '#262626',
  },
  commentsButton: {
    paddingHorizontal: scalePadding(16),
    paddingTop: scalePadding(4),
  },
  commentsText: {
    fontSize: scaleFont(14),
    color: '#8e8e8e',
  },
  timeText: {
    fontSize: scaleFont(10),
    color: '#8e8e8e',
    paddingHorizontal: scalePadding(16),
    paddingTop: scalePadding(8),
    paddingBottom: scalePadding(16),
    textTransform: 'uppercase',
  },
})