import React, { useRef, useState, useCallback } from 'react';
import {
  View,
  Animated,
  Dimensions,
  TouchableOpacity,
  Text,
  StyleSheet,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';

import LandingImages from '@/components/AdoptionBreedingModule/LandingImages';
import PostsList from '@/components/AdoptionBreedingModule/PostsList';

const { height: screenHeight } = Dimensions.get('window');
const LANDING_HEIGHT = screenHeight * 0.4;
const STICKY_THRESHOLD = LANDING_HEIGHT - 100;

const StickyHeader = ({ visible, postType, onFilterPress, scrollY }) => {
  const opacity = scrollY.interpolate({
    inputRange: [STICKY_THRESHOLD - 50, STICKY_THRESHOLD],
    outputRange: [0, 1],
    extrapolate: 'clamp',
  });

  const translateY = scrollY.interpolate({
    inputRange: [STICKY_THRESHOLD - 50, STICKY_THRESHOLD],
    outputRange: [-50, 0],
    extrapolate: 'clamp',
  });

  const getPostTypeTitle = () => {
    const titles = {
      'ADOPTION': 'Pets for Adoption',
      'BREEDING': 'Pets for Breeding',
    };
    return titles[postType] || 'Posts';
  };

  return (
    <Animated.View
      style={[
        styles.stickyHeaderContainer,
        {
          opacity,
          transform: [{ translateY }],
          pointerEvents: visible ? 'auto' : 'none',
        },
      ]}
    >
      <View style={styles.stickyHeaderContent}>
        <View>
          <Text style={styles.stickyTitle}>{getPostTypeTitle()}</Text>
        </View>
        <TouchableOpacity
          style={styles.stickyFilterButton}
          onPress={onFilterPress}
          activeOpacity={0.7}
        >
          <Ionicons name="filter" size={18} color="#9188E5" />
          <Text style={styles.filterText}>Filters</Text>
        </TouchableOpacity>
      </View>
    </Animated.View>
  );
};

// Main PostScreen component
export default function PostScreen({ postType = 'ADOPTION' }) {
  const postsListRef = useRef(null);
  const scrollY = useRef(new Animated.Value(0)).current;
  const [showStickyHeader, setShowStickyHeader] = useState(false);

  const handleScroll = Animated.event(
    [{ nativeEvent: { contentOffset: { y: scrollY } } }],
    {
      useNativeDriver: false,
      listener: (event) => {
        const offsetY = event.nativeEvent.contentOffset.y;
        const shouldShow = offsetY > STICKY_THRESHOLD;

        setShowStickyHeader(prev => {
          if (prev !== shouldShow) return shouldShow;
          return prev;
        });
      },
    }
  );

  // Filter button in sticky header opens PostsList's modal
  const handleFilterPress = useCallback(() => {
    postsListRef.current?.openFilter?.();
  }, []);


  return (
    <View style={styles.container}>
      <StickyHeader
        visible={showStickyHeader}
        postType={postType}
        onFilterPress={handleFilterPress}
        scrollY={scrollY}
      />

      <Animated.ScrollView
        style={styles.scrollView}
        showsVerticalScrollIndicator={false}
        bounces={true}
        scrollEventThrottle={16}
        onScroll={handleScroll}
      >
        <View style={styles.landingContainer}>
          <LandingImages postType={postType} />
        </View>

        <View style={styles.postsContainer}>
          <PostsList
            ref={postsListRef}
            showHeader={!showStickyHeader}
            postType={postType}
          />
        </View>
      </Animated.ScrollView>
    </View>
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
    height: LANDING_HEIGHT,
    backgroundColor: '#f9f9f9',
  },
  postsContainer: {
    flex: 1,
    minHeight: screenHeight,
    backgroundColor: '#f9f9f9',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    marginTop: -60,
    paddingTop: 10,
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
    backgroundColor: 'rgba(249, 249, 249, 0.98)',
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(238, 238, 238, 0.8)',
    paddingTop: 50,
    paddingBottom: 12,
    elevation: 8,
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
  stickyFilterButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 20,
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
    marginLeft: 6,
    fontSize: 14,
    color: '#9188E5',
    fontWeight: '500',
  },
});