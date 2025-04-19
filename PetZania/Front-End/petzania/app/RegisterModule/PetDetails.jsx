import { View, Text, Image, StyleSheet, ScrollView, TouchableOpacity, Alert, TextInput } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useContext, useEffect, useState } from 'react';
import { PetContext } from '@/context/PetContext';

const PetDetails = () => {
    const pet = useLocalSearchParams();
    const { setPet, deletePet, pets } = useContext(PetContext);
    const router = useRouter();
    const [error, setError] = useState('');

    useEffect(() => {
        if (pets.length === 0) {
            router.dismissTo('/RegisterModule/AddPet');
        }
    }, [pets]);

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
                        router.push('/RegisterModule/AddPet6');
                    },
                },
            ]
        );
    };

    const handleInputChange = (field, value) => {
        setPet((prevPet) => ({
            ...prevPet,
            [field]: value,
        }));
        setError('');
    };

    return (
        <ScrollView contentContainerStyle={styles.container}>
            <View style={styles.petInfoContainer}>
                <Image
                    source={pet.image ? { uri: pet.image } : require('@/assets/images/AddPet/Pet Default Pic.png')}
                    style={styles.image}
                />

                {[
                    { label: "Pet's Name", key: "name", required: true },
                    { label: "Type", key: "type", required: true },
                    { label: "Breed", key: "breed" },
                    { label: "Gender", key: "gender" },
                    { label: "Age", key: "age" },
                    { label: "Color", key: "color" },
                    { label: "Description", key: "description" },
                    { label: "Health Condition", key: "healthCondition" },
                ].map(({ label, key, required }) => (
                    <View key={key} style={styles.inputContainer}>
                        <Text style={styles.label}>
                            {label} {required && <Text style={{ fontSize: 18, color: 'red' }}>*</Text>}
                        </Text>
                        <TextInput
                            style={[styles.input, error ? styles.inputError : null]}
                            placeholder={`Enter ${label.toLowerCase()}`}
                            value={pet[key]}
                            onChangeText={(value) => handleInputChange(key, value)}
                        />
                    </View>
                ))}

                <View style={styles.inputContainer}>
                    <Text style={styles.label}>Vaccines</Text>
                    <TextInput
                        style={styles.input}
                        placeholder="Enter vaccines (comma-separated)"
                        onChangeText={(value) => handleInputChange('vaccines', value.split(',').map(v => v.trim()))}
                    />
                </View>
            </View>

            <View style={styles.buttonContainer}>
                <TouchableOpacity style={styles.deleteButton} onPress={handleDeletePet}>
                    <Text style={styles.deleteText}>Delete</Text>
                </TouchableOpacity>
            </View>
        </ScrollView>
    );
};

const styles = StyleSheet.create({
    container: {
        justifyContent: 'space-between',
        backgroundColor: '#fff',
        paddingVertical: 20,
        alignItems: 'center',
    },
    petInfoContainer: {
        alignItems: 'center',
        width: '90%',
    },
    image: {
        width: 220,
        height: 220,
        borderRadius: 110,
        borderWidth: 2,
        borderColor: '#9188E5',
        marginBottom: 20,
    },
    inputContainer: {
        width: '100%',
        alignItems: 'center',
        marginTop: 15,
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
    buttonContainer: {
        padding: 20,
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
    },
    deleteButton: {
        backgroundColor: 'red',
        paddingVertical: 10,
        paddingHorizontal: 20,
        borderRadius: 10,
        alignItems: 'center',
    },
    deleteText: {
        color: 'white',
        fontSize: 18,
        fontWeight: 'bold',
    },
});

export default PetDetails;