import {
    View, Text, Image, StyleSheet, ScrollView, TouchableOpacity,
    Alert, TextInput, Platform, ActivityIndicator
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useContext, useEffect, useState } from 'react';
import { Dropdown } from 'react-native-element-dropdown';
import * as ImagePicker from 'expo-image-picker';
import * as DocumentPicker from 'expo-document-picker';

import { PetContext } from '@/context/PetContext';

import Button from '@/components/Button';

import DateOfBirthInput from '@/components/DateOfBirthInput';
import { PETS } from '@/constants/PETS';
import { PET_BREEDS } from '@/constants/PETBREEDS';
import { updatePet, deletePet, getAllPetsByUserId } from '@/services/petService';
import Toast from 'react-native-toast-message';

const PetDetails = () => {
    const { petId } = useLocalSearchParams();
    const { pets, setPets } = useContext(PetContext);

    const showSuccessMessage = (message) => {
        Toast.show({
            type: 'success',
            text1: message,
            position: 'top',
            visibilityTime: 3000,
            swipeable: true,
        });
    }

    const showErrorMessage = (message, description = '') => {
        Toast.show({
            type: 'error',
            text1: message,
            text2: description,
            position: 'top',
            visibilityTime: 3000,
            swipeable: true,
        });
    }



    const router = useRouter();

    const petIndex = pets.findIndex((pet) => pet.petId === petId);
    const [pet, setPet] = useState(() => pets[petIndex] || null);

    const [gender, setGender] = useState(pet?.gender || '');
    const [species, setSpecies] = useState(pet?.species || '');
    const [breed, setBreed] = useState(pet?.breed || '');
    const [dateOfBirth, setDateOfBirth] = useState(pet?.dateOfBirth || '');

    // const [image, setImage] = useState(pet?.myPicturesURLs || null);

    const [vaccineFiles, setVaccineFiles] = useState(pet?.myVaccinesURLs || []);

    const [isLoading, setIsLoading] = useState(false);
    const [errors, setErrors] = useState({
        name: '',
        species: '',
        gender: '',
        breed: '',
        dateOfBirth: '',
    });

    const genderOptions = [
        { label: 'Male', value: 'MALE' },
        { label: 'Female', value: 'FEMALE' },
    ];

    const speciesOptions = PETS.map(p => ({ label: p.name, value: p.value }));
    const breedOptions = species
        ? (PET_BREEDS[species]?.map(b => ({ label: b.name, value: b.name })) || [])
        : [];

    useEffect(() => {
        if (!pet) {
            Alert.alert('Pet not found', 'Returning to previous page.');
            router.back();
        }
    }, [pet]);

    const handleInputChange = (key, value) => {
        setPet((prev) => prev ? { ...prev, [key]: value } : null);
        setErrors((prev) => ({ ...prev, [key]: '' }));
    };

    const pickImage = async () => {
        const result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ['images'],
            allowsEditing: true,
            aspect: [1, 1],
            quality: 1,
        });

        if (!result.canceled && result.assets?.length > 0) {
            const uri = result.assets[0].uri;
            setImage(uri);
            handleInputChange('myPicturesURLs', uri);
        }
    };

    const deleteImage = () => {
        setImage(null);
        handleInputChange('myPicturesURLs', null);
    };

    const handleFilePick = async () => {
        try {
            setIsLoading(true);
            const result = await DocumentPicker.getDocumentAsync({
                type: 'application/pdf',
                multiple: true,
            });

            if (!result.canceled && result.assets?.length > 0) {
                const files = result.assets.map(file => ({
                    uri: file.uri,
                    name: file.name,
                    size: file.size,
                }));
                const updatedVaccines = [...vaccineFiles, ...files];
                setVaccineFiles(updatedVaccines);
                handleInputChange('myVaccinesURLs', updatedVaccines);
            }
        } catch (err) {
            console.error('Picker Error:', err);
        } finally {
            setIsLoading(false);
        }
    };

    const handleSaveChanges = async () => {  // <-- make it async
        if (!pet) return;
    
        const newErrors = {
            name: !pet.name?.trim() ? 'Name is required' : '',
            species: !species?.trim() ? 'Type is required' : '',
            gender: !gender?.trim() ? 'Gender is required' : '',
            breed: !breed?.trim() ? 'Breed is required' : '',
            dateOfBirth: !dateOfBirth?.trim() ? 'Date of birth is required' : '',
        };

        setErrors(newErrors);

        if (Object.values(newErrors).some(error => error)) return;

        const petData = {
            name: pet.name || undefined,
            description: pet.description || undefined,
            gender: gender || undefined,
            dateOfBirth: dateOfBirth || undefined,
            breed: breed || undefined,
            species: species ? species.toUpperCase() : undefined,
            myVaccinesURLs: vaccineFiles.map(file => file.uri || file),
            myPicturesURLs: Array.isArray(pet.myPicturesURLs) ? pet.myPicturesURLs : [],
        };
    
        try {
            setIsLoading(true);
            await updatePet(petId, petData);  // <-- await here!
    
            setPets(prevPets => prevPets.map(p =>
                p.petId === petId ? { ...p, ...petData } : p
            ));
            showSuccessMessage('Pet updated successfully!');
            router.back();
        } catch (error) {
            showErrorMessage('Failed to update pet', error);
            console.error('Error updating pet:', error);
        } finally {
            setIsLoading(false);
        }
    };
    

    const deletePetByPetId = () => {
        setIsLoading(true);
        deletePet(petId)
            .then(() => {
                setPets(prevPets => prevPets.filter(pet => pet.petId !== petId));
                showSuccessMessage('Pet deleted successfully!');
                router.back();
            })
            .catch((error) => {
                showErrorMessage('Failed to delete pet.', 'Please try again later.');
            })
            .finally(() => setIsLoading(false));
    };

    const handleDeletePet = () => {
        Alert.alert(
            'Delete Pet',
            `Are you sure you want to delete ${pet?.name}?`,
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Delete',
                    style: 'destructive',
                    onPress: deletePetByPetId,
                },
            ]
        );
    };

    const defaultImage = require('@/assets/images/Defaults/default-pet.png');

    if (!pet) return null;

    return (
        <ScrollView contentContainerStyle={styles.container}>
            <View style={styles.petInfoContainer}>
                {/* Image */}
                {/* <View style={styles.imageContainer}>
                    <TouchableOpacity onPress={pickImage} style={styles.imageWrapper}>
                        <Image source={ {uri: defaultImage} } style={styles.image} />
                        {image && (
                            <TouchableOpacity onPress={deleteImage} style={styles.trashIcon}>
                                <AntDesign name="delete" size={20} style={styles.icon} />
                            </TouchableOpacity>
                        )}
                    </TouchableOpacity>
                </View> */}

                {/* Inputs */}
                <View style={styles.inputsContainer}>
                    {/* Name */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Name <Text style={{ color: 'red' }}>*</Text></Text>
                        <TextInput
                            style={[styles.input, errors.name && styles.inputError]}
                            placeholder="Pet's name"
                            value={pet.name}
                            onChangeText={(text) => handleInputChange('name', text)}
                        />
                        {errors.name && <Text style={styles.errorText}>{errors.name}</Text>}
                    </View>

                    {/* Species */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Type <Text style={{ color: 'red' }}>*</Text></Text>
                        <Dropdown
                            style={[styles.input, errors.species && styles.inputError]}
                            placeholder="Select Type"
                            data={speciesOptions}
                            labelField="label"
                            valueField="value"
                            value={species}
                            onChange={(item) => {
                                setSpecies(item.value);
                                setBreed('');
                                handleInputChange('species', item.value);
                            }}
                        />
                        {errors.species && <Text style={styles.errorText}>{errors.species}</Text>}
                    </View>

                    {/* Breed */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>
                            Breed <Text style={{ color: 'red' }}>*</Text>
                        </Text>
                        <TextInput
                            style={[styles.input, errors.breed && styles.inputError]}
                            placeholder="Enter breed"
                            value={breed}
                            onChangeText={(text) => {
                                setBreed(text);
                                handleInputChange('breed', text);
                            }}
                        />
                        {errors.breed && <Text style={styles.errorText}>{errors.breed}</Text>}
                    </View>


                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Gender <Text style={{ color: 'red' }}>*</Text></Text>
                        <Dropdown
                            style={[styles.input, errors.gender && styles.inputError]}
                            placeholder="Select Gender"
                            data={genderOptions}
                            labelField="label"
                            valueField="value"
                            value={gender}
                            onChange={(item) => {
                                setGender(item.value);
                                handleInputChange('gender', item.value);
                            }}
                        />
                        {errors.gender && <Text style={styles.errorText}>{errors.gender}</Text>}
                    </View>

                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>Description</Text>
                        <TextInput
                            style={[styles.input, { height: 100, textAlignVertical: 'top' }]} // taller input
                            placeholder="Pet's description"
                            value={pet.description || ''}
                            onChangeText={(text) => handleInputChange('description', text)}
                            multiline
                            numberOfLines={4}
                        />
                    </View>

                    <View style={styles.inputContainer}>
                        <DateOfBirthInput
                            value={dateOfBirth}
                            onChange={(date) => {
                                setDateOfBirth(date);
                                handleInputChange('dateOfBirth', date);
                            }}
                            errorMessage={errors.dateOfBirth}
                        />
                    </View>
                </View>
            </View>
            <View style={styles.buttonContainer}>
                <TouchableOpacity style={styles.saveButton} onPress={handleSaveChanges} disabled={isLoading}>
                    <Text style={styles.buttonText}>Save changes</Text>
                </TouchableOpacity>
                <TouchableOpacity style={styles.deleteButton} onPress={handleDeletePet} disabled={isLoading}>
                    <Text style={styles.buttonText}>Delete</Text>
                </TouchableOpacity>
            </View>

            {isLoading && <ActivityIndicator size="large" color="#000" style={styles.loading} />}
        </ScrollView>
    );
};

const styles = StyleSheet.create({
    container: {
        justifyContent: 'space-between',
        backgroundColor: '#fff',
        paddingTop: 20,
        alignItems: 'center',
    },
    petInfoContainer: {
        alignItems: 'center',
        width: '90%',
        paddingBottom: 20,
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
    },
    icon: {
        color: '#9188E5',
    },
    inputsContainer: {
        paddingVertical: 30,
        width: '100%',
    },
    inputContainer: {
        alignItems: 'flex-start',
        paddingVertical: 15,
    },
    label: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 5,
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
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
        padding: 20,
        width: '100%',
    },
    saveButton: {
        backgroundColor: '#2CA269',
        paddingVertical: 10,
        borderRadius: 10,
        alignItems: 'center',
        marginBottom: 10,
    },
    deleteButton: {
        backgroundColor: '#F13838',
        paddingVertical: 10,
        borderRadius: 10,
        alignItems: 'center',
    },
    buttonText: {
        color: 'white',
        fontSize: 18,
        fontWeight: 'bold',
    },
});

export default PetDetails;