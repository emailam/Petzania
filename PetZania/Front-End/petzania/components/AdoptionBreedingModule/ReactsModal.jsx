import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Modal,
  ActivityIndicator,
  Image,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';

const ReactsModal = ({
  post,
  visible,
  onClose,
  getUsers,
}) => {
  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!visible) return;

    (async () => {
      setLoading(true);
      try {
        const outs = await Promise.all(
          post.reactedUsersIds.map(id => getUsers(id))
        );
        setProfiles(outs.filter(Boolean));
      } catch (err) {
        console.error('Error fetching user profiles:', err);
      } finally {
        setLoading(false);
      }
    })();
  }, [visible, post.reactedUsersIds, getUsers]);

  const renderUserItem = ({ item }) => (
    <View style={styles.userItem}>
      {item.profilePictureURL ? (
        <Image
          source={{ uri: item.profilePictureURL }}
          style={styles.avatar}
          onError={() => {}}
        />
      ) : (
        <Ionicons name="person-circle-outline" size={32} color="#666" />
      )}
      <Text style={styles.usernameText}>{item.username}</Text>
    </View>
  );

  return (
    <Modal
      animationType="slide"
      transparent
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.overlay}>
        <View style={styles.modal}>
          <View style={styles.header}>
            <Text style={styles.title}>Post Reactions</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
              <Ionicons name="close" size={24} color="#666" />
            </TouchableOpacity>
          </View>

          <View style={styles.content}>
            <Text style={styles.postId}>Post ID: {post.postId}</Text>
            <Text style={styles.count}>
              {post.reactedUsersIds.length} reaction
              {post.reactedUsersIds.length !== 1 ? 's' : ''}
            </Text>

            {loading ? (
              <ActivityIndicator style={{ marginTop: 20 }} />
            ) : profiles.length > 0 ? (
              <FlatList
                data={profiles}
                keyExtractor={u => u.userId}
                renderItem={renderUserItem}
                style={styles.list}
                showsVerticalScrollIndicator={false}
              />
            ) : (
              <Text style={styles.noReacts}>No reactions yet</Text>
            )}
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modal: {
    width: '90%',
    maxHeight: '80%',
    backgroundColor: '#fff',
    borderRadius: 20,
    overflow: 'hidden',
    elevation: 5,
  },
  header: {
    flexDirection: 'row',
    padding: 16,
    borderBottomWidth: 1,
    borderColor: '#eee',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: { fontSize: 18, fontWeight: '600', color: '#333' },
  closeBtn: { padding: 4 },
  content: { padding: 16 },
  postId: {
    fontSize: 12,
    color: '#666',
    fontFamily: 'monospace',
    marginBottom: 8,
  },
  count: {
    fontSize: 16,
    fontWeight: '500',
    color: '#333',
    marginBottom: 12,
  },
  list: { maxHeight: 300 },
  userItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 12,
    backgroundColor: '#f8f8f8',
    borderRadius: 8,
    marginBottom: 8,
  },
  avatar: {
    width: 32,
    height: 32,
    borderRadius: 16,
  },
  usernameText: {
    marginLeft: 8,
    fontSize: 14,
    color: '#333',
  },
  noReacts: {
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
    marginTop: 20,
  },
});

export default ReactsModal;
