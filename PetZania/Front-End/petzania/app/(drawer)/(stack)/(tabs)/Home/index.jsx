import React from 'react'
import { View, Text, StyleSheet, Dimensions, Platform } from 'react-native'
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

export default function index() {
  return (
    <View style={styles.container}>
      {/* Background decoration with paws */}
      <View style={styles.backgroundPaw1}>
        <MaterialCommunityIcons 
          name="paw" 
          size={scaleSize(60)} 
          color="#9188E5" 
          style={styles.pawIcon}
        />
      </View>
      <View style={styles.backgroundPaw2}>
        <MaterialCommunityIcons 
          name="paw" 
          size={scaleSize(45)} 
          color="#9188E5" 
          style={styles.pawIcon}
        />
      </View>
      <View style={styles.backgroundPaw3}>
        <MaterialCommunityIcons 
          name="paw" 
          size={scaleSize(35)} 
          color="#9188E5" 
          style={styles.pawIcon}
        />
      </View>
      <View style={styles.backgroundPaw4}>
        <MaterialCommunityIcons 
          name="paw" 
          size={scaleSize(50)} 
          color="#9188E5" 
          style={styles.pawIcon}
        />
      </View>
      
      {/* Main content */}
      <View style={styles.content}>
        {/* Icon/Symbol */}
        <View style={styles.iconContainer}>
          <MaterialIcons 
            name="construction" 
            size={scaleFont(40)} 
            color="#fff" 
          />
        </View>
        
        {/* Main title */}
        <Text style={styles.title}>Under Development</Text>
        
        {/* Subtitle */}
        <Text style={styles.subtitle}>
          We're working hard to bring you something amazing
        </Text>
        
        {/* Progress indicator */}
        <View style={styles.progressContainer}>
          <View style={styles.progressBar}>
            <View style={styles.progressFill} />
          </View>
          <Text style={styles.progressText}>Coming Soon</Text>
        </View>
        
        {/* Status message */}
        <Text style={styles.statusText}>
          Stay tuned for updates!
        </Text>
      </View>
      
      {/* Footer */}
      <View style={styles.footer}>
        <Text style={styles.footerText}>Thank you for your patience</Text>
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9ff',
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
  },
  backgroundPaw1: {
    position: 'absolute',
    top: height * 0.15,
    right: width * 0.1,
    opacity: 0.15,
    transform: [{ rotate: '15deg' }],
  },
  backgroundPaw2: {
    position: 'absolute',
    bottom: height * 0.25,
    left: width * 0.08,
    opacity: 0.12,
    transform: [{ rotate: '-20deg' }],
  },
  backgroundPaw3: {
    position: 'absolute',
    top: height * 0.35,
    left: width * 0.15,
    opacity: 0.1,
    transform: [{ rotate: '45deg' }],
  },
  backgroundPaw4: {
    position: 'absolute',
    bottom: height * 0.15,
    right: width * 0.12,
    opacity: 0.13,
    transform: [{ rotate: '-10deg' }],
  },
  pawIcon: {
    opacity: 1,
  },
  content: {
    alignItems: 'center',
    paddingHorizontal: scalePadding(40),
    zIndex: 1,
    maxWidth: width * 0.9,
  },
  iconContainer: {
    marginBottom: scalePadding(30),
    padding: scalePadding(20),
    backgroundColor: '#9188E5',
    borderRadius: scaleSize(50),
    shadowColor: '#9188E5',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.3,
    shadowRadius: 10,
    elevation: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  title: {
    fontSize: scaleFont(28),
    fontWeight: 'bold',
    color: '#9188E5',
    marginBottom: scalePadding(15),
    textAlign: 'center',
  },
  subtitle: {
    fontSize: scaleFont(16),
    color: '#666',
    textAlign: 'center',
    marginBottom: scalePadding(40),
    lineHeight: scaleFont(24),
    maxWidth: width * 0.8,
  },
  progressContainer: {
    alignItems: 'center',
    marginBottom: scalePadding(30),
  },
  progressBar: {
    width: Math.min(200, width * 0.6),
    height: scaleSize(6),
    backgroundColor: '#e0e0e0',
    borderRadius: scaleSize(3),
    overflow: 'hidden',
    marginBottom: scalePadding(10),
  },
  progressFill: {
    width: '65%',
    height: '100%',
    backgroundColor: '#9188E5',
    borderRadius: scaleSize(3),
  },
  progressText: {
    fontSize: scaleFont(14),
    color: '#9188E5',
    fontWeight: '600',
  },
  statusText: {
    fontSize: scaleFont(14),
    color: '#888',
    textAlign: 'center',
    fontStyle: 'italic',
    maxWidth: width * 0.7,
  },
  footer: {
    position: 'absolute',
    bottom: Platform.OS === 'ios' ? 50 : 30,
    alignItems: 'center',
    paddingHorizontal: scalePadding(20),
  },
  footerText: {
    fontSize: scaleFont(12),
    color: '#aaa',
    textAlign: 'center',
  },
})