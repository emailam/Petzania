import {
    StyleSheet,
    View,
    Image,
    TouchableOpacity,
    Text,
    TextInput,
    Dimensions,
    KeyboardAvoidingView,
    ScrollView,
    FlatList,
    Platform,
} from 'react-native';
import React, { useState, useContext, useRef } from 'react';
import * as ImagePicker from 'expo-image-picker';
import { AntDesign } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { ActivityIndicator } from 'react-native-paper';

import { uploadFiles } from '@/services/uploadService';

import Button from '@/components/Button';
import { PetContext } from '@/context/PetContext';

const { width } = Dimensions.get('window');

const AddPet1 = () => {
    const flatListRef = useRef(null);
    const [loading, setLoading] = useState(false);

    const defaultImage = require('../../assets/images/AddPet/Pet Default Pic.png');
    const { pet, setPet } = useContext(PetContext);
    const [images, setImages] = useState(pet.images || []);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [error, setError] = useState('');
    const router = useRouter();

    const pickImage = async () => {
        setLoading(true);
        let result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ['images'],
            allowsMultipleSelection: true,
            quality: 0.8,
            aspect: [1, 1],
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
            router.push('/RegisterModule/AddPet2');
        }
    };

    const renderItem = ({ item }) => (
        <View style={styles.carouselItem}>
            <Image source={{ uri: item }} style={styles.carouselImage} />
            <TouchableOpacity
                onPress={() => deleteImage(item)}
                style={styles.trashIconMain}
                disabled={loading}
            >
                <AntDesign name="delete" size={24} style={styles.icon} />
            </TouchableOpacity>
        </View>
    );

    return (
        <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            style={styles.container}
        >
            <ScrollView
                contentContainerStyle={styles.scrollContainer}
                keyboardShouldPersistTaps="handled"
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
        paddingVertical: 20,
        alignItems: 'center',
        justifyContent: 'center',
    },
    imageBlock: {
        alignItems: 'center',
        marginBottom: 20,
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
    trashIconMain: {
        position: 'absolute',
        bottom: 10,
        alignSelf: 'center',
        backgroundColor: 'white',
        padding: 8,
        borderRadius: 20,
        elevation: 3,
    },
    icon: {
        color: '#9188E5',
    },
    inputContainer: {
        paddingHorizontal: '5%',
        width: '100%',
    },
    label: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 10,
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
        marginTop: 10,
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