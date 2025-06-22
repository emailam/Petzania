import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal,
  TouchableOpacity,
  ScrollView,
  TextInput,
  Image,
  Dimensions,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { updatePost, uploadImage } from '../../services/postService';

const { width, height } = Dimensions.get('window');

const EditPostModal = ({ visible, onClose, post, onUpdate }) => {
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    gender: 'male',
    breed: '',
    age: '',
    description: '',
    location: '',
    status: 'pending',
    images: [],
  });

  useEffect(() => {
    if (post && visible) {
      setFormData({
        name: post.petDTO?.name || '',
        gender: post.petDTO?.gender?.toLowerCase() || 'male',
        breed: post.petDTO?.breed || '',
        age: post.petDTO?.age || '',
        description: post.petDTO?.description || '',
        location: post.location || '',
        status: post.postStatus || 'pending',
        images: post.petDTO?.myPicturesURLs || [],
      });
    }
  }, [post, visible]);

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleAddImage = async () => {
    try {
      const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('Permission needed', 'Please grant camera roll permissions to add images.');
        return;
      }

      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsEditing: true,
        aspect: [1, 1],
        quality: 0.8,
      });

      if (!result.canceled && result.assets[0]) {
        const newImage = result.assets[0].uri;
        setFormData(prev => ({
          ...prev,
          images: [...prev.images, newImage]
        }));
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to pick image');
    }
  };

  const handleRemoveImage = (index) => {
    Alert.alert(
      'Remove Image',
      'Are you sure you want to remove this image?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Remove',
          style: 'destructive',
          onPress: () => {
            setFormData(prev => ({
              ...prev,
              images: prev.images.filter((_, i) => i !== index)
            }));
          }
        }
      ]
    );
  };

  const handleSave = async () => {
    if (!formData.name.trim()) {
      Alert.alert('Error', 'Pet name is required');
      return;
    }

    if (formData.images.length === 0) {
      Alert.alert('Error', 'At least one image is required');
      return;
    }

    setLoading(true);
    try {
      // Upload new images that are local URIs
      const uploadedImages = await Promise.all(
        formData.images.map(async (image) => {
          if (image.startsWith('file://') || image.startsWith('content://')) {
            // This is a new local image, upload it
            return await uploadImage(image);
          }
          // This is an existing URL, keep it as is
          return image;
        })
      );

      const updatedPost = {
        ...post,
        petDTO: {
          ...post.petDTO,
          name: formData.name,
          gender: formData.gender,
          breed: formData.breed,
          age: formData.age,
          description: formData.description,
          myPicturesURLs: uploadedImages,
        },
        location: formData.location,
        status: formData.status,
      };

      const result = await updatePost(post.postId, updatedPost);
      onUpdate(result);
      Alert.alert('Success', 'Post updated successfully');
    } catch (error) {
      console.error('Error updating post:', error);
      Alert.alert('Error', 'Failed to update post');
    } finally {
      setLoading(false);
    }
  };

  if (!visible) return null;

  return (
    <Modal
      animationType="slide"
      transparent={true}
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.modalOverlay}>
        <View style={styles.modalContainer}>
          {/* Header */}
          <View style={styles.header}>
            <TouchableOpacity onPress={onClose}>
              <Ionicons name="close" size={24} color="#666" />
            </TouchableOpacity>
            <Text style={styles.headerTitle}>Edit Post</Text>
            <TouchableOpacity 
              onPress={handleSave}
              disabled={loading}
              style={[styles.saveButton, loading && styles.saveButtonDisabled]}
            >
              {loading ? (
                <ActivityIndicator size="small" color="#9188E5" />
              ) : (
                <Text style={styles.saveButtonText}>Save</Text>
              )}
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
            {/* Images Section */}
            <Text style={styles.sectionTitle}>Pet Images</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.imagesContainer}>
              {formData.images.map((image, index) => (
                <View key={index} style={styles.imageWrapper}>
                  <Image source={{ uri: image }} style={styles.image} />
                  <TouchableOpacity 
                    style={styles.removeImageButton}
                    onPress={() => handleRemoveImage(index)}
                  >
                    <Ionicons name="close-circle" size={24} color="#F44336" />
                  </TouchableOpacity>
                </View>
              ))}
              
              {formData.images.length < 5 && (
                <TouchableOpacity style={styles.addImageButton} onPress={handleAddImage}>
                  <Ionicons name="add" size={32} color="#9188E5" />
                </TouchableOpacity>
              )}
            </ScrollView>

            {/* Pet Details */}
            <Text style={styles.sectionTitle}>Pet Details</Text>
            
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Pet Name *</Text>
              <TextInput
                style={styles.textInput}
                value={formData.name}
                onChangeText={(value) => handleInputChange('name', value)}
                placeholder="Enter pet name"
                maxLength={50}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Gender</Text>
              <View style={styles.genderContainer}>
                <TouchableOpacity
                  style={[
                    styles.genderButton,
                    formData.gender === 'male' && styles.genderButtonActive
                  ]}
                  onPress={() => handleInputChange('gender', 'male')}
                >
                  <Ionicons name="male" size={20} color={formData.gender === 'male' ? '#fff' : '#666'} />
                  <Text style={[
                    styles.genderButtonText,
                    formData.gender === 'male' && styles.genderButtonTextActive
                  ]}>Male</Text>
                </TouchableOpacity>
                
                <TouchableOpacity
                  style={[
                    styles.genderButton,
                    formData.gender === 'female' && styles.genderButtonActive
                  ]}
                  onPress={() => handleInputChange('gender', 'female')}
                >
                  <Ionicons name="female" size={20} color={formData.gender === 'female' ? '#fff' : '#666'} />
                  <Text style={[
                    styles.genderButtonText,
                    formData.gender === 'female' && styles.genderButtonTextActive
                  ]}>Female</Text>
                </TouchableOpacity>
              </View>
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Breed</Text>
              <TextInput
                style={styles.textInput}
                value={formData.breed}
                onChangeText={(value) => handleInputChange('breed', value)}
                placeholder="Enter breed"
                maxLength={50}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Age</Text>
              <TextInput
                style={styles.textInput}
                value={formData.age}
                onChangeText={(value) => handleInputChange('age', value)}
                placeholder="e.g., 2 years, 6 months"
                maxLength={20}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Location</Text>
              <TextInput
                style={styles.textInput}
                value={formData.location}
                onChangeText={(value) => handleInputChange('location', value)}
                placeholder="Enter location"
                maxLength={100}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Description</Text>
              <TextInput
                style={[styles.textInput, styles.textArea]}
                value={formData.description}
                onChangeText={(value) => handleInputChange('description', value)}
                placeholder="Tell us about this pet..."
                multiline
                numberOfLines={4}
                maxLength={500}
                textAlignVertical="top"
              />
            </View>

            {/* Post Status */}
            <Text style={styles.sectionTitle}>Post Status</Text>
            <View style={styles.statusContainer}>
              <TouchableOpacity
                style={[
                  styles.statusButton,
                  formData.status === 'pending' && styles.statusButtonActive,
                  { backgroundColor: formData.status === 'pending' ? '#FF9800' : '#f0f0f0' }
                ]}
                onPress={() => handleInputChange('status', 'pending')}
              >
                <Text style={[
                  styles.statusButtonText,
                  formData.status === 'pending' && styles.statusButtonTextActive
                ]}>Pending</Text>
              </TouchableOpacity>
              
              <TouchableOpacity
                style={[
                  styles.statusButton,
                  formData.status === 'completed' && styles.statusButtonActive,
                  { backgroundColor: formData.status === 'completed' ? '#4CAF50' : '#f0f0f0' }
                ]}
                onPress={() => handleInputChange('status', 'completed')}
              >
                <Text style={[
                  styles.statusButtonText,
                  formData.status === 'completed' && styles.statusButtonTextActive
                ]}>Completed</Text>
              </TouchableOpacity>
            </View>

            <View style={styles.bottomPadding} />
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
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
    minHeight: height * 0.7,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
  },
  saveButton: {
    backgroundColor: '#9188E5',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    minWidth: 60,
    alignItems: 'center',
  },
  saveButtonDisabled: {
    opacity: 0.6,
  },
  saveButtonText: {
    color: 'white',
    fontWeight: '500',
  },
  content: {
    flex: 1,
    padding: 16,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 12,
    marginTop: 16,
  },
  imagesContainer: {
    marginBottom: 16,
  },
  imageWrapper: {
    position: 'relative',
    marginRight: 12,
  },
  image: {
    width: 100,
    height: 100,
    borderRadius: 8,
  },
  removeImageButton: {
    position: 'absolute',
    top: -8,
    right: -8,
    backgroundColor: 'white',
    borderRadius: 12,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.2,
    shadowRadius: 2,
  },
  addImageButton: {
    width: 100,
    height: 100,
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
    borderWidth: 2,
    borderColor: '#9188E5',
    borderStyle: 'dashed',
    justifyContent: 'center',
    alignItems: 'center',
  },
  inputGroup: {
    marginBottom: 16,
  },
  inputLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#333',
    marginBottom: 8,
  },
  textInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 14,
    backgroundColor: '#fafafa',
  },
  textArea: {
    height: 100,
  },
  genderContainer: {
    flexDirection: 'row',
    gap: 12,
  },
  genderButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ddd',
    backgroundColor: '#fafafa',
    gap: 8,
  },
  genderButtonActive: {
    backgroundColor: '#9188E5',
    borderColor: '#9188E5',
  },
  genderButtonText: {
    fontSize: 14,
    color: '#666',
    fontWeight: '500',
  },
  genderButtonTextActive: {
    color: '#fff',
  },
  statusContainer: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 16,
  },
  statusButton: {
    flex: 1,
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  statusButtonActive: {
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.2,
    shadowRadius: 2,
  },
  statusButtonText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#666',
  },
  statusButtonTextActive: {
    color: '#fff',
  },
  bottomPadding: {
    height: 50,
  },
});

export default EditPostModal;