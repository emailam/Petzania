import { StyleSheet, View, Text, KeyboardAvoidingView, Platform, TouchableOpacity, FlatList, ActivityIndicator } from 'react-native';
import { Image } from 'expo-image';

import React, { useState, useContext } from 'react';
import { useRouter } from 'expo-router';

import Feather from '@expo/vector-icons/Feather';
import AntDesign from '@expo/vector-icons/AntDesign';

import * as DocumentPicker from 'expo-document-picker';
import * as Linking from 'expo-linking';
import Button from '@/components/Button';
import { PetContext } from '@/context/PetContext';
import { UserContext } from '@/context/UserContext';
import { FlowContext } from '@/context/FlowContext';

import { addPetToUser } from '@/services/petService';
import { uploadFiles } from '@/services/uploadService';

export default function AddPet5() {
    const { pet, setPet, pets, setPets } = useContext(PetContext);
    const [vaccineFiles, setVaccineFiles] = useState(pet.myVaccinesURLs || []);
    const { fromPage, setFromPage } = useContext(FlowContext);
    const [isLoading, setIsLoading] = useState(false);
    const router = useRouter();

    const { user } = useContext(UserContext);

    const goToNextStep = async () => {
        try {
            setIsLoading(true);

            let updatedPet = { ...pet };

            if (vaccineFiles.length > 0) {
                const files = vaccineFiles.map(file => ({
                    uri: file.uri,
                    name: file.name,
                    type: 'application/pdf',
                }));

                const uploadedUrls = await uploadFiles(files);
                updatedPet.myVaccinesURLs = uploadedUrls;

                setPet(prev => ({
                    ...prev,
                    myVaccinesURLs: uploadedUrls,
                }));
            }

            const newPet = await addPetToUser(updatedPet, user.userId);

            setPets(prevPets => [...prevPets, newPet]);
            setPet({});
            router.dismissAll();

            // Store fromPage value before resetting it
            const currentFromPage = fromPage;

            if (currentFromPage === 'Home') {
                // Keep fromPage as 'Home' so AllPets knows we came from Home
                router.push('/PetModule/AllPets');
                return;
            }
            if(currentFromPage !== 'EditProfile') {
                setFromPage("Register");
                router.push('/PetModule/AllPets');
            } else {
                setFromPage(null);
            }
        } catch (err) {
            console.error('Error uploading vaccine files:', err);
        } finally {
            setIsLoading(false);
        }
    };

    const handleFilePick = async () => {
        try {
            setIsLoading(true);
            const result = await DocumentPicker.getDocumentAsync({
                type: ['application/pdf'],
                copyToCacheDirectory: true,
                multiple: true,
            });

            if (result?.assets?.length) {
                const newFiles = result.assets.map(file => ({
                    uri: file.uri,
                    name: file.name,
                }));

                setVaccineFiles(prev => [...prev, ...newFiles]);
            } else {
                console.log('User canceled or no file selected');
            }
        } catch (err) {
            console.error('Picker Error:', err);
        } finally {
            setIsLoading(false);
        }
    };

    const deleteFile = (uriToDelete) => {
        const updated = vaccineFiles.filter(file => file.uri !== uriToDelete);
        setVaccineFiles(updated);
    };

    const openPDF = async (pdfUri) => {
        try {
            if (pdfUri) {
                await Linking.openURL(pdfUri);
            }
        } catch (error) {
            console.error('Error opening PDF:', error);
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
                        <Text style={styles.headerLabel}>Pet Vaccines</Text>
                        <TouchableOpacity style={styles.vaccinesInput} onPress={handleFilePick} disabled={isLoading}>
                            <Image source={require('@/assets/images/AddPet/Vaccines.png')} style={[styles.image, {marginRight: 0, width: 48, height: 48, borderRadius: 12}]} />
                            <View style={styles.vaccineInputTextContainer}>
                                <Text style={styles.vaccineInputTitle}>Add pet vaccines</Text>
                                <Text style={styles.vaccineInputSubtitle}>Upload PDF files of your pet's vaccines.</Text>
                            </View>
                            <View style={styles.rightContainer}>
                                {isLoading
                                    ? <ActivityIndicator size="small" color="#9188E5" />
                                    : <Feather name="file-plus" size={32} color="#9188E5" />}
                            </View>
                        </TouchableOpacity>
                    </>
                }
                data={vaccineFiles}
                keyExtractor={(item, index) => index.toString()}
                renderItem={({ item }) => (
                    <TouchableOpacity
                        style={styles.uploadedFile}
                        onPress={() => openPDF(item.uri)}
                    >
                        <View style={styles.leftContainer}>
                            <Image source={require('@/assets/images/AddPet/PDF.png')} style={styles.image} />
                            <Text style={styles.vaccineLabel}>{item.name}</Text>
                        </View>
                        <TouchableOpacity
                            style={styles.rightContainer}
                            onPress={(e) => {
                                e.stopPropagation();
                                deleteFile(item.uri);
                            }}
                            disabled={isLoading}
                        >
                            <AntDesign name="delete" size={22} color="#C70000" />
                        </TouchableOpacity>
                    </TouchableOpacity>
                )}
                contentContainerStyle={styles.scrollContainer}
                keyboardShouldPersistTaps="handled"
            />

            <View style={styles.buttonContainer}>
                <Button title="Next" borderRadius={10} fontSize={16} onPress={goToNextStep} loading={isLoading} />
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
    buttonContainer: {
        padding: 20,
        borderTopWidth: 1,
        borderTopColor: '#e0e0e0',
        backgroundColor: '#f5f5f5',
    },
    image: {
        marginRight: '7%',
        width: 46,
        height: 46,
    },
    headerLabel: {
        fontSize: 18,
        fontWeight: 'bold',
        color: '#333',
        marginTop: 30,
        marginLeft: '5%',
        marginBottom: 8,
    },
    vaccinesInput: {
        marginTop: 0,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginHorizontal: '5%',
        borderColor: '#9188E5',
        borderWidth: 1.5,
        padding: 16,
        borderRadius: 16,
    },
    vaccineInputTextContainer: {
        flex: 1,
        marginLeft: 10,
    },
    vaccineInputTitle: {
        fontSize: 15,
        fontWeight: 'bold',
        color: '#4B3FA7',
    },
    vaccineInputSubtitle: {
        fontSize: 13,
        color: '#6c6c8a',
        marginTop: 2,
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