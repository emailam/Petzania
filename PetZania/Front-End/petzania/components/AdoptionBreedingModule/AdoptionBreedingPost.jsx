import {useContext} from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
  Image,
  SafeAreaView,
  StatusBar,
} from 'react-native';
import { useForm, Controller } from 'react-hook-form';
import { useRouter } from 'expo-router';

import { UserContext } from '@/context/UserContext';
import { FlowContext } from '@/context/FlowContext';

import FontAwesome5 from '@expo/vector-icons/FontAwesome5';
import Ionicons from '@expo/vector-icons/Ionicons';
import { useCreatePost} from "@/services/postService";
import Toast from 'react-native-toast-message'; // Adjust the import path as needed
import Ionicons from '@expo/vector-icons/Ionicons';
import LottieView from "lottie-react-native";

export default function AdoptionBreedingForm() {
  const {
    control,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm({
    defaultValues: {
      postType: 'ADOPTION',
      petId: '',
      location: '',
      description: '',
    },
  });
  const { mutate: createPost, isPending } = useCreatePost();
  const descriptionValue = watch('description');
  const postType = watch('postType');
  const selectedPetId = watch('petId');
  const location = watch('location');

  const router = useRouter();

  const { user } = useContext(UserContext);
  const { setFromPage } = useContext(FlowContext);
  const defaultPetImage = require('@/assets/images/Defaults/default-pet.png');

  const userPets = user?.myPets;
  const selectedPet = userPets?.find((pet) => pet.petId === selectedPetId);
  const isFormValid = postType && location && descriptionValue && selectedPetId;
  
  const onSubmit = (data) => {
  createPost(data, {
    onSuccess: (res) => {
      Toast.show({
        type: 'success',
        text1: 'Post created successfully!',
      });
      //console.log('Post created:', res);
      router.push('/Home');
    },
    onError: (error) => {
      Toast.show({
        type: 'error',
        text1: 'Failed to create post',
        text2: error.response?.data?.message[0].message || 'Something went wrong',
      });
    },
  });
};

  const handleAddNewPet = () => {
    setFromPage('AdoptionBreedingPost');
    router.push("/PetModule/AddPet1");
    console.log('Add new pet button pressed');
  };

  const getPlaceholderText = () => {
    if (postType === 'ADOPTION') {
      return "Share details about your pet's personality, health, and why you're looking for a new home...";
    } else if (postType === 'BREEDING') {
      return "Describe your pet's lineage, health certifications, and breeding experience...";
    }
    return 'Enter description';
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#fff" />

      <ScrollView 
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        {/* Post Type Selection */}
        <View style={styles.section}>
          <Text style={styles.label}>Post Type</Text>
          <Controller
            control={control}
            name="postType"
            render={({ field: { value, onChange } }) => (
              <View style={styles.typeButtons}>
                <TouchableOpacity
                  style={[
                    styles.typeButton,
                    value === 'ADOPTION' && styles.typeButtonSelected
                  ]}
                  onPress={() => onChange('ADOPTION')}
                >
                  <Ionicons
                    name={value === 'ADOPTION' ? "home" : "home-outline"}
                    size={28}
                    color={value === 'ADOPTION' ? '#FFFFFF' : '#9188E5'}
                  />
                  <Text style={[
                    styles.typeButtonText,
                    value === 'ADOPTION' && styles.typeButtonTextSelected
                  ]}>
                    Adoption
                  </Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[
                    styles.typeButton,
                    value === 'BREEDING' && styles.typeButtonSelected
                  ]}
                  onPress={() => onChange('BREEDING')}
                >
                  <Ionicons
                    name={value === 'BREEDING' ? "heart" : "heart-outline"}
                    size={28}
                    color={value === 'BREEDING' ? '#FFFFFF' : '#9188E5'}
                  />
                  <Text style={[
                    styles.typeButtonText,
                    value === 'BREEDING' && styles.typeButtonTextSelected
                  ]}>
                    Breeding
                  </Text>
                </TouchableOpacity>
              </View>
            )}
          />
        </View>

        <View style={styles.separator} />

        {/* Pet Selection */}
        <View style={styles.section}>
          <Text style={styles.label}>Choose Pet</Text>
          <Controller
            control={control}
            name="petId"
            rules={{ required: 'Please select a pet.' }}
            render={({ field: { value, onChange } }) => (
              <ScrollView 
                horizontal 
                showsHorizontalScrollIndicator={false}
                contentContainerStyle={styles.petList}
              >
                {userPets.map((pet) => (
                  <TouchableOpacity
                    key={pet.petId}
                    style={styles.petItem}
                    onPress={() => onChange(pet.petId)}
                  >
                    <View style={[
                      styles.petImageWrapper,
                      value === pet.petId && styles.petImageWrapperSelected
                    ]}>
                      {pet.myPicturesURLs && pet.myPicturesURLs.length > 0 ? (
                        <Image
                          source={{ uri: pet.myPicturesURLs[0] }}
                          style={styles.petImage}
                        />
                      ) : (
                        <Image
                          source={defaultPetImage}
                          style={styles.petImage}
                        />
                      )}
                      {value === pet.petId && (
                        <View style={styles.petSelectedOverlay}>
                          <View style={styles.petSelectedIndicator}>
                            <View style={styles.petSelectedDot} />
                          </View>
                        </View>
                      )}
                    </View>
                    <Text style={styles.petName} numberOfLines={1}>
                      {pet.name}
                    </Text>
                  </TouchableOpacity>
                ))}
                
                {/* Add New Pet Button */}
                <TouchableOpacity
                  style={styles.petItem}
                  onPress={handleAddNewPet}
                >
                  <View style={styles.addPetWrapper}>
                    <FontAwesome5 name="plus" size={24} color="#9188E5" />
                  </View>
                  <Text style={styles.petName} numberOfLines={1}>
                    Add Pet
                  </Text>
                </TouchableOpacity>
              </ScrollView>
            )}
          />
          {errors.petId && (
            <Text style={styles.errorText}>{errors.petId.message}</Text>
          )}
        </View>

        {/* Selected Pet Details */}
        {selectedPet && (
          <View style={styles.selectedPetCard}>
            <View style={styles.selectedPetContent}>
              <Image
                source={
                  selectedPet.myPicturesURLs?.[0] 
                    ? { uri: selectedPet.myPicturesURLs[0] }
                    : defaultPetImage
                }
                style={styles.selectedPetImage}
              />
              <View style={styles.selectedPetInfo}>
                <Text style={styles.selectedPetName}>{selectedPet.name}</Text>
                {selectedPet.breed && (
                  <Text style={styles.selectedPetBreed}>{selectedPet.breed}</Text>
                )}
                {selectedPet.age && (
                  <View style={styles.selectedPetBadge}>
                    <Text style={styles.selectedPetBadgeText}>{selectedPet.age}</Text>
                  </View>
                )}
              </View>
            </View>
          </View>
        )}

        <View style={styles.separator} />

        {/* Location */}
        <View style={styles.section}>
          <Text style={styles.label}>Location</Text>
          <Controller
            control={control}
            name="location"
            rules={{ required: 'Location is required.' }}
            render={({ field: { value, onChange } }) => (
              <View style={styles.inputWrapper}>
                <Text style={styles.inputIcon}><Ionicons name="location-sharp" size={24} color="#9188E5" /></Text>
                <TextInput
                  style={styles.textInput}
                  placeholder="State, Country"
                  value={value}
                  onChangeText={onChange}
                  placeholderTextColor="#999"
                />
              </View>
            )}
          />
          {errors.location && (
            <Text style={styles.errorText}>{errors.location.message}</Text>
          )}
        </View>

        {/* Description */}
        <View style={styles.section}>
          <Text style={styles.label}>Description</Text>
          <Controller
            control={control}
            name="description"
            rules={{ maxLength: { value: 500, message: 'Max 500 characters.' } }}
            render={({ field: { value, onChange } }) => (
              <View>
                <TextInput
                  style={styles.textArea}
                  placeholder={getPlaceholderText()}
                  value={value}
                  onChangeText={onChange}
                  multiline
                  numberOfLines={6}
                  maxLength={500}
                  textAlignVertical="top"
                  placeholderTextColor="#999"
                />
                <View style={styles.charCountWrapper}>
                  <Text style={styles.charCount}>
                    {(descriptionValue?.length || 0)}/500
                  </Text>
                </View>
              </View>
            )}
          />
          {errors.description && (
            <Text style={styles.errorText}>{errors.description.message}</Text>
          )}
        </View>

        {/* Bottom padding for fixed button */}
        <View style={styles.bottomPadding} />
      </ScrollView>

      {/* Bottom Action Bar */}
      <View style={styles.bottomBar}>
        <TouchableOpacity
          style={[
            styles.postButton,
            (!isFormValid || isPending) && { opacity: 0.6 }
          ]}
          onPress={handleSubmit(onSubmit)}
          disabled={!isFormValid || isPending}
          activeOpacity={0.8}
        >
          {isPending ? (
            <LottieView
              source={require("@/assets/lottie/loading.json")}
              autoPlay
              loop
              style={styles.lottie}
            />
          ) : (
            <>
              <FontAwesome5
                name="telegram-plane"
                size={24}
                color="white"
                style={styles.postButtonIcon}
              />
              <Text style={styles.postButtonText}>Post</Text>
            </>
          )}
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  lottie: {
    width: 70,
    height: 70,
  },
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e5e5',
    backgroundColor: '#fff',
  },
  headerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  headerButton: {
    padding: 8,
  },
  headerButtonText: {
    fontSize: 18,
    color: '#333',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginLeft: 12,
    color: '#000',
  },
  scrollContent: {
    paddingHorizontal: 16,
    paddingVertical: 24,
  },
  section: {
    marginBottom: 24,
  },
  label: {
    fontSize: 16,
    fontWeight: '500',
    marginBottom: 16,
    color: '#000',
  },
  separator: {
    height: 1,
    backgroundColor: '#e5e5e5',
    marginVertical: 8,
  },
  typeButtons: {
    flexDirection: 'row',
    gap: 16,
  },
  typeButton: {
    flex: 1,
    height: 80,
    borderWidth: 1,
    borderColor: 'rgba(145, 136, 229, 0.2)',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
    gap: 8,
  },
  typeButtonSelected: {
    backgroundColor: '#9188E5',
    borderColor: '#9188E5',
  },
  typeButtonText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#333',
  },
  typeButtonTextSelected: {
    color: '#fff',
  },
  petList: {
    paddingVertical: 8,
  },
  petItem: {
    alignItems: 'center',
    marginRight: 16,
    width: 64,
  },
  petImageWrapper: {
    width: 64,
    height: 64,
    borderRadius: 32,
    borderWidth: 3,
    borderColor: '#e5e5e5',
    overflow: 'hidden',
    position: 'relative',
  },
  petImageWrapperSelected: {
    borderColor: '#9188E5',
    shadowColor: '#9188E5',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 8,
    elevation: 5,
  },
  petImage: {
    width: '100%',
    height: '100%',
  },
  petPlaceholder: {
    width: '100%',
    height: '100%',
    backgroundColor: '#e5e5e5',
  },
  petSelectedOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(145, 136, 229, 0.2)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  petSelectedIndicator: {
    width: 16,
    height: 16,
    backgroundColor: '#9188E5',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  petSelectedDot: {
    width: 8,
    height: 8,
    backgroundColor: '#fff',
    borderRadius: 4,
  },
  addPetWrapper: {
    width: 64,
    height: 64,
    borderRadius: 32,
    borderWidth: 2,
    borderColor: '#9188E5',
    borderStyle: 'dashed',
    backgroundColor: 'rgba(145, 136, 229, 0.05)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  petName: {
    fontSize: 12,
    fontWeight: '500',
    marginTop: 8,
    textAlign: 'center',
    color: '#000',
  },
  selectedPetCard: {
    backgroundColor: 'rgba(145, 136, 229, 0.05)',
    borderWidth: 1,
    borderColor: 'rgba(145, 136, 229, 0.2)',
    borderRadius: 12,
    padding: 16,
    marginBottom: 24,
  },
  selectedPetContent: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  selectedPetImage: {
    width: 64,
    height: 64,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: 'rgba(145, 136, 229, 0.2)',
  },
  selectedPetInfo: {
    flex: 1,
    marginLeft: 16,
  },
  selectedPetName: {
    fontSize: 18,
    fontWeight: '600',
    color: '#9188E5',
    marginBottom: 4,
  },
  selectedPetBreed: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
  },
  selectedPetBadge: {
    backgroundColor: 'rgba(145, 136, 229, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(145, 136, 229, 0.2)',
    borderRadius: 12,
    paddingHorizontal: 8,
    paddingVertical: 4,
    alignSelf: 'flex-start',
  },
  selectedPetBadgeText: {
    fontSize: 12,
    color: '#9188E5',
    fontWeight: '500',
  },
  inputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(145, 136, 229, 0.2)',
    borderRadius: 8,
    backgroundColor: '#fff',
    height: 48,
  },
  inputIcon: {
    fontSize: 20,
    marginLeft: 12,
    color: '#9188E5',
  },
  textInput: {
    flex: 1,
    paddingHorizontal: 12,
    fontSize: 16,
    color: '#000',
  },
  textArea: {
    borderWidth: 1,
    borderColor: 'rgba(145, 136, 229, 0.2)',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    color: '#000',
    backgroundColor: '#fff',
    minHeight: 120,
    textAlignVertical: 'top',
  },
  charCountWrapper: {
    alignItems: 'flex-end',
    marginTop: 8,
  },
  charCount: {
    fontSize: 14,
    color: '#666',
  },
  errorText: {
    fontSize: 14,
    color: '#ef4444',
    marginTop: 4,
  },
  bottomPadding: {
    height: 80,
  },
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#e5e5e5',
    padding: 16,
  },
  postButton: {
    backgroundColor: '#9188E5',
    borderRadius: 8,
    height: 48,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: 8,
  },
  postButtonIcon: {
    fontSize: 16,
  },
  postButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});