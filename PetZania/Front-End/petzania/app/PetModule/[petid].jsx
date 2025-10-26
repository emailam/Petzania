import {
    View, Text, StyleSheet, ScrollView, Pressable,
    Alert, Platform, ActivityIndicator,
    KeyboardAvoidingView, TouchableOpacity,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Image } from 'expo-image';
import { useContext, useEffect, useState, useRef } from 'react';
import { Dropdown } from 'react-native-element-dropdown';
import * as ImagePicker from 'expo-image-picker';
import * as DocumentPicker from 'expo-document-picker';
import * as Linking from 'expo-linking';
import { AntDesign, MaterialIcons } from '@expo/vector-icons';
import Feather from '@expo/vector-icons/Feather';
import ImageViewing from 'react-native-image-viewing';

import { PetContext } from '@/context/PetContext';
import { UserContext } from '@/context/UserContext';
import { uploadFiles } from '@/services/uploadService';

import DateOfBirthInput from '@/components/DateOfBirthInput';
import CustomInput from '@/components/CustomInput';
import ImageUploadInput from '@/components/ImageUploadInput';
import BottomSheet from '@/components/BottomSheet';
import { TextInput } from 'react-native-paper';

import { PETS } from '@/constants/PETS';
import { PET_BREEDS } from '@/constants/PETBREEDS';
import { updatePet, deletePet, getPetById } from '@/services/petService';
import Toast from 'react-native-toast-message';
import Button from '@/components/Button';

