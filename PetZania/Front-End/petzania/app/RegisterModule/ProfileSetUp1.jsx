import {
    StyleSheet,
    View,
    Image,
    TouchableOpacity,
    Text,
    TextInput,
    KeyboardAvoidingView,
    ScrollView,
    Platform,
} from 'react-native';
import React, { useState, useContext } from 'react';
import * as ImagePicker from 'expo-image-picker';
import { AntDesign } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import Button from '@/components/Button';
import { isValidPhoneNumber } from 'libphonenumber-js';
import { useActionSheet } from '@expo/react-native-action-sheet';
import ImageViewing from 'react-native-image-viewing';

import { uploadFile } from '@/services/uploadService';
import { updateUserData } from '@/services/userService';

import { UserContext } from '@/context/UserContext';

const ProfileSetUp1 = () => {
    const defaultImage = require('@/assets/images/Defaults/default-user.png');
    const router = useRouter();

    const { user, setUser } = useContext(UserContext);

    const [image, setImage] = useState(user?.profilePictureURL || null);
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
            aspect: [1, 1],
            quality: 1,
            allowsEditing: true,
        });

        if (!result.canceled) {
            setImage(result.assets[0].uri);
        }
    };

    const deleteImage = () => {
        setImage(null);
    };

    const goToNextStep = async () => {
        const isPhoneEmpty = phoneNumber.length === 0;
        const isValid = isPhoneEmpty || isValidPhoneNumber(phoneNumber);

        if (!name.trim()) {
            setError('Full name is required!');
            return;
        }

        if (!isValid) {
            setPhoneError('Please enter a valid phone number including country code (e.g. +1)');
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
            setPhoneError('');
            router.push('/RegisterModule/ProfileSetUp2');
        } catch (err) {
            console.error('Error updating user profile:', err.message);
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
                <Text style={styles.label}>
                    Full Name <Text style={{ color: 'red' }}>*</Text>
                </Text>
                <TextInput
                    style={[styles.input, error ? styles.inputError : null]}
                    placeholder="Tyler Gregory Okonma"
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
                    multiline
                    value={bio}
                    onChangeText={setBio}
                />
            </View>
            </ScrollView>

            <View style={styles.buttonContainer}>
                <Button title="Next" borderRadius={10} fontSize={16} onPress={goToNextStep} loading={isLoading}/>
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
    },
    imageWrapper: {
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
    },
    image: {
        width: 220,
        height: 220,
        borderRadius: 110,
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
    },
    label: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 10,
        color: '#333',
        alignSelf: 'flex-start',
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
    phoneContainer: {
        width: '100%',
        height: 50,
        borderWidth: 1,
        borderColor: '#9188E5',
        borderRadius: 10,
        overflow: 'hidden',
    },
    phoneTextContainer: {
        paddingVertical: 0,
        borderRadius: 10,
        backgroundColor: '#fff',
    },
});

export default ProfileSetUp1;