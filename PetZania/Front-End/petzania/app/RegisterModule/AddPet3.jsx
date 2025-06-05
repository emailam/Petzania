import { StyleSheet, Text, View, FlatList, Image, TouchableOpacity, TextInput } from 'react-native';
import React, { useContext, useState } from 'react';
import { Card } from 'react-native-paper';
import { PetContext } from '@/context/PetContext';
import { PET_BREEDS } from '@/constants/PETBREEDS';

import Button from '@/components/Button';
import { useRouter } from 'expo-router';

export default function AddPet3() {
    const { pet, setPet } = useContext(PetContext);
    const router = useRouter();

    const fallbackImage = require('@/assets/images/Pets/Dog.png');
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
        <TouchableOpacity
            onPress={() => handleSelectPet(item.name)}
            activeOpacity={1}
            style={styles.cardWrapper}
        >
            <Card style={[styles.card, pet.breed === item.name && styles.selectedCard]}>
                <Image
                    source={item.image || fallbackImage}
                    style={styles.image}
                    resizeMode="contain"
                />
                <Text style={[styles.text, pet.breed === item.name && styles.selectedText]}>
                    {item.name}
                </Text>
            </Card>
        </TouchableOpacity>
    );

    const goToNextStep = () => {
        if (!pet.breed.trim()) {
            setError('Please enter or select a breed.');
            return;
        }
        router.push('/RegisterModule/AddPet4');
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
    cardWrapper: {
        flex: 1,
        marginHorizontal: 5,
    },
    card: {
        borderRadius: 16,
        alignItems: 'center',
        paddingVertical: 10,
        elevation: 2,
        backgroundColor: '#fff',
    },
    selectedCard: {
        borderWidth: 2,
        borderColor: '#9188E5',
    },
    image: {
        width: 140,
        height: 140,
        marginBottom: 10,
    },
    text: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
        textAlign: 'center',
    },
    selectedText: {
        color: '#9188E5',
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
