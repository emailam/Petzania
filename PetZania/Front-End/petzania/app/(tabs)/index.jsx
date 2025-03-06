import { Image, StyleSheet, Platform , View, Text } from 'react-native';
import { useState } from 'react';

import Button from '@/components/Button';
import Onboarding from '../onboarding/index'

export default function HomeScreen() {
  const [isSignedIn, setIsSignedIn] = useState(false);
  return (
    <View>
    {
      isSignedIn ? (
        <View>
          <Button title="Click Me" borderRadius={10} width={200} fontSize={20} onPress={() => console.log("Button Clicked")}/>
        </View>
      ) : (
        <Text>Not Signed In</Text>
      )
    }
    </View>
  );
}

const styles = StyleSheet.create({
  
});
