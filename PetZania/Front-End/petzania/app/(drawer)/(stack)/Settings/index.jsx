import { StyleSheet, Text, View, TouchableOpacity, Alert, Linking, ScrollView, Switch, TextInput, Modal } from 'react-native'
import React, { useState, useContext} from 'react'
import { Ionicons } from '@expo/vector-icons'
import { useRouter } from 'expo-router'
import Toast from 'react-native-toast-message'
import { deleteUser, logout } from '@/services/userService'
import { UserContext } from '@/context/UserContext'

export default function Settings() { const router = useRouter();
    const [darkMode, setDarkMode] = useState(false);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [usernameInput, setUsernameInput] = useState('');
    const [inputError, setInputError] = useState('');
    const { user } = useContext(UserContext);

    const handleLogout = () => {
        Alert.alert(
            "Logout",
            "Are you sure you want to logout?",
            [
                {
                    text: "Cancel",
                    style: "cancel"
                },
                {
                    text: "Logout",
                    style: "destructive",
                    onPress: async () => {
                            try {
                              await logout(user?.email);

                              Toast.show({
                                type: 'success',
                                text1: 'Logged out successfully',
                                position: 'top',
                                visibilityTime: 2000,
                              });

                              if(router.canDismiss()) {
                                router.dismissAll();
                              }
                              router.replace('/RegisterModule/LoginScreen');

                            } catch (error) {
                              console.error('Logout error:', error);
                            }
                        }
                    }
            ]
        )
    }

    const handleDeleteAccount = () => {
        Alert.alert(
            "Delete Account",
            "This action cannot be undone. All your data will be permanently deleted.",
            [
                {
                    text: "Cancel",
                    style: "cancel"
                },
                {
                    text: "Continue",
                    style: "destructive",
                    onPress: () => {
                        setUsernameInput('');
                        setInputError('');
                        setShowDeleteModal(true);
                    }
                }
            ]
        )
    }

    const handleConfirmDelete = async () => {
        if (!usernameInput.trim()) {
            setInputError("Username is required to delete your account.");
            return;
        }

        const expectedUsername = user?.username;
        if (usernameInput.trim() !== expectedUsername) {
            setInputError("The username you entered doesn't match your account.");
            return;
        }

        // Proceed with account deletion
        try {
            setShowDeleteModal(false);
            await deleteUser(user?.email);

            Toast.show({
                type: 'success',
                text1: 'Account deleted successfully',
                position: 'top',
                visibilityTime: 2000,
            });

            if(router.canDismiss()) {
                router.dismissAll();
            }
            router.replace('/RegisterModule/LoginScreen');

        } catch (error) {
            console.error('Delete account error:', error);
            Toast.show({
                type: 'error',
                text1: 'Failed to delete account',
                text2: 'Please try again later',
                position: 'top',
                visibilityTime: 3000,
            });
        }
    }

    const closeDeleteModal = () => {
        setShowDeleteModal(false);
        setUsernameInput('');
        setInputError('');
    }

    const openURL = (url) => {
        Linking.openURL(url)
    }

    const SettingItem = ({ icon, title, onPress, rightComponent, showArrow = true, isDestructive = false }) => (
        <TouchableOpacity style={styles.settingItem} onPress={onPress}>
            <View style={styles.settingLeft}>
                <Ionicons name={icon} size={24} color={isDestructive ? "#ff4444" : "#9188E5"} />
                <Text style={[styles.settingTitle, isDestructive && styles.destructiveText]}>{title}</Text>
            </View>
            <View style={styles.settingRight}>
                {rightComponent}
                {showArrow && <Ionicons name="chevron-forward" size={20} color="#ccc" />}
            </View>
        </TouchableOpacity>
    )

    const SettingSection = ({ title, children }) => (
        <View style={styles.section}>
            <Text style={styles.sectionTitle}>{title}</Text>
            <View style={styles.sectionContent}>
                {children}
            </View>
        </View>
    )

    return (
        <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
            <SettingSection title="Account">
                <SettingItem
                    icon="person-outline"
                    title="Edit Profile"
                    onPress={() => router.push(`/UserModule/EditProfile`)}
                />
                <SettingItem
                    icon="key-outline"
                    title="Change Password"
                    onPress={() => router.push('/Settings/ChangePassword')}
                />
            </SettingSection>

            <SettingSection title="Privacy & Safety">
                <SettingItem
                    icon="ban-outline"
                    title="Blocked Users"
                    onPress={() => router.push(`/UserModule/${user?.userId}/Blocked`)}
                />
            </SettingSection>

            <SettingSection title="App Preferences">
                <SettingItem
                    icon="moon-outline"
                    title="Dark Mode"
                    rightComponent={
                        <Switch
                            value={darkMode}
                            onValueChange={setDarkMode}
                            trackColor={{ false: "#e0e0e0", true: "#9188E5" }}
                            thumbColor={darkMode ? "#fff" : "#f4f3f4"}
                        />
                    }
                    showArrow={false}
                />
            </SettingSection>

            <SettingSection title="Pet Care">
                <SettingItem
                    icon="heart-outline"
                    title="Emergency Contacts"
                    onPress={() => router.push('/Settings/EmergencyContacts')}
                />
            </SettingSection>

            <SettingSection title="Support">
                <SettingItem
                    icon="help-circle-outline"
                    title="Help & FAQ"
                    onPress={() => router.push('/Settings/Help')}
                />
                <SettingItem
                    icon="chatbubble-outline"
                    title="Contact Support"
                    onPress={() => router.push('/Settings/ContactSupport')}
                />
            </SettingSection>

            <SettingSection title="Legal">
                <SettingItem
                    icon="document-text-outline"
                    title="Terms of Service"
                    onPress={() => openURL('https://example.com/terms')}
                />
                <SettingItem
                    icon="shield-outline"
                    title="Privacy Policy"
                    onPress={() => openURL('https://example.com/privacy')}
                />
                <SettingItem
                    icon="information-circle-outline"
                    title="About"
                    onPress={() => router.push('/Settings/About')}
                />
            </SettingSection>

            <SettingSection title="Account Actions">
                <SettingItem
                    icon="log-out-outline"
                    title="Logout"
                    onPress={handleLogout}
                    showArrow={false}
                    isDestructive={true}
                />
                <SettingItem
                    icon="trash-outline"
                    title="Delete Account"
                    onPress={handleDeleteAccount}
                    showArrow={false}
                    isDestructive={true}
                />
            </SettingSection>

            <View style={styles.footer}>
                <Text style={styles.footerText}>PetZania v1.0.0</Text>
                <Text style={styles.footerSubtext}>Made with love for pet lovers</Text>
            </View>

            {/* Delete Account Confirmation Modal */}
            <Modal
                visible={showDeleteModal}
                transparent={true}
                animationType="fade"
                onRequestClose={closeDeleteModal}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContainer}>
                        <Text style={styles.modalTitle}>Confirm Account Deletion</Text>
                        <Text style={styles.modalMessage}>
                            Please enter your username "{user?.username || user?.email}" to confirm deletion:
                        </Text>
                        
                        <TextInput
                            style={[styles.modalInput, inputError ? styles.modalInputError : null]}
                            placeholder="Enter your username"
                            value={usernameInput}
                            onChangeText={(text) => {
                                setUsernameInput(text);
                                if (inputError) setInputError('');
                            }}
                            autoCapitalize="none"
                            autoCorrect={false}
                        />
                        
                        {inputError ? (
                            <Text style={styles.modalError}>{inputError}</Text>
                        ) : null}
                        
                        <View style={styles.modalButtons}>
                            <TouchableOpacity 
                                style={styles.modalCancelButton} 
                                onPress={closeDeleteModal}
                            >
                                <Text style={styles.modalCancelText}>Cancel</Text>
                            </TouchableOpacity>
                            
                            <TouchableOpacity 
                                style={styles.modalDeleteButton} 
                                onPress={handleConfirmDelete}
                            >
                                <Text style={styles.modalDeleteText}>Delete Account</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>
        </ScrollView>
    )
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#f8f9fa',
    },
    section: {
        marginTop: 20,
        marginHorizontal: 16,
    },
    sectionTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
        marginBottom: 12,
        paddingHorizontal: 4,
    },
    sectionContent: {
        backgroundColor: '#fff',
        borderRadius: 12,
        overflow: 'hidden',
        // iOS shadow
        shadowColor: '#000',
        shadowOffset: {
            width: 0,
            height: 1,
        },
        shadowOpacity: 0.1,
        shadowRadius: 2,
        // Android shadow
        elevation: 2,
    },
    settingItem: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingVertical: 16,
        paddingHorizontal: 16,
        borderBottomWidth: StyleSheet.hairlineWidth,
        borderBottomColor: '#f0f0f0',
    },
    settingLeft: {
        flexDirection: 'row',
        alignItems: 'center',
        flex: 1,
    },
    settingTitle: {
        fontSize: 16,
        color: '#333',
        marginLeft: 12,
        fontWeight: '500',
    },
    destructiveText: {
        color: '#ff4444',
    },
    settingRight: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
    },
    footer: {
        alignItems: 'center',
        paddingVertical: 32,
        paddingHorizontal: 16,
    },
    footerText: {
        fontSize: 14,
        color: '#999',
        fontWeight: '500',
    },
    footerSubtext: {
        fontSize: 12,
        color: '#ccc',
        marginTop: 4,
    },
    // Modal styles
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        justifyContent: 'center',
        alignItems: 'center',
        padding: 20,
    },
    modalContainer: {
        backgroundColor: '#fff',
        borderRadius: 16,
        padding: 24,
        width: '100%',
        maxWidth: 400,
        alignItems: 'center',
    },
    modalTitle: {
        fontSize: 20,
        fontWeight: '600',
        color: '#333',
        marginBottom: 12,
        textAlign: 'center',
    },
    modalMessage: {
        fontSize: 16,
        color: '#666',
        textAlign: 'center',
        marginBottom: 20,
        lineHeight: 22,
    },
    modalInput: {
        width: '100%',
        height: 50,
        borderWidth: 1,
        borderColor: '#e0e0e0',
        borderRadius: 12,
        paddingHorizontal: 16,
        fontSize: 16,
        backgroundColor: '#f8f9fa',
        marginBottom: 8,
    },
    modalInputError: {
        borderColor: '#ff4444',
        backgroundColor: '#ffebee',
    },
    modalError: {
        color: '#ff4444',
        fontSize: 14,
        textAlign: 'center',
        marginBottom: 16,
    },
    modalButtons: {
        flexDirection: 'row',
        width: '100%',
        gap: 12,
        marginTop: 8,
    },
    modalCancelButton: {
        flex: 1,
        height: 48,
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#e0e0e0',
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#f8f9fa',
    },
    modalCancelText: {
        fontSize: 16,
        fontWeight: '500',
        color: '#666',
    },
    modalDeleteButton: {
        flex: 1,
        height: 48,
        borderRadius: 12,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#ff4444',
    },
    modalDeleteText: {
        fontSize: 16,
        fontWeight: '600',
        color: '#fff',
    },
})