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
    Platform,
} from 'react-native';
import React, { useState, useContext } from 'react';
import * as ImagePicker from 'expo-image-picker';
import { AntDesign } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import Button from '@/components/Button';
import { isValidPhoneNumber } from 'libphonenumber-js';

import { UserContext } from '@/context/UserContext';

import api from '@/api/axiosInstance';

const ProfileSetUp1 = () => {
    const defaultImage = require('@/assets/images/Defaults/default-user.png');
    const router = useRouter();

    const [image, setImage] = useState(null);
    const [name, setName] = useState('');
    const [bio, setBio] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [error, setError] = useState('');
    const [phoneError, setPhoneError] = useState('');

    const { user, setUser } = useContext(UserContext);

    const updateUserData = async (userData) => {
        console.log('Updating user data:', user);
        try {
            const response = await api.patch(`/user/auth/${user.userId}`, userData);
            if (response.status === 200) {
                setUser((prevUser) => ({ ...prevUser, ...userData }));
                console.log('User data updated successfully:', response.data);
            } else {
                console.error('Failed to update user data. Status:', response.status);
            }
        } catch (error) {
            console.error('Error updating user data:', error.response?.data?.message || error.message);
        }
        finally {
            
        }
    };

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

        const userData = {
            name,
            phoneNumber,
            bio,
            profilePictureURL: image || '',
        };

        await updateUserData(userData);

        setError('');
        setPhoneError('');

        router.push('/RegisterModule/ProfileSetUp2');
    };

    return (
    <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.container}
    >
        <ScrollView contentContainerStyle={styles.scrollContainer} keyboardShouldPersistTaps="handled">
        <View style={styles.imageContainer}>
            <TouchableOpacity onPress={pickImage} style={styles.imageWrapper}>
            <Image source={image ? { uri: image } : defaultImage} style={styles.image} />
            {image && (
                <TouchableOpacity onPress={deleteImage} style={styles.trashIcon}>
                <AntDesign name="delete" size={20} style={styles.icon} />
                </TouchableOpacity>
            )}
            </TouchableOpacity>
        </View>

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
            <Button title="Next" borderRadius={10} fontSize={16} onPress={goToNextStep} />
        </View>
    </KeyboardAvoidingView>
    );
};

export default ProfileSetUp1;

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
