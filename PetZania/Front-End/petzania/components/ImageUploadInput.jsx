import React, { useState, useEffect, useRef } from 'react';
import { StyleSheet, View, TouchableOpacity, ActivityIndicator, Text } from 'react-native';
import { Image } from 'expo-image';
import { MaterialIcons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import ImageViewing from 'react-native-image-viewing';

import BottomSheet from '../components/BottomSheet';


const ImageUploadInput = ({
    defaultImage,
    onImageChange,
    size = 180,
    isUploading = false,
    allowsMultipleSelection = false,
    currentImage = null,
    style
}) => {
    const [image, setImage] = useState(currentImage);
    const [showImageViewer, setShowImageViewer] = useState(false);
    const [viewerIndex, setViewerIndex] = useState(0);
    const bottomSheetRef = useRef(null);

    // Sync external currentImage with internal state
    useEffect(() => {
        setImage(currentImage);
    }, [currentImage]);

    const pickImageFromLibrary = async () => {
        let result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ['images'],
            allowsEditing: !allowsMultipleSelection,
            aspect: [1, 1],
            quality: 0.7,
            allowsMultipleSelection: allowsMultipleSelection,
        });

        if (!result.canceled) {
            if (allowsMultipleSelection) {
                const selectedImages = result.assets.map(asset => asset.uri);
                setImage(selectedImages[0]);
                onImageChange?.(selectedImages);
            } else {
                const selectedImage = result.assets[0].uri;
                setImage(selectedImage);
                onImageChange?.(selectedImage);
            }
        }
    };

    const viewPhoto = () => {
        setShowImageViewer(true);
    }

    const takePhoto = async () => {
        const permissionResult = await ImagePicker.requestCameraPermissionsAsync();

        if (permissionResult.granted === false) {
            alert("You've refused to allow this app to access your camera!");
            return;
        }

        let result = await ImagePicker.launchCameraAsync({
            mediaTypes: ['images'],
            allowsEditing: true,
            aspect: [1, 1],
            quality: 0.7,
        });

        if (!result.canceled) {
            const selectedImage = result.assets[0].uri;
            setImage(selectedImage);
            onImageChange?.(allowsMultipleSelection ? [selectedImage] : selectedImage);
        }
    };

    const removeImage = () => {
        setImage(null);
        onImageChange?.(null);
    };

    const handleImagePress = () => {
        bottomSheetRef.current?.present();
    };

    const handleBottomSheetAction = (action) => {
        bottomSheetRef.current?.dismiss();
        setTimeout(() => {
            switch (action) {
                case 'viewPhoto':
                    viewPhoto();
                    break;
                case 'takePhoto':
                    takePhoto();
                    break;
                case 'pickFromLibrary':
                    pickImageFromLibrary();
                    break;
                case 'removeImage':
                    removeImage();
                    break;
            }
        }, 300);
    };

    const imageSource = image ? { uri: image } : defaultImage;
    const borderRadius = size / 2;

    return (
        <View style={[styles.container, style]}>
            <TouchableOpacity
                onPress={handleImagePress}
                style={[
                    styles.imageWrapper,
                    { width: size, height: size, borderRadius }
                ]}
                disabled={isUploading}
            >
                {imageSource ? (
                    <Image
                        source={imageSource}
                        style={[
                            styles.image,
                            { width: size, height: size, borderRadius }
                        ]}
                    />
                ) : (
                    <View style={[
                        styles.placeholderContainer,
                        { width: size, height: size, borderRadius }
                    ]}>
                        <MaterialIcons name="add-a-photo" size={size * 0.25} color="#9188E5" />
                    </View>
                )}

                <View style={[
                    styles.iconContainer,
                    {
                        width: size * 0.25,
                        height: size * 0.25,
                        borderRadius: (size * 0.25) / 2,
                        bottom: size * 0.01,
                        right: size * 0.01
                    }
                ]}>
                    {isUploading ? (
                        <ActivityIndicator
                            size={size * 0.12}
                            color="#FFF"
                        />
                    ) : (
                        <MaterialIcons
                            name="camera-alt"
                            size={size * 0.12}
                            color="#FFF"
                        />
                    )}
                </View>
            </TouchableOpacity>

            <BottomSheet
                ref={bottomSheetRef}
                snapPoints={[image ? '50%' : '30%']}
            >
                <View style={styles.bottomSheetHeader}>
                    <Text style={styles.bottomSheetTitle}>Photo Actions</Text>
                </View>

                <View style={styles.bottomSheetActions}>
                    {image && (
                        <TouchableOpacity
                            style={styles.bottomSheetAction}
                            onPress={() => handleBottomSheetAction('viewPhoto')}
                        >
                            <View style={styles.actionIconContainer}>
                                <MaterialIcons name="photo" size={24} color="#9188E5" />
                            </View>
                            <Text style={styles.actionText}>View Photo</Text>
                        </TouchableOpacity>
                    )}

                    <TouchableOpacity
                        style={styles.bottomSheetAction}
                        onPress={() => handleBottomSheetAction('takePhoto')}
                    >
                        <View style={styles.actionIconContainer}>
                            <MaterialIcons name="camera-alt" size={24} color="#9188E5" />
                        </View>
                        <Text style={styles.actionText}>Take Photo</Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                        style={styles.bottomSheetAction}
                        onPress={() => handleBottomSheetAction('pickFromLibrary')}
                    >
                        <View style={styles.actionIconContainer}>
                            <MaterialIcons name="photo-library" size={24} color="#9188E5" />
                        </View>
                        <Text style={styles.actionText}>Choose from Library</Text>
                    </TouchableOpacity>

                    {image && (
                        <TouchableOpacity
                            style={[styles.bottomSheetAction]}
                            onPress={() => handleBottomSheetAction('removeImage')}
                        >
                            <View style={[styles.actionIconContainer]}>
                                <MaterialIcons name="delete-outline" size={24} color="#FF3B30" />
                            </View>
                            <Text style={[styles.actionText, styles.removeText]}>Remove Photo</Text>
                        </TouchableOpacity>
                    )}
                </View>
            </BottomSheet>
            <ImageViewing
                images={[{ uri: image }]}
                imageIndex={viewerIndex}
                visible={showImageViewer}
                onRequestClose={() => setShowImageViewer(false)}
                backgroundColor="black"
                swipeToCloseEnabled
                doubleTapToZoomEnabled
            />
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        alignItems: 'center',
        justifyContent: 'center',
    },
    imageWrapper: {
        position: 'relative',
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: '#9188E5',
    },
    image: {
        borderWidth: 1,
        borderColor: '#9188E5',
    },
    placeholderContainer: {
        borderWidth: 1,
        borderColor: '#9188E5',
        borderStyle: 'dashed',
        backgroundColor: '#F8F7FF',
        alignItems: 'center',
        justifyContent: 'center',
    },
    iconContainer: {
        position: 'absolute',
        backgroundColor: '#9188E5',
        alignItems: 'center',
        justifyContent: 'center',
        elevation: 4,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.25,
        shadowRadius: 3.84,
    },
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
    removeText: {
        color: '#FF3B30',
    },
});

export default ImageUploadInput;