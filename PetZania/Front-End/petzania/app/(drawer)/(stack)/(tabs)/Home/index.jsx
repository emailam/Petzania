import { StyleSheet, Text, View } from 'react-native'
import React, {useContext} from 'react'
import { useFriendsData } from '@/hooks/useFriendsData'
import { UserContext } from '@/context/UserContext'
import UserPosts from '@/components/AdoptionBreedingModule/UserPosts';

export default function index() {
  const { user } = useContext(UserContext);
  useFriendsData(user?.userId);

  return (
    <UserPosts/>
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