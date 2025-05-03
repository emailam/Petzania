import React from 'react';
import { View, Text, TouchableOpacity, Image, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import Button from '@/components/Button';

export default function ProfileSetUp2() {
    const router = useRouter();

    const goToNextStep = () => {
        router.push('/RegisterModule/AddPet1');
    }

    return (
        <View style={styles.container}>
            <View style={styles.contentWrapper}>
                <Image source={require('../../assets/images/AddPet/Cat and Dog.png')} style={styles.image} />
                <Text style={styles.mainText}>Who's a good pet? 🐾</Text>
                <Text style={styles.description}>
                    Add your pets now to begin tracking their adventures!
                </Text>
            </View>

            <View style={styles.buttonContainer}>
                <Button
                    title="Next"
                    onPress={goToNextStep}
                    fontSize={16}
                />
                <TouchableOpacity onPress={() => router.push('/RegisterModule/ProfileSetUp3')}>
                    <Text style={styles.skipText}>Skip for now</Text>
                </TouchableOpacity>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#fff',
        justifyContent: 'space-between',
    },
    contentWrapper: {
        alignItems: 'center',
        marginTop: 40,
    },
    image: {
        width: 350,
        height: 350,
        resizeMode: 'contain',
        marginVertical: 20,
    },
    mainText: {
        fontSize: 26,
        fontWeight: 'bold',
        color: '#444',
    },
    description: {
        paddingHorizontal: 10,
        fontSize: 16,
        textAlign: 'center',
        color: '#777',
    },
    skipText: {
        fontSize: 14,
        color: '#888',
        paddingVertical: 10,
        fontFamily: 'Inter-Bold',
    },
    buttonContainer: {
        padding: 20,
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
        width: '100%',
        alignItems: 'center',
    },
});
