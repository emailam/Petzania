import { StyleSheet, View, TouchableOpacity, Text, KeyboardAvoidingView, ScrollView, Platform } from 'react-native';
import { Image } from 'expo-image';
import React, { useState, useContext, useEffect } from 'react';
import { AntDesign, MaterialIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import Button from '@/components/Button';
import CustomInput from '@/components/CustomInput';
import ImageUploadInput from '@/components/ImageUploadInput';
import { isValidPhoneNumber } from 'libphonenumber-js';
import { TextInput } from 'react-native-paper';

import { uploadFile } from '@/services/uploadService';
import { updateUserData } from '@/services/userService';

import { UserContext } from '@/context/UserContext';
import { PetContext } from '@/context/PetContext';
import { FlowContext } from '@/context/FlowContext';
import Toast from 'react-native-toast-message';

const EditProfile = () => {
    const defaultImage = require('@/assets/images/Defaults/default-user.png');
    const router = useRouter();

    const { user, setUser } = useContext(UserContext);
    const { pets } = useContext(PetContext);
    const { setFromPage } = useContext(FlowContext);

    const [image, setImage] = useState(user?.profilePictureURL || null);
    const [username, setUsername] = useState(user?.username || '');
    const [email, setEmail] = useState(user?.email || '');
    const [name, setName] = useState(user?.name || '');
    const [bio, setBio] = useState(user?.bio || '');
    const [phoneNumber, setPhoneNumber] = useState(user?.phoneNumber || '');
    const [errors, setErrors] = useState({
        name: '',
        phoneNumber: ''
    });
    const [isLoading, setIsLoading] = useState(false);
    const [isFormValid, setIsFormValid] = useState(false);

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

    // Validation effect
    useEffect(() => {

        // Validate name
        if (!name.trim()) {
            setError('name', 'Name is required');
        } else {
            clearError('name');
        }

        // Validate phone number if provided
        if (phoneNumber && phoneNumber.length > 0 && !isValidPhoneNumber(phoneNumber)) {
            setError('phoneNumber', 'Please enter a valid phone number including country code (e.g. +20)');
        } else {
            clearError('phoneNumber');
        }
    }, [name, phoneNumber]);

    useEffect(() => {
        const nameValid = name;
        const phoneValid = !phoneNumber || phoneNumber.length === 0 || isValidPhoneNumber(phoneNumber);
        const hasErrors = Object.values(errors).some(error => error !== '');

        setIsFormValid(nameValid && phoneValid && !hasErrors);
    }, [name, phoneNumber, errors]);

    const saveProfile = async () => {
        // Double-check validation before submitting
        if (!isFormValid) {
            Toast.show({
                type: 'error',
                text1: 'Validation Error',
                text2: 'Please fix the errors before saving',
                position: 'top',
                visibilityTime: 3000,
            });
            return;
        }

        let uploadedImageUrl = user?.profilePictureURL;
        
        // Handle image removal
        if (image === null) {
            uploadedImageUrl = "";
        } else if (image && image !== user?.profilePictureURL) {
            // Handle new image upload
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
                Toast.show({
                    type: 'error',
                    text1: 'Upload Failed',
                    text2: 'Failed to upload profile picture. Please try again.',
                    position: 'top',
                    visibilityTime: 4000,
                });
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

            const completeUpdatedUser = {
                ...user,
                ...userData,
                ...(updatedUser || {}),
            };

            setUser(completeUpdatedUser);

            Toast.show({
                type: 'success',
                text1: 'Profile Updated!',
                text2: 'Your profile has been successfully updated.',
                position: 'top',
                visibilityTime: 3000,
            });

            router.back();
        } catch (err) {
            console.error('Error updating user profile:', err.message);
            Toast.show({
                type: 'error',
                text1: 'Update Failed',
                text2: err.message || 'Failed to update profile. Please try again.',
                position: 'top',
                visibilityTime: 4000,
            });
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
                {/* Profile Image Section */}
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
                        style={{ marginVertical: 20 }}
                        currentImage={image}
                    />
                </View>
                <View style={styles.inputContainer}>
                    <Text style={styles.label}>Username</Text>
                    <CustomInput
                        disabled={true}
                        placeholderTextColor="#999"
                        style={[styles.disabledInput]}
                        value={`@${username}`}
                        mode="outlined"
                    />

                    <Text style={[styles.label, { marginTop: 12 }]}>Email</Text>
                    <CustomInput
                        disabled={true}
                        style={[styles.disabledInput]}
                        placeholderTextColor="#999"
                        value={email}
                        mode="outlined"
                    />

                    <Text style={[styles.label, { marginTop: 12 }]}>
                        Name <Text style={{ color: 'red' }}>*</Text>
                    </Text>
                    <CustomInput
                        style={[errors.name ? styles.inputError : null]}
                        placeholder="Tyler Gregory Okonma"
                        placeholderTextColor="#999"
                        value={name}
                        onChangeText={(text) => {
                            setName(text);
                        }}
                        maxLength={50}
                        mode="outlined"
                        error={!!errors.name}
                        errorMessage={errors.name}
                    />

                    <Text style={[styles.label, { marginTop: 12 }]}>Phone Number</Text>
                    <CustomInput
                        style={[errors.phoneNumber ? styles.inputError : null]}
                        placeholder="+20 101 234 5678"
                        keyboardType="phone-pad"
                        placeholderTextColor="#999"
                        value={phoneNumber}
                        onChangeText={(text) => {
                            setPhoneNumber(text);
                        }}
                        maxLength={20}
                        mode="outlined"
                        error={!!errors.phoneNumber}
                        errorMessage={errors.phoneNumber}
                    />

                    <Text style={[styles.label, { marginTop: 12 }]}>Bio</Text>
                    <CustomInput
                        style={[styles.bioInput]}
                        placeholder="Tell us about yourself"
                        placeholderTextColor="#999"
                        multiline
                        numberOfLines={4}
                        value={bio}
                        onChangeText={setBio}
                        maxLength={255}
                        returnKeyType="done"
                        mode="outlined"
                        right={<TextInput.Affix text={`${bio.length}/255`} />}
                    />
                </View>
                <View style={styles.petsSection}>
                    <Text style={styles.sectionTitle}>Your Pets</Text>
                    <TouchableOpacity
                        style={styles.viewAllPetsButton}
                        onPress={() =>{ setFromPage("EditProfile"); router.push('/PetModule/AllPets');}}
                        activeOpacity={0.8}
                    >
                        <View style={styles.viewAllPetsContent}>
                            <View style={styles.viewAllPetsIcon}>
                                <MaterialIcons name="pets" size={20} color="#fff" />
                            </View>
                            <View style={styles.viewAllPetsTextContainer}>
                                <Text style={styles.viewAllPetsTitle}>View All Pets</Text>
                                <Text style={styles.viewAllPetsCount}>{pets.length} pets total</Text>
                            </View>
                        </View>
                        <View style={styles.viewAllPetsArrow}>
                            <MaterialIcons name="arrow-forward-ios" size={18} color="#9188E5" />
                        </View>
                    </TouchableOpacity>
                </View>
            </ScrollView>

            <View style={styles.buttonContainer}>
                <Button 
                    title="Save Changes" 
                    borderRadius={10} 
                    fontSize={16} 
                    onPress={saveProfile} 
                    loading={isLoading}
                    disabled={!isFormValid || isLoading}
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
        alignItems: 'center',
        justifyContent: 'center',
    },
    imageContainer: {
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 20,
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
        marginBottom: 20,
    },
    label: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 10,
        color: '#333',
        alignSelf: 'flex-start',
    },
    disabledInput: {
        backgroundColor: '#f5f5f5',
        color: '#666',
        borderColor: '#e0e0e0',
    },
    inputError: {
        borderColor: 'red',
    },
    bioInput: {
        minHeight: 120, // Set minimum height for multiline
        textAlignVertical: 'top', // Align text to top for multiline
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
    petsSection: {
        paddingHorizontal: '5%',
        width: '100%',
        marginBottom: 20,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 15,
        color: '#333',
    },
    viewAllPetsButton: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingVertical: 16,
        paddingHorizontal: 16,
        backgroundColor: '#fff',
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#e8e6f3',
        shadowColor: '#9188E5',
        shadowOffset: {
            width: 0,
            height: 2,
        },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 3,
    },
    viewAllPetsContent: {
        flexDirection: 'row',
        alignItems: 'center',
        flex: 1,
    },
    viewAllPetsIcon: {
        width: 36,
        height: 36,
        borderRadius: 18,
        backgroundColor: '#9188E5',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    viewAllPetsTextContainer: {
        flex: 1,
    },
    viewAllPetsTitle: {
        fontSize: 16,
        color: '#333',
        fontWeight: '600',
        marginBottom: 2,
    },
    viewAllPetsCount: {
        fontSize: 13,
        color: '#9188E5',
        fontWeight: '500',
    },
    viewAllPetsArrow: {
        width: 24,
        height: 24,
        alignItems: 'center',
        justifyContent: 'center',
    },
});

export default EditProfile;