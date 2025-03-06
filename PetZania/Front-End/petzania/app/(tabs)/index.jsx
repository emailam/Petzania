import { Image, StyleSheet, Platform , View } from 'react-native';

import { HelloWave } from '@/components/HelloWave';
import ParallaxScrollView from '@/components/ParallaxScrollView';
import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import Button from '@/components/Button';
import Register from '@/app/RegisterModule/Register';
export default function HomeScreen() {
  return (
    <View>
        
        <Register/>
    </View>
  );
}
