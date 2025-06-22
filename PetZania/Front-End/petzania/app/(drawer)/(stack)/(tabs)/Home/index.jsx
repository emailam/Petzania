import { StyleSheet, Text, View } from 'react-native'
import React, {useContext} from 'react'
import { useFriendsData } from '@/hooks/useFriendsData'
import { UserContext } from '@/context/UserContext'

export default function index() {
  const { user } = useContext(UserContext);
  useFriendsData(user?.userId);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>HOME</Text>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    padding: 20,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 30,
    color: '#333',
  },
})