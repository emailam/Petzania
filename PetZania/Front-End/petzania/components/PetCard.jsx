import React from "react";
import { View, Text, StyleSheet, TouchableOpacity } from "react-native";
import { Image } from 'expo-image';
import Foundation from '@expo/vector-icons/Foundation';
import { useRouter } from "expo-router";

export default function PetCard({ pet, marginHorizontal = 8 }) {
    const router = useRouter();

    const showPetInfo = (pet) => {
        try {
            console.log('PetCard: Navigating to pet details for pet:', pet.petId);
            
            // Validate required fields
            if (!pet || !pet.petId) {
                console.error('PetCard: Invalid pet data - missing petId');
                return;
            }
            
            // Create a clean copy of pet data without potential circular references
            const cleanPetData = {
                petId: pet.petId,
                name: pet.name || '',
                species: pet.species || '',
                breed: pet.breed || '',
                gender: pet.gender || '',
                dateOfBirth: pet.dateOfBirth || null,
                description: pet.description || '',
                myPicturesURLs: Array.isArray(pet.myPicturesURLs) ? pet.myPicturesURLs : [],
                userId: pet.userId || '',
            };
            

            
            const serializedData = JSON.stringify(cleanPetData);
            
            router.push({ 
                pathname: `/PetModule/${pet.petId}`, 
                params: { petData: serializedData } 
            });
        } catch (error) {
            console.error('PetCard: Error serializing pet data:', error);
            console.error('PetCard: Pet object that caused error:', pet);
            // Fallback: navigate without petData parameter
            router.push(`/PetModule/${pet.petId}`);
        }
    };

    const formatSpecies = (species) => {
        if (!species) return '';
        return species
            .replace(/_/g, ' ') // Replace underscores with spaces
            .toLowerCase() // Convert to lowercase first
            .split(' ') // Split into words
            .map(word => word.charAt(0).toUpperCase() + word.slice(1)) // Capitalize each word
            .join(' '); // Join words back together
    };

    return (
        <TouchableOpacity style={[styles.container, { marginHorizontal: marginHorizontal }]} onPress={() => {showPetInfo(pet)}} >
            <View style={styles.leftContainer}>
                <Image style={styles.imageContainer} source={pet?.myPicturesURLs?.length ? { uri: pet.myPicturesURLs[0] } : require('@/assets/images/AddPet/Pet Default Pic.png')}  />
                <View>
                    <Text style={styles.header}>
                        {pet.name}
                    </Text>
                    <Text style={styles.subHeader}>
                        {formatSpecies(pet.species)} | {pet.breed}
                    </Text>
                </View>
            </View>
            <View style={styles.rightContainer}>
                {pet.gender === 'FEMALE' ? <Foundation name="female-symbol" size={35} color="#FF9AD5"  /> : <Foundation name="male-symbol" size={35} color="#1B85F3"  />}
            </View>
        </TouchableOpacity>
    );
}

const styles = StyleSheet.create({
    container: {
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        borderColor: "#9188E5",
        borderWidth: 0.25,
        borderRadius: 10,
        backgroundColor: "#fff",
        elevation: 2,
        marginVertical: 8,
        padding: 8,
    },
    leftContainer: {
        flexDirection: "row",
        alignItems: "center",
    },
    imageContainer: {
        width: 60,
        height: 60,
        borderRadius: 30,
        marginRight: 10,
        borderColor: "#9188E5",
        borderWidth: 1,
    },
    header: {
        fontSize: 18,
        fontWeight: "bold",
    },
    subHeader: {
        fontSize: 14,
        color: "#555",
    },
    rightContainer: {
        marginRight: 10,
    },
});
