import {
    StyleSheet, View, Text, KeyboardAvoidingView, Platform, Image, TouchableOpacity,
    FlatList, ActivityIndicator
} from 'react-native';
import React, { useState, useContext } from 'react';
import { useRouter } from 'expo-router';

import Feather from '@expo/vector-icons/Feather';
import AntDesign from '@expo/vector-icons/AntDesign';

import * as DocumentPicker from 'expo-document-picker';
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

            if (vaccineFiles.length > 0) {
                const files = vaccineFiles.map(file => ({
                    uri: file.uri,
                    name: file.name,
                    type: 'application/pdf',
                }));

                const uploadedUrls = await uploadFiles(files);

                setPet(prev => ({
                    ...prev,
                    myVaccinesURLs: uploadedUrls,
                }));
            }

            const newPet = await addPetToUser(pet, user.userId);
            console.log('New Pet Added:', newPet);

            setPets(prevPets => [...prevPets, newPet]);
            setPet({});
            router.dismissAll();
            setFromPage(null);

            if(fromPage !== 'EditProfile') {setFromPage("Register"); router.push('/PetModule/AllPets');}
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

    return (
        <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            style={styles.container}
        >
            <FlatList
                ListHeaderComponent={
                    <TouchableOpacity style={styles.vaccinesInput} onPress={handleFilePick} disabled={isLoading}>
                        <View style={styles.leftContainer}>
                            <Image source={require('@/assets/images/AddPet/Vaccines.png')} style={styles.image} />
                            <Text style={styles.vaccineLabel}>Vaccines</Text>
                        </View>
                        <View style={styles.rightContainer}>
                            {isLoading
                                ? <ActivityIndicator size="small" color="#9188E5" />
                                : <Feather name="file-plus" size={28} color="#9188E5" />}
                        </View>
                    </TouchableOpacity>
                }
                data={vaccineFiles}
                keyExtractor={(item, index) => index.toString()}
                renderItem={({ item }) => (
                    <View style={styles.uploadedFile}>
                        <View style={styles.leftContainer}>
                            <Image source={require('@/assets/images/AddPet/PDF.png')} style={styles.image} />
                            <Text style={styles.vaccineLabel}>{item.name}</Text>
                        </View>
                        <TouchableOpacity
                            style={styles.rightContainer}
                            onPress={() => deleteFile(item.uri)}
                            disabled={isLoading}
                        >
                            <AntDesign name="delete" size={22} color="#C70000" />
                        </TouchableOpacity>
                    </View>
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