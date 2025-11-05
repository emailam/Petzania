import { StyleSheet, Text, View } from 'react-native'
import React from 'react'
import LottieView from 'lottie-react-native'

export default function LoadingIndicator({ text }) {
  return (
    <View style={{ justifyContent: 'center', alignItems: 'center', flex: 1, backgroundColor: 'transparent' }}>
        <LottieView
            source={require('@/assets/lottie/cat_spinner.json')}
            autoPlay
            loop
            style={{ width: 100, height: 100, alignSelf: 'center' }}
        />
        {text && <Text style={{ textAlign: 'center', color: '#9188E5', marginTop: 10 }}>{text}</Text>}
    </View>
  )
}