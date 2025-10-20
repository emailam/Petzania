import {
    StyleSheet,
    View,
    TouchableOpacity,
    Text,
    TextInput,
    Dimensions,
    KeyboardAvoidingView,
    ScrollView,
    FlatList,
    Platform,
} from 'react-native';
import { Image } from 'expo-image';
import React, { useState, useContext, useRef, useEffect } from 'react';
import * as ImagePicker from 'expo-image-picker';
import { AntDesign } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { ActivityIndicator } from 'react-native-paper';
import { useActionSheet } from '@expo/react-native-action-sheet';
import ImageViewing from 'react-native-image-viewing';
import CustomInput from '@/components/CustomInput';
import ImageUploadInput from '@/components/ImageUploadInput';

import { uploadFiles } from '@/services/uploadService';

import Button from '@/components/Button';
import { PetContext } from '@/context/PetContext';

const { width } = Dimensions.get('window');

const AddPet1 = () => {
    const flatListRef = useRef(null);
    const [loading, setLoading] = useState(false);
    const { showActionSheetWithOptions } = useActionSheet();

    const defaultImage = require('../../assets/images/AddPet/Pet Default Pic.png');
    const { pet, setPet } = useContext(PetContext);
    const [images, setImages] = useState(pet.images || []);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [errors, setErrors] = useState({
        name: '',
        images: ''
    });
    const [isFormComplete, setIsFormComplete] = useState(false);
    const [showImageViewer, setShowImageViewer] = useState(false);
    const [viewerIndex, setViewerIndex] = useState(0);
    const router = useRouter();

    useEffect(() => {
        // Only reset if no existing pet data
        if (!pet.name && !pet.images?.length) {
            setPet({});
        }
        // Sync images with pet context if they exist
        if (pet.images?.length && !images.length) {
            setImages(pet.images);
        }
    }, []);

    // Helper function to set individual field errors
    const setError = (field, message) => {
        setErrors(prev => ({
            ...prev,
            [field]: message
        }));
    };

    // Clear individual field errors
    const clearError = (field) => {
        setErrors(prev => ({
            ...prev,
            [field]: ''
        }));
    };

    // Check if form is complete and valid
    useEffect(() => {
        const nameValid = pet?.name;
        
        // Form is complete when name is valid
        const formValid = nameValid && !errors.name;
        
        setIsFormComplete(formValid);
    }, [pet?.name, errors]);

    const pickImage = async () => {
        setLoading(true);
        clearError('images'); // Clear any previous image errors
        
        try {
            let result = await ImagePicker.launchImageLibraryAsync({
                mediaTypes: ['images'],
                allowsMultipleSelection: true,
                quality: 0.7,
                selectionLimit: 6,
            });
            
            if (!result.canceled) {
                const uris = result.assets.map(asset => asset.uri);
                const newImages = [...images, ...uris];
                setImages(newImages);
                
                // Update pet context with images
                setPet(prev => ({
                    ...prev,
                    images: newImages
                }));
                
                setCurrentIndex(0);
            }
        } catch (error) {
            console.error('Error picking images:', error);
            setError('images', 'Failed to select images. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const deleteImage = (uriToDelete) => {
        const indexToDelete = images.findIndex(uri => uri === uriToDelete);
        const updatedImages = images.filter(uri => uri !== uriToDelete);
        setImages(updatedImages);
        
        // Update pet context
        setPet(prev => ({
            ...prev,
            images: updatedImages
        }));
        
        // Handle currentIndex adjustment based on which image was deleted
        if (updatedImages.length === 0) {
            setCurrentIndex(0);
        } else if (indexToDelete <= currentIndex) {
            // If we deleted an image at or before the current index, adjust accordingly
            const newIndex = Math.max(0, currentIndex - 1);
            setCurrentIndex(newIndex);
            // Force the FlatList to scroll to the correct position
            setTimeout(() => {
                flatListRef.current?.scrollToIndex({ 
                    index: newIndex, 
                    animated: false 
                });
            }, 100);
        } else if (currentIndex >= updatedImages.length) {
            // If current index is out of bounds, set to last image
            const newIndex = updatedImages.length - 1;
            setCurrentIndex(newIndex);
            setTimeout(() => {
                flatListRef.current?.scrollToIndex({ 
                    index: newIndex, 
                    animated: false 
                });
            }, 100);
        }
    };

    const goToNextStep = async () => {
        // Validate all fields before proceeding
        let hasErrors = false;

        // Validate name
        if (!pet?.name?.trim()) {
            setError('name', "Pet's name is required");
            hasErrors = true;
        } else if (pet.name.trim().length < 2) {
            setError('name', 'Name must be at least 2 characters');
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }

        setLoading(true);

        try {
            if (images.length > 0) {
                const reorderedImages = [
                    images[currentIndex],
                    ...images.filter((_, idx) => idx !== currentIndex)
                ];

                const files = reorderedImages.map(image => ({
                    uri: image,
                    name: image.split('/').pop(),
                    type: 'image/jpeg',
                }));

                const uploadedUrls = await uploadFiles(files);

                setPet(prev => ({
                    ...prev,
                    myPicturesURLs: uploadedUrls,
                }));
            }

        } catch (error) {
            console.error('Error uploading images:', error);
            setError('images', 'Something went wrong while uploading. Please try again.');
        } finally {
            setLoading(false);
            if (!errors.images) {
                router.push('/PetModule/AddPet2');
            }
        }
    };

    const handleCarouselImagePress = (index) => {
        const options = [
            'View Photo',
            index === 0 ? null : 'Make Profile Picture',
            'Add More Photos',
            'Delete Photo',
            'Cancel',
        ].filter(Boolean);
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
                    setViewerIndex(index);
                    setShowImageViewer(true);
                } else if (buttonIndex === 1 && index !== 0) {
                    // Make profile picture
                    const newImages = [...images];
                    const [selected] = newImages.splice(index, 1);
                    newImages.unshift(selected);
                    setImages(newImages);
                    
                    // Update pet context
                    setPet(prev => ({
                        ...prev,
                        images: newImages
                    }));
                    
                    setCurrentIndex(0);
                } else if ((buttonIndex === 1 && index === 0) || (buttonIndex === 2 && index !== 0)) {
                    // Add More Photos
                    pickImage();
                } else if ((buttonIndex === 2 && index === 0) || (buttonIndex === 3 && index !== 0)) {
                    // Delete photo
                    deleteImage(images[index]);
                }
            }
        );
    };

    const renderItem = ({ item, index }) => (
        <TouchableOpacity
            activeOpacity={0.85}
            onPress={() => handleCarouselImagePress(index)}
            style={styles.carouselItem}
        >
            <Image source={{ uri: item }} style={styles.carouselImage} />
        </TouchableOpacity>
    );

    return (
        <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            style={styles.container}
            keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
        >
            <ScrollView
                contentContainerStyle={styles.scrollContainer}
                keyboardShouldPersistTaps="handled"
                showsVerticalScrollIndicator={true}
                keyboardDismissMode="interactive"
            >
                <View style={styles.imageBlock}>
                    {images.length > 0 ? (
                        <>
                            <FlatList
                                ref={flatListRef}
                                data={images}
                                horizontal
                                pagingEnabled
                                showsHorizontalScrollIndicator={false}
                                onMomentumScrollEnd={(e) => {
                                    const index = Math.round(e.nativeEvent.contentOffset.x / width);
                                    setCurrentIndex(Math.min(index, images.length - 1));
                                }}
                                keyExtractor={(item, index) => `${item}-${index}`}
                                renderItem={renderItem}
                                getItemLayout={(data, index) => ({
                                    length: width,
                                    offset: width * index,
                                    index,
                                })}
                            />
                            <View style={styles.thumbnailRow}>
                                {images.map((uri, index) => (
                                    <TouchableOpacity
                                        disabled={loading}
                                        onPress={() => {
                                            if (index < images.length) {
                                                flatListRef.current?.scrollToIndex({ index, animated: true });
                                                setCurrentIndex(index);
                                            }
                                        }}
                                        key={index}
                                        style={[
                                            styles.thumbnailWrapper,
                                            currentIndex === index && styles.activeThumbnail,
                                        ]}
                                    >
                                        <Image source={{ uri }} style={styles.thumbnailImage} />
                                    </TouchableOpacity>
                                ))}
                            </View>

                            <Text style={styles.noteText}>
                                The selected image will be used as the default pet image.
                            </Text>
                        </>
                    ) : (
                        <ImageUploadInput
                            defaultImage={defaultImage}
                            onImageChange={(imageUri) => {
                                if (imageUri) {
                                    const newImages = [imageUri];
                                    setImages(newImages);
                                    setPet(prev => ({
                                        ...prev,
                                        images: newImages
                                    }));
                                    setCurrentIndex(0);
                                    clearError('images');
                                }
                            }}
                            size={180}
                            isUploading={loading}
                            style={{ marginVertical: 20 }}
                            allowsMultipleSelection={true}
                        />
                    )}
                </View>
                <View style={styles.inputContainer}>
                    <Text style={styles.label}>
                        What's your pet's name?
                        <Text style={{ fontSize: 18, color: 'red' }}>*</Text>
                    </Text>
                    <CustomInput
                        style={[errors.name ? styles.inputError : null]}
                        placeholder="Buddy"
                        maxLength={50}
                        error={!!errors.name}
                        value={pet.name || ''}
                        onChangeText={(name) => {
                            setPet({ ...pet, name });
                            clearError('name');
                        }}
                        returnKeyType="done"
                        mode="outlined"
                    />
                    {errors.name ? <Text style={styles.errorText}>{errors.name}</Text> : null}
                    {errors.images ? <Text style={styles.errorText}>{errors.images}</Text> : null}
                </View>
            </ScrollView>
            <View style={styles.buttonContainer}>
                <Button
                    title="Next"
                    borderRadius={10}
                    fontSize={16}
                    onPress={goToNextStep}
                    loading={loading}
                    disabled={!isFormComplete || loading}
                />
            </View>

            {/* Image Viewer Modal */}
            <ImageViewing
                images={images.map(uri => ({ uri }))}
                imageIndex={viewerIndex}
                visible={showImageViewer}
                onRequestClose={() => setShowImageViewer(false)}
                backgroundColor="black"
                swipeToCloseEnabled
                doubleTapToZoomEnabled
            />
        </KeyboardAvoidingView>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    scrollContainer: {
        paddingBottom: 100,
        flexGrow: 1,
    },
    imageBlock: {
        alignItems: 'center',
        paddingVertical: 20,
    },
    carouselItem: {
        justifyContent: 'center',
        alignItems: 'center',
        width,
        position: 'relative',
    },
    carouselImage: {
        width: 180,
        height: 180,
        borderRadius: 90,
        borderWidth: 2,
        borderColor: '#9188E5',
        alignSelf: 'center',
    },
    inputContainer: {
        paddingHorizontal: '5%',
    },
    label: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 10,
        color: '#333',
    },
    inputError: {
        borderColor: 'red',
    },
    errorText: {
        color: 'red',
        fontSize: 14,
        marginTop: 5,
    },
    buttonContainer: {
        padding: 20,
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
    },
    mainImage: {
        width: 220,
        height: 220,
        borderRadius: 110,
        borderWidth: 2,
        borderColor: '#9188E5',
    },
    thumbnailRow: {
        flexDirection: 'row',
        marginTop: 20,
        justifyContent: 'center',
        flexWrap: 'wrap',
    },
    thumbnailWrapper: {
        marginHorizontal: 5,
        borderRadius: 10,
        padding: 2,
        borderWidth: 2,
        borderColor: 'transparent',
    },
    activeThumbnail: {
        borderColor: '#9188E5',
    },
    thumbnailImage: {
        width: 60,
        height: 60,
        borderRadius: 10,
    },
    noteText: {
        fontSize: 14,
        color: '#666',
        marginTop: 10,
        textAlign: 'center',
        paddingHorizontal: 20,
    },
    uploadContainer: {
        width: 220,
        height: 220,
        borderRadius: 110,
        borderWidth: 2,
        borderColor: '#9188E5',
        borderStyle: 'dashed',
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#f8f8ff',
    },
    uploadImageContainer: {
        width: 220,
        height: 220,
        borderRadius: 110,
        position: 'relative',
        justifyContent: 'center',
        alignItems: 'center',
    },
    blurredImage: {
        position: 'absolute',
        width: '100%',
        height: '100%',
    },
    uploadOverlay: {
        position: 'absolute',
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: 'rgba(255, 255, 255, 0.8)',
        borderRadius: 110,
        width: '100%',
        height: '100%',
    },
    uploadText: {
        color: '#9188E5',
        fontSize: 16,
        fontWeight: '600',
        marginTop: 8,
    },
    uploadSubtext: {
        color: '#9188E5',
        fontSize: 14,
        fontWeight: '400',
        marginTop: 4,
    },
    loadingContainer: {
        justifyContent: 'center',
        alignItems: 'center',
    },
    loadingText: {
        color: '#9188E5',
        fontSize: 16,
        fontWeight: '600',
        marginTop: 10,
    },
});

export default AddPet1;