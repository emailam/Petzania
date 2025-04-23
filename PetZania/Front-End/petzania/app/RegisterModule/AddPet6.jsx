import { StyleSheet, Text, View, FlatList, TouchableOpacity, Image} from 'react-native'
import PetCard from '@/components/PetCard'
import React, { useContext, useEffect } from 'react';

import { PetContext } from '@/context/PetContext';
import { useRouter } from 'expo-router';

import Ionicons from '@expo/vector-icons/Ionicons';

import Button from '@/components/Button';

export default function AddPet6() {

    const { pets, createNewPet } = useContext(PetContext);

    const router = useRouter();

    useEffect(() => {
        createNewPet();
    }, [router]);

    const goToNextPage = () => {
        // Here we will add the pet to the database
        // and then navigate to the next page
        router.push('/RegisterModule/ProfileSetUp3');
    }

    return (
        <View style={styles.container}>
            <FlatList
                data={pets}
                keyExtractor={(item) => item.name}
                renderItem={({ item }) => <PetCard pet={item} />}
                ListFooterComponent={() => (
                    <TouchableOpacity style={styles.addPetButton} onPress={() => router.push('/RegisterModule/AddPet1')}>
                        <Ionicons name="add-circle-outline" size={55} color="#9188E5" />
                        <Text style={styles.text}>Add a new pet</Text>
                    </TouchableOpacity>
                )}
            />
            <View style={styles.buttonContainer}>
                <Button title="Next" borderRadius={10} fontSize={16} onPress={goToNextPage} />
            </View>
        </View>
    )
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    addPetButton: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#fff',
        elevation: 5,
        borderRadius: 10,
        margin: 10,
        padding: 10,
    },
    text: {
        fontSize: 20,
        fontWeight: 'bold',
        color: '#9188E5',
        marginLeft: 10,
    },
    buttonContainer: {
        padding: 20,
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
    },
});