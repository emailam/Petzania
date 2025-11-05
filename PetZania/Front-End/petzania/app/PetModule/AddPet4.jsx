import { StyleSheet, View, Text, KeyboardAvoidingView, ScrollView, Platform } from 'react-native';
import React, { useState, useContext, useEffect } from 'react';
import { Dropdown } from 'react-native-element-dropdown';
import { useRouter } from 'expo-router';
import { TextInput } from 'react-native-paper';
import DateOfBirthInput from '@/components/DateOfBirthInput';

import Button from '@/components/Button';
import { PetContext } from '@/context/PetContext';
import CustomInput from '@/components/CustomInput';

export default function AddPet4() {
    const { pet, setPet } = useContext(PetContext);
    const [value, setValue] = useState(pet.gender);
    const [description, setDescription] = useState(pet.description || '');
    const [errors, setErrors] = useState({
        gender: '',
        dateOfBirth: '',
        description: ''
    });
    const [isFormComplete, setIsFormComplete] = useState(false);
    const router = useRouter();

    const data = [
        { label: 'Male', value: 'MALE' },
        { label: 'Female', value: 'FEMALE' },
    ];

    // Helper function to set individual field errors
    const setError = (field, message) => {
        setErrors(prev => ({
            ...prev,
            [field]: message
        }));
    };

    // Clear individual field errors
    const clearError = (field) => {
        setErrors(prev => ({
            ...prev,
            [field]: ''
        }));
    };

    // Check if form is complete and valid
    useEffect(() => {
        const genderValid = pet?.gender;
        const dobValid = pet?.dateOfBirth;
        const descriptionValid = description.length <= 255;

        // Form is complete when required fields are valid
        const formValid = genderValid && dobValid && descriptionValid && !errors.gender && !errors.dateOfBirth && !errors.description;

        setIsFormComplete(formValid);
    }, [pet?.gender, pet?.dateOfBirth, description, errors]);

    const goToNextStep = () => {
        // Validate all fields before proceeding
        let hasErrors = false;

        // Validate gender
        if (!pet?.gender) {
            setError('gender', "Pet's gender is required");
            hasErrors = true;
        }

        // Validate date of birth
        if (!pet?.dateOfBirth) {
            setError('dateOfBirth', 'Date of birth is required');
            hasErrors = true;
        }

        // Validate description length
        if (description.length > 255) {
            setError('description', 'Description must be 255 characters or less');
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }

        // Update pet context with description
        setPet(prev => ({
            ...prev,
            description: description.trim()
        }));

        router.push('/PetModule/AddPet5');
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
                        style={[styles.input, errors.gender ? styles.inputError : null]}
                        placeholder="Select gender"
                        placeholderStyle={{ color: '#333333' }}
                        data={data}
                        labelField="label"
                        valueField="value"
                        value={value}
                        onChange={item => {
                            setValue(item.value);
                            setPet({ ...pet, gender: item.value });
                            clearError('gender');
                        }}
                    />
                    {errors.gender ? <Text style={styles.errorText}>{errors.gender}</Text> : null}
                </View>

                <View style={styles.inputContainer}>
                    <DateOfBirthInput
                        value={pet.dateOfBirth}
                        onChange={(date) => {
                            setPet({ ...pet, dateOfBirth: date });
                            clearError('dateOfBirth');
                        }}
                        errorMessage={errors.dateOfBirth}
                    />
                </View>

                <View style={styles.inputContainer}>
                    <Text style={styles.label}>Description</Text>
                    <CustomInput
                        style={[styles.descriptionInput, errors.description ? styles.inputError : null]}
                        placeholder="A brief description about your pet"
                        value={description}
                        onChangeText={(text) => {
                            setDescription(text);
                            clearError('description');
                        }}
                        multiline
                        numberOfLines={4}
                        maxLength={255}
                        mode="outlined"
                        error={!!errors.description}
                        right={<TextInput.Affix text={`${description.length}/255`} />}
                    />
                    {errors.description ? <Text style={styles.errorText}>{errors.description}</Text> : null}
                </View>
            </ScrollView>

            <View style={styles.buttonContainer}>
                <Button
                    title="Next"
                    borderRadius={10}
                    fontSize={16}
                    onPress={goToNextStep}
                    disabled={!isFormComplete}
                />
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
        fontSize: 16,
        borderColor: '#9188E5',
        borderRadius: 10,
        paddingHorizontal: 16,
        justifyContent: 'center',
        backgroundColor: '#fff',
    },
    descriptionInput: {
        minHeight: 120, // Set minimum height for multiline
        textAlignVertical: 'top', // Align text to top for multiline
    },
    inputError: {
        borderColor: 'red',
    },
    errorText: {
        color: 'red',
        fontSize: 14,
        marginTop: 5,
    },
    buttonContainer: {
        padding: 20,
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
    },
});