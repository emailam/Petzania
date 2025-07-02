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
    const [error, setError] = useState('');
    const [showImageViewer, setShowImageViewer] = useState(false);
    const [viewerIndex, setViewerIndex] = useState(0);
    const router = useRouter();

    useEffect(() => {
        setPet({});
    }, []);

    const pickImage = async () => {
        setLoading(true);
        let result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ['images'],
            allowsMultipleSelection: true,
            quality: 0.7,
            selectionLimit: 6,
        });
        if (!result.canceled) {
            setLoading(false);
            const uris = result.assets.map(asset => asset.uri);
            const newImages = [...images, ...uris];
            setImages(newImages);
            setCurrentIndex(0);
        } else {
            setLoading(false);
        }
    };

    const deleteImage = (uriToDelete) => {
        const updatedImages = images.filter(uri => uri !== uriToDelete);
        setImages(updatedImages);
        if (currentIndex >= updatedImages.length) {
            setCurrentIndex(Math.max(0, updatedImages.length - 1));
        }
    };

    const goToNextStep = async () => {
        if (!pet?.name?.trim()) {
            setError("Pet's name is required!");
            return;
        }

        setLoading(true);
        setError('');

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
            setError('Something went wrong while uploading. Please try again.');
        } finally {
            setLoading(false);
            router.push('/PetModule/AddPet2');
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
                                    setCurrentIndex(index);
                                }}
                                keyExtractor={(item, index) => index.toString()}
                                renderItem={renderItem}
                            />
                            <View style={styles.thumbnailRow}>
                                {images.map((uri, index) => (
                                    <TouchableOpacity
                                        disabled={loading}
                                        onPress={() => {
                                            flatListRef.current?.scrollToIndex({ index, animated: true });
                                            setCurrentIndex(index);
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

                            {/* Add the Add More Photos button if less than 6 images */}
                            {images.length > 0 && images.length < 6 && (
                                <TouchableOpacity
                                    style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginTop: 15, backgroundColor: '#f8f8ff', borderRadius: 12, borderWidth: 2, borderColor: '#9188E5', borderStyle: 'dashed', paddingVertical: 12, paddingHorizontal: 24 }}
                                    onPress={pickImage}
                                    disabled={loading}
                                >
                                    <AntDesign name="plus" size={24} color="#9188E5" />
                                    <Text style={{ color: '#9188E5', fontSize: 16, fontWeight: '600', marginLeft: 8 }}>Add More Photos</Text>
                                </TouchableOpacity>
                            )}
                        </>
                    ) : (
                        <TouchableOpacity onPress={pickImage} style={{ marginBottom: 15 }} disabled={loading}>
                            {loading ? (
                                <ActivityIndicator size="large" color="#9188E5" />
                            ) : (
                                <Image
                                    source={defaultImage}
                                    style={styles.mainImage}
                                />
                            )}
                        </TouchableOpacity>
                    )}
                </View>
                <View style={styles.inputContainer}>
                    <Text style={styles.label}>
                        What's your pet's name?
                        <Text style={{ fontSize: 18, color: 'red' }}>*</Text>
                    </Text>
                    <TextInput
                        style={[styles.input, error ? styles.inputError : null]}
                        placeholder="Pet's name"
                        value={pet.name}
                        onChangeText={(name) => {
                            setPet({ ...pet, name });
                            setError('');
                        }}
                        returnKeyType="done"
                    />
                    {error ? <Text style={styles.errorText}>{error}</Text> : null}
                </View>
            </ScrollView>
            <View style={styles.buttonContainer}>
                <Button title="Next" borderRadius={10} fontSize={16} onPress={goToNextStep} loading={loading} />
            </View>
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
        width: 220,
        height: 220,
        borderRadius: 110,
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
    input: {
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
});

export default AddPet1;