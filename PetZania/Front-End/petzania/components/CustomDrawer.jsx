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
  const { user } = useContext(UserContext);
  const { pets } = useContext(PetContext);
  const { setFromPage } = useContext(FlowContext);

  const pathname = usePathname();
  const isActive = (path) => pathname === path;
  const goToPetModule = () => {
    setFromPage('Home');
    router.push('/PetModule/AddPet1');
  };

  const handleLogout = async () => {
    try {
      await logout(user?.email);

      Toast.show({
        type: 'success',
        text1: 'Logged out successfully',
        position: 'top',
        visibilityTime: 2000,
      });

      if(router.canDismiss()) {
        router.dismissAll();
      }
      router.replace('/RegisterModule/LoginScreen');

    } catch (error) {
      console.error('Logout error:', error);
    }
  }

  const closeAndNavigate = (path) => {
    props.navigation.closeDrawer();
    router.push(path);
  };

  return (
    <DrawerContentScrollView {...props} keyboardShouldPersistTaps="handled" scrollToOverflowEnabled={true} contentContainerStyle={{ flexGrow: 1 }}>
      <View style={{ flex: 1 }}>
        {/* Top Section: User Info, Pets, Drawer Items */}
        <View style={{ flexGrow: 1, paddingBottom: 16 }}>          {/* User Info */}
          <TouchableOpacity 
            style={styles.userInfo}
            onPress={() => closeAndNavigate(`/UserModule/${user?.userId}`)}
            activeOpacity={0.7}
          >
            <View style={styles.imageContainer}>
              <Image
                source={(user && user.profilePictureURL) ? { uri: user.profilePictureURL } : require('@/assets/images/AddPet/Pet Default Pic.png')}
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
            <View style={styles.profileArrow}>
              <MaterialIcons name="arrow-forward-ios" size={16} color="#9188E5" />
            </View>
          </TouchableOpacity>
          {/* Pets Section */}
          <View style={styles.divider} />

          <View style={styles.petsSection}>
            <Text style={styles.sectionTitle}>Your Pets</Text>
            <View style={styles.petsCircleContainer}>
              {(
                pets.slice(0, 7).map((pet) => (
                  <TouchableOpacity key={pet.petId} style={styles.petCircleItem} onPress={() => router.push(`/PetModule/${pet.petId}`)}>
                    {(pet.myPicturesURLs && pet.myPicturesURLs.length > 0) ? (
                      <Image source={{ uri: pet.myPicturesURLs[0] }} style={styles.petCircleImage} />
                    ) : (
                      <View style={styles.petCircleFallback}>
                        <Text style={styles.petCircleInitial}>{pet.name?.charAt(0) || '?'}</Text>
                      </View>
                    )}
                    <Text style={styles.petCircleName} numberOfLines={1}>{pet.name}</Text>
                  </TouchableOpacity>
                ))
              )}
              {/* Add Pet Button */}
              <TouchableOpacity style={styles.addPetCircleItem} onPress={goToPetModule}>
                <View style={styles.addPetCircle}>
                  <Feather name="plus" size={28} color="#9188E5" />
                </View>
                <Text style={styles.petCircleName}>Add pet</Text>
              </TouchableOpacity>
            </View>
            {pets.length > 7 && (
              <TouchableOpacity
                style={styles.viewAllPetsButton}
                onPress={() =>{setFromPage('Home'); router.push('/PetModule/AllPets');}}
                activeOpacity={0.8}
              >
                <View style={styles.viewAllPetsContent}>
                  <View style={styles.viewAllPetsIcon}>
                    <MaterialIcons name="pets" size={20} color="#fff" />
                  </View>
                  <View style={styles.viewAllPetsTextContainer}>
                    <Text style={styles.viewAllPetsTitle}>View All Pets</Text>
                    <Text style={styles.viewAllPetsCount}>{pets.length} pets total</Text>
                  </View>
                </View>
                <View style={styles.viewAllPetsArrow}>
                  <MaterialIcons name="arrow-forward-ios" size={18} color="#9188E5" />
                </View>
              </TouchableOpacity>
            )}
          </View>

          <View style={styles.divider} />

          {/* Drawer Items */}
          <View>
            <DrawerItem
              label="Home"
              onPress={() => closeAndNavigate('/Home')}
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
              onPress={() => closeAndNavigate('/Friends')}
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

        <View style={styles.divider} />

        <View>
          <DrawerItem label="Settings" icon={() => <Ionicons name="settings-outline" size={28} />} onPress = { () => { props.navigation.closeDrawer(); router.push('/Settings'); } } />
          <DrawerItem label="Help & Support" icon={() => <Ionicons name="help-circle-outline" size={28} />} onPress = { () => { props.navigation.closeDrawer(); router.push('/Help') } } />
          <DrawerItem label="Log out" icon={() => <MaterialIcons name="logout" size={28} />} onPress = { handleLogout } />
        </View>
      </View>
    </DrawerContentScrollView>
  );
}

const styles = StyleSheet.create({  userInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
  },

  imageContainer: {
    marginRight: 12,
  },

  userInfoText: {
    flexShrink: 1,
    flex: 1,
  },

  profileArrow: {
    marginLeft: 8,
    padding: 4,
  },

  name: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#333',
  },

  username: {
    fontSize: 16,
    color: '#666',
  },

  petsSection: {
    marginBottom: 5,
  },

  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 5,
    color: '#444',
  },

  petsCircleContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: 8,
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
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: '#e0e0e0',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
    borderWidth: 1,
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
    backgroundColor: '#f0f0f0',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: '#9188E5',
    marginBottom: 4,
  },

  addPetCircleItem: {
    alignItems: 'center',
    flexDirection: 'column',
    justifyContent: 'center',
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

  viewAllPetsButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 16,
    paddingHorizontal: 16,
    marginTop: 16,
    backgroundColor: '#fff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#e8e6f3',
    shadowColor: '#9188E5',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },

  viewAllPetsContent: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },

  viewAllPetsIcon: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#9188E5',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },

  viewAllPetsTextContainer: {
    flex: 1,
  },

  viewAllPetsTitle: {
    fontSize: 16,
    color: '#333',
    fontWeight: '600',
    marginBottom: 2,
  },

  viewAllPetsCount: {
    fontSize: 13,
    color: '#9188E5',
    fontWeight: '500',
  },

  viewAllPetsArrow: {
    width: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },

  viewAllPetsText: {
    fontSize: 14,
    color: '#9188E5',
    fontWeight: '600',
  },

  divider: {
    borderBottomColor: '#E0E0E0',
    borderBottomWidth: StyleSheet.hairlineWidth,
    marginVertical: 12,
  },
})