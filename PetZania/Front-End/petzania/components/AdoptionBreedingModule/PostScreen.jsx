import React, { useState, useRef } from 'react';
import { 
  View, 
  Text,
  StyleSheet, 
  ScrollView,
  Dimensions,
  TouchableOpacity,
  Animated,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import LandingImages from '@/components/AdoptionBreedingModule/LandingImages';
import PostsList from '@/components/AdoptionBreedingModule/PostsList'; // Changed from usePosts to PostsList
const { height: screenHeight } = Dimensions.get('window');
const queryClient = new QueryClient();

export default function PostScreen({postType = ""}) {
  const scrollY = useRef(new Animated.Value(0)).current;
  const [showStickyHeader, setShowStickyHeader] = useState(false);
  
  const LANDING_HEIGHT = screenHeight * 0.4;
  const STICKY_THRESHOLD = LANDING_HEIGHT - 100; // Show sticky header 100px before landing disappears

  const handleScroll = Animated.event(
    [{ nativeEvent: { contentOffset: { y: scrollY } } }],
    { 
      useNativeDriver: false,
      listener: (event) => {
        const offsetY = event.nativeEvent.contentOffset.y;
        setShowStickyHeader(offsetY > STICKY_THRESHOLD);
      }
    }
  );

  // Mock data - replace with your actual data fetching logic
  const [filters, setFilters] = useState({});

  const filteredCount = 42; // Replace with actual count
  const totalCount = 100; // Replace with actual count

  const openFilter = () => {
    setTempFilters({ ...filters });
    setModalVisible(true);
  };

  const StickyHeader = () => (
    <Animated.View 
      style={[
        styles.stickyHeaderContainer,
        {
          opacity: showStickyHeader ? 1 : 0,
          transform: [{
            translateY: showStickyHeader ? 0 : -50
          }]
        }
      ]}
    >
      <View style={styles.stickyHeaderContent}>
        <View>
          <Text style={styles.stickyTitle}>Posts</Text>
          <Text style={styles.stickySubtitle}>
            Showing {filteredCount} of {totalCount} pets
          </Text>
        </View>
        <TouchableOpacity
          style={styles.stickyFilterButton}
          onPress={openFilter}
          activeOpacity={0.7}
        >
          <Ionicons name="filter-outline" size={20} color="#9188E5" />
          <Text style={styles.filterText}>Filter by</Text>
        </TouchableOpacity>
      </View>
    </Animated.View>
  );

  return (
    <QueryClientProvider client={queryClient}>
      <View style={styles.container}>
        {/* Sticky Header - positioned absolutely */}
        <StickyHeader />
        
        {/* Main Scroll Content */}
        <Animated.ScrollView
          style={styles.scrollView}
          showsVerticalScrollIndicator={false}
          bounces={true}
          scrollEventThrottle={16}
          onScroll={handleScroll}
        >
          {/* Landing Images Section */}
          <View style={styles.landingContainer}>
            <LandingImages />
          </View>

          {/* Posts Section */}
          <View style={styles.postsContainer}>
            <PostsList showHeader={!showStickyHeader} postType={postType}/>
          </View>
        </Animated.ScrollView>
      </View>
    </QueryClientProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9f9f9',
  },
  scrollView: {
    flex: 1,
  },
  landingContainer: {
    height: screenHeight * 0.4,
  },
  postsContainer: {
    flex: 1,
    minHeight: screenHeight,
    backgroundColor: '#f9f9f9',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    marginTop: -60, // Changed from -20 to -10 (half the original gap)
    paddingTop: 10, // Changed from 20 to 10 (half the original padding)
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: -2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 5,
  },
  stickyHeaderContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 1000,
    backgroundColor: 'rgba(249, 249, 249, 0.95)',
    backdropFilter: 'blur(10px)',
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(238, 238, 238, 0.8)',
    paddingTop: 50, // Account for status bar
    paddingBottom: 12,
  },
  stickyHeaderContent: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
  },
  stickyTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: '#333',
  },
  stickySubtitle: {
    fontSize: 12,
    color: '#666',
    marginTop: 1,
  },
  stickyFilterButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 16,
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
    fontSize: 12,
    color: '#555',
  },
});