import React from 'react';
import { View, Text, TouchableOpacity, Image, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import Button from '@/components/Button';

export default function AddPet() {
    const router = useRouter();

    const goToNextStep = () => {
      router.push('/RegisterModule/AddPet1');
    }

    return (
        <View style={styles.container}>
            <Image source={require('../../assets/images/AddPet/Cat and Dog.png')} style={styles.image} />

            <Text style={styles.mainText}>Uh Oh!</Text>
            <Text style={styles.description}>
                Looks like you have no profiles set up at this moment, add your pet now
            </Text>

            <View style={{ width: '100%', alignItems: 'center', flex: 1, justifyContent: 'flex-end', marginBottom: 20 }}>
                <Button
                    title="Next"
                    borderRadius={10}
                    width="88%"
                    fontSize={20}
                    onPress={goToNextStep}
                />
                <TouchableOpacity onPress={() => router.push('/RegisterModule/AddPet1')}>
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
        backgroundColor: '#F8F8F8',
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
});
