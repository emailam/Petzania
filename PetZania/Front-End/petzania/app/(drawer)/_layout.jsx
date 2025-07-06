import React from 'react';
import { Drawer } from 'expo-router/drawer';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import CustomDrawer from '@/components/CustomDrawer';

export default function Layout() {
    return (
        <GestureHandlerRootView style={{ flex: 1 }}>
            <Drawer
                drawerContent={(props) => <CustomDrawer {...props} />}
                screenOptions={{
                    headerShown: false,
                    drawerStyle: {
                        backgroundColor: '#fff',
                        width: 300,
                    },
                    drawerActiveTintColor: '#9188E5',
                    drawerInactiveTintColor: '#666',
                    swipeEnabled: true,
                    gestureEnabled: true,
                    drawerPosition: 'left',
                    drawerType: 'front'
                }}
            />
        </GestureHandlerRootView>
    );
}