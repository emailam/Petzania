import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { SymbolWeight } from 'expo-symbols';
import React from 'react';
import { OpaqueColorValue, StyleProp, ViewStyle } from 'react-native';

const MAPPING = {
  'house.fill': 'home',
  'pawprint.fill': 'pets',
  'heart.circle.fill': 'favorite',
  'bell.fill': 'notifications',
  'bone.fill': 'restaurant', // Replace with a better match if needed
  'add.fill': 'add-box',
} as const;

export type IconSymbolName = keyof typeof MAPPING;

export function IconSymbol({
  name,
  size,
  color,
  style,
  weight,
}: {
  name: IconSymbolName;
  size?: number;
  color: string | OpaqueColorValue;
  style?: StyleProp<ViewStyle>;
  weight?: SymbolWeight;
}) {
  if (!MAPPING[name]) {
    console.warn(`Icon "${name}" not mapped to MaterialIcons.`);
    return null;
  }

  return <MaterialIcons color={color} size={size} name={MAPPING[name]} />;
}