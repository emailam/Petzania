import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Modal,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';

const ReactsModal = ({
  visible,
  onClose,
  postId,
  reactedUsersIds = [],
}) => {
  const reactsCount = reactedUsersIds.length;
  const hasReactions = reactsCount > 0;

  const renderUserItem = ({ item, index }) => (
    <View style={styles.userIdItem}>
      <Ionicons name="person-circle-outline" size={20} color="#666" />
      <Text style={styles.userIdText}>{item}</Text>
    </View>
  );

  const renderContent = () => (
    <View style={styles.reactsModalContent}>
      <Text style={styles.postIdText}>
        Post ID: {postId}
      </Text>
      <Text style={styles.reactsCountText}>
        {reactsCount} reaction{reactsCount !== 1 ? 's' : ''}
      </Text>
      
      {hasReactions ? (
        <FlatList
          data={reactedUsersIds}
          keyExtractor={(item, index) => `${item}-${index}`}
          renderItem={renderUserItem}
          style={styles.usersList}
          showsVerticalScrollIndicator={false}
        />
      ) : (
        <Text style={styles.noReactsText}>No reactions yet</Text>
      )}
    </View>
  );

  const renderHeader = () => (
    <View style={styles.reactsModalHeader}>
      <Text style={styles.reactsModalTitle}>Post Reactions</Text>
      <TouchableOpacity
        style={styles.closeButton}
        onPress={onClose}
        activeOpacity={0.7}
      >
        <Ionicons name="close" size={24} color="#666" />
      </TouchableOpacity>
    </View>
  );

  return (
    <Modal
      animationType="slide"
      transparent={true}
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.modalOverlay}>
        <View style={styles.reactsModal}>
          {renderHeader()}
          {renderContent()}
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  reactsModal: {
    backgroundColor: 'white',
    borderRadius: 20,
    width: '90%',
    maxHeight: '80%',
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
  },
  reactsModalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  reactsModalTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
  },
  closeButton: {
    padding: 4,
    borderRadius: 12,
  },
  reactsModalContent: {
    padding: 20,
  },
  postIdText: {
    fontSize: 12,
    color: '#666',
    marginBottom: 8,
    fontFamily: 'monospace',
  },
  reactsCountText: {
    fontSize: 16,
    fontWeight: '500',
    color: '#333',
    marginBottom: 16,
  },
  usersList: {
    maxHeight: 300,
  },
  userIdItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 12,
    backgroundColor: '#f8f8f8',
    borderRadius: 8,
    marginBottom: 8,
  },
  userIdText: {
    fontSize: 12,
    color: '#666',
    marginLeft: 8,
    fontFamily: 'monospace',
  },
  noReactsText: {
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
    marginTop: 20,
  },
});

export default ReactsModal;