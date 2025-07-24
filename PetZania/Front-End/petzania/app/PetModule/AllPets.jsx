import { StyleSheet, Text, View, FlatList, TouchableOpacity } from 'react-native'
import PetCard from '@/components/PetCard'
import React, { useContext, useEffect } from 'react';

import Button from '@/components/Button';

import { PetContext } from '@/context/PetContext';
import { UserContext } from '@/context/UserContext';

import { useRouter } from 'expo-router';

import Ionicons from '@expo/vector-icons/Ionicons';

import { getAllPetsByUserId } from '@/services/petService'
import { FlowContext } from '@/context/FlowContext'


export default function AllPets() {
    const { pets, createNewPet, setPets } = useContext(PetContext);
    const { user } = useContext(UserContext);
    const { fromPage, setFromPage } = useContext(FlowContext);

    const router = useRouter();

    const retrievePets = async () => {
        try {
            const userPets = await getAllPetsByUserId(user?.userId);
            setPets(userPets);
            user.myPets = pets; // Update user context with the retrieved pets
        } catch (error) {
            console.error('Error fetching pets:', error);
        }
    };

    useEffect(() => {
        retrievePets();
        createNewPet();
    }, []);

    const goToNextPage = () => {
        if(fromPage === 'Home'){
            setFromPage(null);
            router.dismiss();
            router.push('/Home');
        }
        else if(fromPage === 'Register'){
            setFromPage(null);
            router.push('/RegisterModule/ProfileSetUp3');
        }
        else {
            router.dismiss();
            router.push('/Home');
        }
    }

    return (
        <View style={styles.container}>
            <FlatList
                data={pets}
                keyExtractor={(item) => item.petId}
                renderItem={({ item }) => <PetCard pet={item} />}
                ListFooterComponent={() => (
                    <TouchableOpacity style={styles.addPetButton} onPress={() => router.push('/PetModule/AddPet1')}>
                        <Ionicons name="add-circle-outline" size={55} color="#9188E5" />
                        <Text style={styles.text}>Add a new pet</Text>
                    </TouchableOpacity>
                )}
            />
            {fromPage !== 'EditProfile' && (
                <View style={styles.buttonContainer}>
                    <Button
                        title={fromPage === 'Register' ? 'Next' : 'Return to Home'}
                        borderRadius={10}
                        fontSize={16}
                        onPress={goToNextPage}
                    />
                </View>
            )}
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
        elevation: 2,
        borderRadius: 10,
        elevation: 2,
        marginHorizontal: 10,
        marginVertical: 8,
        padding: 16,
        borderColor: "#9188E5",
        borderWidth: 0.5,
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