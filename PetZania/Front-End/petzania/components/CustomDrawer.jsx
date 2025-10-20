import React, { useContext, useState } from 'react';
import { View, StyleSheet, Text, TouchableOpacity, Modal } from 'react-native';
import { Image } from 'expo-image';
import LottieView from 'lottie-react-native';

import { DrawerContentScrollView, DrawerItem } from '@react-navigation/drawer';
import { FontAwesome, Ionicons, MaterialIcons, Feather, FontAwesome5 } from '@expo/vector-icons';
import { useRouter, usePathname } from 'expo-router';

import { UserContext } from '@/context/UserContext';
import { PetContext } from '@/context/PetContext';
import { FlowContext } from '@/context/FlowContext';

import { logout } from '@/services/userService'
import Toast from 'react-native-toast-message';
import { clearAllTokens } from '@/storage/tokenStorage';

export default function CustomDrawer(props) {
  const router = useRouter();
  const { user } = useContext(UserContext);
  const { pets } = useContext(PetContext);
  const { setFromPage } = useContext(FlowContext);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const pathname = usePathname();
  const isActive = (path) => pathname === path;
  const goToPetModule = () => {
    setFromPage('Home');
    router.push('/PetModule/AddPet1');
  };

  const handleLogout = async () => {
    setIsLoggingOut(true);
    
    try {
      // Try to logout from server
      await logout(user?.email);
      
      Toast.show({
        type: 'success',
        text1: 'Logged out successfully',
        position: 'top',
        visibilityTime: 2000,
      });

    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      // Always clear local storage and navigate to login, regardless of server response
      try {
        await clearAllTokens();
      } catch (storageError) {
        console.error('Error clearing storage:', storageError);
      }

      setIsLoggingOut(false);

      if(router.canDismiss()) {
        router.dismissAll();
      }
      router.replace('/RegisterModule/LoginScreen');
    }
  }

  const closeAndNavigate = (path) => {
    props.navigation.closeDrawer();
    router.push({
      pathname: path,
      params: { username: user.username }
    });
  };

  const isHomeActive = isActive('/Home') || isActive('/Notifications') || isActive('/Adoption') || isActive('/Breeding') || isActive('/AddPost');

  return (
    <DrawerContentScrollView {...props} keyboardShouldPersistTaps="handled" scrollToOverflowEnabled={true} contentContainerStyle={{ flexGrow: 1 }}>
      <View style={{ flex: 1 }}>
        {/* Top Section: User Info, Pets, Drawer Items */}
        <View style={{ flexGrow: 1, paddingBottom: 16 }}>
          {/* User Info */}
          <TouchableOpacity
            style={styles.userInfo}
            onPress={() => closeAndNavigate(`/UserModule/${user?.userId}`)}
            activeOpacity={0.7}
          >
            <View style={styles.imageContainer}>
              <Image
                source={(user && user.profilePictureURL) ? { uri: user.profilePictureURL } : require('@/assets/images/Defaults/default-user.png')}
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
              <Text style={styles.name} numberOfLines={2} ellipsizeMode="tail">
                {user?.name || 'Unknown User'}
              </Text>
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
                  color={isHomeActive ? 'white' : 'black'}
                />
              )}
              style={{
                backgroundColor: isHomeActive ? '#9188E5' : 'transparent',
              }}
              labelStyle={{
                color: isHomeActive ? 'white' : 'black',
              }}
            />
            <DrawerItem
              label="Friends"
              onPress={() => closeAndNavigate('/Friends')}
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
          <DrawerItem 
            label="Log out" 
            icon={() => <MaterialIcons name="logout" size={28} />} 
            onPress={handleLogout} 
          />
        </View>

        {/* Logout Loading Modal */}
        <Modal
          visible={isLoggingOut}
          transparent={true}
          animationType="fade"
          statusBarTranslucent={true}
        >
          <View style={styles.logoutModalOverlay}>
            <View style={styles.logoutModalContent}>
              <LottieView
                source={require("@/assets/lottie/loading.json")}
                autoPlay
                loop
                style={styles.logoutLottie}
              />
              <Text style={styles.logoutModalTitle}>Logging out...</Text>
              <Text style={styles.logoutModalSubtitle}>
                Please wait while we securely log you out
              </Text>
            </View>
          </View>
        </Modal>
      </View>
    </DrawerContentScrollView>
  );
}

const styles = StyleSheet.create({
  userInfo: {
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
    color: '#9188E5',
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
    backgroundColor: '#F9FAFB',
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
    backgroundColor: '#F9FAFB',
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

  // Logout Modal Styles
  logoutModalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    justifyContent: 'center',
    alignItems: 'center',
  },

  logoutModalContent: {
    backgroundColor: '#fff',
    borderRadius: 20,
    padding: 30,
    alignItems: 'center',
    minWidth: 280,
    maxWidth: 320,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 10,
    },
    shadowOpacity: 0.25,
    shadowRadius: 10,
    elevation: 10,
  },

  logoutLottie: {
    width: 80,
    height: 80,
    marginBottom: 20,
  },

  logoutModalTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
    textAlign: 'center',
  },

  logoutModalSubtitle: {
    fontSize: 14,
    color: '#666',
    textAlign: 'center',
    lineHeight: 20,
  },
})