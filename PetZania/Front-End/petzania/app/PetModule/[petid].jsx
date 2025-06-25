import {
    View, Text, StyleSheet, ScrollView, TouchableOpacity,
    Alert, TextInput, Platform, ActivityIndicator,
    Dimensions
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Image } from 'expo-image';
import { useContext, useEffect, useState } from 'react';
import { Dropdown } from 'react-native-element-dropdown';
import * as ImagePicker from 'expo-image-picker';
import * as DocumentPicker from 'expo-document-picker';
import { AntDesign } from '@expo/vector-icons';
import { useActionSheet } from '@expo/react-native-action-sheet';
import ImageViewing from 'react-native-image-viewing';

import { PetContext } from '@/context/PetContext';
import { UserContext } from '@/context/UserContext';
import { uploadFiles } from '@/services/uploadService';

import DateOfBirthInput from '@/components/DateOfBirthInput';
import { PETS } from '@/constants/PETS';
import { PET_BREEDS } from '@/constants/PETBREEDS';
import { updatePet, deletePet, getPetById } from '@/services/petService';
import Toast from 'react-native-toast-message';

const PetDetails = () => {
    const { petId } = useLocalSearchParams();
    const { pets, setPets } = useContext(PetContext);
    const { user: currentUser } = useContext(UserContext);

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
    const petIndex = pets.findIndex((pet) => pet.petId === petId);
    const [pet, setPet] = useState(() => pets[petIndex] || null);
    const [petLoading, setPetLoading] = useState(!pet); // Loading if pet not found in context

    // Check if the current user owns this pet
    const isOwner = currentUser?.userId === pet?.userId;

    // Tab navigation state
    const [activeTab, setActiveTab] = useState('info');    // Photo management state
    const [images, setImages] = useState(pet?.myPicturesURLs || []);
    const [currentImageIndex, setCurrentImageIndex] = useState(0);
    const [uploadingImages, setUploadingImages] = useState(false);
    const [showImageViewer, setShowImageViewer] = useState(false);

    const { showActionSheetWithOptions } = useActionSheet();

    const [gender, setGender] = useState(pet?.gender || '');
    const [species, setSpecies] = useState(pet?.species || '');
    const [breed, setBreed] = useState(pet?.breed || '');
    const [dateOfBirth, setDateOfBirth] = useState(pet?.dateOfBirth || '');

    const [vaccineFiles, setVaccineFiles] = useState(pet?.myVaccinesURLs || []);

    const [isLoading, setIsLoading] = useState(false);
    const [errors, setErrors] = useState({
        name: '',
        species: '',
        gender: '',
        breed: '',
        dateOfBirth: '',
    });

    const genderOptions = [
        { label: 'Male', value: 'MALE' },
        { label: 'Female', value: 'FEMALE' },
    ];    const speciesOptions = PETS.map(p => ({ label: p.name, value: p.value }));
    const breedOptions = species
        ? (PET_BREEDS[species]?.map(b => ({ label: b.name, value: b.name })) || [])
        : [];    // Fetch pet data if not found in context
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
            } else if (pet) {
                console.log('Pet found in context:', pet);
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

    const handleInputChange = (key, value) => {
        setPet((prev) => prev ? { ...prev, [key]: value } : null);
        setErrors((prev) => ({ ...prev, [key]: '' }));
    };
    const pickImage = async () => {
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
                quality: 0.8,
                aspect: [1, 1],
                selectionLimit: remainingSlots,
            });
            
            if (!result.canceled) {
                const uris = result.assets.map(asset => asset.uri);
                const newImages = [...images, ...uris];
                setImages(newImages);
                setCurrentImageIndex(newImages.length - 1); // Show last added image
                
                if (newImages.length === 6) {
                    showSuccessMessage('Photo limit reached', 'You have added the maximum of 6 photos');
                }
            }
        } catch (error) {
            console.error('Error picking images:', error);
            showErrorMessage('Failed to pick images', 'Please try again');
        } finally {
            setUploadingImages(false);
        }
    };

    const deleteImage = (uriToDelete) => {
        const updatedImages = images.filter(uri => uri !== uriToDelete);
        setImages(updatedImages);
        if (currentImageIndex >= updatedImages.length) {
            setCurrentImageIndex(Math.max(0, updatedImages.length - 1));
        }
    };    const uploadAndSaveImages = async () => {
        if (images.length === 0) return [];

        try {
            setUploadingImages(true);

            // Separate local images (need upload) from server URLs (already uploaded)
            const localImages = images.filter(isLocalImage);
            const serverImages = images.filter(isServerImage);

            let uploadedUrls = [];

            if (localImages.length > 0) {
                // Reorder local images to put the current selected image first (if it's local)
                const currentImage = images[currentImageIndex];
                const isCurrentImageLocal = isLocalImage(currentImage);

                const reorderedLocalImages = isCurrentImageLocal
                    ? [currentImage, ...localImages.filter(img => img !== currentImage)]
                    : localImages;

                const files = reorderedLocalImages.map(image => ({
                    uri: image,
                    name: image.split('/').pop() || 'pet-image.jpg',
                    type: 'image/jpeg',
                }));

                uploadedUrls = await uploadFiles(files);
            }
            
            // Combine uploaded URLs with existing server URLs, maintaining order
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
            console.error('Error uploading images:', error);
            showErrorMessage('Failed to upload images', 'Please try again');
            return images.filter(isServerImage); // Return only server images if upload fails
        } finally {
            setUploadingImages(false);
        }
    };

    const handleFilePick = async () => {
        try {
            setIsLoading(true);
            const result = await DocumentPicker.getDocumentAsync({
                type: 'application/pdf',
                multiple: true,
            });

            if (!result.canceled && result.assets?.length > 0) {
                const files = result.assets.map(file => ({
                    uri: file.uri,
                    name: file.name,
                    size: file.size,
                }));
                const updatedVaccines = [...vaccineFiles, ...files];
                setVaccineFiles(updatedVaccines);
                handleInputChange('myVaccinesURLs', updatedVaccines);
            }
        } catch (err) {
            console.error('Picker Error:', err);
        } finally {
            setIsLoading(false);
        }
    };    const handleSaveChanges = async () => {
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
            
            // Upload images if there are any new ones
            const uploadedImageUrls = await uploadAndSaveImages();
            
            const petData = {
                name: pet.name || undefined,
                description: pet.description || undefined,
                gender: gender || undefined,
                dateOfBirth: dateOfBirth || undefined,
                breed: breed || undefined,
                species: species ? species.toUpperCase() : undefined,
                myVaccinesURLs: vaccineFiles.map(file => file.uri || file),
                myPicturesURLs: uploadedImageUrls.length > 0 ? uploadedImageUrls : (Array.isArray(pet.myPicturesURLs) ? pet.myPicturesURLs : []),
            };

            await updatePet(petId, petData);
            setPets(prevPets => prevPets.map(p =>
                p.petId === petId ? { ...p, ...petData } : p
            ));

            const hasImages = uploadedImageUrls.length > 0;
            const successMessage = hasImages ?
                'Pet updated successfully with photos!' :
                'Pet updated successfully!';
            showSuccessMessage(successMessage);
            router.back();
        } catch (error) {
            showErrorMessage('Failed to update pet', error);
            console.error('Error updating pet:', error);
        } finally {
            setIsLoading(false);
        }
    };


    const deletePetByPetId = () => {
        setIsLoading(true);
        deletePet(petId)
            .then(() => {
                setPets(prevPets => prevPets.filter(pet => pet.petId !== petId));
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

    const defaultImage = require('@/assets/images/Defaults/default-pet.png');
    const renderTabContent = () => {
        if (activeTab === 'info') {
            return (
                <View style={styles.inputsContainer}>                    {/* Name */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Name {isOwner && <Text style={{ color: 'red' }}>*</Text>}</Text>
                        <TextInput
                            style={[
                                styles.input, 
                                errors.name && styles.inputError,
                                !isOwner && styles.readOnlyInput
                            ]}
                            placeholder="Pet's name"
                            value={pet?.name || ''}
                            onChangeText={isOwner ? (text) => handleInputChange('name', text) : undefined}
                            editable={isOwner}
                        />
                        {errors.name && isOwner && <Text style={styles.errorText}>{errors.name}</Text>}
                    </View>                    {/* Species */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Type {isOwner && <Text style={{ color: 'red' }}>*</Text>}</Text>
                        {isOwner ? (
                            <Dropdown
                                style={[styles.input, errors.species && styles.inputError]}
                                placeholder="Select Type"
                                data={speciesOptions}
                                labelField="label"
                                valueField="value"
                                value={species}
                                onChange={(item) => {
                                    setSpecies(item.value);
                                    setBreed('');
                                    handleInputChange('species', item.value);
                                }}
                            />
                        ) : (
                            <TextInput
                                style={[styles.input, styles.readOnlyInput]}
                                value={species || 'Not specified'}
                                editable={false}
                            />
                        )}
                        {errors.species && isOwner && <Text style={styles.errorText}>{errors.species}</Text>}
                    </View>                    {/* Breed */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>
                            Breed {isOwner && <Text style={{ color: 'red' }}>*</Text>}
                        </Text>
                        <TextInput
                            style={[
                                styles.input, 
                                errors.breed && styles.inputError,
                                !isOwner && styles.readOnlyInput
                            ]}
                            placeholder="Enter breed"
                            value={breed}
                            onChangeText={isOwner ? (text) => {
                                setBreed(text);
                                handleInputChange('breed', text);
                            } : undefined}
                            editable={isOwner}
                        />
                        {errors.breed && isOwner && <Text style={styles.errorText}>{errors.breed}</Text>}
                    </View>                    {/* Gender */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Gender {isOwner && <Text style={{ color: 'red' }}>*</Text>}</Text>
                        {isOwner ? (
                            <Dropdown
                                style={[styles.input, errors.gender && styles.inputError]}
                                placeholder="Select Gender"
                                data={genderOptions}
                                labelField="label"
                                valueField="value"
                                value={gender}
                                onChange={(item) => {
                                    setGender(item.value);
                                    handleInputChange('gender', item.value);
                                }}
                            />
                        ) : (
                            <TextInput
                                style={[styles.input, styles.readOnlyInput]}
                                value={gender || 'Not specified'}
                                editable={false}
                            />
                        )}
                        {errors.gender && isOwner && <Text style={styles.errorText}>{errors.gender}</Text>}
                    </View>                    {/* Description */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Description</Text>
                        <TextInput
                            style={[
                                styles.input, 
                                { height: 100, textAlignVertical: 'top' },
                                !isOwner && styles.readOnlyInput
                            ]}
                            placeholder="Pet's description"
                            value={pet?.description || ''}
                            onChangeText={isOwner ? (text) => handleInputChange('description', text) : undefined}
                            multiline
                            numberOfLines={4}
                            editable={isOwner}
                        />
                    </View>                    {/* Date of Birth */}
                    <View style={styles.inputContainer}>
                        {isOwner ? (
                            <DateOfBirthInput
                                value={dateOfBirth}
                                onChange={(date) => {
                                    setDateOfBirth(date);
                                    handleInputChange('dateOfBirth', date);
                                }}
                                errorMessage={errors.dateOfBirth}
                            />
                        ) : (
                            <>
                                <Text style={styles.label}>Date of Birth</Text>
                                <TextInput
                                    style={[styles.input, styles.readOnlyInput]}
                                    value={dateOfBirth || 'Not specified'}
                                    editable={false}
                                />
                            </>
                        )}
                    </View>
                </View>
            );
        } else if (activeTab === 'photos') {
            // Photos tab - dedicated photo management
            return (
                <View style={styles.inputsContainer}>
                    <Text style={styles.label}>Pet Photos</Text>
                    {images.length > 0 ? (
                        <View style={styles.photoGalleryContainer}>
                            {/* Photo Gallery Grid */}
                            <View style={styles.photoGrid}>
                                {images.map((uri, index) => {
                                    const isValidUri = isValidImageUri(uri);
                                    return (
                                        <TouchableOpacity
                                            key={`photo-${index}`}
                                            style={[
                                                styles.photoGridItem,
                                                index === 0 && styles.mainPhotoGridItem
                                            ]}
                                            onPress={() => {
                                                if (isOwner) {
                                                    handleImagePress(index);
                                                } else {
                                                    // For non-owners, just show the image viewer
                                                    setCurrentImageIndex(index);
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
                                                placeholder={defaultImage} // Shows while loading
                                                transition={200} // Smooth fade-in
                                                onError={() => console.warn('Failed to load photo:', uri)}
                                            />
                                            {index === 0 && (
                                                <View style={styles.mainPhotoLabel}>
                                                    <Text style={styles.mainPhotoLabelText}>Profile</Text>
                                                </View>
                                            )}
                                        </TouchableOpacity>
                                    );
                                })}                                {/* Add Photo Button - Only for owners */}
                                {isOwner && images.length < 6 && (
                                    <TouchableOpacity
                                        style={styles.addPhotoGridItem}
                                        onPress={pickImage}
                                        disabled={uploadingImages || isLoading}
                                    >
                                        <AntDesign name="plus" size={30} color="#9188E5" />
                                        <Text style={styles.addPhotoGridText}>Add Photo</Text>
                                    </TouchableOpacity>
                                )}
                            </View>
                        </View>
                    ) : (
                        <View style={styles.emptyPhotoContainer}>
                            <Image source={defaultImage} style={styles.emptyPhotoImage} />
                            <Text style={styles.emptyPhotoText}>No photos added yet</Text>
                            {isOwner && (
                                <TouchableOpacity
                                    style={styles.addFirstPhotoButton}
                                    onPress={pickImage}
                                    disabled={uploadingImages || isLoading}
                                >
                                    <AntDesign name="camera" size={20} color="white" />
                                    <Text style={styles.addFirstPhotoButtonText}>Add First Photo</Text>
                                </TouchableOpacity>
                            )}
                        </View>
                    )}
                </View>
            );        } else {
            // Vaccines tab
            return (
                <View style={styles.inputsContainer}>
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Vaccine Documents</Text>
                        {isOwner && (
                            <TouchableOpacity 
                                style={styles.uploadButton} 
                                onPress={handleFilePick}
                                disabled={isLoading}
                            >
                                <AntDesign name="pluscircleo" size={20} color="#9188E5" />
                                <Text style={styles.uploadButtonText}>Add Vaccine Documents</Text>
                            </TouchableOpacity>
                        )}
                        
                        {vaccineFiles.length > 0 ? (
                            <View style={styles.filesList}>
                                {vaccineFiles.map((file, index) => (
                                    <View key={index} style={styles.fileItem}>
                                        <AntDesign name="pdffile1" size={20} color="#FF6B6B" />
                                        <Text style={styles.fileName}>{file.name || `Document ${index + 1}`}</Text>
                                        {isOwner && (
                                            <TouchableOpacity
                                                onPress={() => {
                                                    const updatedFiles = vaccineFiles.filter((_, i) => i !== index);
                                                    setVaccineFiles(updatedFiles);
                                                    handleInputChange('myVaccinesURLs', updatedFiles);
                                                }}
                                            >
                                                <AntDesign name="delete" size={18} color="#FF6B6B" />
                                            </TouchableOpacity>
                                        )}
                                    </View>
                                ))}
                            </View>
                        ) : (
                            !isOwner && (
                                <Text style={styles.emptyStateText}>No vaccine documents available</Text>
                            )
                        )}
                    </View>
                </View>
            );
        }
    };

    // Helper function to distinguish between local and server images
    const isLocalImage = (uri) => {
        return uri && typeof uri === 'string' && (uri.startsWith('file://') || uri.startsWith('content://'));
    };

    const isServerImage = (uri) => {
        return uri && typeof uri === 'string' && uri.startsWith('http');
    };

    const isValidImageUri = (uri) => {
        return isLocalImage(uri) || isServerImage(uri);
    };

    const makeProfilePicture = (targetIndex) => {
        if (targetIndex === 0) return; // Already profile picture
        
        const newImages = [...images];
        const [selectedImage] = newImages.splice(targetIndex, 1);
        newImages.unshift(selectedImage);
        setImages(newImages);
        setCurrentImageIndex(0);
    };

    const handleImagePress = (imageIndex = 0) => {
        const image = images[imageIndex];
        const options = [
            'View Photo',
            imageIndex === 0 ? 'Change Profile Picture' : 'Make Profile Picture',
            'Add More Photos',
            'Delete Photo',
            'Cancel',
        ];
        const cancelButtonIndex = options.length - 1;
        const destructiveButtonIndex = options.indexOf('Delete Photo');

        showActionSheetWithOptions(
            {
                options,
                cancelButtonIndex,
                destructiveButtonIndex,
            },
            (buttonIndex) => {
                if (buttonIndex === 0) {
                    // View Photo
                    setCurrentImageIndex(imageIndex);
                    setShowImageViewer(true);
                } else if (buttonIndex === 1) {
                    // Change Profile Picture or Make Profile Picture
                    if (imageIndex === 0) {
                        pickImage(); // Change profile picture
                    } else {
                        makeProfilePicture(imageIndex); // Make this photo the profile picture
                    }
                } else if (buttonIndex === 2) {
                    // Add More Photos
                    pickImage();
                } else if (buttonIndex === 3) {
                    // Delete Photo
                    deleteImage(image);
                }
            }
        );
    };

    const handleProfileImagePress = () => {
        if (images.length > 0) {
            handleImagePress(0);
        } else {
            const options = [
                'Add Profile Picture',
                'Cancel',
            ];
            const cancelButtonIndex = options.length - 1;
            
            showActionSheetWithOptions(
                {
                    options,
                    cancelButtonIndex,
                },
                (buttonIndex) => {
                    if (buttonIndex === 0) {
                        pickImage();
                    }
                }
            );
        }
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
        <ScrollView contentContainerStyle={styles.container}>
            {/* Profile Picture Section */}
            <View style={styles.profileSection}>
                <TouchableOpacity 
                    style={styles.profileImageContainer}
                    onPress={isOwner ? handleProfileImagePress : () => {
                        // For non-owners, just show the image viewer
                        if (images.length > 0) {
                            setCurrentImageIndex(0);
                            setShowImageViewer(true);
                        }
                    }}
                    disabled={uploadingImages || isLoading}
                >
                    <Image
                        source={images.length > 0 && isValidImageUri(images[0]) ? 
                            { uri: images[0] } : defaultImage}
                        style={styles.profileImage}
                        onError={() => console.warn('Failed to load profile image')}
                    />
                    {isOwner && (
                        <>
                            {uploadingImages ? (
                                <View style={styles.profileImageLoading}>
                                    <ActivityIndicator size="small" color="#9188E5" />
                                </View>
                            ) : (
                                <View style={styles.profileImageOverlay}>
                                    <AntDesign name="camera" size={20} color="white" />
                                </View>
                            )}
                        </>
                    )}
                </TouchableOpacity>
                
                <Text style={styles.profileImageText}>
                    {isOwner 
                        ? (images.length > 0 ? 'Tap to view or change profile picture' : 'Tap to add profile picture')
                        : (images.length > 0 ? 'Tap to view profile picture' : 'No profile picture')
                    }
                </Text>
            </View>

            {/* ImageViewing Component */}
            <ImageViewing
                images={images.map(uri => ({ uri }))}
                imageIndex={currentImageIndex}
                visible={showImageViewer}
                onRequestClose={() => setShowImageViewer(false)}
                backgroundColor="black"
                swipeToCloseEnabled
                doubleTapToZoomEnabled
            />

            {/* Tab Navigation */}
            <View style={styles.tabContainer}>
                <TouchableOpacity
                    style={[styles.tab, activeTab === 'info' && styles.activeTab]}
                    onPress={() => setActiveTab('info')}
                >
                    <Text style={[styles.tabText, activeTab === 'info' && styles.activeTabText]}>
                        Pet Info
                    </Text>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[styles.tab, activeTab === 'photos' && styles.activeTab]}
                    onPress={() => setActiveTab('photos')}
                >
                    <Text style={[styles.tabText, activeTab === 'photos' && styles.activeTabText]}>
                        Photos
                    </Text>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[styles.tab, activeTab === 'vaccines' && styles.activeTab]}
                    onPress={() => setActiveTab('vaccines')}
                >
                    <Text style={[styles.tabText, activeTab === 'vaccines' && styles.activeTabText]}>
                        Vaccines
                    </Text>
                </TouchableOpacity>
            </View>

            {/* Tab Content */}
            {renderTabContent()}
            {/* Action Buttons - Only show for pet owner */}
            {isOwner && (
                <View style={styles.buttonContainer}>
                    <TouchableOpacity 
                        style={styles.saveButton} 
                        onPress={handleSaveChanges} 
                        disabled={isLoading || uploadingImages}
                    >
                     <Text style={styles.buttonText}>Save Changes</Text>
                    </TouchableOpacity>
                    <TouchableOpacity 
                        style={styles.deleteButton} 
                        onPress={handleDeletePet} 
                        disabled={isLoading || uploadingImages}
                    >
                        <Text style={styles.buttonText}>Delete Pet</Text>
                    </TouchableOpacity>
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
    );
};

const styles = StyleSheet.create({
    container: {
        backgroundColor: '#fff',
        paddingTop: 20,
    },
    // Profile Section Styles
    profileSection: {
        alignItems: 'center',
        marginBottom: 20,
        paddingHorizontal: 20,
    },
    profileImageContainer: {
        position: 'relative',
        marginBottom: 10,
    },
    profileImage: {
        width: 160,
        height: 160,
        borderRadius: 80,
        borderWidth: 2,
        borderColor: '#9188E5',
    },
    profileImageOverlay: {
        position: 'absolute',
        bottom: 0,
        right: 0,
        backgroundColor: '#9188E5',
        borderRadius: 15,
        width: 30,
        height: 30,
        justifyContent: 'center',
        alignItems: 'center',
        borderWidth: 2,
        borderColor: 'white',
    },
    profileImageLoading: {
        position: 'absolute',
        bottom: 0,
        right: 0,
        backgroundColor: 'white',
        borderRadius: 15,
        width: 30,
        height: 30,
        justifyContent: 'center',
        alignItems: 'center',
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
        marginHorizontal: 20,
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
        marginBottom: 10,
        borderRadius: 12,
        borderWidth: 2,
        borderColor: '#9188E5',
        borderStyle: 'dashed',
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#f8f8ff',
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
        paddingVertical: 40,
    },
    emptyPhotoImage: {
        width: 100,
        height: 100,
        borderRadius: 50,
        opacity: 0.3,
        marginBottom: 15,
    },
    emptyPhotoText: {
        fontSize: 16,
        color: '#666',
        marginBottom: 20,
    },
    addFirstPhotoButton: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#9188E5',
        paddingHorizontal: 20,
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
        paddingHorizontal: 20,
        minHeight: Dimensions.get('window').height - 600,
        width: '100%',
    },
    inputContainer: {
        alignItems: 'flex-start',
        paddingVertical: 15,
    },
    label: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 5,
        color: '#333',
    },
    input: {
        width: '100%',
        height: 50,
        borderWidth: 1,
        borderColor: '#9188E5',
        borderRadius: 10,
        paddingHorizontal: 15,
        fontSize: 16,
        backgroundColor: '#fff',
    },
    inputError: {
        borderColor: 'red',
    },
    errorText: {
        color: 'red',
        fontSize: 14,
        marginTop: 5,
    },
    
    // Vaccine Files Styles
    uploadButton: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: 12,
        borderWidth: 1,
        borderColor: '#9188E5',
        borderRadius: 8,
        backgroundColor: '#f8f8ff',
        marginBottom: 10,
    },
    uploadButtonText: {
        marginLeft: 8,
        color: '#9188E5',
        fontSize: 16,
        fontWeight: '500',
    },
    filesList: {
        marginTop: 10,
    },
    fileItem: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: 10,
        backgroundColor: '#f5f5f5',
        borderRadius: 8,
        marginBottom: 8,
    },
    fileName: {
        flex: 1,
        marginLeft: 10,
        fontSize: 14,
        color: '#333',
    },

    // Button Styles
    buttonContainer: {
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
        padding: 20,
        width: '100%',
        marginTop: 20,
    },
    saveButton: {
        backgroundColor: '#2CA269',
        paddingVertical: 15,
        borderRadius: 10,
        alignItems: 'center',
        marginBottom: 10,
    },
    deleteButton: {
        backgroundColor: '#F13838',
        paddingVertical: 15,
        borderRadius: 10,
        alignItems: 'center',
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
    },loadingText: {
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
    emptyStateText: {
        fontSize: 16,
        color: '#999',
        textAlign: 'center',
        fontStyle: 'italic',
        marginTop: 20,
        paddingVertical: 20,
    },
});

export default PetDetails;