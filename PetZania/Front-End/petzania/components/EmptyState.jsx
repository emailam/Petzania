import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

const EmptyState = ({ 
  iconName = 'people-outline', 
  iconSize = 60, 
  iconColor = '#999',
  title = 'No Data',
  subtitle = 'No information available',
  style,
  titleStyle,
  subtitleStyle
}) => {
  return (
    <View style={[styles.container, style]}>
      <Ionicons name={iconName} size={iconSize} color={iconColor} />
      <Text style={[styles.title, titleStyle]}>{title}</Text>
      <Text style={[styles.subtitle, subtitleStyle]}>{subtitle}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 40,
  },
  title: {
    fontSize: 18,
    fontWeight: '600',
    color: '#999',
    marginTop: 16,
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 14,
    color: '#999',
    marginTop: 8,
    textAlign: 'center',
    lineHeight: 20,
  },
});

export default EmptyState;
