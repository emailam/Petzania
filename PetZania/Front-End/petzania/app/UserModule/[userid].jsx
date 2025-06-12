import React, { useState, useEffect, useContext } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Image,
  ScrollView,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons, MaterialIcons } from '@expo/vector-icons';
import { UserContext } from '@/context/UserContext';
import { getUserById } from '@/services/userService';

export default function UserProfile() {
    const { userid } = useLocalSearchParams();
    const router = useRouter();
    const { user: currentUser } = useContext(UserContext);

    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isFollowing, setIsFollowing] = useState(false);

    useEffect(() => {
        fetchUserProfile();
    }, [userid]);

    const fetchUserProfile = async () => {
        try {
            setLoading(true);
            const userData = await getUserById(userid);
            setUser(userData);
        }
        catch (error) {
            console.error('Error fetching user profile:', error);
            Alert.alert('Error', 'Failed to load user profile');
        }
        finally {
            setLoading(false);
        }
    };

  const handleFollowToggle = async () => {
    try {
      setIsFollowing(!isFollowing);
      Alert.alert(
        'Success',
        isFollowing ? 'Unfollowed user' : 'Following user'
      );
    } catch (error) {
      console.error('Error toggling follow:', error);
      Alert.alert('Error', 'Failed to update follow status');
    }
  };

    const handleSendMessage = () => {
        router.push(`/(drawer)/(stack)/Chat/${userid}`);
    };

    const isOwnProfile = currentUser?.userId === userid;

    if (loading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#9188E5" />
                <Text style={styles.loadingText}>Loading profile...</Text>
            </View>
        );
    }

  if (!user) {
    return (
      <View style={styles.errorContainer}>
        <Ionicons name="person-remove" size={80} color="#ccc" />
        <Text style={styles.errorText}>User not found</Text>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => router.back()}
        >
          <Text style={styles.backButtonText}>Go Back</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      {/* Profile Header */}
      <View style={styles.headerContainer}>
        <View style={styles.profileImageContainer}>
          {user.profilePictureURL ? (
            <Image
              source={{ uri: user.profilePictureURL }}
              style={styles.profileImage}
            />
          ) : (
            <View style={styles.defaultProfileImage}>
              <Ionicons name="person" size={60} color="#9188E5" />
            </View>
          )}
        </View>        <View style={styles.userInfoContainer}>
          <Text style={styles.name}>{user.name || 'Unknown User'}</Text>
          <Text style={styles.username}>@{user.username}</Text>
          {user.email && (
            <Text style={styles.email}>{user.email}</Text>
          )}
        </View>

        {isOwnProfile && (
          <View style={styles.ownProfileButtonsContainer}>
            <TouchableOpacity
              style={styles.editProfileButton}
              onPress={() => router.push('/UserModule/EditProfile')}
            >
              <Ionicons name="create-outline" size={20} color="#9188E5" />
              <Text style={styles.editProfileButtonText}>Edit Profile</Text>
            </TouchableOpacity>
          </View>
        )}

        {!isOwnProfile && (
          <View style={styles.actionButtonsContainer}>
            <TouchableOpacity
              style={[
                styles.actionButton,
                isFollowing ? styles.followingButton : styles.followButton,
              ]}
              onPress={handleFollowToggle}
            >
              <Ionicons
                name={isFollowing ? 'person-remove' : 'person-add'}
                size={20}
                color={isFollowing ? '#666' : '#fff'}
              />
              <Text
                style={[
                  styles.actionButtonText,
                  isFollowing ? styles.followingButtonText : styles.followButtonText,
                ]}
              >
                {isFollowing ? 'Following' : 'Follow'}
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.messageButton}
              onPress={handleSendMessage}
            >
              <Ionicons name="chatbubble" size={20} color="#9188E5" />
              <Text style={styles.messageButtonText}>Message</Text>
            </TouchableOpacity>
          </View>
        )}
      </View>

      {/* User Stats */}
        <View style={styles.statsContainer}>
            <View style={styles.statItem}>
                <Text style={styles.statNumber}>{user.myPets?.length || 0}</Text>
                <Text style={styles.statLabel}>Pets</Text>
            </View>
            <View style={styles.statItem}>
                <Text style={styles.statNumber}>{user.followersCount || 0}</Text>
                <Text style={styles.statLabel}>Followers</Text>
            </View>
            <View style={styles.statItem}>
                <Text style={styles.statNumber}>{user.followingCount || 0}</Text>
                <Text style={styles.statLabel}>Following</Text>
            </View>
        </View>

      {/* Bio Section */}
    <View style={styles.bioContainer}>
        <Text style={styles.sectionTitle}>About</Text>
        <Text style={styles.bioText}>{user.bio}</Text>
    </View>

      {/* Pets Section */}
      <View style={styles.petsSection}>
        <Text style={styles.sectionTitle}>Pets</Text>
        {user.myPets && user.myPets.length > 0 ? (
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            {user.myPets.map((pet) => (
              <TouchableOpacity
                key={pet.petId}
                style={styles.petCard}
                onPress={() => router.push(`/PetModule/${pet.petId}`)}
              >
                {pet.myPicturesURLs && pet.myPicturesURLs.length > 0 ? (
                  <Image
                    source={{ uri: pet.myPicturesURLs[0] }}
                    style={styles.petImage}
                  />
                ) : (
                  <View style={styles.defaultPetImage}>
                    <MaterialIcons name="pets" size={30} color="#9188E5" />
                  </View>
                )}
                <Text style={styles.petName} numberOfLines={1}>
                  {pet.name}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        ) : (
          <View style={styles.noPetsContainer}>
            <MaterialIcons name="pets" size={40} color="#ccc" />
            <Text style={styles.noPetsText}>No pets yet</Text>
          </View>
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#9188E5',
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
    paddingHorizontal: 40,
  },
  errorText: {
    marginTop: 16,
    fontSize: 18,
    color: '#666',
    textAlign: 'center',
  },
  backButton: {
    marginTop: 20,
    paddingVertical: 12,
    paddingHorizontal: 24,
    backgroundColor: '#9188E5',
    borderRadius: 8,
  },
  backButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  headerContainer: {
    padding: 20,
    backgroundColor: '#f8f9fa',
  },
  profileImageContainer: {
    alignItems: 'center',
    marginBottom: 16,
  },
  profileImage: {
    width: 120,
    height: 120,
    borderRadius: 60,
    borderWidth: 2,
    borderColor: '#9188E5',
  },
  defaultProfileImage: {
    width: 120,
    height: 120,
    borderRadius: 60,
    backgroundColor: '#f0f0f0',
    borderWidth: 2,
    borderColor: '#9188E5',
    alignItems: 'center',
    justifyContent: 'center',
  },
  userInfoContainer: {
    alignItems: 'center',
    marginBottom: 20,
  },
  name: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  username: {
    fontSize: 16,
    color: '#9188E5',
    marginBottom: 4,
  },
  email: {
    fontSize: 14,
    color: '#666',
  },
  actionButtonsContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 12,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 8,
    gap: 8,
  },
  followButton: {
    backgroundColor: '#9188E5',
  },
  followingButton: {
    backgroundColor: '#f0f0f0',
    borderWidth: 1,
    borderColor: '#ddd',
  },
  actionButtonText: {
    fontSize: 16,
    fontWeight: '600',
  },
  followButtonText: {
    color: '#fff',
  },
  followingButtonText: {
    color: '#666',
  },
  messageButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 8,
    backgroundColor: '#f0f0f0',
    borderWidth: 1,
    borderColor: '#9188E5',
    gap: 8,
  },  messageButtonText: {
    color: '#9188E5',
    fontSize: 16,
    fontWeight: '600',
  },
  ownProfileButtonsContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 12,
  },
  editProfileButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 8,
    backgroundColor: '#f0f0f0',
    borderWidth: 1,
    borderColor: '#9188E5',
    gap: 8,
  },
  editProfileButtonText: {
    color: '#9188E5',
    fontSize: 16,
    fontWeight: '600',
  },
  statsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingVertical: 20,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  statItem: {
    alignItems: 'center',
  },
  statNumber: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  statLabel: {
    fontSize: 14,
    color: '#666',
    marginTop: 4,
  },
  bioContainer: {
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  bioText: {
    fontSize: 16,
    color: '#666',
    lineHeight: 24,
  },
  petsSection: {
    padding: 20,
  },
  petCard: {
    alignItems: 'center',
    marginRight: 16,
    width: 80,
  },
  petImage: {
    width: 70,
    height: 70,
    borderRadius: 35,
    borderWidth: 2,
    borderColor: '#9188E5',
    marginBottom: 8,
  },
  defaultPetImage: {
    width: 70,
    height: 70,
    borderRadius: 35,
    backgroundColor: '#f0f0f0',
    borderWidth: 2,
    borderColor: '#9188E5',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  petName: {
    fontSize: 12,
    color: '#333',
    textAlign: 'center',
  },
  noPetsContainer: {
    alignItems: 'center',
    paddingVertical: 20,
  },
  noPetsText: {
    marginTop: 8,
    fontSize: 16,
    color: '#999',
  },
});