import React, { useState , memo } from 'react';
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
import { FlatList } from 'react-native-gesture-handler';
const { height } = Dimensions.get('window');

const EditPostModal = memo(({visible ,onClose, post, onUpdate, onDelete}) => {

  if (!post) return null;

  const [descriptionValue, setDescriptionValue] = useState(post?.description || '');
  const [statusValue, setStatusValue] = useState(post?.postStatus || 'PENDING');
  const [locationValue, setLocationValue] = useState(post?.location || '');

  const [formErrors, setFormErrors] = useState({});
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const petDTO = post.petDTO || {};

  const validateForm = () => {
    const errors = {};
    
    if (!descriptionValue.trim()) {
      errors.description = 'Description is required';
    } else if (descriptionValue.length > 500) {
      errors.description = 'Description must be 500 characters or less';
    }
    
    if (!locationValue.trim()) {
      errors.location = 'Location is required';
    } else if (locationValue.length > 100) {
      errors.location = 'Location must be 100 characters or less';
    }
    
    if (!statusValue || !['PENDING', 'COMPLETED'].includes(statusValue)) {
      errors.status = 'Please select a valid status';
    }
    
    return errors;
  };

  // Only enable save if any edited field has changed
  const hasChanges = () => {
    return (
      descriptionValue !== (post?.description || '') ||
      statusValue !== (post?.postStatus || 'PENDING') ||
      locationValue !== (post?.location || '')
    );
  };

  // Only allow if description is not empty
  const formValid = Object.keys(validateForm()).length === 0;

  const handleSave = async () => {
if (isSaving || isDeleting) return;    
    // Client-side validation
    const errors = validateForm();
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    
    setIsSaving(true);
    setFormErrors({});
    
    try {
      const updatedData = {
        description: descriptionValue.trim(),
        postStatus: statusValue,
        location: locationValue.trim(),
        updatePetDTO: petDTO,
      }; 
      await onUpdate?.(post.postId, updatedData);
      onClose?.();
    } catch (error) {
      console.error('Save failed:', error);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = () => {
  Alert.alert(
    'Delete Post',
    'Are you sure you want to delete this post? This action cannot be undone.',
    [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          setIsDeleting(true);
          try {
            await onDelete?.(post.postId);
            onClose?.();
          } catch (error) {
            console.error('Delete failed:', error);
          } finally {
            setIsDeleting(false);
          }
        },
      },
    ]
  );
};

  const handleCancel = () => {
    if (hasChanges()) {
      Alert.alert(
        'Discard Changes',
        'You have unsaved changes. Are you sure you want to discard them?',
        [
          { text: 'Keep Editing', style: 'cancel' },
          {
            text: 'Discard',
            style: 'destructive',
            onPress: () => {
              setFormErrors({});
              onClose?.();
            },
          },
        ]
      );
    } else {
      onClose?.();
    }
  };

  

  return (
    <Modal
      animationType="slide"
      transparent={true}
      onRequestClose={onClose}
      visible={visible}
    >
      <View style={styles.modalOverlay}>
        <View style={styles.modalContainer}>
          {/* Header */}
          <View style={styles.header}>
            <TouchableOpacity
              onPress={handleCancel}  // Changed from onClose to handleCancel
              hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
            >
              <Ionicons name="close" size={24} color="#666" />
            </TouchableOpacity>
            <Text style={styles.headerTitle}>Edit Post</Text>
            <View style={styles.headerSpacer} />
          </View>

          <ScrollView
            style={styles.content}
            showsVerticalScrollIndicator={false}
            keyboardShouldPersistTaps="handled"
          >
            {/* Pet Image (read-only) */}
            <Text style={styles.sectionTitle}>Pet Images</Text>
            <View style={styles.imagesContainer}>
              {petDTO?.myPicturesURLs && petDTO.myPicturesURLs.length > 0 ? (
                <FlatList
                  horizontal
                  data={petDTO.myPicturesURLs}
                  keyExtractor={(item, index) => index.toString()}
                  showsHorizontalScrollIndicator={false}
                  contentContainerStyle={styles.imagesScrollContainer}
                  renderItem={({ item, index }) => (
                    <View style={styles.imageWrapper}>
                      <Image 
                        source={{ uri: item }} 
                        style={styles.image} 
                        resizeMode="cover"
                      />
                    </View>
                  )}
                />
              ) : (
                <View style={styles.imageContainer}>
                  <Image 
                    source={require('@/assets/images/Defaults/default-pet.png')} 
                    style={styles.image} 
                    resizeMode="cover"
                  />
                </View>
              )}
            </View>

            {/* Pet Details (read-only except location, description) */}
            <Text style={styles.sectionTitle}>Pet Details</Text>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Pet Name</Text>
              <TextInput
                style={[styles.textInput, { backgroundColor: '#e7e7e7' }]}
                value={petDTO?.name || ''}
                editable={false}
              />
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Gender</Text>
              <TextInput
                style={[styles.textInput, { backgroundColor: '#e7e7e7' }]}
                value={petDTO?.gender || ''}
                editable={false}
              />
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Breed</Text>
              <TextInput
                style={[styles.textInput, { backgroundColor: '#e7e7e7' }]}
                value={petDTO?.breed || ''}
                editable={false}
              />
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Age</Text>
              <TextInput
                style={[styles.textInput, { backgroundColor: '#e7e7e7' }]}
                value={petDTO?.age || ''}
                editable={false}
              />
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Location</Text>
              <TextInput
                style={[
                  styles.textInput,
                  formErrors.location && styles.textInputError
                ]}
                value={locationValue}
                onChangeText={(text) => {
                  setLocationValue(text);
                  if (formErrors.location) {
                    setFormErrors(prev => ({ ...prev, location: null }));
                  }
                }}
                placeholder="Enter location"
                maxLength={100}
                editable={!isSaving}
              />
              {formErrors.location && (
                <Text style={styles.errorText}>{formErrors.location}</Text>
              )}
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Description</Text>
              <TextInput
                style={[
                  styles.textInput, 
                  styles.textArea,
                  formErrors.description && styles.textInputError
                ]}
                value={descriptionValue}
                onChangeText={(text) => {
                  setDescriptionValue(text);
                  if (formErrors.description) {
                    setFormErrors(prev => ({ ...prev, description: null }));
                  }
                }}
                placeholder="Tell us about this pet..."
                multiline
                numberOfLines={4}
                maxLength={500}
                textAlignVertical="top"
                editable={!isSaving}
              />
              <Text style={styles.charCount}>
                {descriptionValue.length}/500
              </Text>
              {formErrors.description && (
                <Text style={styles.errorText}>{formErrors.description}</Text>
              )}
          </View>

            {/* Editable Post Status */}
            <Text style={styles.sectionTitle}>Post Status</Text>
            <View style={styles.statusContainer}>
              <TouchableOpacity
                style={[
                  styles.statusButton,
                  statusValue === 'PENDING' && styles.statusButtonActive,
                  { backgroundColor: statusValue === 'PENDING' ? '#FF9800' : '#f0f0f0' },
                  formErrors.status && styles.statusButtonError
                ]}
                onPress={() => {
                  setStatusValue('PENDING');
                  if (formErrors.status) {
                    setFormErrors(prev => ({ ...prev, status: null }));
                  }
                }}
                activeOpacity={0.7}
                disabled={isSaving}
              >
                <Text style={[
                  styles.statusButtonText,
                  statusValue === 'PENDING' && styles.statusButtonTextActive
                ]}>Pending</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[
                  styles.statusButton,
                  statusValue === 'COMPLETED' && styles.statusButtonActive,
                  { backgroundColor: statusValue === 'COMPLETED' ? '#4CAF50' : '#f0f0f0' },
                  formErrors.status && styles.statusButtonError
                ]}
                onPress={() => {
                  setStatusValue('COMPLETED');
                  if (formErrors.status) {
                    setFormErrors(prev => ({ ...prev, status: null }));
                  }
                }}
                activeOpacity={0.7}
                disabled={isSaving}
              >
                <Text style={[
                  styles.statusButtonText,
                  statusValue === 'COMPLETED' && styles.statusButtonTextActive
                ]}>Completed</Text>
              </TouchableOpacity>
            </View>
            {formErrors.status && (
              <Text style={styles.errorText}>{formErrors.status}</Text>
            )}
            <View style={styles.bottomPadding} />
          </ScrollView>

          {/* Action Buttons */}
          <View style={styles.actionButtons}>
            {/* Save Button */}
            <TouchableOpacity
              onPress={handleSave}
              disabled={isSaving || isDeleting || !hasChanges() || !formValid}
              style={[
                styles.saveButton,
                (isSaving || isDeleting || !hasChanges() || !formValid) && styles.saveButtonDisabled,
              ]}
              activeOpacity={0.8}
            >
              {isSaving ? (
                <ActivityIndicator size="small" color="#fff" />
              ) : (
                <Text style={styles.saveButtonText}>Save Changes</Text>
              )}
            </TouchableOpacity>

            {/* Delete Button */}
            <TouchableOpacity
              onPress={handleDelete}
              disabled={isSaving || isDeleting}
              style={[styles.deleteButton, (isSaving || isDeleting) && styles.deleteButtonDisabled]}
              activeOpacity={0.8}
            >
              {isDeleting ? (
                <ActivityIndicator size="small" color="#fff" />
              ) : (
                <>
                  <Ionicons name="trash-outline" size={16} color="#fff" />
                  <Text style={styles.deleteButtonText}>Delete</Text>
                </>
              )}
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
});

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
  headerSpacer: {
    width: 24,
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
    marginBottom: 20,
  },
  imagesScrollView: {
    maxHeight: 120, // Adjust based on your image height
  },
  imagesScrollContainer: {
    paddingHorizontal: 5,
  },
  imageWrapper: {
    marginRight: 10,
    borderRadius: 8,
    overflow: 'hidden',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.2,
    shadowRadius: 2,
  },
  image: {
    width: 100,  // Adjust width as needed
    height: 100, // Adjust height as needed
    borderRadius: 8,
  },
  // Keep your existing imageContainer style for the default image case
  imageContainer: {
    alignItems: 'center',
    marginBottom: 10,
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
    paddingTop: 12,
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
    height: 20,
  },
  actionButtons: {
    flexDirection: 'row',
    padding: 16,
    gap: 12,
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
    backgroundColor: '#fff',
  },
  saveButton: {
    flex: 2,
    backgroundColor: '#9188E5',
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  saveButtonDisabled: {
    backgroundColor: '#ccc',
    opacity: 0.6,
  },
  saveButtonText: {
    color: 'white',
    fontWeight: '600',
    fontSize: 16,
  },
  deleteButton: {
    flex: 1,
    backgroundColor: '#FF3040',
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    gap: 8,
  },
  deleteButtonDisabled: {
    opacity: 0.6,
  },
  deleteButtonText: {
    color: 'white',
    fontWeight: '600',
    fontSize: 16,
  },
  textInputError: {
    borderColor: '#FF3040',
    borderWidth: 2,
  },
  errorText: {
    color: '#FF3040',
    fontSize: 12,
    marginTop: 4,
    marginLeft: 4,
  },
  statusButtonError: {
    borderColor: '#FF3040',
    borderWidth: 2,
  },
});

EditPostModal.displayName = 'EditPostModal';

export default EditPostModal;