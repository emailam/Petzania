import { Tabs } from 'expo-router';
import { DrawerActions, useNavigation } from '@react-navigation/native';
import { useRouter } from 'expo-router';

import React, { useState, useContext } from 'react';
import { Platform, View, TouchableOpacity, Image, StyleSheet, Text } from 'react-native';

import { HapticTab } from '@/components/HapticTab';
import { IconSymbol } from '@/components/ui/IconSymbol';
import TabBarBackground from '@/components/ui/TabBarBackground';

import { Ionicons, Feather, AntDesign } from '@expo/vector-icons'; // or any icon pack you prefer

import { UserContext } from '@/context/UserContext';

export default function TabLayout() {

  const defaultImage = require('@/assets/images/AddPet/Pet Default Pic.png');

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
        <Feather name="search" size={22} color="#9188E5" />
      </TouchableOpacity>
      <Text style={{ fontSize: 22, color: '#808B9A' }}> | </Text>
      <TouchableOpacity onPress={() => { router.push('Chat') }}>
        <Ionicons name="chatbubble-ellipses-outline" size={24} color="#9188E5" />
      </TouchableOpacity>
      <TouchableOpacity onPress={() => { router.push('Notifications') }}>
        <Ionicons name="notifications" size={24} color="#9188E5" />
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
          title: '',
          tabBarIcon: ({ color, focused }) => (
            <View style={[
              styles.addButtonContainer,
              {
                backgroundColor: focused ? '#5348BD' : '#9188E5',
              }
            ]}>
              <AntDesign name="plus" size={30} color="#fff" />
            </View>
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
        name="Pet Services"
        options={{
          title: 'Pet Services',
          tabBarIcon: ({ color }) => (
            <IconSymbol name="bone.fill" size={28} color={color} />
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
  addButtonContainer: {
    backgroundColor: '#9188E5',
    width: 48,
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
  },
});