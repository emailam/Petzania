import React, { useContext } from 'react';
import { View, StyleSheet, Text, Image, TouchableOpacity } from 'react-native';

import { DrawerContentScrollView, DrawerItem } from '@react-navigation/drawer';
import { FontAwesome, Ionicons, MaterialIcons, Feather } from '@expo/vector-icons';
import { useRouter, usePathname } from 'expo-router';

import { UserContext } from '@/context/UserContext';
import { PetContext } from '@/context/PetContext';
import { FlowContext } from '@/context/FlowContext';

import { logout } from '@/services/userService'
import Toast from 'react-native-toast-message';

export default function CustomDrawer(props) {
  const router = useRouter();
  const { user, setUser } = useContext(UserContext);
  const { pets, setPets } = useContext(PetContext);
  const { fromPage, setFromPage } = useContext(FlowContext);

  const pathname = usePathname();
  const isActive = (path) => pathname === path;

  const goToPetModule = () => {
    setFromPage('Home');
    router.push('/PetModule/AddPet1')
  }

  const handleLogout = async () => {
    try {
      await logout(user?.email);
      Toast.show({
        type: 'success',
        text1: 'Logged out successfully',
        position: 'top',
        visibilityTime: 2000,
      });
      if(router.canDismiss()) router.dismissAll();
      router.replace('/RegisterModule/LoginScreen');
      setUser(null);
      setPets(null);
    } catch (error) {
      console.error('Logout error:', error);
    }
  }

  return (
    <DrawerContentScrollView {...props} keyboardShouldPersistTaps="always" scrollToOverflowEnabled={true} contentContainerStyle={{ flexGrow: 1 }}>
      <View style={{ flex: 1 }}>
        {/* Top Section: User Info, Pets, Drawer Items */}
        <View style={{ flexGrow: 1, paddingBottom: 16 }}>
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

          <View style={styles.divider} />

          {/* Pets Section */}
          <View style={styles.petsSection}>
            <Text style={styles.sectionTitle}>Your Pets</Text>
            <View style={styles.petsCircleContainer}>
              {pets.length > 0 ? (
                pets.map((pet) => (
                  <TouchableOpacity key={pet.petId} style={styles.petCircleItem} onPress={() => router.push(`/PetModule/${pet.petId}`)}>
                    {pet.myPicturesURLs ? (
                      <Image source={{ uri: pet.myPicturesURLs[0] }} style={styles.petCircleImage} />
                    ) : (
                      <View style={styles.petCircleFallback}>
                        <Text style={styles.petCircleInitial}>{pet.name?.charAt(0) || '?'}</Text>
                      </View>
                    )}
                    <Text style={styles.petCircleName} numberOfLines={1}>{pet.name}</Text>
                  </TouchableOpacity>
                ))
              ) : (
                <Text style={styles.noPetsText}>No pets added yet</Text>
              )}
              {/* Add Pet Button */}
              <TouchableOpacity style={styles.petCircleItem} onPress={goToPetModule}>
                <View style={styles.addPetCircle}>
                  <Feather name="plus" size={28} color="#9188E5" />
                </View>
                <Text style={styles.petCircleName}>Add pet</Text>
              </TouchableOpacity>
            </View>
          </View>

          <View style={styles.divider} />

          {/* Drawer Items */}
          <View style={styles.drawerItems}>
            <DrawerItem
              label="Home"
              onPress={() => router.push('/Home')}
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
              onPress={() => router.push('/Friends')}
              // pressColor="rgba(145, 136, 229, 0.4)"
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

        {/* Bottom Drawer Items */}
        <View style={{ paddingBottom: 16 }}>
          <View style={styles.divider} />
          <DrawerItem label="Settings" icon={() => <Ionicons name="settings-outline" size={28} />} onPress = { () => { router.push('/Settings')} } />
          <DrawerItem label="Help & Support" icon={() => <Ionicons name="help-circle-outline" size={28} />} onPress = { ()=>{ router.push('/Help')} } />
          <DrawerItem label="Log out" icon={() => <MaterialIcons name="logout" size={28} />} onPress = { handleLogout } />
        </View>
      </View>
    </DrawerContentScrollView>
  );
}

const styles = StyleSheet.create({
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

  petsCircleContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: 12,
    marginTop: 8,
  },

  petCircleItem: {
    alignItems: 'center',
    marginRight: 8,
    marginBottom: 4,
  },

  petCircleImage: {
    width: 60,
    height: 60,
    borderRadius: 30,
    borderWidth: 1,
    borderColor: '#9188E5',
    marginBottom: 4,
  },

  petCircleFallback: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#e0e0e0',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
    borderWidth: 2,
    borderColor: '#9188E5',
  },

  petCircleInitial: {
    fontSize: 22,
    color: '#9188E5',
    fontWeight: 'bold',
  },

  petCircleName: {
    fontSize: 12,
    color: '#444',
    textAlign: 'center',
  },

  addPetCircle: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: '#f5f5f5',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: '#9188E5',
    marginBottom: 4,
    marginRight: 8,
  },

  addPetText: {
    fontSize: 12,
    color: '#9188E5',
    textAlign: 'center',
    marginTop: 2,
    maxWidth: 60,
  },

  noPetsText: {
    color: '#999',
    fontStyle: 'italic',
  },

  drawerItems: {
    marginTop: 10,
  },

  divider: {
    borderBottomColor: '#E0E0E0',
    borderBottomWidth: StyleSheet.hairlineWidth,
    marginVertical: 12,
  },
})