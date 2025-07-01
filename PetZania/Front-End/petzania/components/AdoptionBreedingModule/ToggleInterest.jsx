import React, { useState, useRef, useCallback, memo } from 'react';
import {
  View,
  TouchableOpacity,
  Text,
  Modal,
  Animated,
  StyleSheet,
  Dimensions,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';

const { width } = Dimensions.get('window');

const ToggleInterest = memo(({ 
  postId, 
  onInterestChange, 
  initialInterest = null 
}) => {
  const [selected, setSelected] = useState(initialInterest);
  const [showMenu, setShowMenu] = useState(false);
  const [menuPos, setMenuPos] = useState({ x: 0, y: 0 });
  const buttonRef = useRef(null);
  const fadeAnim = useRef(new Animated.Value(0)).current;

  const openMenu = useCallback(() => {
    if (!buttonRef.current) return;
    
    buttonRef.current.measure((fx, fy, w, h, px, py) => {
      // Calculate position to keep menu within screen bounds
      const menuWidth = 160;
      const menuX = px - menuWidth + w;
      const adjustedX = Math.max(10, Math.min(menuX, width - menuWidth - 10));
      
      setMenuPos({ x: adjustedX, y: py + h + 5 });
      setShowMenu(true);
      
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: 200,
        useNativeDriver: true,
      }).start();
    });
  }, [fadeAnim]);

  const closeMenu = useCallback(() => {
    Animated.timing(fadeAnim, {
      toValue: 0,
      duration: 150,
      useNativeDriver: true,
    }).start(() => setShowMenu(false));
  }, [fadeAnim]);

  const handleSelection = useCallback((choice) => {
    setSelected(choice);
    closeMenu();
    onInterestChange?.(postId, choice);
  }, [postId, closeMenu, onInterestChange]);

  const renderIndicator = useCallback(() => {
    if (selected === 'interested') {
      return <Ionicons name="checkmark-circle" size={16} color="#4CAF50" />;
    }
    if (selected === 'not') {
      return <Ionicons name="close-circle" size={16} color="#FF5722" />;
    }
    return null;
  }, [selected]);

  const getButtonStyle = useCallback(() => {
    if (selected === 'interested') return styles.menuButtonInterested;
    if (selected === 'not') return styles.menuButtonNotInterested;
    return null;
  }, [selected]);

  return (
    <View style={styles.wrapper}>
      <TouchableOpacity
        ref={buttonRef}
        onPress={openMenu}
        style={[styles.menuButton, getButtonStyle()]}
        hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
        activeOpacity={0.7}
      >
        <Ionicons name="ellipsis-vertical" size={18} color="#666" />
        {selected && (
          <View style={styles.indicator}>
            {renderIndicator()}
          </View>
        )}
      </TouchableOpacity>

      <Modal 
        visible={showMenu} 
        transparent 
        animationType="none" 
        onRequestClose={closeMenu}
      >
        <TouchableOpacity 
          style={styles.modalOverlay} 
          activeOpacity={1} 
          onPress={closeMenu}
        >
          <Animated.View
            style={[
              styles.dropdown,
              {
                opacity: fadeAnim,
                transform: [{
                  translateY: fadeAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [-10, 0],
                  }),
                }],
                left: menuPos.x,
                top: menuPos.y,
              },
            ]}
          >
            <TouchableOpacity 
              style={[
                styles.option, 
                selected === 'interested' && styles.optionSelected
              ]} 
              onPress={() => handleSelection('interested')}
              activeOpacity={0.7}
            >
              <Ionicons name="heart" size={18} color="#4CAF50" />
              <Text style={[styles.optionText, styles.optionTextInterested]}>
                Interested
              </Text>
              {selected === 'interested' && (
                <Ionicons name="checkmark" size={16} color="#4CAF50" style={styles.checkmark} />
              )}
            </TouchableOpacity>
            
            <View style={styles.separator} />
            
            <TouchableOpacity 
              style={[
                styles.option, 
                selected === 'not' && styles.optionSelected
              ]} 
              onPress={() => handleSelection('not')}
              activeOpacity={0.7}
            >
              <Ionicons name="heart-dislike" size={18} color="#FF5722" />
              <Text style={[styles.optionText, styles.optionTextNotInterested]}>
                Not Interested
              </Text>
              {selected === 'not' && (
                <Ionicons name="checkmark" size={16} color="#FF5722" style={styles.checkmark} />
              )}
            </TouchableOpacity>
            
            {selected && (
              <>
                <View style={styles.separator} />
                <TouchableOpacity 
                  style={styles.option} 
                  onPress={() => handleSelection(null)}
                  activeOpacity={0.7}
                >
                  <Ionicons name="refresh" size={18} color="#666" />
                  <Text style={styles.optionText}>Clear</Text>
                </TouchableOpacity>
              </>
            )}
          </Animated.View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
});

const styles = StyleSheet.create({
  wrapper: {
    marginLeft: 8,
  },
  menuButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#f8f8f8',
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  menuButtonInterested: {
    backgroundColor: '#e8f5e9',
    borderWidth: 1,
    borderColor: '#4CAF50',
  },
  menuButtonNotInterested: {
    backgroundColor: '#ffebee',
    borderWidth: 1,
    borderColor: '#FF5722',
  },
  indicator: {
    position: 'absolute',
    bottom: -2,
    right: -2,
  },
  modalOverlay: {
    flex: 1,
  },
  dropdown: {
    position: 'absolute',
    backgroundColor: '#fff',
    borderRadius: 12,
    paddingVertical: 8,
    minWidth: 160,
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 8,
  },
  option: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  optionSelected: {
    backgroundColor: '#f5f5f5',
  },
  optionText: {
    fontSize: 14,
    fontWeight: '500',
    marginLeft: 12,
    flex: 1,
    color: '#666',
  },
  optionTextInterested: {
    color: '#4CAF50',
  },
  optionTextNotInterested: {
    color: '#FF5722',
  },
  checkmark: {
    marginLeft: 'auto',
  },
  separator: {
    height: 1,
    backgroundColor: '#f0f0f0',
    marginHorizontal: 12,
  },
});

ToggleInterest.displayName = 'ToggleInterest';

export default ToggleInterest;