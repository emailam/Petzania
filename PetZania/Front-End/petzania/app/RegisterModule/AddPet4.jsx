import { StyleSheet, View, Text, TextInput, KeyboardAvoidingView, ScrollView, Platform } from 'react-native';
import React, { useState, useContext } from 'react';
import { Dropdown } from 'react-native-element-dropdown';
import { useRouter } from 'expo-router';

import Button from '@/components/Button';
import { PetContext } from '@/context/PetContext';

export default function AddPet4() {
    const { pet, setPet } = useContext(PetContext);
    const [error, setError] = useState('');
    const [value, setValue] = useState(pet.gender);
    const router = useRouter();

    const data = [
        { label: 'Male', value: 'Male' },
        { label: 'Female', value: 'Female' },
    ];

    const goToNextStep = () => {
        if (!pet.gender.trim()) {
            setError("Pet's gender is required!");
            return;
        }
        setError('');
        router.push('/RegisterModule/AddPet5');
    };

    return (
        <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            style={styles.container}
        >
            <ScrollView
                contentContainerStyle={styles.scrollContainer}
                keyboardShouldPersistTaps="handled"
            >
                <View style={styles.inputContainer}>
                    <Text style={styles.label}>Gender<Text style = {{fontSize: '18', color: 'red'}}>*</Text></Text>
                    <Dropdown
                        style={styles.input}
                        placeholder="Select gender"
                        data={data}
                        labelField="label"
                        valueField="value"
                        value={value}
                        onChange={item => {
                            setValue(item.value);
                            setPet({ ...pet, gender: item.value });
                        }}
                    />
                    {error ? <Text style={{ color: 'red' }}>{error}</Text> : null}
                </View>

                <View style={styles.inputContainer}>
                    <Text style={styles.label}>Age</Text>
                    <TextInput
                        style={styles.input}
                        placeholder="Enter age"
                        keyboardType="number-pad"
                        value={pet.age}
                        onChangeText={(text) => setPet({ ...pet, age: text })}
                    />
                </View>

                <View style={styles.inputContainer}>
                    <Text style={styles.label}>Color</Text>
                    <TextInput
                        style={styles.input}
                        placeholder="Enter color"
                        value={pet.color}
                        onChangeText={(text) => setPet({ ...pet, color: text })}
                    />
                </View>

                <View style={styles.inputContainer}>
                    <Text style={styles.label}>Description</Text>
                    <TextInput
                        style={[styles.input, styles.descriptionInput]}
                        placeholder="Enter description"
                        value={pet.description}
                        onChangeText={(text) => setPet({ ...pet, description: text })}
                        multiline
                        numberOfLines={6}
                        textAlignVertical="top"
                    />
                </View>
            </ScrollView>

            <View style={styles.buttonContainer}>
                <Button title="Next" borderRadius={10} fontSize={16} onPress={goToNextStep} />
            </View>
        </KeyboardAvoidingView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    scrollContainer: {
        paddingBottom: 20,
    },
    inputContainer: {
        paddingHorizontal: '5%',
        marginTop: 20,
    },
    label: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 10,
        color: '#333',
    },
    input: {
        width: '100%',
        height: 50,
        borderWidth: 1,
        borderColor: '#9188E5',
        borderRadius: 10,
        paddingHorizontal: 15,
        fontSize: 16,
        backgroundColor: '#fff',
    },
    descriptionInput: {
        height: 120,
    },
    buttonContainer: {
        padding: 20,
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
    },
});
