import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Image,
  Dimensions,
  Animated,
} from 'react-native';

const { width, height } = Dimensions.get('window');

const LandingImages = ({ 
  images = [
    'https://media.istockphoto.com/id/1367150296/photo/happy-young-african-american-man-petting-his-dog-outdoors-in-nature.jpg?s=612x612&w=0&k=20&c=HZT5V05AdmWbcUjeoYcJypF_20VYII8vv6iXxb2gJCg=',
    'https://emmenetonchien.com/wp-content/uploads/2025/04/Premieres-vacances-avec-son-chiot-_-que-prevoir.jpg',
    'https://9prints-bucket-data-sync-efs.s3.us-east-2.amazonaws.com/8/storage/editor/640x430.353.EEYJN60ff75b9368f7.jpg'
  ],
  text = "Ready to find your new friend?",
  intervalTime = 3000 
}) => {
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [fadeAnim] = useState(new Animated.Value(1));

  useEffect(() => {
    const interval = setInterval(() => {
      // Start fade out
      Animated.timing(fadeAnim, {
        toValue: 0,
        duration: 300,
        useNativeDriver: true,
      }).start(() => {
        // Change image
        setCurrentImageIndex((prevIndex) => 
          (prevIndex + 1) % images.length
        );
        
        // Fade in
        Animated.timing(fadeAnim, {
          toValue: 1,
          duration: 300,
          useNativeDriver: true,
        }).start();
      });
    }, intervalTime);

    return () => clearInterval(interval);
  }, [images.length, intervalTime, fadeAnim]);

  return (
    <View style={styles.container}>
      <Animated.Image
        source={{ uri: images[currentImageIndex] }}
        style={[styles.image, { opacity: fadeAnim }]}
        resizeMode="cover"
      />
      
      {/* Dark overlay for better text readability */}
      <View style={styles.overlay} />
      
      {/* Text overlay */}
      <View style={styles.textContainer}>
        <Text style={styles.mainText}>{text}</Text>
      </View>
      
      {/* Image indicators */}
      <View style={styles.indicatorContainer}>
        {images.map((_, index) => (
          <View
            key={index}
            style={[
              styles.indicator,
              currentImageIndex === index && styles.activeIndicator
            ]}
          />
        ))}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: width,
    height: height * 0.3, // Adjust height as needed
    position: 'relative',
    justifyContent: 'center',
    alignItems: 'center',
  },
  image: {
    width: '100%',
    height: '100%',
    position: 'absolute',
    top: 0,
    left: 0,
  },
  overlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.4)', // Semi-transparent dark overlay
  },
  textContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'flex-start', // Align to left
    paddingHorizontal: 180, // More padding for better positioning
    paddingLeft: 15, // Extra left padding
    zIndex: 2,
  },
  mainText: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#ffffff',
    textAlign: 'left', // Left align text
    textShadowColor: 'rgba(0, 0, 0, 0.8)',
    textShadowOffset: { width: 2, height: 2 },
    textShadowRadius: 4,
    lineHeight: 44,
    maxWidth: '70%', // Limit width to left portion of screen
  },
  indicatorContainer: {
    position: 'absolute',
    bottom: 20,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 3,
  },
  indicator: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: 'rgba(255, 255, 255, 0.5)',
    marginHorizontal: 4,
  },
  activeIndicator: {
    backgroundColor: '#ffffff',
    width: 10,
    height: 10,
    borderRadius: 5,
  },
});

export default LandingImages;