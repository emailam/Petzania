import React, { useState, useContext, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  TextInput,
  Image,
  Alert,
  FlatList,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { useForm, Controller } from 'react-hook-form';
import { UserContext } from '@/context/UserContext';

const PetPostForm = ({ 
  mode = 'create', // 'create' or 'edit'
  initialData = null,
  onSubmit,
  showPostType = true,
  showPetSelection = true,
  showPetDetails = true,
  showPostStatus = false,
  submitButtonText = 'Submit'
}) => {
  const { user } = useContext(UserContext);
  const userPets = user?.myPets || [];

  const {
    control,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors }
  } = useForm({
    defaultValues: {
      type: 'adoption',
      petId: null,
      name: '',
      gender: 'male',
      breed: '',
      age: '',
      description: '',
      location: '',
      status: 'pending',
      images: [],
    }
  });

  const watchedImages = watch('images');
  const watchedType = watch('type');
  const watchedPetId = watch('petId');
  const descriptionValue = watch('description');

  // Initialize form data
  useEffect(() => {
    if (initialData) {
      reset({
        type: initialData.type || 'adoption',
        petId: initialData.petDTO?.petId || initialData.petId || null,
        name: initialData.petDTO?.name || initialData.name || '',
        gender: initialData.petDTO?.gender?.toLowerCase() || initialData.gender || 'male',
        breed: initialData.petDTO?.breed || initialData.breed || '',
        age: initialData.petDTO?.age || initialData.age || '',
        description: initialData.petDTO?.description || initialData.description || '',
        location: initialData.location || '',
        status: initialData.postStatus || initialData.status || 'pending',
        images: initialData.petDTO?.myPicturesURLs || initialData.images || [],
      });
    }
  }, [initialData, reset]);

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
        const currentImages = watchedImages || [];
        setValue('images', [...currentImages, newImage]);
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
            const currentImages = watchedImages || [];
            setValue('images', currentImages.filter((_, i) => i !== index));
          }
        }
      ]
    );
  };

  const onFormSubmit = (data) => {
    // Basic validation
    if (showPetSelection && watchedType === 'adoption' && !data.petId) {
      Alert.alert('Error', 'Please select a pet');
      return;
    }

    if (mode === 'edit' && !data.name.trim()) {
      Alert.alert('Error', 'Pet name is required');
      return;
    }

    if (!data.location.trim()) {
      Alert.alert('Error', 'Location is required');
      return;
    }

    if (data.images.length === 0) {
      Alert.alert('Error', 'At least one image is required');
      return;
    }

    onSubmit(data);
  };

  // Determine what to show based on mode and props
  const shouldShowPostType = showPostType && mode === 'create';
  const shouldShowPetSelection = showPetSelection && mode === 'create' && watchedType === 'adoption';
  const shouldShowPetDetails = showPetDetails && (mode === 'edit' || watchedType === 'breeding');
  const shouldShowPostStatus = showPostStatus && mode === 'edit';

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      {/* Post Type */}
      {shouldShowPostType && (
        <>
          <Text style={styles.sectionTitle}>Post Type</Text>
          <Controller
            control={control}
            name="type"
            render={({ field: { value, onChange } }) => (
              <View style={styles.radioGroup}>
                {['adoption', 'breeding'].map(option => (
                  <TouchableOpacity
                    key={option}
                    style={[
                      styles.radioOption,
                      value === option && styles.radioSelected
                    ]}
                    onPress={() => onChange(option)}
                  >
                    <Text style={[
                      styles.radioText,
                      value === option && styles.radioTextSelected
                    ]}>
                      {option.charAt(0).toUpperCase() + option.slice(1)}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            )}
          />
        </>
      )}

      {/* Pet Selection */}
      {shouldShowPetSelection && (
        <>
          <Text style={styles.sectionTitle}>Choose Pet</Text>
          <Controller
            control={control}
            name="petId"
            rules={{ required: 'Please select a pet.' }}
            render={({ field: { value, onChange } }) => (
              <FlatList
                data={userPets}
                horizontal
                keyExtractor={item => item.petId}
                contentContainerStyle={styles.petList}
                renderItem={({ item }) => {
                  const selected = value === item.petId;
                  return (
                    <TouchableOpacity onPress={() => onChange(item.petId)} style={styles.petItem}>
                      <View style={[styles.petImageWrapper, selected && styles.petSelected]}>  
                        {item.myPicturesURLs && item.myPicturesURLs.length > 0
                          ? <Image source={{ uri: item.myPicturesURLs[0] }} style={styles.petImage} />
                          : <View style={styles.petPlaceholder} />
                        }
                      </View>
                      <Text style={styles.petName}>{item.name}</Text>
                    </TouchableOpacity>
                  );
                }}
              />
            )}
          />
          {errors.petId && <Text style={styles.error}>{errors.petId.message}</Text>}
        </>
      )}

      {/* Images Section */}
      <Text style={styles.sectionTitle}>Pet Images</Text>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.imagesContainer}>
        {(watchedImages || []).map((image, index) => (
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
        
        {(watchedImages?.length || 0) < 5 && (
          <TouchableOpacity style={styles.addImageButton} onPress={handleAddImage}>
            <Ionicons name="add" size={32} color="#9188E5" />
          </TouchableOpacity>
        )}
      </ScrollView>

      {/* Pet Details */}
      {shouldShowPetDetails && (
        <>
          <Text style={styles.sectionTitle}>Pet Details</Text>
          
          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Pet Name *</Text>
            <Controller
              control={control}
              name="name"
              rules={{ required: 'Pet name is required' }}
              render={({ field: { value, onChange } }) => (
                <TextInput
                  style={styles.textInput}
                  value={value}
                  onChangeText={onChange}
                  placeholder="Enter pet name"
                  maxLength={50}
                />
              )}
            />
            {errors.name && <Text style={styles.error}>{errors.name.message}</Text>}
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Gender</Text>
            <Controller
              control={control}
              name="gender"
              render={({ field: { value, onChange } }) => (
                <View style={styles.genderContainer}>
                  <TouchableOpacity
                    style={[
                      styles.genderButton,
                      value === 'male' && styles.genderButtonActive
                    ]}
                    onPress={() => onChange('male')}
                  >
                    <Ionicons name="male" size={20} color={value === 'male' ? '#fff' : '#666'} />
                    <Text style={[
                      styles.genderButtonText,
                      value === 'male' && styles.genderButtonTextActive
                    ]}>Male</Text>
                  </TouchableOpacity>
                  
                  <TouchableOpacity
                    style={[
                      styles.genderButton,
                      value === 'female' && styles.genderButtonActive
                    ]}
                    onPress={() => onChange('female')}
                  >
                    <Ionicons name="female" size={20} color={value === 'female' ? '#fff' : '#666'} />
                    <Text style={[
                      styles.genderButtonText,
                      value === 'female' && styles.genderButtonTextActive
                    ]}>Female</Text>
                  </TouchableOpacity>
                </View>
              )}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Breed</Text>
            <Controller
              control={control}
              name="breed"
              render={({ field: { value, onChange } }) => (
                <TextInput
                  style={styles.textInput}
                  value={value}
                  onChangeText={onChange}
                  placeholder="Enter breed"
                  maxLength={50}
                />
              )}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Age</Text>
            <Controller
              control={control}
              name="age"
              render={({ field: { value, onChange } }) => (
                <TextInput
                  style={styles.textInput}
                  value={value}
                  onChangeText={onChange}
                  placeholder="e.g., 2 years, 6 months"
                  maxLength={20}
                />
              )}
            />
          </View>
        </>
      )}

      {/* Location */}
      <Text style={styles.sectionTitle}>Location</Text>
      <View style={styles.inputGroup}>
        <Controller
          control={control}
          name="location"
          rules={{ required: 'Location is required.' }}
          render={({ field: { value, onChange } }) => (
            <TextInput
              style={styles.textInput}
              onChangeText={onChange}
              value={value}
              placeholder="State, Country"
            />
          )}
        />
        {errors.location && <Text style={styles.error}>{errors.location.message}</Text>}
      </View>

      {/* Description */}
      <Text style={styles.sectionTitle}>Description</Text>
      <View style={styles.inputGroup}>
        <Controller
          control={control}
          name="description"
          rules={{ maxLength: { value: 500, message: 'Max 500 characters.' } }}
          render={({ field: { value, onChange } }) => (
            <TextInput
              style={[styles.textInput, styles.textArea]}
              multiline
              maxLength={500}
              onChangeText={onChange}
              value={value}
              placeholder="Tell us about this pet..."
              textAlignVertical="top"
            />
          )}
        />
        <Text style={styles.charCount}>{descriptionValue?.length || 0}/500</Text>
        {errors.description && <Text style={styles.error}>{errors.description.message}</Text>}
      </View>

      {/* Post Status */}
      {shouldShowPostStatus && (
        <>
          <Text style={styles.sectionTitle}>Post Status</Text>
          <Controller
            control={control}
            name="status"
            render={({ field: { value, onChange } }) => (
              <View style={styles.statusContainer}>
                <TouchableOpacity
                  style={[
                    styles.statusButton,
                    value === 'pending' && styles.statusButtonActive,
                    { backgroundColor: value === 'pending' ? '#FF9800' : '#f0f0f0' }
                  ]}
                  onPress={() => onChange('pending')}
                >
                  <Text style={[
                    styles.statusButtonText,
                    value === 'pending' && styles.statusButtonTextActive
                  ]}>Pending</Text>
                </TouchableOpacity>
                
                <TouchableOpacity
                  style={[
                    styles.statusButton,
                    value === 'completed' && styles.statusButtonActive,
                    { backgroundColor: value === 'completed' ? '#4CAF50' : '#f0f0f0' }
                  ]}
                  onPress={() => onChange('completed')}
                >
                  <Text style={[
                    styles.statusButtonText,
                    value === 'completed' && styles.statusButtonTextActive
                  ]}>Completed</Text>
                </TouchableOpacity>
              </View>
            )}
          />
        </>
      )}

      {/* Submit Button */}
      <TouchableOpacity style={styles.submitButton} onPress={handleSubmit(onFormSubmit)}>
        <Text style={styles.submitButtonText}>{submitButtonText}</Text>
      </TouchableOpacity>

      <View style={styles.bottomPadding} />
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    backgroundColor: '#fff',
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 12,
    marginTop: 16,
  },
  radioGroup: {
    flexDirection: 'row',
    marginBottom: 16,
  },
  radioOption: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderWidth: 1,
    borderColor: '#333',
    borderRadius: 20,
    marginRight: 12,
    backgroundColor: '#fff',
  },
  radioSelected: {
    backgroundColor: '#9188E5',
    borderColor: '#9188E5',
  },
  radioText: {
    fontSize: 14,
    color: '#000',
  },
  radioTextSelected: {
    color: '#fff',
  },
  petList: {
    paddingVertical: 8,
    marginBottom: 16,
  },
  petItem: {
    alignItems: 'center',
    marginRight: 16,
  },
  petImageWrapper: {
    width: 64,
    height: 64,
    borderRadius: 32,
    borderWidth: 1,
    borderColor: '#ccc',
    overflow: 'hidden',
    justifyContent: 'center',
    alignItems: 'center',
  },
  petSelected: {
    borderColor: '#9188E5',
    borderWidth: 2,
  },
  petImage: {
    width: 64,
    height: 64,
  },
  petPlaceholder: {
    width: 64,
    height: 64,
    backgroundColor: '#ddd',
  },
  petName: {
    marginTop: 4,
    fontSize: 12,
    color: '#000',
    marginBottom: 8,
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
    borderColor: '#333',
    borderRadius: 8,
    padding: 12,
    fontSize: 14,
    backgroundColor: '#fafafa',
    color: '#000',
  },
  textArea: {
    height: 120,
  },
  charCount: {
    alignSelf: 'flex-end',
    marginTop: 4,
    fontSize: 12,
    color: '#666',
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
  submitButton: {
    marginTop: 24,
    backgroundColor: '#9188E5',
    borderColor: '#9188E5',
    borderWidth: 1,
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
  },
  submitButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  bottomPadding: {
    height: 50,
  },
  error: {
    color: 'red',
    fontSize: 12,
    marginTop: 4,
  },
});

export default PetPostForm;