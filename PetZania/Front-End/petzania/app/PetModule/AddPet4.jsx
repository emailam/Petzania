import { StyleSheet, View, Text, TextInput, KeyboardAvoidingView, ScrollView, Platform, TouchableOpacity } from 'react-native';
import React, { useState, useContext } from 'react';
import { Dropdown } from 'react-native-element-dropdown';
import { useRouter } from 'expo-router';
import DateOfBirthInput from '@/components/DateOfBirthInput';

import Button from '@/components/Button';
import { PetContext } from '@/context/PetContext';

export default function AddPet4() {
    const { pet, setPet } = useContext(PetContext);
    const [genderError, setGenderError] = useState('');
    const [dobError, setDobError] = useState('');
    const [value, setValue] = useState(pet.gender);
    const router = useRouter();

    const data = [
        { label: 'Male', value: 'MALE' },
        { label: 'Female', value: 'FEMALE' },
    ];

    const goToNextStep = () => {
        let hasError = false;

        if (!pet.gender?.trim()) {
            setGenderError("Pet's gender is required!");
            hasError = true;
        } else {
            setGenderError('');
        }

        if (!pet.dateOfBirth) {
            setDobError("Date of birth is required!");
            hasError = true;
        } else {
            setDobError('');
        }

        if (!hasError) {
            router.push('/PetModule/AddPet5');
        }
    };

    return (
        <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            style={styles.container}
            keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
        >
            <ScrollView
                contentContainerStyle={styles.scrollContainer}
                keyboardShouldPersistTaps="handled"
            >
                <View style={styles.inputContainer}>
                    <Text style={styles.label}>Gender<Text style={{ fontSize: 18, color: 'red' }}>*</Text></Text>
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
                    {genderError ? <Text style={{ color: 'red' }}>{genderError}</Text> : null}
                </View>

                <View style={styles.inputContainer}>
                    <DateOfBirthInput
                        value={pet.dateOfBirth}
                        onChange={(date) => setPet({ ...pet, dateOfBirth: date })}
                        errorMessage={dobError}
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
                        scrollEnabled={true}
                        showsVerticalScrollIndicator={true}
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
        paddingBottom: 50,
        flexGrow: 1,
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
        justifyContent: 'center',
        backgroundColor: '#fff',
    },
    descriptionInput: {
        height: 120,
        paddingTop: 10,
    },
    buttonContainer: {
        padding: 20,
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
    },
});