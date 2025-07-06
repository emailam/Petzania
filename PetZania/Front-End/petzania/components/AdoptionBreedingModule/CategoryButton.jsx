import React from 'react';
import { View, Text, Pressable, StyleSheet, Dimensions, Platform } from 'react-native';

const { width } = Dimensions.get('window');

export default function CategoryButton({
  icon,
  title,
  onPress,
  style,
}) {
  // "isSelected" pattern: expects 'categorySelected' from your modal styles as an object with backgroundColor: '#9188E5'
  const isSelected = style && style.some && style.some(s => s?.backgroundColor === '#9188E5');

  return (
    <Pressable style={[styles.container, isSelected && styles.selected, style]} onPress={onPress}>
      <View style={styles.content}>
        {icon && React.cloneElement(icon, {
          color: isSelected ? '#ffffff' : '#495057'
        })}
        <Text style={[
          styles.title,
          icon && styles.titleWithIcon,
          isSelected && styles.titleSelected
        ]}>
          {title}
        </Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#f8f9fa',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    minHeight: 44,
    borderWidth: 1.5,
    borderColor: '#e9ecef',
    marginVertical: 4,
    marginHorizontal: 4,
    overflow: 'hidden',
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
      },
      android: {
        elevation: 2,
      },
    }),
  },
  selected: {
    backgroundColor: '#9188E5',
    borderColor: '#9188E5',
  },
  content: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
  },
  title: {
    fontSize: 14,
    fontWeight: '600',
    textAlign: 'center',
    color: '#495057',
    letterSpacing: -0.2,
  },
  titleWithIcon: {
    marginLeft: 8,
  },
  titleSelected: {
    color: '#ffffff',
  },
});