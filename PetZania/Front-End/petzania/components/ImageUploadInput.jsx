import React, { useState, useEffect } from 'react';
import { StyleSheet, View, TouchableOpacity, ActivityIndicator } from 'react-native';
import { Image } from 'expo-image';
import { MaterialIcons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { useActionSheet } from '@expo/react-native-action-sheet';

const ImageUploadInput = ({ 
  defaultImage, 
  onImageChange, 
  size = 180, 
  isUploading = false,
  allowsMultipleSelection = false,
  currentImage = null,
  style
}) => {
  const [image, setImage] = useState(currentImage);
  const { showActionSheetWithOptions } = useActionSheet();

  // Sync external currentImage with internal state
  useEffect(() => {
    setImage(currentImage);
  }, [currentImage]);

  const pickImageFromLibrary = async () => {
    let result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.7,
      allowsMultipleSelection: allowsMultipleSelection,
    });

    if (!result.canceled) {
      const selectedImage = result.assets[0].uri;
      setImage(selectedImage);
      onImageChange?.(selectedImage);
    }
  };

  const takePhoto = async () => {
    const permissionResult = await ImagePicker.requestCameraPermissionsAsync();

    if (permissionResult.granted === false) {
      alert("You've refused to allow this app to access your camera!");
      return;
    }

    let result = await ImagePicker.launchCameraAsync({
      mediaTypes: ['images'],
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.7,
      allowsMultipleSelection: allowsMultipleSelection,
    });

    if (!result.canceled) {
      const selectedImage = result.assets[0].uri;
      setImage(selectedImage);
      onImageChange?.(selectedImage);
    }
  };

  const removeImage = () => {
    setImage(null);
    onImageChange?.(null);
  };

  const handleImagePress = () => {
    const options = [
      'Take Photo',
      'Choose from Library',
      ...(image ? ['Remove Image'] : []),
      'Cancel',
    ];
    const cancelButtonIndex = options.length - 1;
    const destructiveButtonIndex = image ? options.indexOf('Remove Image') : undefined;

    showActionSheetWithOptions(
      {
        options,
        cancelButtonIndex,
        destructiveButtonIndex,
      },
      (buttonIndex) => {
        if (buttonIndex === 0) {
          takePhoto();
        } else if (buttonIndex === 1) {
          pickImageFromLibrary();
        } else if (image && buttonIndex === options.indexOf('Remove Image')) {
          removeImage();
        }
      }
    );
  };

  const imageSource = image ? { uri: image } : defaultImage;
  const borderRadius = size / 2;

  return (
    <View style={[styles.container, style]}>
      <TouchableOpacity
        onPress={handleImagePress}
        style={[
          styles.imageWrapper,
          { width: size, height: size, borderRadius }
        ]}
        disabled={isUploading}
      >
        {imageSource ? (
          <Image
            source={imageSource}
            style={[
              styles.image,
              { width: size, height: size, borderRadius }
            ]}
          />
        ) : (
          <View style={[
            styles.placeholderContainer,
            { width: size, height: size, borderRadius }
          ]}>
            <MaterialIcons name="add-a-photo" size={size * 0.25} color="#9188E5" />
          </View>
        )}

        {/* Camera Icon / Spinner in bottom right */}
        <View style={[
          styles.iconContainer,
          {
            width: size * 0.25,
            height: size * 0.25,
            borderRadius: (size * 0.25) / 2,
            bottom: size * 0.01,
            right: size * 0.01
          }
        ]}>
          {isUploading ? (
            <ActivityIndicator
              size={size * 0.12}
              color="#FFF"
            />
          ) : (
            <MaterialIcons
              name="camera-alt"
              size={size * 0.12}
              color="#FFF"
            />
          )}
        </View>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  imageWrapper: {
    position: 'relative',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: '#9188E5',
  },
  image: {
    borderWidth: 1,
    borderColor: '#9188E5',
  },
  placeholderContainer: {
    borderWidth: 1,
    borderColor: '#9188E5',
    borderStyle: 'dashed',
    backgroundColor: '#F8F7FF',
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconContainer: {
    position: 'absolute',
    backgroundColor: '#9188E5',
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
  },
});

export default ImageUploadInput;
