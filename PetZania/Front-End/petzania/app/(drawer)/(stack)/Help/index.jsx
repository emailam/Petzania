import { StyleSheet, Text, View, ScrollView } from 'react-native'
import React from 'react'
import { Ionicons } from '@expo/vector-icons'

export default function HelpScreen() {
  return (
    <ScrollView style={styles.container}>

      <View style={styles.content}>
        <View style={styles.messageCard}>
          <View style={styles.iconContainer}>
            <Ionicons name="mail-outline" size={32} color="#918CE5" />
          </View>
          
          <Text style={styles.mainMessage}>
            Having trouble with the app?
          </Text>
          
          <Text style={styles.description}>
            If you're experiencing any issues or have questions about PetZania, 
            please don't hesitate to reach out to our development team. 
            We're always happy to help!
          </Text>

          <View style={styles.contactInfo}>
            <Text style={styles.contactLabel}>Send us an email:</Text>
            <Text style={styles.emailAddress}>petzaniasystem@gmail.com</Text>
          </View>
        </View>
      </View>
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  header: {
    padding: 24,
    backgroundColor: '#F8F9FA',
    borderBottomWidth: 1,
    borderBottomColor: '#E5E5EA',
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: '700',
    color: '#000000',
    marginBottom: 4,
  },
  headerSubtitle: {
    fontSize: 16,
    color: '#666666',
  },
  content: {
    padding: 20,
    flex: 1,
    justifyContent: 'center',
  },
  messageCard: {
    backgroundColor: '#FFFFFF',
    padding: 24,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#E5E5EA',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  iconContainer: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: '#F0F0F7',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 20,
  },
  mainMessage: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#000000',
    textAlign: 'center',
    marginBottom: 12,
  },
  description: {
    fontSize: 16,
    color: '#666666',
    textAlign: 'center',
    lineHeight: 22,
    marginBottom: 24,
  },
  contactInfo: {
    alignItems: 'center',
    marginBottom: 16,
  },
  contactLabel: {
    fontSize: 14,
    color: '#666666',
    marginBottom: 4,
  },
  emailAddress: {
    fontSize: 16,
    fontWeight: '600',
    color: '#918CE5',
  }
})