import { StyleSheet, View, TouchableOpacity, Text, TextInput, KeyboardAvoidingView, ScrollView, Platform } from 'react-native';
import { Image } from 'expo-image';
import React, { useState, useContext } from 'react';
import * as ImagePicker from 'expo-image-picker';
import { AntDesign, MaterialIcons, Feather } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import Button from '@/components/Button';
import { isValidPhoneNumber } from 'libphonenumber-js';
import { useActionSheet } from '@expo/react-native-action-sheet';
import ImageViewing from 'react-native-image-viewing';

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
    const [error, setError] = useState('');
    const [phoneError, setPhoneError] = useState('');
    const [showImageViewer, setShowImageViewer] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const { showActionSheetWithOptions } = useActionSheet();

    const pickImage = async () => {
        let result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ['images'],
            allowsEditing: true,
            aspect: [1, 1],
            quality: 0.7,
        });

        if (!result.canceled) {
            setImage(result.assets[0].uri);
        }
    };

    const deleteImage = () => {
        setImage(null);
        user.profilePictureURL = null;
        setUser((prevUser) => ({ ...prevUser, profilePictureURL: null }));
    };

    const saveProfile = async () => {
        const isPhoneEmpty = phoneNumber.length === 0;
        const isValid = isPhoneEmpty || isValidPhoneNumber(phoneNumber);

        if (!name.trim()) {
            setError('Full name is required!');
            Toast.show({
                type: 'error',
                text1: 'Validation Error',
                text2: 'Full name is required!',
                position: 'top',
                visibilityTime: 3000,
            });
            return;
        }

        if (!isValid) {
            setPhoneError('Please enter a valid phone number including country code (e.g. +1)');
            Toast.show({
                type: 'error',
                text1: 'Validation Error',
                text2: 'Please enter a valid phone number including country code (e.g. +1)',
                position: 'top',
                visibilityTime: 4000,
            });
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

            setError('');
            setPhoneError('');

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

    const handleImagePress = () => {
        const options = [
            ...(image ? ['View Profile Picture'] : []),
            'Change Profile Picture',
            ...(image ? ['Remove Profile Picture'] : []),
            'Cancel',
        ];
        const cancelButtonIndex = options.length - 1;
        const destructiveButtonIndex = image ? options.indexOf('Remove Profile Picture') : undefined;

        showActionSheetWithOptions(
            {
                options,
                cancelButtonIndex,
                destructiveButtonIndex,
            },
            (buttonIndex) => {
                if (image && buttonIndex === 0) setShowImageViewer(true);
                else if ((image && buttonIndex === 1) || (!image && buttonIndex === 0)) pickImage();
                else if (image && buttonIndex === options.indexOf('Remove Profile Picture')) deleteImage();
            }
        );
    };

    return (
        <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            style={styles.container}
        >
            <ScrollView contentContainerStyle={styles.scrollContainer} keyboardShouldPersistTaps="handled">
                {/* Profile Image Section */}
                <View style={styles.imageContainer}>
                    <TouchableOpacity onPress={handleImagePress} style={styles.imageWrapper}>
                        <Image source={image ? { uri: image } : defaultImage} style={styles.image} />
                        {image && (
                            <TouchableOpacity onPress={deleteImage} style={styles.trashIcon}>
                                <AntDesign name="delete" size={20} style={styles.icon} />
                            </TouchableOpacity>
                        )}
                    </TouchableOpacity>
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
                    <Text style={styles.label}>Username</Text>
                    <TextInput
                        style={[styles.input, styles.disabledInput]}
                        placeholder="@username"
                        placeholderTextColor="#999"
                        value={`@${username}`}
                        editable={false}
                    />

                    <Text style={[styles.label, { marginTop: 12 }]}>Email</Text>
                    <TextInput
                        style={[styles.input, styles.disabledInput]}
                        placeholder="Email"
                        placeholderTextColor="#999"
                        value={email}
                        editable={false}
                    />

                    <Text style={[styles.label, { marginTop: 12 }]}>
                        Full Name <Text style={{ color: 'red' }}>*</Text>
                    </Text>
                    <TextInput
                        style={[styles.input, error ? styles.inputError : null]}
                        placeholder="Tyler Gregory Okonma"
                        placeholderTextColor="#999"
                        value={name}
                        onChangeText={(text) => {
                            setName(text);
                            setError('');
                        }}
                    />
                    {error ? <Text style={styles.errorText}>{error}</Text> : null}

                    <Text style={[styles.label, { marginTop: 12 }]}>Phone Number</Text>
                    <TextInput
                        style={[styles.input, phoneError ? styles.inputError : null]}
                        placeholder="+20 101 234 5678"
                        keyboardType="phone-pad"
                        placeholderTextColor="#999"
                        value={phoneNumber}
                        onChangeText={(text) => {
                            setPhoneNumber(text);
                            setPhoneError('');
                        }}
                    />
                    {phoneError ? <Text style={styles.errorText}>{phoneError}</Text> : null}

                    <Text style={[styles.label, { marginTop: 12 }]}>Bio</Text>
                    <TextInput
                        style={[styles.input, styles.textArea]}
                        placeholder="About"
                        placeholderTextColor="#999"
                        multiline
                        value={bio}
                        onChangeText={setBio}
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
                <Button title="Save Changes" borderRadius={10} fontSize={16} onPress={saveProfile} loading={isLoading} />
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
        alignItems: 'center',
        marginTop: 20,
        marginBottom: 20,
    },
    label: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 10,
        color: '#333',
        alignSelf: 'flex-start',
    },    input: {
        width: '100%',
        height: 50,
        borderWidth: 1,
        borderColor: '#9188E5',
        borderRadius: 10,
        paddingHorizontal: 15,
        fontSize: 16,
        backgroundColor: '#fff',
    },
    disabledInput: {
        backgroundColor: '#f5f5f5',
        color: '#666',
        borderColor: '#e0e0e0',
    },
    inputError: {
        borderColor: 'red',
    },
    textArea: {
        height: 100,
        textAlignVertical: 'top',
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