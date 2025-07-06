import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';

export default function PrivacyPolicy() {
    return (
        <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
            <View style={styles.content}>
                <Text style={styles.title}>Privacy Policy</Text>
                <Text style={styles.lastUpdated}>Last updated: July 6, 2025</Text>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>1. Information We Collect</Text>
                    <Text style={styles.text}>
                        We collect information you provide when you:{'\n'}
                        • Create an account (name, email, profile picture){'\n'}
                        • Post content (photos, text, pet information){'\n'}
                        • Use our features (messaging, following, etc.)
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>2. How We Use Your Information</Text>
                    <Text style={styles.text}>
                        We use your information to:{'\n'}
                        • Provide and improve our services{'\n'}
                        • Connect you with other pet owners{'\n'}
                        • Send you important updates{'\n'}
                        • Keep the platform safe and secure
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>3. Information Sharing</Text>
                    <Text style={styles.text}>
                        We don't sell your personal information. We may share information:{'\n'}
                        • With other users as part of the social features{'\n'}
                        • If required by law{'\n'}
                        • To protect our users' safety
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>4. Your Choices</Text>
                    <Text style={styles.text}>
                        You can:{'\n'}
                        • Update your profile information anytime{'\n'}
                        • Control who can see your content{'\n'}
                        • Delete your account{'\n'}
                        • Block or report other users
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>5. Data Security</Text>
                    <Text style={styles.text}>
                        We take reasonable steps to protect your information, but no system is 100% secure. Please keep your account password safe.
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>6. Children's Privacy</Text>
                    <Text style={styles.text}>
                        PetZania is not intended for children under 13. If we learn that a child under 13 has provided us with personal information, we will delete it.
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>7. Location Information</Text>
                    <Text style={styles.text}>
                        We may collect location information to help you find nearby pets and services. You can disable location sharing in your device settings.
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>8. Changes to This Policy</Text>
                    <Text style={styles.text}>
                        We may update this privacy policy from time to time. We'll notify you of significant changes through the app.
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>9. Contact Us</Text>
                    <Text style={styles.text}>
                        If you have questions about this privacy policy, contact us at petzaniasystem@gmail.com
                    </Text>
                </View>
            </View>
        </ScrollView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    content: {
        padding: 20,
    },
    title: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#333',
        marginBottom: 8,
        textAlign: 'center',
    },
    lastUpdated: {
        fontSize: 14,
        color: '#666',
        textAlign: 'center',
        marginBottom: 24,
        fontStyle: 'italic',
    },
    section: {
        marginBottom: 20,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '600',
        color: '#9188E5',
        marginBottom: 8,
    },
    text: {
        fontSize: 16,
        color: '#333',
        lineHeight: 24,
    },
});
