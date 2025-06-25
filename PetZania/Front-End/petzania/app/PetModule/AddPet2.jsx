import { StyleSheet, Text, View, FlatList } from 'react-native';
import React, { useContext, useState } from 'react';
import { PetContext } from '@/context/PetContext';
import { PETS } from '@/constants/PETS';
import PetSelectionCard from '@/components/PetSelectionCard';

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
        <PetSelectionCard
            item={item}
            isSelected={pet.species === item.value}
            onPress={handleSelectPet}
        />
    );

    const goToNextStep = () => {
        if (!pet.species) {
            setError("Pet's type is required!");
            return;
        }
        setError('');
        router.push('/PetModule/AddPet3');
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