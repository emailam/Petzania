import React from "react";
import { View, Text, Image, StyleSheet, TouchableOpacity } from "react-native";
import Foundation from '@expo/vector-icons/Foundation';
import { useRouter } from "expo-router";

export default function PetCard({ pet }) {
    const router = useRouter();

    const showPetInfo = (pet) => {
        router.push({ pathname: `/PetModule/${pet.petId}` });
    };

    return (
        <TouchableOpacity style={styles.container} onPress={() => {showPetInfo(pet)}} >
            <View style={styles.leftContainer}>
                <Image style={styles.imageContainer} source={pet?.myPicturesURLs?.length ? { uri: pet.myPicturesURLs[0] } : require('@/assets/images/AddPet/Pet Default Pic.png')}  />
                <View>
                    <Text style={styles.header}>
                        {pet.name}
                    </Text>
                    <Text style={styles.subHeader}>
                        {pet.species.charAt(0).toUpperCase() + pet.species.slice(1).toLowerCase()} | {pet.breed}
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
        borderRadius: 10,
        backgroundColor: "#fff",
        elevation: 5,
        margin: 10,
        padding: 10,
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
