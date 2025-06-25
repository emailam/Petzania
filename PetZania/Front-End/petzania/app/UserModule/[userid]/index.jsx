import React, { useState, useEffect, useContext } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Alert, ActivityIndicator } from 'react-native';
import { Image } from 'expo-image';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons, MaterialIcons } from '@expo/vector-icons';
import { useActionSheet } from '@expo/react-native-action-sheet';
import ImageViewing from 'react-native-image-viewing';

import { UserContext } from '@/context/UserContext';

import { createChat } from '@/services/chatService';

import { useFriendsData } from '@/hooks/useFriendsData'

import { getUserById } from '@/services/userService';
import {
    sendFriendRequest,
    acceptFriendRequest,
    cancelFriendRequest,
    removeFriend,
    blockUser,
    followUser,
    unfollowUser,
    unblockUser,
    isFriend,
    isFriendRequestExists,
    isBlockingExists,
    isFollowing,
    getReceivedFriendRequests
} from '@/services/friendsService';

import Toast from 'react-native-toast-message';


export default function UserProfile() {
    const { userid } = useLocalSearchParams();
    const router = useRouter();
    const { user: currentUser, } = useContext(UserContext);
    const { showActionSheetWithOptions } = useActionSheet();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [friendshipStatus, setFriendshipStatus] = useState('none');
    const [isBlocked, setIsBlocked] = useState(false);
    const [isFollowingUser, setIsFollowingUser] = useState(false);
    const [activeTab, setActiveTab] = useState('posts');
    const [friendRequestId, setFriendRequestId] = useState(null);
    const [incomingRequestId, setIncomingRequestId] = useState(null); // For received requests
    const [actionLoading, setActionLoading] = useState(false);
    const [showImageViewer, setShowImageViewer] = useState(false);

    // Local state for counts specific to this user profile
    const [profileCounts, setProfileCounts] = useState({
        friendsCount: 0,
        followersCount: 0,
        followingCount: 0
    });

    const { loadAllCounts } = useFriendsData(userid);
    const isOwnProfile = currentUser?.userId === userid;

    useEffect(() => {
        if (isOwnProfile && currentUser) {
            setUser(currentUser);
        }
    }, [currentUser, isOwnProfile]);

    useEffect(() => {
        const initializeProfile = async () => {
            // Only show initial loading screen on first load
            setLoading(true);

            // Load all data concurrently
            const promises = [
                fetchUserProfile(),
                loadUserCounts()
            ];

            if (!isOwnProfile) {
                promises.push(
                    fetchFriendshipStatus(),
                    fetchFollowingStatus()
                );
            }

            try {
                await Promise.all(promises);
            } catch (error) {
                console.error('Error initializing profile:', error);
            } finally {
                setLoading(false);
            }
        };

        initializeProfile();
    }, [userid]);

    const loadUserCounts = async () => {
        try {
            const counts = await loadAllCounts();
            if (counts) {
                setProfileCounts({
                    friendsCount: counts.friendsCount || 0,
                    followersCount: counts.followersCount || 0,
                    followingCount: counts.followingCount || 0
                });
            }
        } catch (error) {
            console.error('Error loading user counts:', error);
            // Set default values on error
            setProfileCounts({
                friendsCount: 0,
                followersCount: 0,
                followingCount: 0
            });
        }
    };

    const fetchUserProfile = async () => {
        try {
            // If it's the user's own profile, use currentUser from context first
            if (isOwnProfile && currentUser) {
                setUser(currentUser);
            } else {
                // For other users, fetch from API
                const userData = await getUserById(userid);
                setUser(userData);
            }
        }
        catch (error) {
            console.error('Error fetching user profile:', error);
            Alert.alert('Error', 'Failed to load user profile');
        }
    };

    const fetchFriendshipStatus = async () => {
        try {
            const blockingStatus = await isBlockingExists(userid);
            if (blockingStatus) {
                setFriendshipStatus('blocked');
                setIsBlocked(true);
                return;
            }

            // Check if they are friends
            const friendStatus = await isFriend(userid);

            if (friendStatus) {
                setFriendshipStatus('friends');
                return;
            }

            // Check if there's a pending friend request sent by current user
            const outgoingRequestId = await isFriendRequestExists(userid);

            if (outgoingRequestId) {
                setFriendshipStatus('pending');
                setFriendRequestId(outgoingRequestId);
                return;
            }

            // Check if there's an incoming friend request from the viewed user
            try {
                const receivedRequests = await getReceivedFriendRequests(0, 50); // Get more requests to ensure we find it
                const incomingRequest = receivedRequests.content?.find(
                    request => request.sender.userId === userid
                );

                if (incomingRequest) {
                    setFriendshipStatus('incoming');
                    setIncomingRequestId(incomingRequest.requestId);
                    return;
                }
            } catch (receivedRequestsError) {
                console.error('Error checking received requests:', receivedRequestsError);
            }

            setFriendshipStatus('none');
        } catch (error) {
            console.error('Error fetching friendship status:', error);
            // Default to 'none' if there's an error
            setFriendshipStatus('none');
        }
    };

    const fetchFollowingStatus = async () => {
        try {
            const followingStatus = await isFollowing(userid);
            setIsFollowingUser(followingStatus);
        } catch (error) {
            console.error('Error fetching following status:', error);
            setIsFollowingUser(false);
        }
    };

    const handleFriendRequest = async () => {
        if (actionLoading) return;
        try {
            setActionLoading(true);
            if (friendshipStatus === 'none') {
                // Send friend request
                const response = await sendFriendRequest(userid);
                setFriendshipStatus('pending');
                setFriendRequestId(response.id);

                Toast.show({
                    type: 'success',
                    text1: 'Friend Request Sent',
                    text2: `Friend request sent to ${user?.name || 'user'}`,
                    position: 'top',
                    visibilityTime: 3000,                });
            } else if (friendshipStatus === 'friends') {
                // Remove friend
                Alert.alert(
                    'Remove Friend',
                    `Are you sure you want to remove ${user?.name || 'this user'} from your friends?`,
                    [
                        { text: 'Cancel', style: 'cancel' },
                        {
                            text: 'Remove',
                            style: 'destructive',
                            onPress: async () => {
                              try {
                                    setActionLoading(true);
                                    await removeFriend(userid);
                                    
                                    // Update local state immediately
                                    setFriendshipStatus('none');
                                    
                                    // Refresh friendship status and counts in background
                                    await Promise.all([
                                        fetchFriendshipStatus(),
                                        loadUserCounts()
                                    ]);

                                    Toast.show({
                                        type: 'info',
                                        text1: 'Friend Removed',
                                        text2: `${user?.name || 'User'} has been removed from your friends`,
                                        position: 'top',
                                        visibilityTime: 3000,
                                    });
                                } catch (error) {
                                    console.error('Error removing friend:', error);
                                    Toast.show({
                                        type: 'error',
                                        text1: 'Error',
                                        text2: 'Failed to remove friend',
                                        position: 'top',
                                        visibilityTime: 3000,
                                    });
                                } finally {
                                    setActionLoading(false);
                                }
                            },
                        },
                    ]
                );
              }
              else if (friendshipStatus === 'pending') {
                try {
                    await cancelFriendRequest(friendRequestId);

                    setFriendshipStatus('none');
                    setFriendRequestId(null);

                    Toast.show({
                        type: 'info',
                        text1: 'Friend Request Cancelled',
                        text2: 'Friend request has been cancelled',
                        position: 'top',
                        visibilityTime: 3000,
                    });
                } catch (error) {
                    console.error('Error cancelling friend request:', error);
                    Toast.show({
                        type: 'error',
                        text1: 'Error',
                        text2: 'Failed to cancel friend request',
                        position: 'top',
                        visibilityTime: 3000,
                    });
                }            }
            // Refresh data in background without showing loading states
            if (!isOwnProfile) {
                await Promise.all([
                    fetchFriendshipStatus(),
                    loadUserCounts()
                ]);
            }
        }
        catch (error) {
            console.error('Error handling friend request:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to process friend request',
                position: 'top',
                visibilityTime: 3000,
            });
        }
        finally {
            setActionLoading(false);
        }
      };

    const handleRespondToRequest = () => {
        const options = ['Accept', 'Decline', 'Cancel'];
        const cancelButtonIndex = 2;
        const destructiveButtonIndex = 1;

        showActionSheetWithOptions(
            {
                options,
                cancelButtonIndex,
                destructiveButtonIndex,
                title: `Respond to ${user?.name || 'User'}'s friend request`,
                message: 'What would you like to do?',
            },
            async (buttonIndex) => {
                switch (buttonIndex) {
                    case 0:
                        // Accept
                        await handleAcceptRequest();
                        break;
                    case 1:
                        // Decline
                        await handleDeclineRequest();
                        break;
                }
            }
        );
    };

    const handleAcceptRequest = async () => {
        if (actionLoading) return;

        try {
            setActionLoading(true);

            await acceptFriendRequest(incomingRequestId);
            setFriendshipStatus('friends');
            setIncomingRequestId(null);

            Toast.show({
                type: 'success',
                text1: 'Friend Request Accepted',
                text2: `You are now friends with ${user?.name || 'user'}`,
                position: 'top',
                visibilityTime: 3000,
            });

            // Refresh data in background
            if (!isOwnProfile) {
                await Promise.all([
                    fetchFriendshipStatus(),
                    loadUserCounts()
                ]);
            }
        } catch (error) {
            console.error('Error accepting friend request:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to accept friend request',
                position: 'top',
                visibilityTime: 3000,
            });
        } finally {
            setActionLoading(false);
        }
    };

    const handleDeclineRequest = async () => {
        if (actionLoading) return;
        
        try {
            setActionLoading(true);
            
            // Use cancelFriendRequest with the incoming request ID
            await cancelFriendRequest(incomingRequestId);
            
            // Update local state
            setFriendshipStatus('none');
            setIncomingRequestId(null);
            
            Toast.show({
                type: 'info',
                text1: 'Friend Request Declined',
                text2: 'Friend request has been declined',
                position: 'top',
                visibilityTime: 3000,
            });
            
            // Refresh data in background
            if (!isOwnProfile) {
                await Promise.all([
                    fetchFriendshipStatus(),
                    loadUserCounts()
                ]);
            }
        } catch (error) {
            console.error('Error declining friend request:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to decline friend request',
                position: 'top',
                visibilityTime: 3000,
            });
        } finally {
            setActionLoading(false);
        }
    };

    const handleSendMessage = async () => {
        if (isBlocked) {
            Toast.show({
                type: 'error',
                text1: 'Cannot Send Message',
                text2: 'You cannot message this user',
                position: 'top',
                visibilityTime: 3000,
            });
            return;
        }

        try {
            const chat = await createChat(userid);
            router.push(`/Chat/${chat.chatId}`);
        } catch (error) {
            console.error('Error creating chat:', error);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to start conversation',
                position: 'top',
                visibilityTime: 3000,
            });
        }
    };

    const handleMoreOptions = () => {
        // Build options array based on block status
        const options = [];

        // Only show follow/unfollow if user is not blocked
        if (!isBlocked) {
            options.push(isFollowingUser ? 'Unfollow User' : 'Follow User');
        }

        // Show Block/Unblock based on current status
        options.push(isBlocked ? 'Unblock User' : 'Block User');
        options.push('Cancel');

        const cancelButtonIndex = options.length - 1;
        const destructiveButtonIndex = options.length - 2;

        showActionSheetWithOptions(
            {
              options,
              cancelButtonIndex,
              destructiveButtonIndex,
              title: 'More Options',
            },
            (buttonIndex) => {
                if (isBlocked) {
                    // When user is blocked, only block/unblock option is available
                    switch (buttonIndex) {
                        case 0:
                            handleUnblockUser();
                            break;
                    }
                } else {
                    // When user is not blocked, both follow and block options are available
                    switch (buttonIndex) {
                        case 0:
                            handleFollowUser();
                            break;
                        case 1:
                            handleBlockUser();
                            break;
                    }
                }
            }
        );
    };

    const handleFollowUser = async () => {
        try {
            if (isFollowingUser) {
                await unfollowUser(userid);
                setIsFollowingUser(false);
                Toast.show({
                    type: 'info',
                    text1: 'Unfollowed',
                    text2: `You unfollowed ${user?.name || 'this user'}`,
                    position: 'top',
                    visibilityTime: 3000,
                });
            } else {
                await followUser(userid);
                setIsFollowingUser(true);
                Toast.show({
                    type: 'success',
                    text1: 'Following',
                    text2: `You are now following ${user?.name || 'this user'}`,
                    position: 'top',
                    visibilityTime: 3000,
                });
            }

            // Refresh following status and counts in background
            await Promise.all([
                fetchFollowingStatus(),
                loadUserCounts()
            ]);
        } catch (error) {
            console.error('Error updating follow status:', error);
            // Revert the optimistic update on error
            setIsFollowingUser(!isFollowingUser);
            Toast.show({
                type: 'error',
                text1: 'Error',
                text2: 'Failed to update follow status',
                position: 'top',
                visibilityTime: 3000,
            });
        }
    };
    const handleBlockUser = () => {
        Alert.alert(
            'Block User',
            `Are you sure you want to block ${user?.name || 'this user'}? They won't be able to message you or see your posts.`,
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Block',
                    style: 'destructive',
                    onPress: async () => {
                      try {
                            await blockUser(userid);
                            setIsBlocked(true);
                            setFriendshipStatus('blocked');
                            
                            // Refresh friendship status and counts in background
                            await Promise.all([
                                fetchFriendshipStatus(),
                                loadUserCounts()
                            ]);
                            
                            Toast.show({
                                type: 'success',
                                text1: 'User Blocked',
                                text2: `${user?.name || 'User'} has been blocked`,
                                position: 'top',
                                visibilityTime: 3000,
                            });
                        } catch (error) {
                            console.error('Error blocking user:', error);
                            Toast.show({
                                type: 'error',
                                text1: 'Error',
                                text2: 'Failed to block user',
                                position: 'top',
                                visibilityTime: 3000,
                            });
                        }
                    },
                },
      ]);
    };
    const handleUnblockUser = () => {
        Alert.alert(
            'Unblock User',
            `Are you sure you want to unblock ${user?.name || 'this user'}? They will be able to message you and see your posts again.`,
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Unblock',
                    style: 'default',
                    onPress: async () => {
                        try {
                            await unblockUser(userid);
                            setIsBlocked(false);
                            setFriendshipStatus('none');
                            
                            // Refresh friendship status and counts in background
                            await Promise.all([
                                fetchFriendshipStatus(),
                                loadUserCounts()
                            ]);
                            
                            Toast.show({
                                type: 'success',
                                text1: 'User Unblocked',
                                text2: `${user?.name || 'User'} has been unblocked`,
                                position: 'top',
                                visibilityTime: 3000,
                            });
                        } catch (error) {
                            console.error('Error unblocking user:', error);
                            Toast.show({
                                type: 'error',
                                text1: 'Error',
                                text2: 'Failed to unblock user',
                                position: 'top',
                                visibilityTime: 3000,
                            });
                        }
                    },
                },
            ]
        );
    };

    const handleImagePress = () => {
        if (!user?.profilePictureURL) return;
        setShowImageViewer(true);
    };

    const getFriendButtonConfig = () => {
        switch (friendshipStatus) {
            case 'none':
                return {
                    icon: 'person-add',
                    text: 'Add Friend',
                    style: styles.addFriendButton,
                    textStyle: styles.addFriendButtonText,
                };
            case 'pending':
                return {
                    icon: 'hourglass',
                    text: 'Pending',
                    style: styles.pendingButton,
                    textStyle: styles.pendingButtonText,
                };
            case 'incoming':
                return {
                    icon: 'person-add',
                    text: 'Accept',
                    style: styles.acceptButton,
                    textStyle: styles.acceptButtonText,
                };
            case 'friends':
                return {
                    icon: 'checkmark-circle',
                    text: 'Friends',
                    style: styles.friendsButton,
                    textStyle: styles.friendsButtonText,
                };
            case 'blocked':
                return {
                    icon: 'ban',
                    text: 'Blocked',
                    style: styles.blockedButton,
                    textStyle: styles.blockedButtonText,
                };
            default:
                return {
                    icon: 'person-add',
                    text: 'Add Friend',
                    style: styles.addFriendButton,
                    textStyle: styles.addFriendButtonText,
                };
        }
    };

    if (loading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#9188E5" />
                <Text style={styles.loadingText}>Loading profile...</Text>
                <Text style={styles.loadingSubText}>
                    Getting user information and status
                </Text>
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
      <View style={styles.headerContainer}>
        <View style={{ flexDirection: 'row', alignItems: 'center'}}>
          <TouchableOpacity
            style={styles.profileImageContainer}
            onPress={handleImagePress}
            activeOpacity={user?.profilePictureURL ? 0.7 : 1}
          >
            {user?.profilePictureURL ? (
              <Image
                source={{ uri: user.profilePictureURL }}
                style={styles.profileImage}
              />
            ) : (
              <View style={styles.defaultProfileImage}>
                <Ionicons name="person" size={60} color="#9188E5" />
              </View>
            )}
          </TouchableOpacity>

          <View style={styles.userInfoContainer}>
            <Text style={styles.name}>{user?.name || 'Unknown User'}</Text>
          </View>
        </View>

        {isOwnProfile && (
          <View style={styles.ownProfileButtonsContainer}>
            <TouchableOpacity
              style={styles.editProfileButton}
              onPress={() => router.push('/UserModule/EditProfile')}
            >
              <Ionicons name="create-outline" size={20} color="#fff" />
              <Text style={styles.editProfileButtonText}>Edit Profile</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.settingsButton}
              onPress={() => router.push('/Settings')}
            >
              <Ionicons name="settings-outline" size={20} color="#9188E5" />
              <Text style={styles.settingsButtonText}>Settings</Text>
            </TouchableOpacity>
          </View>
        )}

        {!isOwnProfile && (
          <View style={styles.actionButtonsContainer}>
            {friendshipStatus === 'incoming' ? (
              // Show Respond button for incoming requests
              <TouchableOpacity
                style={[styles.actionButton, styles.respondButton, actionLoading && styles.disabledButton]}
                onPress={handleRespondToRequest}
                disabled={actionLoading}
              >
                {actionLoading ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Ionicons name="chatbubble-ellipses" size={20} color="#fff" />
                )}
                <Text style={[styles.actionButtonText, styles.respondButtonText]}>
                  {actionLoading ? 'Loading...' : 'Respond'}
                </Text>
              </TouchableOpacity>
            ) : (
              // Show single friend button for other states
              <TouchableOpacity
                style={[
                  styles.actionButton,
                  getFriendButtonConfig().style,
                  (actionLoading || friendshipStatus === 'blocked') && styles.disabledButton
                ]}
                onPress={handleFriendRequest}
                disabled={actionLoading || friendshipStatus === 'blocked'}
              >
                {actionLoading ? (
                  <ActivityIndicator size="small" color={getFriendButtonConfig().textStyle.color} />
                ) : (
                  <Ionicons
                    name={getFriendButtonConfig().icon}
                    size={20}
                    color={getFriendButtonConfig().textStyle.color}
                  />
                )}
                <Text style={[styles.actionButtonText, getFriendButtonConfig().textStyle]}>
                  {actionLoading ? 'Loading...' : getFriendButtonConfig().text}
                </Text>
              </TouchableOpacity>
            )}

            {/* Message Button */}
            <TouchableOpacity
              style={[
                styles.actionButton,
                styles.messageButton,
                isBlocked && styles.disabledButton
              ]}
              onPress={handleSendMessage}
              disabled={isBlocked}
            >
              <Ionicons 
                name="chatbubble" 
                size={20} 
                color={isBlocked ? '#ccc' : '#9188E5'} 
              />
              <Text style={[
                styles.messageButtonText,
                isBlocked && styles.disabledButtonText
              ]}>
                Message
              </Text>
            </TouchableOpacity>

            {/* More Options Button */}
            <TouchableOpacity
              style={[styles.actionButton, styles.moreButton]}
              onPress={handleMoreOptions}
            >
              <Ionicons name="ellipsis-horizontal" size={20} color="#666" />
            </TouchableOpacity>
          </View>
        )}
      </View>

      {/* User Stats */}
        <View style={styles.statsContainer}>
          <TouchableOpacity
            style={styles.statItem}
            onPress={() => router.push({
                pathname: `/UserModule/${userid}/Friends`,
                params: { username: user.username }
            })}
          >
            <Text style={styles.statNumber}>{profileCounts.friendsCount || 0}</Text>
            <Text style={styles.statLabel}>Friends</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.statItem}
            onPress={() => router.push({
                pathname: `/UserModule/${userid}/Followers`,
                params: { username: user.username }
            })}
          >
            <Text style={styles.statNumber}>{profileCounts.followersCount || 0}</Text>
            <Text style={styles.statLabel}>Followers</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.statItem}
            onPress={() => router.push({
                pathname: `/UserModule/${userid}/Following`,
                params: { username: user.username }
            })}
          >
            <Text style={styles.statNumber}>{profileCounts.followingCount || 0}</Text>
            <Text style={styles.statLabel}>Following</Text>
          </TouchableOpacity>
          
        </View>
        {/* Bio Section */}
        <View style={styles.bioContainer}>
        <Text style={styles.sectionTitle}>About</Text>
        <Text style={styles.bioText}>{user?.bio || 'No bio available'}</Text>
      </View>

      {/* Tab Navigation */}
      <View style={styles.tabContainer}>
        <TouchableOpacity
          style={[styles.tabButton, activeTab === 'posts' && styles.activeTabButton]}
          onPress={() => setActiveTab('posts')}
        >
          <Ionicons
            name="grid"
            size={20}
            color={activeTab === 'posts' ? '#9188E5' : '#666'}
          />
          <Text style={[styles.tabText, activeTab === 'posts' && styles.activeTabText]}>
            Posts
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.tabButton, activeTab === 'pets' && styles.activeTabButton]}
          onPress={() => setActiveTab('pets')}
        >
          <MaterialIcons
            name="pets"
            size={20}
            color={activeTab === 'pets' ? '#9188E5' : '#666'}
          />
          <Text style={[styles.tabText, activeTab === 'pets' && styles.activeTabText]}>
            Pets
          </Text>
        </TouchableOpacity>
      </View>

      {/* Tab Content */}
      {activeTab === 'posts' && (
        <View style={styles.tabContent}>
            {/* No posts message */}
            {false && (
              <View style={styles.noContentContainer}>
                <Ionicons name="grid" size={40} color="#ccc" />
                <Text style={styles.noContentText}>No posts yet</Text>
                <Text style={styles.noContentSubText}>
                  {isOwnProfile ? "Share your first post!" : `${user?.name || 'User'} hasn't posted yet`}
                </Text>
              </View>
            )}
          </View>
      )}

      {activeTab === 'pets' && (
        <View style={styles.tabContent}>
          {/* Pets Section */}
          <View style={styles.petsSection}>
            {user?.myPets && user?.myPets.length > 0 ? (
              <View style={styles.petsGrid}>
                {user?.myPets.map((pet) => (
                  <TouchableOpacity
                    key={pet.petId}
                    style={styles.petGridCard}
                    onPress={() => { router.push(`/PetModule/${pet.petId}`)}}
                  >
                    {pet.myPicturesURLs && pet.myPicturesURLs.length > 0 ? (
                      <Image
                        source={{ uri: pet.myPicturesURLs[0] }}
                        style={styles.petGridImage}
                      />
                    ) : (
                      <View style={styles.defaultPetGridImage}>
                        <MaterialIcons name="pets" size={30} color="#9188E5" />
                      </View>
                    )}
                    <View style={styles.petGridInfo}>
                      <Text style={styles.petGridName} numberOfLines={1}>
                        {pet.name}
                      </Text>
                      <Text style={styles.petGridType} numberOfLines={1}>
                        {pet.type || 'Pet'}
                      </Text>
                    </View>
                  </TouchableOpacity>
                ))}
              </View>
            ) : (
              <View style={styles.noContentContainer}>
                <MaterialIcons name="pets" size={40} color="#ccc" />
                <Text style={styles.noContentText}>No pets yet</Text>
                <Text style={styles.noContentSubText}>
                  {isOwnProfile ? "Add your first pet!" : `${user?.name || 'User'} hasn't added any pets yet`}
                </Text>
              </View>
            )}
            </View>
        </View>
      )}

      <ImageViewing
        images={[{ uri: user?.profilePictureURL || '' }]}
        imageIndex={0}
        visible={showImageViewer}
        onRequestClose={() => setShowImageViewer(false)}
        backgroundColor="black"
        swipeToCloseEnabled
        doubleTapToZoomEnabled
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
    paddingHorizontal: 20,
  },
  loadingText: {
    marginTop: 16,
    fontSize: 18,
    color: '#9188E5',
    fontWeight: '600',
  },
  loadingSubText: {
    marginTop: 8,
    fontSize: 14,
    color: '#666',
    textAlign: 'center',
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
    marginBottom: 16,
  },
  profileImage: {
    width: 100,
    height: 100,
    borderRadius: 50,
    borderWidth: 2,
    borderColor: '#9188E5',
  },
  defaultProfileImage: {
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: '#f0f0f0',
    borderWidth: 2,
    borderColor: '#9188E5',
    justifyContent: 'center',
  },
  userInfoContainer: {
    marginLeft: 16,
  },
  name: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  actionButtonsContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 8,
  },
  actionButton: {
    flexDirection: 'row',
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 8,
    gap: 6,
    flex: 1,
    justifyContent: 'center',
  },
  // Friend Button States
  addFriendButton: {
    backgroundColor: '#9188E5',
  },
  addFriendButtonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
  },
  pendingButton: {
    backgroundColor: '#f0f0f0',
    borderWidth: 1,
    borderColor: '#ddd',
  },
  pendingButtonText: {
    color: '#666',
    fontSize: 15,
    fontWeight: '600',
  },
  friendsButton: {
    backgroundColor: '#4CAF50',
    borderWidth: 1,
    borderColor: '#4CAF50',
  },
  friendsButtonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
  },
  blockedButton: {
    backgroundColor: '#ffebee',
    borderWidth: 1,
    borderColor: '#f44336',
  },
  blockedButtonText: {
    color: '#f44336',
    fontSize: 15,
    fontWeight: '600',
  },
  // Respond Button
  respondButton: {
    backgroundColor: '#9188E5',
    borderWidth: 1,
    borderColor: '#9188E5',
  },
  respondButtonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
  },
  // Accept Button
  acceptButton: {
    backgroundColor: '#4CAF50',
    borderWidth: 1,
    borderColor: '#4CAF50',
  },
  acceptButtonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
  },
  // Decline Button
  declineButton: {
    backgroundColor: '#f0f0f0',
    borderWidth: 1,
    borderColor: '#f44336',
  },
  declineButtonText: {
    color: '#f44336',
    fontSize: 15,
    fontWeight: '600',
  },
  disabledButton: {
    opacity: 0.5,
  },
  // Message Button
  messageButton: {
    backgroundColor: '#f0f0f0',
    borderWidth: 1,
    borderColor: '#9188E5',
  },
  messageButtonText: {
    color: '#9188E5',
    fontSize: 15,
    fontWeight: '600',
  },
  // More Options Button
  moreButton: {
    backgroundColor: '#f0f0f0',
    borderWidth: 1,
    borderColor: '#ddd',
    flex: 0,
    paddingHorizontal: 12,
  },
  // Disabled states
  disabledButton: {
    backgroundColor: '#f5f5f5',
    borderColor: '#e0e0e0',
  },
  disabledButtonText: {
    color: '#ccc',
  },
  actionButtonText: {
    fontSize: 15,
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
    backgroundColor: '#9188E5',
    borderWidth: 1,
    flex: 1,
    borderColor: '#9188E5',
    gap: 8,
  },
  editProfileButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  settingsButtonText: {
    color: '#9188E5',
    fontSize: 16,
    fontWeight: '600',
  },
  settingsButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 8,
    backgroundColor: '#f8f9fa',
    borderWidth: 1,
    borderColor: '#9188E5',
    gap: 8,
    flex: 1,
    justifyContent: 'center',
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
    fontSize: 16,
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
  // Tab Navigation Styles
  tabContainer: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  tabButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 16,
    gap: 8,
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  activeTabButton: {
    borderBottomColor: '#9188E5',
  },
  tabText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#666',
  },
  activeTabText: {
    color: '#9188E5',
  },
  tabContent: {
    flex: 1,
  },
  // Posts Section Styles
  postsSection: {
    paddingTop: 10,
  },
  postCard: {
    backgroundColor: '#fff',
    marginHorizontal: 16,
    marginBottom: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#f0f0f0',
    padding: 16,
  },
  postHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  postUserInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  postUserImage: {
    width: 40,
    height: 40,
    borderRadius: 20,
    marginRight: 12,
  },
  postDefaultUserImage: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#f0f0f0',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  postUserDetails: {
    flex: 1,
  },
  postUserName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  postTime: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  postMoreButton: {
    padding: 4,
  },
  postContent: {
    fontSize: 16,
    color: '#333',
    lineHeight: 22,
    marginBottom: 12,
  },
  postImageContainer: {
    marginBottom: 12,
  },
  mockPostImage: {
    height: 200,
    backgroundColor: '#f8f9fa',
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  postActions: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
    gap: 24,
  },
  postActionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  postActionText: {
    fontSize: 14,
    color: '#666',
    fontWeight: '500',
  },
  // Updated Pets Section Styles
  petsSection: {
    padding: 16,
  },
  petsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  petGridCard: {
    width: '48%',
    backgroundColor: '#fff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#f0f0f0',
    padding: 12,
    marginBottom: 16,
    alignItems: 'center',
  },
  petGridImage: {
    width: 80,
    height: 80,
    borderRadius: 40,
    borderWidth: 2,
    borderColor: '#9188E5',
    marginBottom: 8,
  },
  defaultPetGridImage: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#f0f0f0',
    borderWidth: 2,
    borderColor: '#9188E5',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  petGridInfo: {
    alignItems: 'center',
    width: '100%',
  },
  petGridName: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    textAlign: 'center',
    marginBottom: 2,
  },
  petGridType: {
    fontSize: 12,
    color: '#666',
    textAlign: 'center',
  },
  // No Content Styles
  noContentContainer: {
    alignItems: 'center',
    paddingVertical: 40,
    paddingHorizontal: 20,
  },
  noContentText: {
    marginTop: 12,
    fontSize: 18,
    fontWeight: '600',
    color: '#999',
    textAlign: 'center',
  },
  noContentSubText: {
    marginTop: 6,
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
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