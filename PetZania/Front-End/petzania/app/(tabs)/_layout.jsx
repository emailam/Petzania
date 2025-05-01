import { Tabs } from 'expo-router';
import React, {useState} from 'react';
import { Platform, View, TouchableOpacity, Image, StyleSheet } from 'react-native';

import { HapticTab } from '@/components/HapticTab';
import { IconSymbol } from '@/components/ui/IconSymbol';
import TabBarBackground from '@/components/ui/TabBarBackground';

import { Ionicons, Feather } from '@expo/vector-icons'; // or any icon pack you prefer

export default function TabLayout() {

  const defaultImage = require('@/assets/images/AddPet/Pet Default Pic.png');
  const [image, setImage] = useState(null);

  const HeaderLeft = () => (
    <TouchableOpacity onPress={() => { /* navigate to profile */ }} style={{ marginLeft: 16 }}>
      <View style={styles.imageContainer}>
        <Image source={image ? { uri: image } : defaultImage} style={styles.image} />
      </View>
    </TouchableOpacity>
  );

  const HeaderRight = () => (
    <View style={{ flexDirection: 'row', gap: 16, marginRight: 16 }}>
      <TouchableOpacity onPress={() => {  }}>
        <Feather name="search" size={22} color="#9188E5" />
      </TouchableOpacity>
      <TouchableOpacity onPress={() => { /* handle chat */ }}>
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
        tabBarLabelStyle: {
          fontSize: 11,
          paddingBottom: Platform.OS === 'ios' ? 12 : 7,
        },
        tabBarItemStyle: {
          justifyContent: 'center',
          alignItems: 'center',
        },
        headerLeft: () => <HeaderLeft />,
        headerRight: () => <HeaderRight />,
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
            <IconSymbol name="bell.fill" size={28} color={color} />
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
});