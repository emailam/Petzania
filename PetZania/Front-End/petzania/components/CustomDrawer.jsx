import { DrawerContentScrollView, DrawerItem } from '@react-navigation/drawer';
import { View } from 'react-native';
import { FontAwesome, Ionicons, MaterialIcons } from '@expo/vector-icons';

import { useRouter, usePathname } from 'expo-router';

export default function CustomDrawer(props) {
  const router = useRouter();
  return (
    <DrawerContentScrollView {...props} contentContainerStyle={{ flex: 1 }}>
      <View style={{ flex: 1, justifyContent: 'space-between' }}>

        <View>
          <DrawerItem label="Home" icon={() => <FontAwesome name="home" size={20} />} onPress = {()=>{ router.navigate('/Home')}} activeBackgroundColor='#9188E5'  />
          <DrawerItem label="Friends" icon={() => <MaterialIcons name="groups" size={20} color="black" /> } onPress = {()=>{ router.push('/(drawer)/Friends')}} activeBackgroundColor='#9188E5' />
        </View>

        <View >
          <DrawerItem label="Settings" icon={() => <Ionicons name="settings-outline" size={20} />} onPress = {()=>{ router.navigate('/Settings')}} />
          <DrawerItem label="Help & Support" icon={() => <Ionicons name="help-circle-outline" size={20} />} onPress = {()=>{ router.navigate('/Help')}} />
          <DrawerItem label="Log out" icon={() => <MaterialIcons name="logout" size={20} />} onPress = {{}} />
        </View>
      </View>
    </DrawerContentScrollView>
  );
}
