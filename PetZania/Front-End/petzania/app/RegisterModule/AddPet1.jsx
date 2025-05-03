import { StyleSheet, View, Image, TouchableOpacity, Text, TextInput, Dimensions, KeyboardAvoidingView, ScrollView, Platform } from 'react-native';
import React, { useState, useContext, useEffect } from 'react';
import * as ImagePicker from 'expo-image-picker';
import { AntDesign } from '@expo/vector-icons';
import { useRouter } from 'expo-router';

import Button from '@/components/Button';
import { PetContext } from '@/context/PetContext';

const AddPet1 = () => {
    const defaultImage = require('../../assets/images/AddPet/Pet Default Pic.png');
    const { pet, setPet } = useContext(PetContext);
    const [image, setImage] = useState(pet.image || null);
    const [error, setError] = useState('');
    const router = useRouter();

    const goToNextStep = () => {
        if (!pet.name.trim()) {
            setError("Pet's name is required!");
            return;
        }
        setError('');
        router.push('/RegisterModule/AddPet2');
    };

    const pickImage = async () => {
        let result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ['images'],
            aspect: [1, 1],
            quality: 1,
            allowsEditing: true,
            allowsMultipleSelection: true,
        });

        if (!result.canceled) {
            setImage(result.assets[0].uri);
            setPet({ ...pet, image: result.assets[0].uri });
            console.log(result.assets[0].uri);
        }
    };

    const deleteImage = () => {
        setImage(null);
        setPet({ ...pet, image: null });
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
                    <Text style={styles.label}>What's your pet's name?<Text style = {{fontSize: '18', color: 'red'}}>*</Text></Text>
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
                <Button title="Next" borderRadius={10} fontSize={16} onPress={goToNextStep} />
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
        boxShadow: '0px 4px 4px rgba(0, 0, 0, 0.25)',
    },
    icon: {
        color: '#9188E5',
    },
    inputContainer: {
        paddingHorizontal: '5%',
        width: '100%',
        alignItems: 'center',
        marginTop: 40,
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
});

export default AddPet1;