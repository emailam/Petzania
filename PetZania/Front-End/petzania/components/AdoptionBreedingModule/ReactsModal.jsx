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
    backgroundColor: 'rgba(16, 18, 26, 0.50)', // slightly deeper overlay for modern look
    justifyContent: 'center',
    alignItems: 'center',
  },
  modal: {
    width: '90%',
    maxHeight: '80%',
    backgroundColor: '#fff',
    borderRadius: 12, // match input/button/card system
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  header: {
    flexDirection: 'row',
    padding: 16,
    borderBottomWidth: 1,
    borderColor: '#E5E7EB', // match input card border
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#F9FAFB', // subtle off-white
  },
  title: {
    fontSize: 16, // match to input-style label
    fontWeight: '500',
    color: '#374151', // gray-700
  },
  closeBtn: {
    padding: 4,
    borderRadius: 8,
    backgroundColor: '#F3F4F6', // subtle hover look on close
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    padding: 16,
    backgroundColor: '#FFFFFF',
  },
  count: {
    fontSize: 14,
    fontWeight: '500',
    color: '#374151',
    marginBottom: 12,
  },
  list: { maxHeight: 320 }, // slight increase for better spacing
  userItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 12,
    backgroundColor: '#F9FAFB', // subtle card background
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E5E7EB', // match to input border
    marginBottom: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.03,
    shadowRadius: 1,
    elevation: 1,
  },
  avatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#E5E7EB',
  },
  usernameText: {
    marginLeft: 12,
    fontSize: 16,
    color: '#111827', // gray-900
    fontWeight: '500',
  },
  noReacts: {
    fontSize: 14,
    color: '#9CA3AF', // gray-400
    textAlign: 'center',
    marginTop: 32,
  },
});

export default ReactsModal;
