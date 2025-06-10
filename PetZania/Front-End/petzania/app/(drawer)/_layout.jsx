import { Drawer } from 'expo-router/drawer';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import CustomDrawer from '@/components/CustomDrawer';

export default function Layout() {
    return (
        <GestureHandlerRootView>
            <Drawer
                screenOptions={{ headerShown: false }}
                drawerContent={(props) => <CustomDrawer {...props} />}
            />
        </GestureHandlerRootView>
    );
}
