import React, { useState, memo, useMemo, useRef, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  Dimensions,
  Alert,
  ActivityIndicator,
  BackHandler,
} from 'react-native';
import { Image } from 'expo-image';
import PetCard from '../PetCard';
import ImageViewing from 'react-native-image-viewing';
import { Ionicons } from '@expo/vector-icons';
import { FlatList } from 'react-native-gesture-handler';
import {
  BottomSheetModal,
  BottomSheetBackdrop,
  BottomSheetScrollView,
} from '@gorhom/bottom-sheet';

const EditPostModal = memo(({ visible, onClose, post, onUpdate, onDelete }) => {
  const bottomSheetModalRef = useRef(null);

  // Memoized snap points
  const snapPoints = useMemo(() => ['90%'], []);

  // Handle modal visibility changes
  useEffect(() => {
    if (visible) {
      bottomSheetModalRef.current?.present();
    } else {
      bottomSheetModalRef.current?.dismiss();
    }
  }, [visible]);

  // Handle Android back button
  useEffect(() => {
    const backAction = () => {
      if (visible) {
        handleCancel();
        return true;
      }
      return false;
    };

    const backHandler = BackHandler.addEventListener('hardwareBackPress', backAction);
    return () => backHandler.remove();
  }, [visible]);

  // Handle modal dismiss
  const handleSheetChanges = useCallback((index) => {
    if (index === -1) {
      onClose?.();
    }
  }, [onClose]);

  // Backdrop component
  const renderBackdrop = useCallback(
    (props) => (
      <BottomSheetBackdrop
        {...props}
        disappearsOnIndex={-1}
        appearsOnIndex={0}
        opacity={0.5}
        pressBehavior="close"
      />
    ),
    []
  );

  if (!post) return null;

  const [descriptionValue, setDescriptionValue] = useState(post?.description || '');
  const [statusValue, setStatusValue] = useState(post?.postStatus || 'PENDING');
  const [locationValue, setLocationValue] = useState(post?.location || '');

  const [formErrors, setFormErrors] = useState({});
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [showImageViewer, setShowImageViewer] = useState(false);
  const petDTO = post.petDTO || {};

  const handleImagePress = (imageIndex) => {
      setCurrentImageIndex(imageIndex);
      setShowImageViewer(true);
  };

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
      bottomSheetModalRef.current?.dismiss();
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
            bottomSheetModalRef.current?.dismiss();
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
    bottomSheetModalRef.current?.dismiss();
  };

  return (
    <BottomSheetModal
      ref={bottomSheetModalRef}
      snapPoints={snapPoints}
      onChange={handleSheetChanges}
      backdropComponent={renderBackdrop}
      enablePanDownToClose={true}
      enableDismissOnClose={true}
      handleIndicatorStyle={styles.handleIndicator}
      backgroundStyle={styles.bottomSheetBackground}
    >
      <BottomSheetScrollView
        style={styles.scrollContent}
        contentContainerStyle={styles.scrollContentContainer}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
      >
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Edit Post</Text>
          <View style={styles.headerSpacer} />
        </View>
            <Text style={styles.sectionTitle}>Pet Details</Text>
            <View style={styles.petContainer}>
              <PetCard pet={petDTO} marginHorizontal={0} />
            </View>

            {/* Pet Details (read-only except location, description) */}
            <Text style={styles.sectionTitle}>Post Details</Text>
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

          {/* Action Buttons - Fixed at bottom */}
          <View style={styles.actionButtons}>
            {/* Save Button */}
            <TouchableOpacity
              onPress={handleSave}
              disabled={isSaving || isDeleting || !hasChanges() || !formValid}
              activeOpacity={0.8}
              style={[
                styles.saveButton,
                (isSaving || isDeleting || !hasChanges() || !formValid) && styles.saveButtonDisabled,
              ]}
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
          <ImageViewing
              images={petDTO?.myPicturesURLs.map(uri => ({ uri }))}
              imageIndex={currentImageIndex}
              visible={showImageViewer}
              onRequestClose={() => setShowImageViewer(false)}
              backgroundColor="black"
              swipeToCloseEnabled
              doubleTapToZoomEnabled
          />
        </BottomSheetScrollView>
      </BottomSheetModal>
  );
});

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'white',
  },
  bottomSheetBackground: {
    backgroundColor: 'white',
  },
  handleIndicator: {
    backgroundColor: '#9188E5',
    width: 40,
  },
  saveButtonWrapper: {
    flex: 2,
    borderRadius: 8,
    overflow: 'hidden',
  },
  saveButtonDisabled: {
    opacity: 0.6,
  },
  header: {
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: '600',
    color: '#333',
  },
  headerSpacer: {
    width: 24,
  },
  scrollContent: {
    flex: 1,
  },
  scrollContentContainer: {
    paddingHorizontal: 12,
    paddingBottom: 20,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: '#000',
    marginBottom: 12,
    marginTop: 16,
  },
  petContainer:{
    marginBottom: 20,
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
    width: 100,
    height: 100,
    borderRadius: 8,
    borderColor: '#9188E5',
    borderWidth: 1,
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
    height: 16,
  },
  actionButtons: {
    flexDirection: 'row',
    paddingVertical: 16,
    gap: 12,
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
    backgroundColor: '#fff',
    marginTop: 8,
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
    fontSize: 16, 
    color: '#fff', 
    fontWeight: '700',
    zIndex: 1,
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
  charCount: {
    fontSize: 12,
    color: '#666',
    marginTop: 4,
    textAlign: 'right',
  },
});

EditPostModal.displayName = 'EditPostModal';

export default EditPostModal;