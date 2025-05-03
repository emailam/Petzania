import { StyleSheet, Text, View, FlatList, Image, TouchableOpacity, TextInput } from 'react-native';
import React, { useContext, useState } from 'react';
import { Card } from 'react-native-paper';
import { PetContext } from '@/context/PetContext';
import { PETS } from '@/constants/PETS';

import Button from '@/components/Button';
import { useRouter } from 'expo-router';

export default function AddPet2() {
    const { pet, setPet } = useContext(PetContext);
    const router = useRouter();
    const [error, setError] = useState('');

    const handleSelectPet = (value) => {
        setPet({ ...pet, species: value });
        setError('');
    };

    const renderItem = ({ item }) => (
        <TouchableOpacity
            onPress={() => handleSelectPet(item.value)}
            activeOpacity={1}
            style={styles.cardWrapper}
        >
            <Card style={[styles.card, pet.species === item.value && styles.selectedCard]}>
                <Image source={item.image} style={styles.image} resizeMode="contain" />
                <Text style={[styles.text, pet.species === item.value && styles.selectedText]}>
                    {item.name}
                </Text>
            </Card>
        </TouchableOpacity>
    );

    const goToNextStep = () => {
        if (!pet.species) {
            setError("Pet's type is required!");
            return;
        }
        setError('');
        router.push('/RegisterModule/AddPet3');
    };

    return (
        <View style={styles.container}>
            <FlatList
                numColumns={2}
                renderItem={renderItem}
                data={PETS}
                keyExtractor={(item) => item.name}
                columnWrapperStyle={styles.row}
            />

            <View style={styles.buttonContainer}>
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
        backgroundColor: '#fff',
        marginBottom: 10,
        fontSize: 16,
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