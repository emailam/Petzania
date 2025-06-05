// app/(drawer)/_layout.tsx or _layout.jsx
import { Drawer } from 'expo-router/drawer';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import CustomDrawer from '@/components/CustomDrawer';

export default function Layout() {
    return (
        <GestureHandlerRootView style={{ flex: 1 }}>
            <Drawer screenOptions={{headerShown: false}} drawerContent={(props) => <CustomDrawer {...props} />} />
        </GestureHandlerRootView>
    );
}
