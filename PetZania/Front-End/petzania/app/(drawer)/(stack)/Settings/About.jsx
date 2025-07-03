import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Platform,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';


export default function About() {
  const InfoCard = ({ icon, title, description, onPress, showArrow = false }) => (
    <TouchableOpacity
      style={styles.infoCard}
      onPress={onPress}
      activeOpacity={onPress ? 0.7 : 1}
    >
      <View style={styles.cardIcon}>
        <Ionicons name={icon} size={24} color="#918CE5" />
      </View>
      <View style={styles.cardContent}>
        <Text style={styles.cardTitle}>{title}</Text>
        <Text style={styles.cardDescription}>{description}</Text>
      </View>
      {showArrow && (
        <Ionicons name="chevron-forward" size={20} color="#999" />
      )}
    </TouchableOpacity>
  );

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>

      {/* Mission Statement */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Our Mission</Text>
        <Text style={styles.missionText}>
          PetZania is dedicated to creating a vibrant community where pet lovers can connect, 
          share experiences, and provide the best care for their furry, feathered, and 
          scaled companions. We believe every pet deserves love, care, and a community 
          that supports their wellbeing.
        </Text>
      </View>

      {/* Features */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Key Features</Text>
        
        <InfoCard
          icon="people-outline"
          title="Pet Social Network"
          description="Connect with fellow pet owners and share your pet's journey"
        />
        
        <InfoCard
          icon="chatbubbles-outline"
          title="Real-time Chat"
          description="Communicate instantly with other pet lovers in your community"
        />
        
        <InfoCard
          icon="heart-outline"
          title="Pet Profiles"
          description="Create detailed profiles for all your beloved pets"
        />
        
        <InfoCard
          icon="medical-outline"
          title="Health Tracking"
          description="Keep track of vaccinations, vet visits, and health records"
        />
        
        <InfoCard
          icon="location-outline"
          title="Local Community"
          description="Find pet services and connect with nearby pet owners"
        />
      </View>

      {/* Development Team */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Development Team</Text>
        <View style={styles.teamContainer}>
          <Text style={styles.teamText}>
            PetZania is lovingly crafted by a team of passionate developers 
            and pet enthusiasts who understand the special bond between pets and their families.
          </Text>
          <Text style={styles.teamCredit}>
            Made with ❤️ for pet lovers everywhere
          </Text>
        </View>
      </View>

      {/* Footer */}
      <View style={styles.footer}>
        <Text style={styles.footerText}>
          © 2025 PetZania. All rights reserved.
        </Text>
        <Text style={styles.footerSubtext}>
          Connecting pets and people, one paw at a time 🐾
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    paddingTop: Platform.OS === 'ios' ? 50 : 12,
    borderBottomWidth: 1,
    borderBottomColor: '#E5E5EA',
    backgroundColor: '#FFFFFF',
  },
  backButton: {
    marginRight: 16,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#000000',
  },
  appSection: {
    alignItems: 'center',
    paddingVertical: 32,
    paddingHorizontal: 16,
    backgroundColor: '#F8F9FA',
  },
  appLogo: {
    width: 80,
    height: 80,
    borderRadius: 16,
    marginBottom: 16,
  },
  appName: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#918CE5',
    marginBottom: 8,
  },
  appTagline: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
    marginBottom: 16,
    lineHeight: 22,
  },
  versionContainer: {
    backgroundColor: '#918CE5',
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 12,
  },
  versionText: {
    fontSize: 12,
    color: '#FFFFFF',
    fontWeight: '600',
  },
  section: {
    paddingHorizontal: 16,
    paddingVertical: 24,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#000000',
    marginBottom: 16,
  },
  missionText: {
    fontSize: 16,
    color: '#444',
    lineHeight: 24,
    textAlign: 'justify',
  },
  infoCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    borderWidth: 1,
    borderColor: '#F0F0F0',
  },
  cardIcon: {
    width: 40,
    height: 40,
    backgroundColor: '#F8F9FA',
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  cardContent: {
    flex: 1,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#000000',
    marginBottom: 4,
  },
  cardDescription: {
    fontSize: 14,
    color: '#666',
    lineHeight: 20,
  },
  teamContainer: {
    backgroundColor: '#F8F9FA',
    borderRadius: 12,
    padding: 20,
  },
  teamText: {
    fontSize: 16,
    color: '#444',
    lineHeight: 24,
    textAlign: 'center',
    marginBottom: 16,
  },
  teamCredit: {
    fontSize: 14,
    color: '#918CE5',
    textAlign: 'center',
    fontWeight: '600',
  },
  footer: {
    alignItems: 'center',
    paddingVertical: 32,
    paddingHorizontal: 16,
    backgroundColor: '#F8F9FA',
    marginTop: 16,
  },
  footerText: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
  },
  footerSubtext: {
    fontSize: 14,
    color: '#918CE5',
    fontWeight: '500',
  },
});
