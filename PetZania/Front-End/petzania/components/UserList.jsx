import React from 'react';
import { StyleSheet, Text, View, FlatList, TouchableOpacity } from 'react-native';
import { Image } from 'expo-image';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';

export default function UserList({
  users = [],
  onUserPress,
  keyExtractor,
  EmptyComponent,
  FooterComponent,
  contentContainerStyle,
  showChevron = true,
  itemStyle = {},
  ...flatListProps
}){

    const router = useRouter();

    const handleUserPress = (user) => {
      console.log("User pressed:", user);
        if (onUserPress) {
            onUserPress(user);
        } else {
            router.push({
                pathname: `/UserModule/${user.userId}`,
                params: { username: user.username }
            });
        }
    };

    const defaultKeyExtractor = (item, index) => {
      console.log("THE ITEM IS:", item);
        return item.follower?.userId?.toString() || item.blockId?.toString() || item.followed?.userId?.toString();
    };

    const renderUser = ({ item }) => (
        <TouchableOpacity
            style={[styles.userItem, itemStyle]}
            onPress={() => handleUserPress(item)}
            activeOpacity={0.7}
        >
            <View style={styles.profileImageContainer}>
                {item.profilePictureURL ? (
                <Image
                    source={{ uri: item.profilePictureURL }}
                    style={styles.profileImage}
                />
                ) : (
                <View style={styles.defaultProfileImage}>
                    <Ionicons name="person" size={24} color="#9188E5" />
                </View>
                )}
            </View>

            <View style={styles.userInfoContainer}>
                <Text style={styles.userName}> {item.name || item.username || 'Unknown User'} </Text>
                {item.username && item.name && (
                    <Text style={styles.userHandle}>@{item.username}</Text>
                )}
            </View>

            {showChevron && (
                <Ionicons name="chevron-forward" size={20} color="#ccc" />
            )}
        </TouchableOpacity>
    );

    return (
        <FlatList
            data={users}
            keyExtractor={keyExtractor || defaultKeyExtractor}
            renderItem={renderUser}
            ListEmptyComponent={EmptyComponent}
            ListFooterComponent={FooterComponent}
            contentContainerStyle={contentContainerStyle}
            {...flatListProps}
        />
    );
}

const styles = StyleSheet.create({
  userItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
    backgroundColor: '#fff',
  },
  profileImageContainer: {
    marginRight: 12,
  },
  profileImage: {
    width: 48,
    height: 48,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: '#9188E5',
  },
  defaultProfileImage: {
    width: 48,
    height: 48,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: '#9188E5',
    backgroundColor: '#f0f0f0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  userInfoContainer: {
    flex: 1,
    marginRight: 8,
  },
  userName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  userHandle: {
    fontSize: 14,
    color: '#9188E5',
    marginTop: 2,
  },
});
