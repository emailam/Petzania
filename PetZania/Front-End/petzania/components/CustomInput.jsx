import { StyleSheet, Text, View } from 'react-native';
import { TextInput } from 'react-native-paper';
import React from 'react';

export default function CustomInput({
  label,
  placeholder,
  value,
  onChangeText,
  onBlur,
  error,
  mode = "outlined",
  errorMessage,
  icon,
  maxLength,
  returnKeyType = 'next',
  onSubmitEditing,
  inputRef,
  style,
  ...props
}) {
  return (
    <View style={styles.container}>
      <TextInput
        {...props}
        mode={mode}
        label={label}
        placeholder={placeholder}
        value={value}
        onChangeText={onChangeText}
        onBlur={onBlur}
        maxLength={maxLength}
        returnKeyType={returnKeyType}
        onSubmitEditing={onSubmitEditing}
        ref={inputRef}
        style={[styles.input, style]}
        error={!!error}
        theme={{
          colors: {
            primary: '#7a6bfdff', // Darker purple when focused
            outline: error ? '#F44336' : '#9188E5', // Normal state outline
            outlineVariant: '#CCCCCC', // Unfocused state outline (lighter)
            background: '#fff',
            surface: '#7a6bfdff', // Darker purple for surface when focused
            onSurface: '#333333',
            surfaceVariant: 'transparent',
            onBackground: 'transparent',
          },
        }}
        left={icon ? <TextInput.Icon icon={() => icon} /> : undefined}
        contentStyle={[
          styles.inputContent,
          props.multiline && {
            textAlignVertical: 'top',
            paddingTop: 12,
            minHeight: 80,
          }
        ]}
        outlineStyle={styles.inputOutline}
      />
      {error && errorMessage && (
        <View style={styles.errorContainer}>
          <Text style={styles.errorText}>{errorMessage}</Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginBottom: 4,
    backgroundColor: 'transparent',
  },
  input: {
    backgroundColor: 'transparent',
    fontSize: 16,
  },
  inputContent: {
    fontSize: 16,
    color: '#333333',
    backgroundColor: 'transparent',
  },
  inputOutline: {
    borderRadius: 12,
    borderWidth: 1.3,
  },
  errorContainer: {
    paddingHorizontal: 4,
    paddingTop: 4,
    backgroundColor: 'transparent',
  },
  errorText: {
    color: '#F44336',
    fontSize: 12,
    fontWeight: '500',
  },
});