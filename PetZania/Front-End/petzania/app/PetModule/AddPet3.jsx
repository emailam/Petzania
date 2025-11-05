import { StyleSheet, Text, View, FlatList, TextInput } from 'react-native';
import React, { useContext, useState } from 'react';
import { PetContext } from '@/context/PetContext';
import { PET_BREEDS } from '@/constants/PETBREEDS';

import Button from '@/components/Button';
import PetSelectionCard from '@/components/PetSelectionCard';
import { useRouter } from 'expo-router';
import CustomInput from '@/components/CustomInput';

export default function AddPet3() {
    const { pet, setPet } = useContext(PetContext);
    const router = useRouter();

    const [error, setError] = useState('');
    const [breed, setBreed] = useState(pet.breed || '');


    const handleSelectPet = (name) => {
        setError('');
        breed === name ? setBreed('') : setBreed(name);
    };

    const handleManualInput = (text) => {
        setError(''); // clear error when user types
        setBreed(text);
    };

    const allBreeds = PET_BREEDS[pet.species] || [];

    const renderItem = ({ item }) => (
        <PetSelectionCard
            item={item}
            isSelected={breed === item.name}
            onPress={handleSelectPet}
        />
    );

    const goToNextStep = () => {
        if (!breed.trim()) {
            setError('Please enter or select a breed.');
            return;
        }
        pet.breed = breed;
        router.push('/PetModule/AddPet4');
    };

    return (
        <View style={styles.container}>
            <FlatList
                numColumns={2}
                renderItem={renderItem}
                data={allBreeds}
                keyExtractor={(item) => item.name}
                columnWrapperStyle={styles.row}
            />

            <View style={styles.buttonContainer}>
                <Text style={styles.orText}>Or enter it manually</Text>
                <CustomInput
                    style={styles.input}
                    label="Pet breed"
                    placeholder={"e.g., Golden Retriever"}
                    value={!allBreeds.some(p => p.name === breed) ? breed : ''}
                    onChangeText={handleManualInput}
                    error={error}
                />
                {error ? <Text style={styles.error}>{error}</Text> : null}
                <Button title="Next" borderRadius={10} fontSize={16} onPress={goToNextStep} />
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    row: {
        justifyContent: 'space-between',
        padding: 10,
    },
    orText: {
        textAlign: 'center',
        marginVertical: 10,
        fontSize: 14,
        color: '#666',
    },
    input: {
        marginBottom: 10,
    },
    buttonContainer: {
        padding: 20,
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
    },
    error: {
        color: 'red',
        textAlign: 'center',
        marginBottom: 10,
        fontSize: 14,
    },
});
