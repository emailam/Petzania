import { Image, StyleSheet, Platform , View, Text } from 'react-native';

import Register from '@/app/RegisterModule/Register';
import Login from '../RegisterModule/Login';
export default function HomeScreen() {
  return (
    <View> 
      <Register/>
    </View>
  );
}