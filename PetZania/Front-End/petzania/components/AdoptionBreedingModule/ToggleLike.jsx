import React, { useState, useCallback, memo } from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
const ToggleLike = memo(({ 
  post,
  onLikeChange,
}) => {
  const initialLiked = post?.reactedUsersIds?.includes(post?.ownerId) || false;

  const [liked, setLiked] = useState(initialLiked);

  const handleToggle = useCallback(() => {

    const nextLiked = !liked;
    setLiked(nextLiked);    
    onLikeChange?.(nextLiked);
    
  }, [liked, post, onLikeChange]);


  return (
    <>
      <View style={styles.container}>
        <TouchableOpacity 
          style={[styles.likeButton, isLoading && styles.likeButtonDisabled]}
          onPress={handleToggle}
          hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
          disabled={isLoading}
          activeOpacity={0.7}
        >
          <Ionicons 
            name={liked ? 'heart' : 'heart-outline'} 
            size={20} 
            color={liked ? '#FF3040' : '#666'} 
          />
        </TouchableOpacity>
      </View>
    </>
  );
});

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    gap: 4,
  },
  likeButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#f8f8f8',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  likeButtonDisabled: {
    opacity: 0.5,
  },
  reactsCounter: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    backgroundColor: '#f0f0f0',
    borderRadius: 12,
    minWidth: 30,
    alignItems: 'center',
  },
  reactsCounterDisabled: {
    backgroundColor: '#f8f8f8',
  },
  reactsText: {
    fontSize: 12,
    color: '#999',
    fontWeight: '500',
  },
  reactsTextActive: {
    color: '#666',
  },
});

ToggleLike.displayName = 'ToggleLike';

export default ToggleLike;