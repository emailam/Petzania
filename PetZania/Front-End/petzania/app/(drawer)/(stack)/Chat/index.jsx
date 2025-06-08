import React, { useEffect, useState } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet, Image } from 'react-native';
import { useRouter } from 'expo-router';

const dummyChats = [
  {
    id: '1',
    name: 'Alice',
    lastMessage: 'Hey, how are you?',
    timestamp: '2:30 PM',
    avatar: 'https://i.pravatar.cc/150?img=1',
  },
  {
    id: '2',
    name: 'Bob',
    lastMessage: 'Let’s meet tomorrow.',
    timestamp: '1:15 PM',
    avatar: 'https://i.pravatar.cc/150?img=2',
  },
];

export default function ChatIndex() {
  const router = useRouter();
  const [chats, setChats] = useState([]);

  useEffect(() => {
    // TODO: Replace with real API call
    setChats(dummyChats);
  }, []);

  const renderItem = ({ item }) => (
    <TouchableOpacity
      style={styles.chatItem}
      onPress={() => router.push(`/Chat/${item.id}`)}
    >
      <Image source={{ uri: item.avatar }} style={styles.avatar} />
      <View style={styles.chatInfo}>
        <Text style={styles.name}>{item.name}</Text>
        <Text style={styles.lastMessage}>{item.lastMessage}</Text>
      </View>
      <Text style={styles.timestamp}>{item.timestamp}</Text>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <FlatList
        data={chats}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        contentContainerStyle={{ paddingVertical: 10 }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', paddingHorizontal: 16 },
  chatItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#ccc',
  },
  avatar: { width: 48, height: 48, borderRadius: 24, marginRight: 12 },
  chatInfo: { flex: 1 },
  name: { fontSize: 16, fontWeight: 'bold' },
  lastMessage: { fontSize: 14, color: '#666', marginTop: 2 },
  timestamp: { fontSize: 12, color: '#999' },
});
