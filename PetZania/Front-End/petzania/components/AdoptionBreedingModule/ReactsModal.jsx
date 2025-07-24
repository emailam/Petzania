import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  Image,
  BackHandler,
} from 'react-native';
import {
  BottomSheetModal,
  BottomSheetBackdrop,
  BottomSheetFlatList,
} from '@gorhom/bottom-sheet';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';

const ReactsModal = ({
  post,
  visible,
  onClose,
  getUsers,
}) => {
  const bottomSheetModalRef = useRef(null);
  const snapPoints = useMemo(() => ['60%'], []);
  const router = useRouter();

  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(false);

  // Handle modal visibility
  useEffect(() => {
    if (visible) {
      bottomSheetModalRef.current?.present();
    } else {
      bottomSheetModalRef.current?.dismiss();
    }
  }, [visible]);

  // Handle Android back button
  useEffect(() => {
    const backAction = () => {
      if (visible) {
        onClose?.();
        return true;
      }
      return false;
    };

    const backHandler = BackHandler.addEventListener('hardwareBackPress', backAction);
    return () => backHandler.remove();
  }, [visible, onClose]);

  // Handle modal dismiss
  const handleSheetChanges = useCallback((index) => {
    if (index === -1) {
      onClose?.();
    }
  }, [onClose]);

  // Backdrop component
  const renderBackdrop = useCallback(
    (props) => (
      <BottomSheetBackdrop
        {...props}
        disappearsOnIndex={-1}
        appearsOnIndex={0}
        opacity={0.5}
        pressBehavior="close"
      />
    ),
    []
  );

  const handleNavigateToUser = useCallback((item) => {
    bottomSheetModalRef.current?.dismiss();
    router.push({
      pathname: `/UserModule/${item.userId}`,
      params: { username: item.username }
    });
  }, [router]);

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
    <TouchableOpacity style={styles.userItem} onPress={() => handleNavigateToUser(item)}>
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
    </TouchableOpacity>
  );

  return (
    <BottomSheetModal
      ref={bottomSheetModalRef}
      onChange={handleSheetChanges}
      snapPoints={snapPoints}
      backdropComponent={renderBackdrop}
      enablePanDownToClose={true}
      enableDismissOnClose={true}
      handleIndicatorStyle={styles.handleIndicator}
      backgroundStyle={styles.bottomSheetBackground}
    >
    <BottomSheetFlatList
      data={loading ? [] : profiles}
      keyExtractor={u => u.userId}
      renderItem={renderUserItem}
      showsVerticalScrollIndicator={false}
      contentContainerStyle={styles.flatListContent}
      ListHeaderComponent={
        <>
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
            {loading && (
              <ActivityIndicator style={{ marginTop: 20 }} />
            )}
            {!loading && profiles.length === 0 && (
              <Text style={styles.noReacts}>No reactions yet</Text>
            )}
          </View>
        </>
      }
      ListEmptyComponent={null}
    />
    </BottomSheetModal>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'white',
  },
  bottomSheetBackground: {
    backgroundColor: 'white',
  },
  handleIndicator: {
    backgroundColor: '#9188E5',
    width: 40,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  title: {
    fontSize: 16,
    fontWeight: '500',
    color: '#374151',
  },
  closeBtn: {
    padding: 4,
    borderRadius: 8,
    backgroundColor: '#F3F4F6',
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    padding: 16,
    backgroundColor: '#FFFFFF',
    flex: 1,
  },
  count: {
    fontSize: 14,
    fontWeight: '500',
    color: '#374151',
    marginBottom: 12,
  },
  flatListContent: {
    paddingBottom: 20,
  },
  userItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 12,
    marginHorizontal: 16,
    backgroundColor: '#F9FAFB',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E5E7EB',
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
    color: '#111827',
    fontWeight: '500',
  },
  noReacts: {
    fontSize: 14,
    color: '#9CA3AF',
    textAlign: 'center',
    marginTop: 32,
  },
});

export default ReactsModal;
