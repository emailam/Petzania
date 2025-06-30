import { Tabs } from 'expo-router';
import { DrawerActions, useNavigation } from '@react-navigation/native';
import { useRouter } from 'expo-router';

import React, { useContext } from 'react';
import { View, TouchableOpacity, StyleSheet, Text } from 'react-native';
import { Image } from 'expo-image';


import { HapticTab } from '@/components/HapticTab';
import { IconSymbol } from '@/components/ui/IconSymbol';
import TabBarBackground from '@/components/ui/TabBarBackground';

import { Ionicons } from '@expo/vector-icons';

import { UserContext } from '@/context/UserContext';
import { useNotifications } from '@/context/NotificationContext';

export default function TabLayout() {

  const defaultImage = require('@/assets/images/Defaults/default-user.png');
  const { unreadCount } = useNotifications();

  const { user } = useContext(UserContext);

  const navigation = useNavigation();
  const router = useRouter();

  const HeaderLeft = () => (
    <View style={styles.leftHeader}>
      <TouchableOpacity onPress={() => navigation.dispatch(DrawerActions.toggleDrawer())} style={{ marginRight: 8 }}>
        <Ionicons name="menu" size={26} color="#9188E5" />
      </TouchableOpacity>
      <TouchableOpacity onPress={() => { /* navigate to profile */ }} >
        <View style={styles.imageContainer}>
          <Image source={user?.profilePictureURL ? { uri: user.profilePictureURL } : defaultImage} style={styles.image} />
        </View>
      </TouchableOpacity>
    </View>
  );

  const HeaderRight = () => (
    <View style={{ flexDirection: 'row', gap: 4, marginRight: 16, alignItems: 'center' }}>
      <TouchableOpacity onPress={() => { router.push('Search') }}>
        <Ionicons name="search-outline" size={24} color="#9188E5" />
      </TouchableOpacity>
      <Text style={{ fontSize: 22, color: '#808B9A' }}> | </Text>
      <TouchableOpacity onPress={() => { router.push('Chat') }}>
        <Ionicons name="chatbubble-ellipses-outline" size={24} color="#9188E5" />
      </TouchableOpacity>
    </View>
  );
  return (
    <Tabs
      screenOptions={{
        headerShown: true,
        tabBarActiveTintColor: '#5348BD',
        tabBarInactiveTintColor: '#9188E5',
        tabBarButton: HapticTab,
        tabBarBackground: TabBarBackground,
        tabBarPosition: 'bottom',
        tabBarItemStyle: {
          justifyContent: 'center',
          alignItems: 'center',
        },
        headerLeft: () => <HeaderLeft />,
        headerRight: () => <HeaderRight />,
        headerTitle: () => (
          <View style={{ marginLeft: 4 }}>
            <Text style={{ fontSize: 14, fontWeight: '', color: '#808B9A' }}>Hello,</Text>
            <Text style={{ fontSize: 18, fontWeight: '700', color: '#9188E5' }}>
              {user ? user.username : 'User'}
            </Text>
          </View>
        ),
      }}
    >
      <Tabs.Screen
        name="Home"
        options={{
          title: 'Home',
          tabBarIcon: ({ color }) => (
            <IconSymbol name="house.fill" size={28} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="Adoption"
        options={{
          title: 'Adoption',
          tabBarIcon: ({ color }) => (
            <IconSymbol name="pawprint.fill" size={28} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="Temp/index"
        options={{
          title: 'Post',
          tabBarIcon: ({ color }) => (
            <IconSymbol name="add.fill" size={28} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="Breeding"
        options={{
          title: 'Breeding',
          tabBarIcon: ({ color }) => (
            <IconSymbol name="heart.circle.fill" size={28} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="Notifications"
        options={{
          title: 'Notifications',
          tabBarIcon: ({ color }) => (
            <View>
              <IconSymbol name="bell.fill" size={28} color={color} />
              {unreadCount > 0 && (
                <View style={{
                  position: 'absolute',
                  top: -4,
                  right: -8,
                  backgroundColor: '#FF3B30',
                  borderRadius: 8,
                  minWidth: 16,
                  height: 16,
                  justifyContent: 'center',
                  alignItems: 'center',
                  paddingHorizontal: 4,
                  zIndex: 10,
                }}>
                  <Text style={{ color: '#fff', fontSize: 10, fontWeight: 'bold' }}>{unreadCount}</Text>
                </View>
              )}
            </View>
          ),
        }}
      />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  imageContainer: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  imageWrapper: {
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  image: {
    width: 46,
    height: 46,
    borderRadius: 23,
    borderWidth: 1,
    borderColor: '#9188E5',
  },
  leftHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginLeft: 16,
  },
});