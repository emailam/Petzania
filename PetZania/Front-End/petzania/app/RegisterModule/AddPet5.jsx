import { StyleSheet, View, Text, TextInput, KeyboardAvoidingView, ScrollView, Platform, Image, TouchableOpacity, FlatList, ActivityIndicator } from 'react-native';
import React, { useState, useContext } from 'react';
import { useRouter } from 'expo-router';

import Feather from '@expo/vector-icons/Feather';
import AntDesign from '@expo/vector-icons/AntDesign';

import * as DocumentPicker from 'expo-document-picker';
import Button from '@/components/Button';
import { PetContext } from '@/context/PetContext';

export default function AddPet5() {
    const { pet, setPet, addPet } = useContext(PetContext); // merged context usage
    const [vaccineFiles, setVaccineFiles] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const router = useRouter();

    const goToNextStep = () => {
        addPet(pet);
        router.dismissAll();
        router.push('/RegisterModule/AddPet6');
    };

    const handleFilePick = async () => {
        try {
            setIsLoading(true);
            const result = await DocumentPicker.getDocumentAsync({
                type: ['application/pdf'],
                copyToCacheDirectory: true,
                multiple: false,
            });
            if (result?.assets) {
                const file = result.assets[0];
                setPet({ ...pet, vaccines: file });
                setVaccineFiles((prev) => [...prev, file]);
            } else if (result?.name) {
                setPet({ ...pet, vaccines: result });
                setVaccineFiles((prev) => [...prev, result]);
            } else {
                console.log('User canceled or no file selected');
            }
        } catch (err) {
            console.error('Picker Error:', err);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            style={styles.container}
        >
            <FlatList
                ListHeaderComponent={
                    <>
                        <View style={styles.inputContainer}>
                            <Text style={styles.label}>Health Condition</Text>
                            <TextInput
                                style={[styles.input, styles.healthConditionInput]}
                                placeholder="Enter health condition"
                                value={pet.healthCondition}
                                onChangeText={(text) => setPet({ ...pet, healthCondition: text })}
                                multiline
                                numberOfLines={6}
                                textAlignVertical="top"
                            />
                        </View>

                        <TouchableOpacity style={styles.vaccinesInput} onPress={handleFilePick}>
                            <View style={styles.leftContainer}>
                                <Image source={require('@/assets/images/AddPet/Vaccines.png')} style={styles.image} />
                                <Text style={styles.vaccineLabel}>Vaccines</Text>
                            </View>
                            <View style={styles.rightContainer}>
                                {isLoading ? (
                                    <ActivityIndicator size="small" color="#9188E5" />
                                ) : (
                                    <Feather name="file-plus" size={28} color="#9188E5" />
                                )}
                            </View>
                        </TouchableOpacity>
                    </>
                }
                data={vaccineFiles}
                keyExtractor={(item, index) => index.toString()}
                renderItem={({ item }) => (
                    <View style={styles.uploadedFile}>
                        <View style={styles.leftContainer}>
                            <Image source={require('@/assets/images/AddPet/PDF.png')} style={styles.image} />
                            <Text style={styles.vaccineLabel}>{item.name}</Text>
                        </View>
                        <TouchableOpacity style={styles.rightContainer} onPress={() => setVaccineFiles((prev) => prev.filter((file) => file.uri !== item.uri))}>
                            <AntDesign name="delete" size={22} color="#C70000" />
                        </TouchableOpacity>
                    </View>
                )}
                contentContainerStyle={styles.scrollContainer}
                keyboardShouldPersistTaps="handled"
            />
            <View style={styles.buttonContainer}>
                <Button title="Next" borderRadius={10} fontSize={20} onPress={goToNextStep} />
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
        borderWidth: 1,
        borderColor: '#9188E5',
        borderRadius: 10,
        paddingHorizontal: 15,
        fontSize: 16,
    },
    healthConditionInput: {
        height: 120,
    },
    buttonContainer: {
        padding: 20,
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
    },
    image: {
        marginRight: '7%',
    },
    vaccinesInput: {
        marginTop: 20,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginHorizontal: '5%',
        borderColor: '#9188E5',
        borderWidth: 1,
        padding: 14,
        borderRadius: 10,
    },
    uploadedFile: {
        marginTop: 20,
        flexDirection: 'row',
        alignItems: 'center',
        marginHorizontal: '5%',
        backgroundColor: 'rgba(145, 136, 229, 0.1)',
        padding: 10,
        borderRadius: 10,
    },
    vaccineLabel: {
        fontSize: 19,
        color: '#333',
        fontWeight: 'bold',
        flex: 1,
    },
    leftContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        flex: 1,
    },
    rightContainer: {
        padding: 1,
    }
});