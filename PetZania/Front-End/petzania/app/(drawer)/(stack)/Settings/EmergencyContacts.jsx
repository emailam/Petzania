import { StyleSheet, Text, View, ScrollView, TouchableOpacity, Linking, Alert } from 'react-native'
import React, { useState } from 'react'
import { Ionicons } from '@expo/vector-icons'

export default function EmergencyContacts() {
    const [contacts] = useState([
        {
            id: 1,
            name: "Cairo Veterinary Emergency",
            type: "Emergency Vet",
            phone: "+20-2-2735-4321",
            address: "Zamalek, Cairo",
            available: "24/7"
        },
        {
            id: 2,
            name: "Animal Medical Center",
            type: "Primary Veterinarian",
            phone: "+20-2-2749-8765",
            address: "Maadi, Cairo",
            available: "Daily 9AM-9PM"
        },
        {
            id: 3,
            name: "Egyptian Animal Poison Control",
            type: "Poison Control",
            phone: "+20-2-2794-1234",
            address: "National Hotline",
            available: "24/7"
        },
        {
            id: 4,
            name: "Pet Care Egypt",
            type: "Pet Sitter",
            phone: "+20-10-1234-5678",
            address: "Greater Cairo Area",
            available: "Daily 7AM-10PM"
        },
        {
            id: 5,
            name: "Alexandria Vet Clinic",
            type: "Emergency Vet",
            phone: "+20-3-486-7890",
            address: "Alexandria",
            available: "24/7"
        },
        {
            id: 6,
            name: "Giza Animal Hospital",
            type: "Primary Veterinarian",
            phone: "+20-2-3765-4321",
            address: "Giza",
            available: "Sat-Thu 8AM-8PM"
        }
    ]);

    const handleCall = (phone, name) => {
        Alert.alert(
            "Call Contact",
            `Call ${name} at ${phone}?`,
            [
                { text: "Cancel", style: "cancel" },
                {
                    text: "Call",
                    onPress: () => {
                        const phoneNumber = phone.replace(/[^+\d]/g, '');
                        Linking.openURL(`tel:${phoneNumber}`);
                    }
                }
            ]
        );
    };

    const getTypeIcon = (type) => {
        switch (type) {
            case 'Emergency Vet':
                return 'medical';
            case 'Primary Veterinarian':
                return 'heart';
            case 'Poison Control':
                return 'warning';
            case 'Pet Sitter':
                return 'home';
            default:
                return 'call';
        }
    };

    const getTypeColor = (type) => {
        switch (type) {
            case 'Emergency Vet':
                return '#ff4444';
            case 'Primary Veterinarian':
                return '#9188E5';
            case 'Poison Control':
                return '#ff9500';
            case 'Pet Sitter':
                return '#4CAF50';
            default:
                return '#666';
        }
    };

    return (
        <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
            <View style={styles.emergencyBanner}>
                <Ionicons name="warning" size={24} color="#fff" />
                <View style={styles.emergencyText}>
                    <Text style={styles.emergencyTitle}>In Case of Emergency</Text>
                    <Text style={styles.emergencySubtitleEn}>
                        Call your nearest emergency vet or poison control immediately
                    </Text>
                </View>
            </View>

            <View style={styles.contactsList}>
                {contacts.map((contact) => (
                    <TouchableOpacity
                        key={contact.id}
                        style={styles.contactCard}
                        onPress={() => handleCall(contact.phone, contact.name)}
                        activeOpacity={0.7}
                    >
                        <View style={styles.contactHeader}>
                            <View style={[styles.iconContainer, { backgroundColor: getTypeColor(contact.type) }]}>
                                <Ionicons 
                                    name={getTypeIcon(contact.type)} 
                                    size={20} 
                                    color="#fff" 
                                />
                            </View>
                            <View style={styles.contactInfo}>
                                <Text style={styles.contactName}>{contact.name}</Text>
                                <Text style={styles.contactType}>{contact.type}</Text>
                            </View>
                            <TouchableOpacity
                                style={styles.callButton}
                                onPress={() => handleCall(contact.phone, contact.name)}
                            >
                                <Ionicons name="call" size={20} color="#9188E5" />
                            </TouchableOpacity>
                        </View>
                        
                        <View style={styles.contactDetails}>
                            <View style={styles.detailRow}>
                                <Ionicons name="call-outline" size={16} color="#666" />
                                <Text style={styles.detailText}>{contact.phone}</Text>
                            </View>
                            <View style={styles.detailRow}>
                                <Ionicons name="location-outline" size={16} color="#666" />
                                <Text style={styles.detailText}>{contact.address}</Text>
                            </View>
                            <View style={styles.detailRow}>
                                <Ionicons name="time-outline" size={16} color="#666" />
                                <Text style={styles.detailText}>{contact.available}</Text>
                            </View>
                        </View>
                    </TouchableOpacity>
                ))}
            </View>

            <View style={styles.footer}>
                <Text style={styles.footerTextEn}>
                    💡 Tip: Save these numbers in your phone's favorites for quick access
                </Text>
            </View>
        </ScrollView>
    )
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    emergencyBanner: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#ff4444',
        margin: 16,
        padding: 16,
        borderRadius: 12,
    },
    emergencyText: {
        marginLeft: 12,
        flex: 1,
    },
    emergencyTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: '#fff',
        marginBottom: 4,
    },
    emergencySubtitleEn: {
        fontSize: 12,
        color: '#fff',
        opacity: 0.8,
    },
    contactsList: {
        paddingHorizontal: 16,
    },
    contactCard: {
        backgroundColor: '#fff',
        borderRadius: 12,
        padding: 16,
        marginBottom: 12,
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
    contactHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 12,
    },
    iconContainer: {
        width: 40,
        height: 40,
        borderRadius: 20,
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 12,
    },
    contactInfo: {
        flex: 1,
    },
    contactName: {
        fontSize: 16,
        fontWeight: '600',
        color: '#333',
        marginBottom: 2,
    },
    contactType: {
        fontSize: 14,
        color: '#666',
    },
    callButton: {
        width: 40,
        height: 40,
        borderRadius: 20,
        backgroundColor: '#f0f0f0',
        justifyContent: 'center',
        alignItems: 'center',
    },
    contactDetails: {
        paddingLeft: 52,
    },
    detailRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 6,
    },
    detailText: {
        fontSize: 14,
        color: '#666',
        marginLeft: 8,
    },
    footer: {
        padding: 20,
        alignItems: 'center',
    },
    footerTextEn: {
        fontSize: 12,
        color: '#bbb',
        textAlign: 'center',
        fontStyle: 'italic',
    },
})