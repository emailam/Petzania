import {
    View, Text, Image, StyleSheet, ScrollView, TouchableOpacity,
    Alert, TextInput, Platform, ActivityIndicator
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useContext, useEffect, useState } from 'react';
import { Dropdown } from 'react-native-element-dropdown';
import * as ImagePicker from 'expo-image-picker';
import * as DocumentPicker from 'expo-document-picker';
import { AntDesign } from '@expo/vector-icons';
import { PetContext } from '@/context/PetContext';

const PetDetails = () => {
    const { name } = useLocalSearchParams();
    const { pets, setPets, deletePet } = useContext(PetContext);

    const petIndex = pets.findIndex((pet) => pet.name === name);
    const [pet, setPet] = useState(pets[petIndex]);

    const router = useRouter();
    const [gender, setGender] = useState(pet.gender || '');
    const [type, setType] = useState(pet.type || '');
    const [image, setImage] = useState(pet.image || null);
    const [vaccines, setVaccines] = useState(pet.vaccines || '');
    const [vaccineFiles, setVaccineFiles] = useState([]);

    const [errors, setErrors] = useState({
        name: '',
        type: '',
        gender: '',
    });

    const genderOptions = [
        { label: 'Male', value: 'Male' },
        { label: 'Female', value: 'Female' },
    ];

    useEffect(() => {
        if (pets.length === 0) {
            router.dismissTo('/RegisterModule/ProfileSetUp2');
        }
    }, [pets]);

    const handleInputChange = (key, value) => {
        setPet((prevPet) => ({ ...prevPet, [key]: value }));
        setErrors((prev) => ({ ...prev, [key]: '' }));
    };

    const handleDeletePet = () => {
        Alert.alert(
            'Delete Pet',
            `Are you sure you want to delete ${pet.name}?`,
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Delete',
                    onPress: () => {
                        deletePet(pet.name);
                        router.back();
                    },
                },
            ]
        );
    };

    const pickImage = async () => {
        let result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ['images'],
            allowsEditing: true,
            aspect: [1, 1],
            quality: 1,
        });

        if (!result.canceled) {
            setImage(result.assets[0].uri);
            handleInputChange('image', result.assets[0].uri);
        }
    };

    const deleteImage = () => {
        setImage(null);
        handleInputChange('image', null);
    };

    const handleFilePick = async () => {
        try {
            setIsLoading(true);
            const result = await DocumentPicker.getDocumentAsync({
                type: ['application/pdf'],
                copyToCacheDirectory: true,
                multiple: false,
            });

            if (result?.assets) {
                const file = result.assets[0];
                setPet((prev) => ({ ...prev, vaccines: file }));
                setVaccineFiles((prev) => [...prev, file]);
            } else if (result?.name) {
                setPet((prev) => ({ ...prev, vaccines: result }));
                setVaccineFiles((prev) => [...prev, result]);
            } else {
                console.log('User canceled or no file selected');
            }
        } catch (err) {
            console.error('Picker Error:', err);
        } finally {
            setIsLoading(false);
        }
    };

    const handleSaveChanges = () => {
        const newErrors = {
            name: !pet.name.trim() ? 'Name is required' : '',
            type: !type.trim() ? 'Type is required' : '',
            gender: !gender.trim() ? 'Gender is required' : '',
        };

        setErrors(newErrors);

        const hasErrors = Object.values(newErrors).some((error) => error !== '');
        if (hasErrors) return;

        const updatedPets = [...pets];
        updatedPets[petIndex] = { ...pet, gender, type, image, vaccines };
        setPets(updatedPets);
        Alert.alert('Success', 'Pet details updated successfully!');
        setTimeout(() => {
            router.back();
        }, 1000);
    };

    const defaultImage = require('@/assets/images/Defaults/default-pet.png');

    return (
        <ScrollView contentContainerStyle={styles.container}>
            <View style={styles.petInfoContainer}>
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

                <View style={styles.inputsContainer}>
                    {/* Pet Name */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>
                            Name <Text style={{ color: 'red' }}>*</Text>
                        </Text>
                        <TextInput
                            style={[
                                styles.input,
                                errors.name ? styles.inputError : null
                            ]}
                            placeholder="Pet's name"
                            value={pet.name}
                            onChangeText={(text) => handleInputChange('name', text)}
                        />
                        {errors.name && <Text style={styles.errorText}>{errors.name}</Text>}
                    </View>

                    {/* Pet Type */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>
                            Type <Text style={{ color: 'red' }}>*</Text>
                        </Text>
                        <TextInput
                            style={[
                                styles.input,
                                errors.type ? styles.inputError : null
                            ]}
                            placeholder="Enter pet type"
                            value={type}
                            onChangeText={(text) => {
                                setType(text);
                                setErrors((prev) => ({ ...prev, type: '' }));
                                handleInputChange('type', text);
                            }}
                        />
                        {errors.type && <Text style={styles.errorText}>{errors.type}</Text>}
                    </View>

                    {/* Gender */}
                    <View style={styles.inputContainer}>
                        <Text style={styles.label}>
                            Gender <Text style={{ color: 'red' }}>*</Text>
                        </Text>
                        <Dropdown
                            style={[
                                styles.input,
                                errors.gender ? styles.inputError : null
                            ]}
                            placeholder="Select gender"
                            data={genderOptions}
                            labelField="label"
                            valueField="value"
                            value={gender}
                            onChange={(item) => {
                                setGender(item.value);
                                setErrors((prev) => ({ ...prev, gender: '' }));
                                handleInputChange('gender', item.value);
                            }}
                        />
                        {errors.gender && <Text style={styles.errorText}>{errors.gender}</Text>}
                    </View>

                    {/* Optional Fields */}
                    {[{ label: 'Breed', key: 'breed' },
                    { label: 'Age', key: 'age' },
                    { label: 'Color', key: 'color' },
                    { label: 'Description', key: 'description' },
                    { label: 'Health Condition', key: 'healthCondition' }].map(({ label, key }) => (
                        <View key={key} style={styles.inputContainer}>
                            <Text style={styles.label}>{label}</Text>
                            <TextInput
                                style={styles.input}
                                placeholder={`Enter ${label.toLowerCase()}`}
                                value={pet[key]}
                                onChangeText={(value) => handleInputChange(key, value)}
                            />
                        </View>
                    ))}
                </View>
            </View>

            <View style={styles.buttonContainer}>
                <TouchableOpacity style={styles.saveButton} onPress={handleSaveChanges}>
                    <Text style={styles.buttonText}>Save changes</Text>
                </TouchableOpacity>
                <TouchableOpacity style={styles.deleteButton} onPress={handleDeletePet}>
                    <Text style={styles.buttonText}>Delete</Text>
                </TouchableOpacity>
            </View>
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
        alignItems: 'left',
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
