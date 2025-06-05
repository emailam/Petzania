import React, { useContext } from 'react';
import { View, StyleSheet, Text, Image} from 'react-native';

import { DrawerContentScrollView, DrawerItem } from '@react-navigation/drawer';
import { FontAwesome, Ionicons, MaterialIcons } from '@expo/vector-icons';
import { useRouter, usePathname } from 'expo-router';

import { UserContext } from '@/context/UserContext';
import { PetContext } from '@/context/PetContext';
import { logout } from '@/services/userService'
import Toast from 'react-native-toast-message';

export default function CustomDrawer(props) {
  const router = useRouter();
  const { user, setUser } = useContext(UserContext);
  const { pets, setPets } = useContext(PetContext);

  const pathname = usePathname();
  const isActive = (path) => pathname === path;

  const handleLogout = async () => {
    try {
      await logout(user?.email);
      Toast.show({
        type: 'success',
        text1: 'Logged out successfully',
        position: 'top',
        visibilityTime: 2000,
      });
      router.dismissAll();
      router.replace('/RegisterModule/LoginScreen');
      setUser(null);
      setPets(null);
    } catch (error) {
      console.error('Logout error:', error);
    }
  }

  return (
    <DrawerContentScrollView {...props} contentContainerStyle={{ flex: 1 }}>
      <View style={{ flex: 1, justifyContent: 'space-between' }}>
        <View style={styles.container}>
          <View style={styles.topSection}>

            {/* User Info */}
            <View style={styles.userInfo}>
              <View style={styles.imageContainer}>
                <Image
                  source={user?.profilePictureURL? { uri: user.profilePictureURL } : require('@/assets/images/AddPet/Pet Default Pic.png')}
                  style={{
                    width: 70,
                    height: 70,
                    borderRadius: 35,
                    borderWidth: 1,
                    borderColor: '#9188E5',
                  }}
                />
              </View>
              <View style={styles.userInfoText}>
                <Text style={styles.name}>{user?.name || 'Guest'}</Text>
                <Text style={styles.username}>@{user?.username || 'CreamOfSomeYoungGuy69'}</Text>
              </View>
            </View>

            <View
              style={{
                borderBottomColor: '#E0E0E0',
                borderBottomWidth: StyleSheet.hairlineWidth,
                marginVertical: 20,
                shadowOffset: { width: 0, height: 1 },
                shadowOpacity: 0.2,
                shadowRadius: 1.5,
                shadowColor: 'black',
                elevation: 2,
              }}
            />

            {/* Pets Section */}
            <View style={styles.petsSection}>
              <Text style={styles.sectionTitle}>Your Pets</Text>
              <View style={styles.petsContainer}>
                {user?.pets?.length > 0 ? (
                  user.pets.map((pet, index) => (
                    <View key={index} style={styles.petItem}>
                      <Text>{pet.name}</Text>
                    </View>
                  ))
                ) : (
                  <Text style={styles.noPetsText}>No pets added yet</Text>
                )}
              </View>
            </View>

            <View
              style={{
                borderBottomColor: '#E0E0E0',
                borderBottomWidth: StyleSheet.hairlineWidth,
                marginVertical: 20,
                shadowOffset: { width: 0, height: 1 },
                shadowOpacity: 0.2,
                shadowRadius: 1.5,
                shadowColor: 'black',
                elevation: 2,
              }}
            />

            {/* Drawer Items */}
            <View style={styles.drawerItems}>
              <DrawerItem
                label="Home"
                onPress={() => router.push('Home')}
                icon={() => (
                  <FontAwesome
                    name="home"
                    size={28}
                    color={isActive('/Home') ? 'white' : 'black'}
                  />
                )}
                style={{
                  backgroundColor: isActive('/Home') ? '#9188E5' : 'transparent',
                }}
                labelStyle={{
                  color: isActive('/Home') ? 'white' : 'black',
                }}
              />
              <DrawerItem
                label="Friends"
                onPress={() => router.push('Friends')}
                icon={() => (
                  <MaterialIcons
                    name="groups"
                    size={28}
                    color={isActive('/Friends') ? 'white' : 'black'}
                  />
                )}
                style={{
                  backgroundColor: isActive('/Friends') ? '#9188E5' : 'transparent',
                }}
                labelStyle={{
                  color: isActive('/Friends') ? 'white' : 'black',
                }}
              />
            </View>
          </View>
        </View>

        {/* Bottom Drawer Items */}
        <View >
          <DrawerItem label="Settings" icon={() => <Ionicons name="settings-outline" size={28} />} onPress = { () => { router.push('Settings')} } />
          <DrawerItem label="Help & Support" icon={() => <Ionicons name="help-circle-outline" size={28} />} onPress = { ()=>{ router.push('Help')} } />
          <DrawerItem label="Log out" icon={() => <MaterialIcons name="logout" size={28} />} onPress = { handleLogout } />
        </View>
      </View>
    </DrawerContentScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-between',
  },

  topSection: {
    flex: 1,
  },

  userInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 20,
  },

  imageContainer: {
    marginRight: 12,
  },

  userInfoText: {
    flexShrink: 1,
  },

  name: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },

  username: {
    fontSize: 16,
    color: '#666',
  },

  petsSection: {
    marginBottom: 20,
  },

  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
    color: '#444',
  },

  petsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },

  petItem: {
    backgroundColor: '#f0f0f0',
    padding: 8,
    borderRadius: 8,
    marginRight: 8,
    marginBottom: 8,
  },

  noPetsText: {
    color: '#999',
    fontStyle: 'italic',
  },

  drawerItems: {
    marginTop: 10,
  },
})