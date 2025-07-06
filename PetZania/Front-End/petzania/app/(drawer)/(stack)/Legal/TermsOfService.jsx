import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';

export default function TermsOfService() {
    return (
        <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
            <View style={styles.content}>
                <Text style={styles.title}>Terms of Service</Text>
                <Text style={styles.lastUpdated}>Last updated: July 6, 2025</Text>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>1. Acceptance of Terms</Text>
                    <Text style={styles.text}>
                        By using PetZania, you agree to these Terms of Service. If you don't agree, please don't use our app.
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>2. About PetZania</Text>
                    <Text style={styles.text}>
                        PetZania is a social platform for pet owners to connect, share, and find pet adoption and breeding opportunities.
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>3. Your Account</Text>
                    <Text style={styles.text}>
                        • You must provide accurate information when creating your account{'\n'}
                        • You're responsible for keeping your account secure{'\n'}
                        • One account per person{'\n'}
                        • You must be at least 13 years old to use PetZania
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>4. User Behavior</Text>
                    <Text style={styles.text}>
                        • Be respectful to other users{'\n'}
                        • Don't post harmful, illegal, or inappropriate content{'\n'}
                        • Don't spam or harass other users{'\n'}
                        • Don't share false information about pets or pet care
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>5. Pet Safety</Text>
                    <Text style={styles.text}>
                        • All pet adoption and breeding activities are between users{'\n'}
                        • We recommend meeting in safe, public places{'\n'}
                        • Verify pet health and vaccination records{'\n'}
                        • We're not responsible for transactions between users
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>6. Content You Post</Text>
                    <Text style={styles.text}>
                        • You own the content you post{'\n'}
                        • You give us permission to display your content in the app{'\n'}
                        • Don't post copyrighted material that isn't yours{'\n'}
                        • We may remove content that violates these terms
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>7. Privacy</Text>
                    <Text style={styles.text}>
                        Your privacy is important to us. Please read our Privacy Policy to understand how we handle your information.
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>8. Changes to Terms</Text>
                    <Text style={styles.text}>
                        We may update these terms occasionally. We'll notify you of significant changes through the app.
                    </Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTitle}>9. Contact Us</Text>
                    <Text style={styles.text}>
                        If you have questions about these terms, contact us at petzaniasystem@gmail.com
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
