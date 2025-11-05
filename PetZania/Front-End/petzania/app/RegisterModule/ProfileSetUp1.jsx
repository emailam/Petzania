import { StyleSheet, View, Text, KeyboardAvoidingView, ScrollView, Platform } from 'react-native';

import React, { useState, useContext, useEffect } from 'react';

import { useRouter } from 'expo-router';
import Button from '@/components/Button';
import ImageUploadInput from '@/components/ImageUploadInput';
import { isValidPhoneNumber } from 'libphonenumber-js';
import ImageViewing from 'react-native-image-viewing';

import { uploadFile } from '@/services/uploadService';
import { updateUserData } from '@/services/userService';

import { UserContext } from '@/context/UserContext';
import CustomInput from '@/components/CustomInput';
import { TextInput } from 'react-native-paper';

const ProfileSetUp1 = () => {
    const router = useRouter();

    const { user, setUser } = useContext(UserContext);

    const [image, setImage] = useState(user?.profilePictureURL || null);
    const [name, setName] = useState(user?.name || '');
    const [bio, setBio] = useState(user?.bio || '');
    const [phoneNumber, setPhoneNumber] = useState(user?.phoneNumber || '');
    const [errors, setErrors] = useState({
        name: '',
        phoneNumber: '',
        bio: ''
    });
    const [showImageViewer, setShowImageViewer] = useState(false);
    const [isFormComplete, setIsFormComplete] = useState(false);

    const defaultImage = require('@/assets/images/Defaults/default-user.png');

    const [isLoading, setIsLoading] = useState(false);

    // Helper function to set individual field errors (like RegisterForm)
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

    // Check if form is complete and valid (like RegisterForm)
    useEffect(() => {
        const nameValid = name && name.trim().length >= 2; // Name minimum 2 chars
        const phoneValid = !phoneNumber || phoneNumber.length === 0 || isValidPhoneNumber(phoneNumber);
        const bioValid = bio.length <= 255; // Bio within character limit

        // Form is complete when name is valid and phone is valid (if provided)
        const formValid = nameValid && phoneValid && bioValid && !errors.name && !errors.phoneNumber;

        setIsFormComplete(formValid);
    }, [name, phoneNumber, bio, errors]);

    const goToNextStep = async () => {
        // Validate all fields before proceeding
        let hasErrors = false;

        // Validate name
        if (!name.trim()) {
            setError('name', 'Full name is required');
            hasErrors = true;
        }

        // Validate phone number if provided
        if (phoneNumber && phoneNumber.length > 0 && !isValidPhoneNumber(phoneNumber)) {
            setError('phoneNumber', 'Please enter a valid phone number including country code (e.g. +20)');
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }

        let uploadedImageUrl = user?.profilePictureURL;
        if(image && image !== user?.profilePictureURL) {
            try {
                const file = {
                    uri: image,
                    type: 'image/jpeg',
                    name: 'profile.jpg',
                };
                setIsLoading(true);
                uploadedImageUrl = await uploadFile(file);
            } catch (err) {
                console.error('Failed to upload profile picture:', err.message);
                return;
            } finally {
                setIsLoading(false);
            }
        }

        const userData = {
            name,
            phoneNumber,
            bio,
            profilePictureURL: uploadedImageUrl || "",
        };

        try {
            setIsLoading(true);
            const updatedUser = await updateUserData(user.userId, userData);
            setUser((prevUser) => ({ ...prevUser, ...updatedUser }));
            setError('');
            router.push('/RegisterModule/ProfileSetUp2');
        } catch (err) {
            console.error('Error updating user profile:', err.message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            style={styles.container}
        >
            <ScrollView contentContainerStyle={styles.scrollContainer} keyboardShouldPersistTaps="handled">
            <View style={styles.imageContainer}>
                <ImageUploadInput
                    defaultImage={defaultImage}
                    onImageChange={(imageUri) => {
                        if (imageUri) {
                            setImage(imageUri);
                        } else {
                            setImage(null);
                        }
                    }}
                    size={180}
                    isUploading={isLoading}
                    currentImage={image}
                />
            </View>

            <ImageViewing
                images={[{ uri: image || '' }]}
                imageIndex={0}
                visible={showImageViewer}
                onRequestClose={() => setShowImageViewer(false)}
                backgroundColor="black"
                swipeToCloseEnabled
                doubleTapToZoomEnabled
            />
            <View style={styles.inputContainer}>
                <Text style={styles.label}>Name <Text style={{ color: 'red' }}>*</Text> </Text>
                <CustomInput
                    style={[errors.name ? styles.inputError : null]}
                    placeholder={"Tyler Gregory"}
                    placeholderTextColor="#999"
                    value={name}
                    maxLength={50}
                    onChangeText={(text) => {
                        setName(text);
                        clearError('name');
                    }}
                    returnKeyType="next"
                    mode="outlined"
                    error={!!errors.name}
                />
                {errors.name ? <Text style={styles.errorText}>{errors.name}</Text> : null}

                <Text style={[styles.label, { marginTop: 12 }]}>Phone Number</Text>
                <CustomInput
                    style={[errors.phoneNumber ? styles.inputError : null]}
                    placeholder={"+201123456789"}
                    placeholderTextColor="#999"
                    keyboardType="phone-pad"
                    value={phoneNumber}
                    maxLength={20}
                    onChangeText={(text) => {
                        setPhoneNumber(text);
                        clearError('phoneNumber');
                    }}
                    returnKeyType="next"
                    mode="outlined"
                    error={!!errors.phoneNumber}
                />
                {errors.phoneNumber ? <Text style={styles.errorText}>{errors.phoneNumber}</Text> : null}

                <Text style={[styles.label, { marginTop: 12 }]}>Bio</Text>
                <CustomInput
                    style={[styles.bioInput, errors.bio ? styles.inputError : null]}
                    placeholder={"Tell us about yourself"}
                    placeholderTextColor="#999"
                    multiline
                    numberOfLines={4}
                    value={bio}
                    maxLength={255}
                    onChangeText={(text) => {
                        setBio(text);
                        clearError('bio');
                    }}
                    returnKeyType="done"
                    mode="outlined"
                    error={!!errors.bio}
                    right={<TextInput.Affix text={`${bio.length}/255`} />}
                />
                {errors.bio ? <Text style={styles.errorText}>{errors.bio}</Text> : null}
            </View>
            </ScrollView>

            <View style={styles.buttonContainer}>
                <Button
                    title="Next"
                    borderRadius={10}
                    fontSize={16}
                    onPress={goToNextStep}
                    loading={isLoading}
                    disabled={!isFormComplete}
                />
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
        paddingBottom: 50,
        alignItems: 'center',
        justifyContent: 'center',
    },
    imageContainer: {
        justifyContent: 'center',
        alignItems: 'center',
        paddingVertical: 20,
    },
    imageWrapper: {
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
    },
    image: {
        width: 180,
        height: 180,
        borderRadius: 90,
        borderWidth: 2,
        borderColor: '#9188E5',
    },
    placeholderContainer: {
        width: 180,
        height: 180,
        borderRadius: 90,
        borderWidth: 2,
        borderColor: '#9188E5',
        borderStyle: 'dashed',
        backgroundColor: '#F8F7FF',
        justifyContent: 'center',
        alignItems: 'center',
        position: 'relative',
    },
    placeholderImageArea: {
        justifyContent: 'center',
        alignItems: 'center',
    },
    uploadText: {
        fontSize: 16,
        fontWeight: '600',
        color: '#9188E5',
        marginTop: 8,
    },
    uploadSubtext: {
        fontSize: 12,
        color: '#666',
        marginTop: 4,
    },
    uploadIconContainer: {
        position: 'absolute',
        bottom: 10,
        right: 10,
        backgroundColor: '#9188E5',
        borderRadius: 20,
        width: 40,
        height: 40,
        justifyContent: 'center',
        alignItems: 'center',
        elevation: 4,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.25,
        shadowRadius: 3.84,
    },
    trashIcon: {
        position: 'absolute',
        bottom: -30,
        alignSelf: 'center',
        backgroundColor: 'white',
        padding: 8,
        borderRadius: 20,
        elevation: 4,
    },
    icon: {
        color: '#9188E5',
    },
    inputContainer: {
        paddingHorizontal: '5%',
        width: '100%',
        marginTop: 20,
    },
    label: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 6,
        color: '#333',
        alignSelf: 'flex-start',
    },
    errorText: {
        color: 'red',
        fontSize: 14,
        marginTop: 5,
        alignSelf: 'flex-start',
    },
    buttonContainer: {
        padding: 20,
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
    },
    bioInput: {
        minHeight: 120, // Set minimum height for multiline
        textAlignVertical: 'top', // Align text to top for multiline
    },
});

export default ProfileSetUp1;