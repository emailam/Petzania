import { StyleSheet, Text, View, TouchableOpacity, TextInput, Alert, Linking, KeyboardAvoidingView, ScrollView, Platform } from 'react-native'
import React, { useState, useContext } from 'react'
import { Ionicons } from '@expo/vector-icons'
import { UserContext } from '@/context/UserContext'
import Toast from 'react-native-toast-message'

export default function ContactSupport() {
    const { user } = useContext(UserContext);
    const [subject, setSubject] = useState('');
    const [message, setMessage] = useState('');
    const [category, setCategory] = useState('General');

    const categories = [
        'General',
        'Bug Report',
        'Feature Request',
        'Account Issue',
        'Pet Care Question',
        'Technical Support'
    ];

    const handleSendEmail = () => {
        if (!subject.trim() || !message.trim()) {
            Alert.alert(
                'Missing Information',
                'Please fill in both subject and message fields.',
                [{ text: 'OK' }]
            );
            return;
        }

        const emailSubject = `[PetZania Support] ${category}: ${subject}`;
        const emailBody = `Hello PetZania Support Team,

            Category: ${category}
            User: ${user?.name || 'Guest'} (${user?.email || 'No email'})
            User ID: ${user?.userId || 'N/A'}

            Message:
            ${message}

            ---
            Sent from PetZania Mobile App
            App Version: 1.0.0
            Date: ${new Date().toLocaleString()}`;

        const emailUrl = `mailto:petzaniasystem@gmail.com?subject=${encodeURIComponent(emailSubject)}&body=${encodeURIComponent(emailBody)}`;

        Linking.canOpenURL(emailUrl).then((supported) => {
            if (supported) {
                Linking.openURL(emailUrl);
                Toast.show({
                    type: 'success',
                    text1: 'Email Client Opened',
                    text2: 'Your message has been prepared for sending',
                    position: 'top',
                    visibilityTime: 3000,
                });
                // Reset form after opening email client
                setSubject('');
                setMessage('');
                setCategory('General');
            } else {
                Alert.alert(
                    'Email Not Available',
                    'No email client is available on this device. Please email us directly at petzaniasystem@gmail.com',
                    [{ text: 'OK' }]
                );
            }
        });
    };

    return (
        <KeyboardAvoidingView
            style={styles.container}
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
        >
            <ScrollView style={styles.scrollView} showsVerticalScrollIndicator={false}>
                <View style={styles.header}>
                    <Ionicons name="mail" size={32} color="#9188E5" />
                    <Text style={styles.title}>Contact Support</Text>
                    <Text style={styles.subtitle}>
                        Need help? Send us a message and we'll get back to you as soon as possible.
                    </Text>
                </View>

                <View style={styles.form}>
                    {/* Category Selection */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>Category</Text>
                        <ScrollView
                            horizontal
                            showsHorizontalScrollIndicator={false}
                            style={styles.categoryContainer}
                        >
                            {categories.map((cat) => (
                                <TouchableOpacity
                                    key={cat}
                                    style={[
                                        styles.categoryChip,
                                        category === cat && styles.categoryChipSelected
                                    ]}
                                    onPress={() => setCategory(cat)}
                                >
                                    <Text style={[
                                        styles.categoryText,
                                        category === cat && styles.categoryTextSelected
                                    ]}>
                                        {cat}
                                    </Text>
                                </TouchableOpacity>
                            ))}
                        </ScrollView>
                    </View>

                    {/* Subject Input */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>Subject</Text>
                        <TextInput
                            style={styles.textInput}
                            value={subject}
                            onChangeText={setSubject}
                            placeholder="Brief description of your issue or question"
                            placeholderTextColor="#999"
                            maxLength={100}
                        />
                        <Text style={styles.charCount}>{subject.length}/100</Text>
                    </View>

                    {/* Message Input */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>Message</Text>
                        <TextInput
                            style={[styles.textInput, styles.messageInput]}
                            value={message}
                            onChangeText={setMessage}
                            placeholder="Please provide detailed information about your issue, question, or feedback. Include any relevant details that might help us assist you better."
                            placeholderTextColor="#999"
                            multiline
                            numberOfLines={6}
                            textAlignVertical="top"
                            maxLength={1000}
                        />
                        <Text style={styles.charCount}>{message.length}/1000</Text>
                    </View>

                    {/* User Info Display */}
                    <View style={styles.userInfo}>
                        <Text style={styles.userInfoTitle}>Your Information</Text>
                        <View style={styles.userInfoRow}>
                            <Ionicons name="person" size={16} color="#666" />
                            <Text style={styles.userInfoText}>
                                {user?.name || 'Guest User'}
                            </Text>
                        </View>
                        <View style={styles.userInfoRow}>
                            <Ionicons name="mail" size={16} color="#666" />
                            <Text style={styles.userInfoText}>
                                {user?.email || 'No email provided'}
                            </Text>
                        </View>
                        <Text style={styles.userInfoNote}>
                            This information will be included in your support request
                        </Text>
                    </View>

                    {/* Send Button */}
                    <TouchableOpacity
                        style={styles.sendButton}
                        onPress={handleSendEmail}
                    >
                        <Ionicons name="send" size={20} color="#fff" />
                        <Text style={styles.sendButtonText}>Send Message</Text>
                    </TouchableOpacity>
                </View>
            </ScrollView>
        </KeyboardAvoidingView>
    )
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#f8f9fa',
    },
    scrollView: {
        flex: 1,
    },
    header: {
        padding: 24,
        alignItems: 'center',
        borderBottomWidth: 1,
        borderBottomColor: '#f0f0f0',
    },
    title: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#333',
        marginTop: 6,
        marginBottom: 8,
    },
    subtitle: {
        fontSize: 16,
        color: '#666',
        textAlign: 'center',
        lineHeight: 22,
    },
    form: {
        padding: 20,
    },
    inputGroup: {
        marginBottom: 20,
    },
    label: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
        marginBottom: 8,
    },
    categoryContainer: {
        flexDirection: 'row',
    },
    categoryChip: {
        paddingHorizontal: 16,
        paddingVertical: 8,
        marginRight: 8,
        backgroundColor: '#f0f0f0',
        borderRadius: 20,
        borderWidth: 1,
        borderColor: '#e0e0e0',
    },
    categoryChipSelected: {
        backgroundColor: '#9188E5',
        borderColor: '#9188E5',
    },
    categoryText: {
        fontSize: 14,
        color: '#666',
        fontWeight: '500',
    },
    categoryTextSelected: {
        color: '#fff',
    },
    textInput: {
        backgroundColor: '#fff',
        borderWidth: 1,
        borderColor: '#e0e0e0',
        borderRadius: 12,
        padding: 16,
        fontSize: 16,
        color: '#333',
    },
    messageInput: {
        minHeight: 120,
        maxHeight: 200,
    },
    charCount: {
        fontSize: 12,
        color: '#999',
        textAlign: 'right',
        marginTop: 4,
    },
    userInfo: {
        backgroundColor: '#fff',
        borderRadius: 12,
        padding: 16,
        marginBottom: 20,
        borderWidth: 1,
        borderColor: '#f0f0f0',
    },
    userInfoTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
        marginBottom: 12,
    },
    userInfoRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 8,
    },
    userInfoText: {
        fontSize: 14,
        color: '#666',
        marginLeft: 8,
    },
    userInfoNote: {
        fontSize: 12,
        color: '#999',
        fontStyle: 'italic',
        marginTop: 8,
    },
    sendButton: {
        backgroundColor: '#9188E5',
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 16,
        borderRadius: 12,
        marginBottom: 20,
    },
    sendButtonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '600',
        marginLeft: 8,
    },
})