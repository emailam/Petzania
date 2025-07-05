import { StyleSheet, Text, View, TouchableOpacity, Alert, Linking, ScrollView } from 'react-native'
import React, { useContext } from 'react'
import { Ionicons } from '@expo/vector-icons'
import { UserContext } from '@/context/UserContext'
import Toast from 'react-native-toast-message'

export default function ContactSupport() {
    const { user } = useContext(UserContext);

    const supportEmail = 'petzaniasystem@gmail.com';

    const handleEmailPress = () => {
        const emailUrl = `mailto:${supportEmail}`;
        
        Linking.canOpenURL(emailUrl).then((supported) => {
            if (supported) {
                Linking.openURL(emailUrl);
                Toast.show({
                    type: 'success',
                    text1: 'Email Client Opened',
                    text2: 'You can now compose your message',
                    position: 'top',
                    visibilityTime: 3000,
                });
            } else {
                Alert.alert(
                    'Email Not Available',
                    `No email client is available on this device. Please email us directly at ${supportEmail}`,
                    [{ text: 'OK' }]
                );
            }
        });
    };

    const handleCopyEmail = () => {
        // Since Clipboard is not imported, we'll show the email in an alert for easy copying
        Alert.alert(
            'Support Email',
            supportEmail,
            [
                { text: 'OK', style: 'default' }
            ]
        );
    };

    return (
        <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>

            {/* Contact Information */}
            <View style={styles.contactSection}>
                <Text style={styles.sectionTitle}>Get in Touch</Text>
                
                {/* Email Contact */}
                <TouchableOpacity style={styles.contactItem} onPress={handleEmailPress}>
                    <View style={styles.contactIcon}>
                        <Ionicons name="mail" size={24} color="#9188E5" />
                    </View>
                    <View style={styles.contactInfo}>
                        <Text style={styles.contactTitle}>Email Support</Text>
                        <Text style={styles.contactEmail}>{supportEmail}</Text>
                        <Text style={styles.contactDescription}>
                            Send us an email and we'll get back to you within 24 hours
                        </Text>
                    </View>
                    <Ionicons name="chevron-forward" size={20} color="#C7C7CC" />
                </TouchableOpacity>

                {/* Copy Email Option */}
                <TouchableOpacity style={styles.contactItem} onPress={handleCopyEmail}>
                    <View style={styles.contactIcon}>
                        <Ionicons name="copy" size={24} color="#9188E5" />
                    </View>
                    <View style={styles.contactInfo}>
                        <Text style={styles.contactTitle}>Copy Email Address</Text>
                        <Text style={styles.contactDescription}>
                            View the email address to copy manually
                        </Text>
                    </View>
                    <Ionicons name="chevron-forward" size={20} color="#C7C7CC" />
                </TouchableOpacity>
            </View>

            {/* Support Categories */}
            <View style={styles.categoriesSection}>
                <Text style={styles.sectionTitle}>We Can Help With</Text>
                <View style={styles.categoriesGrid}>
                    {[
                        { icon: 'help-circle', title: 'General Questions', description: 'General inquiries about PetZania' },
                        { icon: 'bug', title: 'Bug Reports', description: 'Report technical issues or bugs' },
                        { icon: 'star', title: 'Feature Requests', description: 'Suggest new features or improvements' },
                        { icon: 'person', title: 'Account Issues', description: 'Problems with your account or profile' },
                        { icon: 'heart', title: 'Pet Care Questions', description: 'Questions about pet care and health' },
                        { icon: 'settings', title: 'Technical Support', description: 'App functionality and technical help' }
                    ].map((category, index) => (
                        <View key={index} style={styles.categoryCard}>
                            <Ionicons name={category.icon} size={20} color="#9188E5" />
                            <Text style={styles.categoryTitle}>{category.title}</Text>
                            <Text style={styles.categoryDescription}>{category.description}</Text>
                        </View>
                    ))}
                </View>
            </View>
        </ScrollView>
    )
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#f8f9fa',
    },
    contactSection: {
        padding: 20,
        backgroundColor: '#fff',
        marginTop: 12,
        marginBottom: 12,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '600',
        color: '#333',
        marginBottom: 16,
    },
    contactItem: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 16,
        paddingHorizontal: 12,
        marginBottom: 12,
        backgroundColor: '#f8f9fa',
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#f0f0f0',
    },
    contactIcon: {
        width: 40,
        height: 40,
        borderRadius: 20,
        backgroundColor: '#f0f4ff',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    contactInfo: {
        flex: 1,
    },
    contactTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
        marginBottom: 4,
    },
    contactEmail: {
        fontSize: 14,
        color: '#9188E5',
        fontWeight: '500',
        marginBottom: 4,
    },
    contactDescription: {
        fontSize: 13,
        color: '#666',
        lineHeight: 18,
    },
    categoriesSection: {
        padding: 20,
        backgroundColor: '#fff',
        marginBottom: 12,
    },
    categoriesGrid: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
    },
    categoryCard: {
        width: '48%',
        backgroundColor: '#f8f9fa',
        borderRadius: 12,
        padding: 16,
        marginBottom: 12,
        borderWidth: 1,
        borderColor: '#f0f0f0',
        alignItems: 'center',
    },
    categoryTitle: {
        fontSize: 14,
        fontWeight: '600',
        color: '#333',
        marginTop: 8,
        marginBottom: 4,
        textAlign: 'center',
    },
    categoryDescription: {
        fontSize: 12,
        color: '#666',
        textAlign: 'center',
        lineHeight: 16,
    },
    userSection: {
        padding: 20,
        backgroundColor: '#fff',
        marginBottom: 20,
    },
    userNote: {
        fontSize: 14,
        color: '#666',
        marginBottom: 12,
        lineHeight: 20,
    },
    userInfo: {
        backgroundColor: '#f8f9fa',
        borderRadius: 12,
        padding: 16,
        borderWidth: 1,
        borderColor: '#f0f0f0',
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
        flex: 1,
    },
})