export default function PetDetails() {
    const { petId, petData } = useLocalSearchParams();
    const { pets, setPets } = useContext(PetContext);
    const { user: currentUser } = useContext(UserContext);

    const bottomSheetRef = useRef(null);

    const petIndex = pets.findIndex((pet) => pet.petId === petId);

    // Safely parse pet data with error handling
    const parsedPetData = (() => {
        if (!petData) return null;
        try {
            return JSON.parse(petData);
        } catch (error) {
            console.error('Error parsing petData:', error);
            return null;
        }
    })();

    const [pet, setPet] = useState(() => pets[petIndex] || parsedPetData || null);

    const showSuccessMessage = (message) => {
        Toast.show({
            type: 'success',
            text1: message,
            position: 'top',
            visibilityTime: 3000,
            swipeable: true,
        });
    }

    const showErrorMessage = (message, description = '') => {
        Toast.show({
            type: 'error',
            text1: message,
            text2: description,
            position: 'top',
            visibilityTime: 3000,
            swipeable: true,
        });
    }

    const router = useRouter();
    const [petLoading, setPetLoading] = useState(!pet);

    // Check if the current user owns this pet
    const isOwner = currentUser?.userId === pet?.userId;

    // Tab navigation state
    const [activeTab, setActiveTab] = useState('info');
    
    // Photo management state
    const [images, setImages] = useState(pet?.myPicturesURLs || []);
    const [selectedImageIndex, setSelectedImageIndex] = useState(null);
    const [uploadingImages, setUploadingImages] = useState(false);
    const [showImageViewer, setShowImageViewer] = useState(false);
    const [viewerIndex, setViewerIndex] = useState(0);

    const [gender, setGender] = useState(pet?.gender || '');
    const [species, setSpecies] = useState(pet?.species || '');
    const [breed, setBreed] = useState(pet?.breed || '');
    const [dateOfBirth, setDateOfBirth] = useState(pet?.dateOfBirth || '');

    const [vaccineFiles, setVaccineFiles] = useState(pet?.myVaccinesURLs || []);

    const [isLoading, setIsLoading] = useState(false);
    const [isFormValid, setIsFormValid] = useState(true);
    const [errors, setErrors] = useState({
        name: '',
        species: '',
        gender: '',
        breed: '',
        dateOfBirth: '',
        description: '',
    });

    // Helper functions for error handling
    const setError = (field, message) => {
        setErrors(prev => ({
            ...prev,
            [field]: message
        }));
    };

    const clearError = (field) => {
        setErrors(prev => ({
            ...prev,
            [field]: ''
        }));
    };

    const genderOptions = [
        { label: 'Male', value: 'MALE' },
        { label: 'Female', value: 'FEMALE' },
    ];
    const speciesOptions = PETS.map(p => ({ label: p.name, value: p.value }));
    const breedOptions = species
        ? (PET_BREEDS[species]?.map(b => ({ label: b.name, value: b.name })) || [])
        : [];

    // Fetch pet data if not found in context
    useEffect(() => {
        const fetchPetData = async () => {
            if (!pet && petId) {
                console.log('Pet not found in context, fetching pet with ID:', petId);
                setPetLoading(true);
                try {
                    const petData = await getPetById(petId);
                    console.log('Fetched pet data:', petData);
                    setPet(petData);
                } catch (error) {
                    console.error('Error fetching pet:', error);
                    showErrorMessage('Pet not found', 'Unable to load pet details');
                    router.back();
                } finally {
                    setPetLoading(false);
                }
            }
        };

        fetchPetData();
    }, [petId, pet]);

    useEffect(() => {
        if (!pet && !petLoading) {
            Alert.alert('Pet not found', 'Returning to previous page.');
            router.back();
        }
    }, [pet, petLoading]);

    // Sync images when pet data changes
    useEffect(() => {
        if (pet?.myPicturesURLs && Array.isArray(pet.myPicturesURLs)) {
            setImages(pet.myPicturesURLs);
        }
    }, [pet?.myPicturesURLs]);

    // Sync form data when pet data changes
    useEffect(() => {
        if (pet) {
            setGender(pet.gender || '');
            setSpecies(pet.species || '');
            setBreed(pet.breed || '');
            setDateOfBirth(pet.dateOfBirth || '');
            setVaccineFiles(pet.myVaccinesURLs || []);
        }
    }, [pet]);

    // Validation effect
    useEffect(() => {
        if (isOwner) {
            if (!pet?.name?.trim()) {
                setError('name', 'Name is required');
            } else {
                clearError('name');
            }

            if (!species?.trim()) {
                setError('species', 'Type is required');
            } else {
                clearError('species');
            }

            if (!breed?.trim()) {
                setError('breed', 'Breed is required');
            } else {
                clearError('breed');
            }

            if (!dateOfBirth?.trim()) {
                setError('dateOfBirth', 'Date of birth is required');
            } else {
                clearError('dateOfBirth');
            }
        } else {
            clearError('name');
            clearError('species');
            clearError('breed');
            clearError('dateOfBirth');
        }
    }, [pet?.name, species, breed, dateOfBirth, isOwner]);

    // Check form validity
    useEffect(() => {
        const hasErrors = Object.values(errors).some(error => error !== '');
        setIsFormValid(!hasErrors);
    }, [errors]);

    const handleInputChange = (key, value) => {
        setPet((prev) => prev ? { ...prev, [key]: value } : null);
        setErrors((prev) => ({ ...prev, [key]: '' }));
    };

    const pickImage = async (replaceIndex = null) => {
        if (replaceIndex === 0 && images.length >= 1) {
            // Replace the profile picture (first image)
            setUploadingImages(true);
            try {
                let result = await ImagePicker.launchImageLibraryAsync({
                    mediaTypes: ['images'],
                    allowsMultipleSelection: false,
                    quality: 0.7,
                    aspect: [1, 1],
                    selectionLimit: 1,
                });
                if (!result.canceled) {
                    const uri = result.assets[0].uri;
                    const newImages = [...images];
                    newImages[0] = uri;
                    setImages(newImages);
                }
            } catch (error) {
                showErrorMessage('Failed to pick image', 'Please try again');
            } finally {
                setUploadingImages(false);
            }
            return;
        }

        if (images.length >= 6) {
            showErrorMessage('Photo limit reached', 'You can add up to 6 photos per pet');
            return;
        }

        setUploadingImages(true);
        try {
            const remainingSlots = 6 - images.length;
            let result = await ImagePicker.launchImageLibraryAsync({
                mediaTypes: ['images'],
                allowsMultipleSelection: true,
                quality: 0.7,
                aspect: [1, 1],
                selectionLimit: remainingSlots,
            });
            
            if (!result.canceled) {
                const uris = result.assets.map(asset => asset.uri);
                const newImages = [...images, ...uris];
                setImages(newImages);
                
                if (newImages.length === 6) {
                    showSuccessMessage('Photo limit reached');
                }
            }
        } catch (error) {
            showErrorMessage('Failed to pick images', 'Please try again');
        } finally {
            setUploadingImages(false);
        }
    };

    const deleteImage = (uriToDelete) => {
        const updatedImages = images.filter(uri => uri !== uriToDelete);
        setImages(updatedImages);
    };
    
    const uploadAndSaveImages = async () => {
        if (images.length === 0) return [];

        try {
            setUploadingImages(true);

            const localImages = images.filter(isLocalImage);
            const serverImages = images.filter(isServerImage);

            let uploadedUrls = [];

            if (localImages.length > 0) {
                const files = localImages.map(image => ({
                    uri: image,
                    name: image.split('/').pop() || 'pet-image.jpg',
                    type: 'image/jpeg',
                }));

                uploadedUrls = await uploadFiles(files);
            }
            
            const finalImages = [];
            let uploadIndex = 0;
            
            for (const originalImage of images) {
                if (isLocalImage(originalImage)) {
                    if (uploadedUrls[uploadIndex]) {
                        finalImages.push(uploadedUrls[uploadIndex]);
                        uploadIndex++;
                    }
                } else if (isServerImage(originalImage)) {
                    finalImages.push(originalImage);
                }
            }
            return finalImages;
        } catch (error) {
            showErrorMessage('Failed to upload images', 'Please try again');
            return images.filter(isServerImage);
        } finally {
            setUploadingImages(false);
        }
    };

    const handleFilePick = async () => {
        try {
            setIsLoading(true);
            const result = await DocumentPicker.getDocumentAsync({
                type: ['application/pdf'],
                copyToCacheDirectory: true,
                multiple: true,
            });

            if (result?.assets?.length) {
                const newFiles = result.assets.map(file => ({
                    uri: file.uri,
                    name: file.name,
                }));

                const updatedVaccines = [...vaccineFiles, ...newFiles];
                setVaccineFiles(updatedVaccines);
                handleInputChange('myVaccinesURLs', updatedVaccines);
            }
        } catch (err) {
            console.error('Picker Error:', err);
        } finally {
            setIsLoading(false);
        }
    };

    const deleteFile = (uriToDelete) => {
        const updated = vaccineFiles.filter(file => (file.uri || file) !== uriToDelete);
        setVaccineFiles(updated);
        handleInputChange('myVaccinesURLs', updated);
    };

    const openPDF = async (pdfUri) => {
        try {
            if (pdfUri) {
                await Linking.openURL(pdfUri);
            }
        } catch (error) {
            showErrorMessage('Cannot open PDF', 'Unable to open the document');
        }
    };

    const handleSaveChanges = async () => {
        if (!pet) return;

        const newErrors = {
            name: !pet.name?.trim() ? 'Name is required' : '',
            species: !species?.trim() ? 'Type is required' : '',
            gender: !gender?.trim() ? 'Gender is required' : '',
            breed: !breed?.trim() ? 'Breed is required' : '',
            dateOfBirth: !dateOfBirth?.trim() ? 'Date of birth is required' : '',
        };

        setErrors(newErrors);

        if (Object.values(newErrors).some(error => error)) return;

        try {
            setIsLoading(true);

            const uploadedImageUrls = await uploadAndSaveImages();

            let uploadedVaccineUrls = [];
            if (vaccineFiles.length > 0) {
                const localVaccineFiles = vaccineFiles.filter(file => file.uri && file.uri.startsWith('file://'));
                const serverVaccineFiles = vaccineFiles.filter(file => !file.uri || file.uri.startsWith('http'));

                if (localVaccineFiles.length > 0) {
                    const files = localVaccineFiles.map(file => ({
                        uri: file.uri,
                        name: file.name,
                        type: 'application/pdf',
                    }));

                    const uploadedUrls = await uploadFiles(files);
                    uploadedVaccineUrls = [...serverVaccineFiles.map(f => f.uri || f), ...uploadedUrls];
                } else {
                    uploadedVaccineUrls = serverVaccineFiles.map(f => f.uri || f);
                }
            }

            const petData = {
                name: pet.name || undefined,
                description: pet.description || 0,
                gender: gender || undefined,
                dateOfBirth: dateOfBirth || undefined,
                breed: breed || undefined,
                species: species ? species.toUpperCase() : undefined,
                myVaccinesURLs: uploadedVaccineUrls,
                myPicturesURLs: uploadedImageUrls.length > 0 ? uploadedImageUrls : (Array.isArray(pet.myPicturesURLs) ? pet.myPicturesURLs : []),
            };

            await updatePet(petId, petData);
            setPets(prevPets => {
                const updatedPets = prevPets.map(p =>
                    p.petId === petId ? { ...p, ...petData } : p
                );
                if (currentUser) currentUser.myPets = updatedPets;
                return updatedPets;
            });

            showSuccessMessage('Pet updated successfully!');
            router.back();
        } catch (error) {
            showErrorMessage('Failed to update pet', error);
        } finally {
            setIsLoading(false);
        }
    };

    const deletePetByPetId = () => {
        setIsLoading(true);
        deletePet(petId)
            .then(() => {
                setPets(prevPets => prevPets.filter(pet => pet.petId !== petId));
                if (currentUser) {
                    currentUser.myPets = currentUser.myPets.filter(pet => pet.petId !== petId);
                }
                showSuccessMessage('Pet deleted successfully!');
                router.back();
            })
            .catch((error) => {
                showErrorMessage('Failed to delete pet.', 'Please try again later.');
            })
            .finally(() => setIsLoading(false));
    };

    const handleDeletePet = () => {
        Alert.alert(
            'Delete Pet',
            `Are you sure you want to delete ${pet?.name}?`,
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Delete',
                    style: 'destructive',
                    onPress: deletePetByPetId,
                },
            ]
        );
    };

    // BottomSheet handlers
    const handleImagePress = (imageIndex) => {
        setSelectedImageIndex(imageIndex);
        bottomSheetRef.current?.present();
    };

    const handleBottomSheetAction = (action) => {
        bottomSheetRef.current?.dismiss();
        setTimeout(() => {
            const index = selectedImageIndex;
            switch (action) {
                case 'viewPhoto':
                    setViewerIndex(index);
                    setShowImageViewer(true);
                    break;
                case 'makeProfile':
                    const newImages = [...images];
                    const [selected] = newImages.splice(index, 1);
                    newImages.unshift(selected);
                    setImages(newImages);
                    break;
                case 'addMore':
                    pickImage();
                    break;
                case 'delete':
                    deleteImage(images[index]);
                    break;
            }
        }, 300);
    };

    const defaultImage = require('@/assets/images/Defaults/default-pet.png');

    const renderTabContent = () => {
        if (activeTab === 'info') {
            return (
                <View style={styles.inputsContainer}>
                    {/* Name */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Name{isOwner && <Text style={{ color: 'red' }}>*</Text>}</Text>
                        <CustomInput
                            style={[
                                errors.name ? styles.inputError : null,
                                !isOwner && styles.readOnlyInput
                            ]}
                            error={!!errors.name}
                            maxLength={50}
                            placeholder="Pet's name"
                            placeholderTextColor="#999"
                            value={pet?.name || ''}
                            onChangeText={isOwner ? (text) => handleInputChange('name', text) : undefined}
                            editable={isOwner}
                        />
                        {errors.name && isOwner && <Text style={styles.errorText}>{errors.name}</Text>}
                    </View>
                    {/* Species */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Type{isOwner && <Text style={{ color: 'red' }}>*</Text>}</Text>
                        {isOwner ? (
                            <Dropdown
                                style={[styles.dropdownInput, errors.species && styles.inputError]}
                                placeholder="Select Type"
                                placeholderTextColor="#999"
                                data={speciesOptions}
                                labelField="label"
                                valueField="value"
                                value={species}
                                onChange={(item) => {
                                    setSpecies(item.value);
                                    setBreed('');
                                    handleInputChange('species', item.value);
                                    clearError('species');
                                }}
                            />
                        ) : (
                            <CustomInput
                                style={[styles.input, styles.readOnlyInput]}
                                value={species ? (speciesOptions.find(s => s.value === species)?.label || species) : 'Not specified'}
                                editable={false}
                            />
                        )}
                        {errors.species && isOwner && <Text style={styles.errorText}>{errors.species}</Text>}
                    </View>
                    {/* Breed */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>
                            Breed{isOwner && <Text style={{ color: 'red' }}>*</Text>}
                        </Text>
                        <CustomInput
                            style={[
                                errors.breed ? styles.inputError : null,
                                !isOwner && styles.readOnlyInput
                            ]}
                            error={!!errors.breed}
                            maxLength={32}
                            placeholder={isOwner ? "Enter breed" : "Not specified"}
                            placeholderTextColor="#999"
                            value={breed || (isOwner ? '' : 'Not specified')}
                            onChangeText={isOwner ? (text) => {
                                setBreed(text);
                                handleInputChange('breed', text);
                                clearError('breed');
                            } : undefined}
                            editable={isOwner}
                        />
                        {errors.breed && isOwner && <Text style={styles.errorText}>{errors.breed}</Text>}
                    </View>
                    {/* Gender */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Gender{isOwner && <Text style={{ color: 'red' }}>*</Text>}</Text>
                        {isOwner ? (
                            <Dropdown
                                style={[styles.dropdownInput, errors.gender && styles.inputError]}
                                placeholder="Select Gender"
                                placeholderTextColor="#999"
                                data={genderOptions}
                                labelField="label"
                                valueField="value"
                                value={gender}
                                onChange={(item) => {
                                    setGender(item.value);
                                    handleInputChange('gender', item.value);
                                    clearError('gender');
                                }}
                            />
                        ) : (
                            <CustomInput
                                style={[styles.readOnlyInput]}
                                value={gender ? (genderOptions.find(g => g.value === gender)?.label || gender) : 'Not specified'}
                                editable={false}
                            />
                        )}
                        {errors.gender && isOwner && <Text style={styles.errorText}>{errors.gender}</Text>}
                    </View>
                    {/* Description */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Description</Text>
                        <CustomInput
                            style={[styles.descriptionInput,
                                !isOwner && styles.readOnlyInput
                            ]}
                            placeholder="Pet's description"
                            value={pet?.description || ''}
                            onChangeText={isOwner ? (text) => handleInputChange('description', text) : undefined}
                            multiline
                            error={!!errors.description}
                            numberOfLines={4}
                            editable={isOwner}
                            maxLength={255}
                            showsVerticalScrollIndicator={true}
                            blurOnSubmit={false}
                            returnKeyType="default"
                            right={<TextInput.Affix text={`${pet?.description?.length || 0}/255`} />}
                        />
                    </View>
                    {/* Date of Birth */}
                    <View style={styles.inputContainer}>
                        {isOwner ? (
                            <DateOfBirthInput
                                value={dateOfBirth}
                                onChange={(date) => {
                                    setDateOfBirth(date);
                                    handleInputChange('dateOfBirth', date);
                                    clearError('dateOfBirth');
                                }}
                                errorMessage={errors.dateOfBirth}
                            />
                        ) : (
                            <>
                                <Text style={styles.label}>Date of Birth</Text>
                                <CustomInput
                                    style={[styles.readOnlyInput]}
                                    value={dateOfBirth || 'Not specified'}
                                    editable={false}
                                />
                            </>
                        )}
                    </View>
                </View>
            );
        } else if (activeTab === 'photos') {
            return (
                <View style={styles.inputsContainerFull}>
                    <Text style={styles.label}>Pet Photos</Text>
                    {images.length > 0 ? (
                        <View style={styles.photoGalleryContainer}>
                            <View style={styles.photoGrid}>
                                {images.map((uri, index) => {
                                    const isValidUri = isValidImageUri(uri);
                                    return (
                                        <Pressable
                                            key={`photo-${index}`}
                                            style={[
                                                styles.photoGridItem,
                                                index === 0 && styles.mainPhotoGridItem
                                            ]}
                                            onPress={() => {
                                                if (isOwner) {
                                                    handleImagePress(index);
                                                } else {
                                                    setViewerIndex(index);
                                                    setShowImageViewer(true);
                                                }
                                            }}
                                        >
                                            <Image
                                                source={isValidUri ? { uri } : defaultImage}
                                                style={[
                                                    styles.photoGridImage,
                                                    index === 0 && styles.mainPhotoGridImage
                                                ]}
                                                placeholder={defaultImage}
                                                transition={200}
                                            />
                                            {index === 0 && (
                                                <View style={styles.mainPhotoLabel}>
                                                    <Text style={styles.mainPhotoLabelText}>Profile</Text>
                                                </View>
                                            )}
                                        </Pressable>
                                    );
                                })}
                                {isOwner && images.length < 6 && (
                                    <Pressable
                                        style={styles.addPhotoGridItem}
                                        onPress={pickImage}
                                        disabled={uploadingImages || isLoading}
                                    >
                                        <AntDesign name="plus" size={30} color="#9188E5" />
                                        <Text style={styles.addPhotoGridText}>Add Photo</Text>
                                    </Pressable>
                                )}
                            </View>
                        </View>
                    ) : (
                        <View style={styles.emptyPhotoContainer}>
                            <Image source={defaultImage} style={styles.emptyImage} />
                            <Text style={styles.emptyStateText}>No photos added yet</Text>
                            {isOwner && (
                                <Pressable
                                    style={styles.addFirstPhotoButton}
                                    onPress={pickImage}
                                    disabled={uploadingImages || isLoading}
                                >
                                    <AntDesign name="camera" size={20} color="white" />
                                    <Text style={styles.addFirstPhotoButtonText}>Add First Photo</Text>
                                </Pressable>
                            )}
                        </View>
                    )}
                </View>
            );
        } else {
            return (
                <View style={styles.inputsContainerFull}>
                    <Text style={styles.label}>Pet Vaccines</Text>
                    {isOwner && (
                        <Pressable
                            style={styles.vaccinesInput}
                            onPress={handleFilePick}
                            disabled={isLoading}
                        >
                            <Image source={require('@/assets/images/AddPet/Vaccines.png')} style={[styles.image, {marginRight: 0, width: 48, height: 48, borderRadius: 12}]} />
                            <View style={styles.vaccineInputTextContainer}>
                                <Text style={styles.vaccineInputTitle}>Add pet vaccines</Text>
                                <Text style={styles.vaccineInputSubtitle}>Upload PDF files of your pet's vaccines.</Text>
                            </View>
                            <View style={styles.rightContainer}>
                                {isLoading
                                    ? <ActivityIndicator size="small" color="#9188E5" />
                                    : <Feather name="file-plus" size={28} color="#9188E5" />
                                }
                            </View>
                        </Pressable>
                    )}

                    {vaccineFiles.map((item, index) => (
                        <Pressable
                            key={index}
                            style={styles.uploadedFile}
                            onPress={() => openPDF(item.uri || item)}
                        >
                            <View style={styles.leftContainer}>
                                <Image source={require('@/assets/images/AddPet/PDF.png')} style={styles.image} />
                                <Text style={styles.vaccineLabel}>{item.name || `Document ${index + 1}`}</Text>
                            </View>
                            {isOwner && (
                                <Pressable
                                    style={styles.rightContainer}
                                    onPress={(e) => {
                                        e.stopPropagation();
                                        deleteFile(item.uri || item);
                                    }}
                                    disabled={isLoading}
                                >
                                    <AntDesign name="delete" size={22} color="#C70000" />
                                </Pressable>
                            )}
                        </Pressable>
                    ))}

                    {!isOwner && vaccineFiles.length === 0 && (
                        <View style={styles.emptyPhotoContainer}>
                            <Image source={require('@/assets/images/AddPet/Vaccines.png')} style={styles.emptyImage} />
                            <Text style={styles.emptyStateText}>No vaccines added yet</Text>
                        </View>
                    )}
                </View>
            );
        }
    };

    const isLocalImage = (uri) => {
        return uri && typeof uri === 'string' && (uri.startsWith('file://') || uri.startsWith('content://'));
    };

    const isServerImage = (uri) => {
        return uri && typeof uri === 'string' && uri.startsWith('http');
    };

    const isValidImageUri = (uri) => {
        return isLocalImage(uri) || isServerImage(uri);
    };

    if (petLoading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#9188E5" />
                <Text style={styles.loadingText}>Loading pet details...</Text>
            </View>
        );
    }

    if (!pet) return null;

    return (
        <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            style={{ flex: 1 }}
            keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
        >
            <ScrollView 
                contentContainerStyle={styles.container}
                keyboardShouldPersistTaps="handled"
                showsVerticalScrollIndicator={true}
                keyboardDismissMode="interactive"
            >
                {/* Profile Picture Section */}
                <View style={styles.profileSection}>
                    {isOwner ? (
                        <ImageUploadInput
                            defaultImage={defaultImage}
                            currentImage={images[0] || null}
                            onImageChange={(imageData) => {
                                if (imageData) {
                                    const newImages = Array.isArray(imageData) ? imageData : [imageData];
                                    // If there are existing images, replace the first one, otherwise set new images
                                    const updatedImages = images.length > 0 
                                        ? [newImages[0], ...images.slice(1)]
                                        : newImages;
                                    setImages(updatedImages);
                                } else {
                                    // Remove image if null
                                    const updatedImages = images.slice(1);
                                    setImages(updatedImages);
                                }
                            }}
                            size={160}
                            isUploading={uploadingImages}
                            allowsMultipleSelection={true}
                        />
                    ) : (
                        <Pressable onPress={() => {
                            if (images.length > 0) {
                                setViewerIndex(0);
                                setShowImageViewer(true);
                            }
                        }}>
                            <Image
                                source={images[0] ? { uri: images[0] } : defaultImage}
                                style={styles.profileImage}
                                placeholder={defaultImage}
                                transition={200}
                            />
                        </Pressable>
                    )}

                    <Text style={styles.profileImageText}>
                        {isOwner
                            ? (images.length > 0 ? 'Tap to manage profile picture' : 'Tap to add profile picture')
                            : (images.length > 0 ? 'Tap to view profile picture' : 'No profile picture')
                        }
                    </Text>
                </View>

                {/* ImageViewing Component */}
                <ImageViewing
                    images={images.map(uri => ({ uri }))}
                    imageIndex={viewerIndex}
                    visible={showImageViewer}
                    onRequestClose={() => setShowImageViewer(false)}
                    backgroundColor="black"
                    swipeToCloseEnabled
                    doubleTapToZoomEnabled
                />

                {/* Tab Navigation */}
                <View style={styles.tabContainer}>
                    <Pressable
                        style={[styles.tab, activeTab === 'info' && styles.activeTab]}
                        onPress={() => setActiveTab('info')}
                    >
                        <Text style={[styles.tabText, activeTab === 'info' && styles.activeTabText]}>
                            Pet Info
                        </Text>
                    </Pressable>
                    <Pressable
                        style={[styles.tab, activeTab === 'photos' && styles.activeTab]}
                        onPress={() => setActiveTab('photos')}
                    >
                        <Text style={[styles.tabText, activeTab === 'photos' && styles.activeTabText]}>
                            Photos
                        </Text>
                    </Pressable>
                    <Pressable
                        style={[styles.tab, activeTab === 'vaccines' && styles.activeTab]}
                        onPress={() => setActiveTab('vaccines')}
                    >
                        <Text style={[styles.tabText, activeTab === 'vaccines' && styles.activeTabText]}>
                            Vaccines
                        </Text>
                    </Pressable>
                </View>

                {/* Tab Content */}
                {renderTabContent()}

                {/* Action Buttons - Only show for pet owner */}
                {isOwner && (
                    <View style={styles.buttonsContainer}>
                        <Button
                            title="Save Changes"
                            style={styles.saveButton}
                            onPress={handleSaveChanges}
                            loading={isLoading}
                            disabled={isLoading || uploadingImages || !isFormValid}
                        >
                            <Text style={styles.buttonText}>Save Changes</Text>
                        </Button>
                        <Button
                            title="Delete Pet"
                            style={styles.deleteButton}
                            onPress={handleDeletePet}
                            loading={isLoading}
                            disabled={isLoading || uploadingImages}
                        >
                            <Text style={styles.buttonText}>Delete Pet</Text>
                        </Button>
                    </View>
                )}

                {(isLoading || uploadingImages) && (
                    <View style={styles.loadingOverlay}>
                        <ActivityIndicator size="large" color="#9188E5" />
                        <Text style={styles.loadingText}>
                            {uploadingImages ? 'Uploading images...' : 'Saving changes...'}
                        </Text>
                    </View>
                )}
            </ScrollView>

            {/* BottomSheet for gallery image actions */}
            <BottomSheet
                ref={bottomSheetRef}
                snapPoints={selectedImageIndex === 0 ? ['40%'] : ['50%']}
            >
                <View style={styles.bottomSheetHeader}>
                    <Text style={styles.bottomSheetTitle}>Photo Actions</Text>
                </View>

                <View style={styles.bottomSheetActions}>
                    <TouchableOpacity
                        style={styles.bottomSheetAction}
                        onPress={() => handleBottomSheetAction('viewPhoto')}
                    >
                        <View style={styles.actionIconContainer}>
                            <MaterialIcons name="photo" size={24} color="#9188E5" />
                        </View>
                        <Text style={styles.actionText}>View Photo</Text>
                    </TouchableOpacity>

                    {selectedImageIndex !== 0 && (
                        <TouchableOpacity
                            style={styles.bottomSheetAction}
                            onPress={() => handleBottomSheetAction('makeProfile')}
                        >
                            <View style={styles.actionIconContainer}>
                                <MaterialIcons name="star" size={24} color="#9188E5" />
                            </View>
                            <Text style={styles.actionText}>Make Profile Picture</Text>
                        </TouchableOpacity>
                    )}

                    <TouchableOpacity
                        style={styles.bottomSheetAction}
                        onPress={() => handleBottomSheetAction('addMore')}
                    >
                        <View style={styles.actionIconContainer}>
                            <MaterialIcons name="add-photo-alternate" size={24} color="#9188E5" />
                        </View>
                        <Text style={styles.actionText}>Add More Photos</Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                        style={styles.bottomSheetAction}
                        onPress={() => handleBottomSheetAction('delete')}
                    >
                        <View style={styles.actionIconContainer}>
                            <MaterialIcons name="delete-outline" size={24} color="#FF3B30" />
                        </View>
                        <Text style={[styles.actionText, styles.deleteText]}>Delete Photo</Text>
                    </TouchableOpacity>
                </View>
            </BottomSheet>
        </KeyboardAvoidingView>
    );
};

const styles = StyleSheet.create({
    container: {
        backgroundColor: '#fff',
        paddingTop: 20,
        flexGrow: 1,
    },
    // Profile Section Styles
    profileSection: {
        alignItems: 'center',
        marginBottom: 20,
    },
    profileImage: {
        width: 160,
        height: 160,
        borderRadius: 80,
        borderWidth: 2,
        borderColor: '#9188E5',
    },
    profileImageText: {
        fontSize: 14,
        color: '#666',
        textAlign: 'center',
        marginTop: 5,
    },

    // Tab Navigation Styles
    tabContainer: {
        flexDirection: 'row',
        marginHorizontal: 16,
        marginBottom: 20,
        borderBottomWidth: 1,
        borderBottomColor: '#e0e0e0',
    },
    tab: {
        flex: 1,
        paddingVertical: 15,
        alignItems: 'center',
        borderBottomWidth: 2,
        borderBottomColor: 'transparent',
    },
    activeTab: {
        borderBottomColor: '#9188E5',
    },
    tabText: {
        fontSize: 16,
        color: '#666',
        fontWeight: '500',
    },
    activeTabText: {
        color: '#9188E5',
        fontWeight: '600',
    },

    // Photo Gallery Styles
    photoGalleryContainer: {
        marginTop: 10,
    },
    photoGrid: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
        marginBottom: 15,
    },
    photoGridItem: {
        width: '48%',
        aspectRatio: 1,
        marginBottom: 10,
        padding: 2,
        borderRadius: 12,
        position: 'relative',
        overflow: 'hidden',
    },
    mainPhotoGridItem: {
        width: '100%',
        aspectRatio: 1.5,
        marginBottom: 15,
    },
    photoGridImage: {
        width: '100%',
        height: '100%',
        borderRadius: 12,
        borderColor: '#9188E5',
        borderWidth: 1,
    },
    mainPhotoGridImage: {
        borderRadius: 15,
    },
    mainPhotoLabel: {
        position: 'absolute',
        top: 10,
        left: 10,
        backgroundColor: '#9188E5',
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 12,
    },
    mainPhotoLabelText: {
        color: 'white',
        fontSize: 12,
        fontWeight: '600',
    },
    addPhotoGridItem: {
        width: '48%',
        aspectRatio: 1,
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#9188E5',
        borderStyle: 'dashed',
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#f8f8ff',
        minHeight: 180,
    },
    addPhotoGridText: {
        color: '#9188E5',
        fontSize: 14,
        fontWeight: '500',
        marginTop: 5,
    },

    // Empty Photo State
    emptyPhotoContainer: {
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 40,
    },
    emptyImage: {
        width: 100,
        height: 100,
        borderRadius: 50,
        opacity: 0.3,
        marginBottom: 15,
    },
    emptyStateText: {
        fontSize: 16,
        color: '#666',
        marginBottom: 20,
    },
    addFirstPhotoButton: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#9188E5',
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderRadius: 25,
    },
    addFirstPhotoButtonText: {
        color: 'white',
        fontSize: 16,
        fontWeight: '600',
        marginLeft: 8,
    },

    // Form Styles
    inputsContainer: {
        paddingHorizontal: 16,
    },
    inputsContainerFull: {
        paddingHorizontal: 16,
    },
    inputContainer: {
        marginBottom: 20,
    },
    label: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 10,
        color: '#333',
    },
    dropdownInput: {
        borderWidth: 1,
        borderColor: '#9188E5',
        borderRadius: 10,
        paddingHorizontal: 16,
        fontSize: 16,
        backgroundColor: '#fff',
        height: 50,
    },
    inputError: {
        borderColor: 'red',
    },
    errorText: {
        color: 'red',
        fontSize: 14,
        marginTop: 5,
    },
    descriptionInput: {
        minHeight: 120,
        textAlignVertical: 'top',
    },

    // Vaccine Files Styles
    vaccinesInput: {
        marginTop: 20,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderColor: '#9188E5',
        borderWidth: 1.5,
        padding: 14,
        borderRadius: 16,
    },
    vaccineInputTextContainer: {
        flex: 1,
        marginLeft: 10,
    },
    vaccineInputTitle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#4B3FA7',
    },
    vaccineInputSubtitle: {
        fontSize: 13,
        color: '#6c6c8a',
        marginTop: 2,
    },
    uploadedFile: {
        marginTop: 20,
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(145, 136, 229, 0.1)',
        padding: 10,
        borderRadius: 10,
    },
    vaccineLabel: {
        fontSize: 14,
        color: '#333',
        fontWeight: 'bold',
        flex: 1,
    },
    leftContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        flex: 1,
    },
    rightContainer: {
        padding: 1,
    },
    image: {
        width: 40,
        height: 40,
        marginRight: 10,
        borderRadius: 5,
    },

    // Button Styles
    buttonsContainer: {
        borderTopWidth: 1,
        borderTopColor: '#fff',
        backgroundColor: '#fff',
        paddingHorizontal: 16,
        width: '100%',
        marginVertical: 20,
        flex: 1,
        justifyContent: 'flex-end',
    },
    saveButton: {
        backgroundColor: '#9188E5',
        marginBottom: 12,
    },
    deleteButton: {
        backgroundColor: '#F13838',
        marginBottom: 12,
    },
    buttonText: {
        color: 'white',
        fontSize: 18,
        fontWeight: 'bold',
    },

    // Loading Styles
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#fff',
        padding: 20,
    },
    loadingOverlay: {
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(255, 255, 255, 0.8)',
        justifyContent: 'center',
        alignItems: 'center',
        zIndex: 1000,
    },
    loadingText: {
        marginTop: 10,
        fontSize: 16,
        color: '#9188E5',
        textAlign: 'center',
    },

    // Read-only styles for non-owners
    readOnlyInput: {
        backgroundColor: '#f5f5f5',
        color: '#666',
    },

    // BottomSheet Styles
    bottomSheetHeader: {
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#f0f0f0',
        marginBottom: 8,
    },
    bottomSheetTitle: {
        fontSize: 18,
        fontWeight: '600',
        color: '#333',
        textAlign: 'center',
    },
    bottomSheetActions: {
        flex: 1,
    },
    bottomSheetAction: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 8,
        borderRadius: 12,
        marginBottom: 8,
    },
    actionIconContainer: {
        width: 40,
        height: 40,
        borderRadius: 20,
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    actionText: {
        fontSize: 16,
        fontWeight: '500',
        color: '#333',
    },
    deleteText: {
        color: '#FF3B30',
    },
});