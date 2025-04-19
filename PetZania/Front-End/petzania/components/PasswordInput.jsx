import { Controller } from 'react-hook-form';
import { View, TextInput, TouchableOpacity, StyleSheet, Text } from 'react-native';
import Ionicons from '@expo/vector-icons/Ionicons';

export default function PasswordInput({ 
  control, 
  name, 
  errors, 
  showPassword, 
  toggleShow, 
  icon,
  placeholder
}) {
  return (
    <View style={styles.container}>
      <Controller
        control={control}
        name={name}
        render={({ field }) => (
          <View style={[styles.inputContainer, errors[name] && styles.errorBorder]}>
            {/* Render icon if provided */}
            {icon && <View style={styles.iconContainer}>{icon}</View>}
            
            <TextInput
              placeholder={placeholder}
              secureTextEntry={!showPassword}
              style={styles.input}
              placeholderTextColor="#989898"
              onChangeText={field.onChange}
            />
            <TouchableOpacity onPress={toggleShow}>
              <Ionicons 
                name={showPassword ? "eye-off" : "eye"} 
                size={24} 
                color="#9188E5" 
              />
            </TouchableOpacity>
          </View>
        )}
      />
      {errors[name] && <Text style={styles.errorText}>{errors[name].message}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    width: '80%',
    marginVertical: 12,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'gray',
    borderRadius: 10,
    paddingHorizontal: 12,
  },
  iconContainer: {
    marginRight: 10,
    marginLeft:4,
  },
  input: {
    flex: 1,
    height: 50,
    fontSize: 16,
    color: 'black',
  },
  errorBorder: {
    borderColor: 'red',
  },
  errorText: {
    color: 'red',
    fontSize: 12,
    marginTop: 4,
  }
});