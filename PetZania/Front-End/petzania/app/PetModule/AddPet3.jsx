import { StyleSheet, Text, View, FlatList, TextInput } from 'react-native';
import React, { useContext, useState } from 'react';
import { PetContext } from '@/context/PetContext';
import { PET_BREEDS } from '@/constants/PETBREEDS';

import Button from '@/components/Button';
import PetSelectionCard from '@/components/PetSelectionCard';
import { useRouter } from 'expo-router';

export default function AddPet3() {
    const { pet, setPet } = useContext(PetContext);
    const router = useRouter();

    const [error, setError] = useState(''); // <-- Add error state

    const handleSelectPet = (name) => {
        setError(''); // clear error when user selects
        setPet({ ...pet, breed: name });
    };

    const handleManualInput = (text) => {
        setError(''); // clear error when user types
        setPet({ ...pet, breed: text });
    };

    const allBreeds = PET_BREEDS[pet.species] || [];

    const renderItem = ({ item }) => (
        <PetSelectionCard
            item={item}
            isSelected={pet.breed === item.name}
            onPress={handleSelectPet}
        />
    );

    const goToNextStep = () => {
        if (!pet.breed.trim()) {
            setError('Please enter or select a breed.');
            return;
        }
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
                <TextInput
                    placeholder="Enter pet breed"
                    style={styles.input}
                    value={!allBreeds.some(p => p.name === pet.breed) ? pet.breed : ''}
                    onChangeText={handleManualInput}
                    placeholderTextColor="#999"
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
        height: 50,
        borderColor: '#9188E5',
        borderWidth: 1,
        borderRadius: 10,
        paddingHorizontal: 15,
        marginBottom: 10,
        fontSize: 16,
        backgroundColor : '#fff'
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
