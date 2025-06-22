import { StyleSheet, Text, View } from 'react-native'
import React from 'react'
import UserPosts from '@/components/AdoptionBreedingModule/UserPosts'
export default function index() {
  useFriendsData();

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