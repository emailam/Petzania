import { Controller } from 'react-hook-form';
import { View, StyleSheet, Text } from 'react-native';
import { TextInput } from 'react-native-paper';
import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

export default function PasswordInput({ 
  control, 
  name, 
  errors, 
  showPassword, 
  toggleShow, 
  icon,
  placeholder,
  label,
  onSubmitEditing,
  returnKeyType = 'next',
  inputRef
}) {
  return (
    <View style={styles.container}>
      <Controller
        control={control}
        name={name}
        render={({ field }) => (
          <TextInput
            mode="outlined"
            label={label || placeholder}
            secureTextEntry={!showPassword}
            style={styles.input}
            returnKeyType={returnKeyType}
            onSubmitEditing={onSubmitEditing}
            ref={inputRef}
            theme={{
              colors: {
                primary: '#9188E5',
                outline: errors[name] ? '#F44336' : '#9188E5',
                // onSurfaceVariant: '#666666',
                background: '#fff',
                surface: 'transparent',
                onSurface: '#333333',
                surfaceVariant: 'transparent',
                onBackground: 'transparent',
              },
            }}
            left={icon ? <TextInput.Icon icon={() => icon} /> : undefined}
            right={
              <TextInput.Icon
                forceTextInputFocus={false}
                icon={() => (
                  <Ionicons
                    name={showPassword ? "eye-off" : "eye"} 
                    size={24}
                    color="#9188E5"
                  />
                )}
                onPress={toggleShow}
              />
            }
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            value={field.value}
            error={!!errors[name]}
            contentStyle={styles.inputContent}
            outlineStyle={styles.inputOutline}
          />
        )}
      />
      {errors[name] && (
        <View style={styles.errorContainer}>
          <Text style={styles.errorText}>{errors[name].message}</Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginBottom: 4,
  },
  input: {
    backgroundColor: 'transparent',
    fontSize: 16,
  },
  inputContent: {
    fontSize: 16,
    color: '#333333',
  },
  inputOutline: {
    borderRadius: 12,
    borderWidth: 1.5,
  },
  errorContainer: {
    paddingHorizontal: 4,
    paddingTop: 4,
  },
  errorText: {
    color: '#F44336',
    fontSize: 12,
    fontWeight: '500',
  }
});