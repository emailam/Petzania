import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, ScrollView, TouchableOpacity, Alert } from 'react-native';
import { Image } from 'expo-image';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import ImageViewing from 'react-native-image-viewing';
import Toast from 'react-native-toast-message';
import { getUserById } from '@/services/userService';
import { getUserChatByChatId, deleteUserChat } from '@/services/chatService';

const defaultImage = require('@/assets/images/Defaults/default-user.png');

export default function UserProfileScreen() {
    const { userId, chatId } = useLocalSearchParams();
    const router = useRouter();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [deleting, setDeleting] = useState(false);
    const [showImageViewer, setShowImageViewer] = useState(false);

    // Log parameters for debugging
    console.log('📱 UserProfile params - userId:', userId, 'chatId:', chatId);

    const handleImagePress = () => {
        if (!user?.profilePictureURL) return;
        setShowImageViewer(true);
    };

    // Messenger-style actions
    const handleDeleteChat = async () => {
        if (!chatId) {
            Alert.alert('Error', 'No conversation found to delete.');
            return;
        }

        if (deleting) {
            return; // Prevent multiple deletions
        }

        Alert.alert(
            'Delete Conversation',
            'Are you sure you want to delete this conversation? This action cannot be undone.',
            [
                { text: 'Cancel', style: 'cancel' },
                { text: 'Delete', style: 'destructive', onPress: async () => {
                    setDeleting(true);
                    try {
                        console.log('🗑️ Starting chat deletion process for chatId:', chatId);
                        
                        // First, get the userChat by chatId
                        const userChat = await getUserChatByChatId(chatId);
                        
                        if (!userChat) {
                            throw new Error('Conversation not found or already deleted');
                        }

                        if (!userChat.userChatId) {
                            throw new Error('Invalid conversation data');
                        }

                        console.log('📋 Found userChat:', userChat.userChatId);

                        // Then delete the userChat using userChatId
                        await deleteUserChat(userChat.userChatId);
                        
                        console.log('✅ User chat deleted successfully:', userChat.userChatId);
                        
                        // Show success toast
                        Toast.show({
                            type: 'success',
                            text1: 'Conversation Deleted',
                            text2: 'The conversation has been removed from your chat list',
                            position: 'top',
                            visibilityTime: 3000,
                        });
                        
                        // Navigate back to chat list
                        router.replace('/Chat');
                        
                    } catch (error) {
                        console.error('❌ Error deleting chat:', error);
                        
                        let errorMessage = 'Failed to delete conversation. Please try again.';
                        
                        // Handle specific error cases
                        if (error.response?.status === 404) {
                            errorMessage = 'Conversation not found or already deleted.';
                        } else if (error.response?.status === 403) {
                            errorMessage = 'You do not have permission to delete this conversation.';
                        } else if (error.message) {
                            errorMessage = error.message;
                        }
                        
                        Alert.alert('Error', errorMessage);
                        
                        Toast.show({
                            type: 'error',
                            text1: 'Delete Failed',
                            text2: errorMessage,
                            position: 'top',
                            visibilityTime: 4000,
                        });
                    } finally {
                        setDeleting(false);
                    }
                }}
            ]
        );
    };

    useEffect(() => {
        const fetchUser = async () => {
            try {
                const userData = await getUserById(userId);
                setUser(userData);
            } catch (err) {
                setError('Failed to load user profile.');
            } finally {
                setLoading(false);
            }
        };
        fetchUser();
    }, [userId]);

    if (loading) {
        return (
            <View style={styles.centered}>
                <ActivityIndicator size="large" color="#9188E5" />
                <Text style={styles.loadingText}>Loading profile...</Text>
            </View>
        );
    }

    if (error || !user) {
        return (
            <View style={styles.centered}>
                <Ionicons name="alert-circle-outline" size={60} color="#ff4444" />
                <Text style={styles.errorText}>{error || 'User not found.'}</Text>
            </View>
        );
    }

    return (
        <ScrollView style={styles.container}>
            {/* User profile section */}
            <View style={styles.profileSection}>
                <TouchableOpacity
                    style={styles.profileImageContainer}
                    onPress={handleImagePress}
                    activeOpacity={user?.profilePictureURL ? 0.7 : 1}
                >
                    <Image
                        source={user.profilePictureURL ? { uri: user.profilePictureURL } : defaultImage}
                        style={styles.profileAvatar}
                        contentFit="cover"
                    />
                </TouchableOpacity>
                <Text style={styles.profileName}>{user.name || 'Unknown User'}</Text>
                <Text style={styles.profileStatus}>
                    {user.online ? 'Online' : 'Offline'}
                </Text>
                {user.bio && (
                    <Text style={styles.profileBio}>{user.bio}</Text>
                )}
            </View>

            {/* Options list */}
            <View style={styles.optionsList}>
                <TouchableOpacity style={styles.optionItem} onPress={() => router.push({
                    pathname: `/UserModule/${userId}`,
                    params: { username: user.username }
                })}
                >
                    <View style={styles.optionIcon}>
                        <Ionicons name="person-outline" size={24} color="#9188E5" />
                    </View>
                    <Text style={styles.optionText}>View Profile</Text>
                    <Ionicons name="chevron-forward" size={20} color="#C7C7CC" />
                </TouchableOpacity>
            </View>

            {/* Contact info section */}
            {(user.email || user.phone) && (
                <View style={styles.contactSection}>
                    <Text style={styles.sectionTitle}>Contact Info</Text>
                    {user.email && (
                        <View style={styles.contactItem}>
                            <Ionicons name="mail-outline" size={20} color="#9188E5" />
                            <Text style={styles.contactText}>{user.email}</Text>
                        </View>
                    )}
                    {user.phone && (
                        <View style={styles.contactItem}>
                            <Ionicons name="call-outline" size={20} color="#9188E5" />
                            <Text style={styles.contactText}>{user.phone}</Text>
                        </View>
                    )}
                </View>
            )}

            {/* Danger zone - only show if we have a chatId */}
            {chatId && (
                <View style={styles.dangerSection}>
                    <TouchableOpacity 
                        style={[
                            styles.optionItem, 
                            styles.dangerItem,
                            deleting && styles.disabledItem
                        ]} 
                        onPress={handleDeleteChat}
                        disabled={deleting}
                    >
                        <View style={styles.optionIcon}>
                            {deleting ? (
                                <ActivityIndicator size={24} color="#FF3B30" />
                            ) : (
                                <Ionicons name="trash-outline" size={24} color="#FF3B30" />
                            )}
                        </View>
                        <Text style={[styles.optionText, styles.dangerText]}>
                            {deleting ? 'Deleting...' : 'Delete Conversation'}
                        </Text>
                    </TouchableOpacity>
                </View>
            )}
            
            <ImageViewing
                images={[{ uri: user?.profilePictureURL || '' }]}
                imageIndex={0}
                visible={showImageViewer}
                onRequestClose={() => setShowImageViewer(false)}
                backgroundColor="black"
                swipeToCloseEnabled
                doubleTapToZoomEnabled
            />
        </ScrollView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    centered: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#fff',
        padding: 24,
    },
    loadingText: {
        marginTop: 12,
        fontSize: 16,
        color: '#9188E5',
    },
    errorText: {
        marginTop: 16,
        fontSize: 16,
        color: '#ff4444',
        textAlign: 'center',
    },
    profileSection: {
        backgroundColor: '#fff',
        alignItems: 'center',
        paddingVertical: 30,
        marginBottom: 20,
    },
    profileImageContainer: {
        borderRadius: 75,
        marginBottom: 15,
    },
    profileAvatar: {
        width: 150,
        height: 150,
        borderRadius: 75,
        borderWidth: 3,
        borderColor: '#9188E5',
    },
    profileName: {
        fontSize: 22,
        fontWeight: '600',
        color: '#000',
        marginBottom: 5,
    },
    profileStatus: {
        fontSize: 16,
        color: '#9188E5',
        marginBottom: 10,
    },
    profileBio: {
        fontSize: 16,
        color: '#666',
        textAlign: 'center',
        marginHorizontal: 16,
        lineHeight: 22,
    },
    optionsList: {
        backgroundColor: '#fff',
        marginBottom: 20,
        borderTopWidth: 1,
        borderBottomWidth: 1,
        borderColor: '#E5E5EA',
    },
    optionItem: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 12,
        paddingHorizontal: 16,
        minHeight: 44,
        backgroundColor: '#fff',
    },
    optionIcon: {
        width: 30,
        alignItems: 'center',
        marginRight: 12,
    },
    optionText: {
        flex: 1,
        fontSize: 17,
        color: '#000',
    },
    separator: {
        height: 0.5,
        backgroundColor: '#E5E5EA',
        marginLeft: 58,
    },
    contactSection: {
        backgroundColor: '#fff',
        marginBottom: 20,
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderTopWidth: 1,
        borderBottomWidth: 1,
        borderColor: '#E5E5EA',
    },
    sectionTitle: {
        fontSize: 13,
        color: '#8E8E93',
        textTransform: 'uppercase',
        marginBottom: 10,
        fontWeight: '600',
    },
    contactItem: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 8,
    },
    contactText: {
        fontSize: 17,
        color: '#9188E5',
        marginLeft: 12,
    },
    dangerSection: {
        backgroundColor: '#fff',
        marginBottom: 20,
        borderTopWidth: 1,
        borderBottomWidth: 1,
        borderColor: '#E5E5EA',
    },
    dangerItem: {
        // Additional styling for danger items
    },
    disabledItem: {
        opacity: 0.6,
    },
    dangerText: {
        color: '#FF3B30',
    },
});
