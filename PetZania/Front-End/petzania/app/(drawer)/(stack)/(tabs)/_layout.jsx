import { Tabs } from 'expo-router';
import { DrawerActions, useNavigation } from '@react-navigation/native';
import { useRouter } from 'expo-router';

import React, { useContext, useMemo } from 'react';
import { View, TouchableOpacity, StyleSheet, Text } from 'react-native';
import { Image } from 'expo-image';


import { HapticTab } from '@/components/HapticTab';
import TabBarBackground from '@/components/ui/TabBarBackground';

import { Ionicons } from '@expo/vector-icons';

import { UserContext } from '@/context/UserContext';
import { useNotifications } from '@/context/NotificationContext';
import { useChat } from '@/context/ChatContext';

// Memoized indicator component to prevent unnecessary re-renders
const ChatIndicator = React.memo(({ hasUnread }) => {
  if (!hasUnread) return null;
  
  return (
    <View style={{
      position: 'absolute',
      top: 0,
      right: 0,
      width: 8,
      height: 8,
      borderRadius: 4,
      backgroundColor: '#FF3B30',
      zIndex: 10,
    }} />
  );
});

export default function TabLayout() {

  const defaultImage = require('@/assets/images/Defaults/default-user.png');
  const { unreadCount } = useNotifications();
  const { hasUnreadMessages } = useChat();

  // Memoize the unread indicator to prevent unnecessary re-renders
  const memoizedChatUnreadIndicator = useMemo(() => hasUnreadMessages, [hasUnreadMessages]);

  // Debug logging for unread indicators (only when they change)
  React.useEffect(() => {
    console.log('🏷️ TabLayout unread indicators updated:', { 
      notifications: unreadCount, 
      chat: hasUnreadMessages 
    });
  }, [unreadCount, hasUnreadMessages]);

  const { user } = useContext(UserContext);

  const navigation = useNavigation();
  const router = useRouter();

  // Memoized header components to prevent unnecessary re-renders
  const HeaderLeft = React.memo(() => (
    <View style={styles.leftHeader}>
      <TouchableOpacity onPress={() => navigation.dispatch(DrawerActions.toggleDrawer())} style={{ marginRight: 6 }}>
        <Ionicons name="menu" size={24} color="#9188E8" />
      </TouchableOpacity>
      <TouchableOpacity onPress={() => { /* navigate to profile */ }} >
        <View style={styles.imageContainer}>
          <Image source={user?.profilePictureURL ? { uri: user.profilePictureURL } : defaultImage} style={styles.image} />
        </View>
      </TouchableOpacity>
    </View>
  ));

  const HeaderRight = React.memo(() => (
    <View style={{ flexDirection: 'row', gap: 4, marginRight: 12, alignItems: 'center' }}>
      <TouchableOpacity onPress={() => { router.push('Search') }}>
        <Ionicons name="search-outline" size={22} color="#9188E5" />
      </TouchableOpacity>
      <Text style={{ fontSize: 20, color: '#808B9A' }}> | </Text>
      <TouchableOpacity onPress={() => { router.push('Chat') }} style={{ position: 'relative' }}>
        <Ionicons name="chatbubble-ellipses-outline" size={22} color="#9188E5" />
        <ChatIndicator hasUnread={memoizedChatUnreadIndicator} />
      </TouchableOpacity>
    </View>
  ));

  return (
    <Tabs
      screenOptions={{
        headerShown: true,
        tabBarActiveTintColor: '#9188E5',
        tabBarInactiveTintColor: '#808B9A',
        tabBarButton: HapticTab,
        tabBarBackground: TabBarBackground,
        tabBarPosition: 'bottom',
        headerStyle: {
          height: 80,
        },
        tabBarItemStyle: {
          justifyContent: 'center',
          alignItems: 'center',
        },
        headerLeft: () => <HeaderLeft />,
        headerRight: () => <HeaderRight />,
        headerTitle: () => (
          <View style={{ marginLeft: 4 }}>
            <Text style={{ fontSize: 12, fontWeight: '', color: '#808B9A' }}>Hello,</Text>
            <Text style={{ fontSize: 16, fontWeight: '700', color: '#9188E5' }}>
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
          tabBarIcon: ({ color, focused }) => (
            <Ionicons name={focused ? "home" : "home-outline"} size={28} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="Adoption"
        options={{
          title: 'Adoption',
          tabBarIcon: ({ color, focused }) => (
            <Ionicons name={focused ? "paw" : "paw-outline"} size={28} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="AddPost"
        options={{
          title: 'Post',
          tabBarIcon: ({ color, focused }) => (
            <Ionicons name={focused ? "add-circle" : "add-circle-outline"} size={28} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="Breeding"
        options={{
          title: 'Breeding',
          tabBarIcon: ({ color, focused }) => (
            <Ionicons name={focused ? "heart" : "heart-outline"} size={28} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="Notifications"
        options={{
          title: 'Notifications',
          tabBarIcon: ({ color, focused }) => (
            <View>
              <Ionicons name={focused ? "notifications" : "notifications-outline"} size={28} color={color} />
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
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#9188E5',
  },
  leftHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginLeft: 12,
  },
});