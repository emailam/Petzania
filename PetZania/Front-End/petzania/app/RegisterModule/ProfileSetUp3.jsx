import { StyleSheet, Text, View, FlatList, TouchableOpacity, Image } from 'react-native'
import React, { useContext, useEffect } from 'react'
import { PetContext } from '@/context/PetContext';
import PetCard from '@/components/PetCard'
import { useRouter } from 'expo-router';
import Button from '@/components/Button';

import { UserContext } from '@/context/UserContext';

import { getAllPetsByUserId } from '@/services/petService';

export default function ProfileSetUp3() {
    const { pets, setPets } = useContext(PetContext);
    const router = useRouter();

    const numOfPets = pets.length;
    const defaultUserImage = require('@/assets/images/Defaults/default-user.png');

    const { user } = useContext(UserContext);

    const retrievePets = async () => {
        try {
            const userPets = await getAllPetsByUserId(user.userId);
            setPets(userPets);
        } catch (error) {
            console.error('Error fetching pets:', error);
        }
    };

    useEffect(() => {
        retrievePets();
    }, []);



    const goToNextStep = () => {
        router.push('/(tabs)');
    }
    return (
        <View style={styles.container}>
            <View style={styles.userContainer}>
                <Image
                    source={defaultUserImage}
                    style={styles.userImage}
                />
                <View style={styles.userInfo}>
                    <Text style={styles.userName}>Tyler Gregory Okonma</Text>
                    <Text style={styles.userHandle}>@tylerthecreator</Text>
                </View>
            </View>

            <View style={styles.petsContainer}>
                <View style={styles.petHeaderContainer}>
                    <Text style={styles.petProfilesHeader}>Active pet profiles</Text>
                    <View style={styles.numberBox}>
                        <Text style={styles.numberText}>{numOfPets}</Text>
                    </View>
                </View>

                <FlatList
                    data={pets}
                    keyExtractor={(item) => item.petId}
                    renderItem={({ item }) => <PetCard pet={item} />}
                    style={styles.petList}
                />
            </View>

            <View style={styles.buttonContainer}>
                <Button title="Finish your profile" borderRadius={10} fontSize={16} onPress={goToNextStep} />
            </View>
        </View>
    )
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    userContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 30,
        padding: 10,
        paddingVertical: 20,
    },
    userImage: {
        width: 80,
        height: 80,
        borderRadius: 40,
        marginRight: 15,
        borderWidth: 1,
        borderColor: '#9188E5',
    },
    userInfo: {
        flex: 1,
    },
    userName: {
        fontSize: 20,
        fontWeight: 'bold',
    },
    userHandle: {
        fontSize: 14,
        color: '#888',
    },
    petsContainer: {
        flex: 1,
    },
    petHeaderContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 15,
        paddingHorizontal: 10,
    },
    petProfilesHeader: {
        fontSize: 18,
        fontWeight: '600',
        marginRight: 10,
    },
    numberBox: {
        width: 28,
        height: 28,
        borderRadius: 14,
        backgroundColor: '#f0f0f0',
        justifyContent: 'center',
        alignItems: 'center',
    },
    numberText: {
        fontSize: 16,
        fontWeight: '500',
    },
    petList: {
        flex: 1,
    },
    buttonContainer: {
        padding: 20,
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
    },
})