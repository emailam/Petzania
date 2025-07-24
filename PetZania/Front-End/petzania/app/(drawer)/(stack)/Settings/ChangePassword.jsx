import React, { useState, useContext } from 'react';
import { View, Text, StyleSheet, TextInput, KeyboardAvoidingView, Platform, ScrollView, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import Toast from 'react-native-toast-message';
import { UserContext } from '@/context/UserContext';
import { changePassword } from '@/services/userService';
import Button from '@/components/Button';

export default function ChangePasswordScreen() {
  const router = useRouter();
  const { user } = useContext(UserContext);
  
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChangePassword = async () => {
    setError('');
    
    // Validation
    if (!newPassword.trim()) {
      setError('New password is required');
      return;
    }
    
    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters long');
      return;
    }
    
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    
    setLoading(true);
    
    try {      
      // Call the changePassword service
      await changePassword(user?.email, newPassword);
      
      Toast.show({
        type: 'success',
        text1: 'Password Changed',
        text2: 'Your password has been updated successfully',
        position: 'top',
        visibilityTime: 3000,
      });
      
      // Navigate back to settings
      router.back();
      
    } catch (error) {
      console.error('Change password error:', error);
      
      let errorMessage = 'Failed to change password. Please try again.';
      if (error.response?.status === 400) {
        errorMessage = 'Invalid password format';
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      setError(errorMessage);
      
      Toast.show({
        type: 'error',
        text1: 'Error',
        text2: errorMessage,
        position: 'top',
        visibilityTime: 3000,
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={styles.container}
    >
      <ScrollView
        contentContainerStyle={styles.scrollContainer}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.inputContainer}>
          <Text style={styles.label}>
            New Password <Text style={{ fontSize: 18, color: 'red' }}>*</Text>
          </Text>
          <View style={styles.passwordContainer}>
            <TextInput
              style={[styles.input, error ? styles.inputError : null]}
              placeholder="Enter your new password"
              placeholderTextColor="#999"
              value={newPassword}
              onChangeText={(text) => {
                setNewPassword(text);
                setError('');
              }}
              secureTextEntry={!showNewPassword}
              returnKeyType="next"
            />
            <TouchableOpacity
              style={styles.eyeIcon}
              onPress={() => setShowNewPassword(!showNewPassword)}
            >
              <Ionicons
                name={showNewPassword ? 'eye-off' : 'eye'}
                size={24}
                color="#9188E5"
              />
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.inputContainer}>
          <Text style={styles.label}>
            Confirm Password
            <Text style={{ fontSize: 18, color: 'red' }}>*</Text>
          </Text>
          <View style={styles.passwordContainer}>
            <TextInput
              style={[styles.input, error ? styles.inputError : null]}
              placeholder="Confirm your new password"
              placeholderTextColor="#999"
              value={confirmPassword}
              onChangeText={(text) => {
                setConfirmPassword(text);
                setError('');
              }}
              secureTextEntry={!showConfirmPassword}
              returnKeyType="done"
            />
            <TouchableOpacity
              style={styles.eyeIcon}
              onPress={() => setShowConfirmPassword(!showConfirmPassword)}
            >
              <Ionicons
                name={showConfirmPassword ? 'eye-off' : 'eye'}
                size={24}
                color="#9188E5"
              />
            </TouchableOpacity>
          </View>
        </View>

        {/* Password Requirements */}
        <View style={styles.requirementsContainer}>
          <Text style={styles.requirementsTitle}>Password Requirements:</Text>
          <View style={styles.requirement}>
            <Ionicons
              name={newPassword && newPassword.length >= 8 ? 'checkmark-circle' : 'radio-button-off'}
              size={16}
              color={newPassword && newPassword.length >= 8 ? '#4CAF50' : '#ccc'}
            />
            <Text style={[
              styles.requirementText,
              newPassword && newPassword.length >= 8 && styles.requirementMet
            ]}>
              At least 8 characters
            </Text>
          </View>
          <View style={styles.requirement}>
            <Ionicons
              name={newPassword && /[A-Z]/.test(newPassword) ? 'checkmark-circle' : 'radio-button-off'}
              size={16}
              color={newPassword && /[A-Z]/.test(newPassword) ? '#4CAF50' : '#ccc'}
            />
            <Text style={[
              styles.requirementText,
              newPassword && /[A-Z]/.test(newPassword) && styles.requirementMet
            ]}>
              One uppercase letter
            </Text>
          </View>
          <View style={styles.requirement}>
            <Ionicons
              name={newPassword && /[a-z]/.test(newPassword) ? 'checkmark-circle' : 'radio-button-off'}
              size={16}
              color={newPassword && /[a-z]/.test(newPassword) ? '#4CAF50' : '#ccc'}
            />
            <Text style={[
              styles.requirementText,
              newPassword && /[a-z]/.test(newPassword) && styles.requirementMet
            ]}>
              One lowercase letter
            </Text>
          </View>
          <View style={styles.requirement}>
            <Ionicons
              name={newPassword && /\d/.test(newPassword) ? 'checkmark-circle' : 'radio-button-off'}
              size={16}
              color={newPassword && /\d/.test(newPassword) ? '#4CAF50' : '#ccc'}
            />
            <Text style={[
              styles.requirementText,
              newPassword && /\d/.test(newPassword) && styles.requirementMet
            ]}>
              One number
            </Text>
          </View>
          <View style={styles.requirement}>
            <Ionicons
              name={newPassword && /[!@#$%^&*(),.?":{}|<>]/.test(newPassword) ? 'checkmark-circle' : 'radio-button-off'}
              size={16}
              color={newPassword && /[!@#$%^&*(),.?":{}|<>]/.test(newPassword) ? '#4CAF50' : '#ccc'}
            />
            <Text style={[
              styles.requirementText,
              newPassword && /[!@#$%^&*(),.?":{}|<>]/.test(newPassword) && styles.requirementMet
            ]}>
              One special character
            </Text>
          </View>
          <View style={styles.requirement}>
            <Ionicons
              name={newPassword && confirmPassword && newPassword === confirmPassword ? 'checkmark-circle' : 'radio-button-off'}
              size={16}
              color={newPassword && confirmPassword && newPassword === confirmPassword ? '#4CAF50' : '#ccc'}
            />
            <Text style={[
              styles.requirementText,
              newPassword && confirmPassword && newPassword === confirmPassword && styles.requirementMet
            ]}>
              Passwords match
            </Text>
          </View>
        </View>
      </ScrollView>

      <View style={styles.buttonContainer}>
        {error ? <Text style={styles.errorText}>{error}</Text> : null}
        <Button 
          title="Change Password" 
          borderRadius={10} 
          fontSize={16} 
          onPress={handleChangePassword} 
          loading={loading} 
        />
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  scrollContainer: {
    paddingVertical: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  inputContainer: {
    paddingHorizontal: '5%',
    width: '100%',
    marginBottom: 20,
  },
  label: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#333',
  },
  passwordContainer: {
    position: 'relative',
    width: '100%',
  },
  input: {
    width: '100%',
    height: 50,
    borderWidth: 1,
    borderColor: '#9188E5',
    borderRadius: 10,
    paddingHorizontal: 16,
    paddingRight: 50,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  inputError: {
    borderColor: 'red',
  },
  eyeIcon: {
    position: 'absolute',
    right: 15,
    top: 13,
  },
  requirementsContainer: {
    marginHorizontal: '5%',
    marginBottom: 20,
    padding: 16,
    backgroundColor: '#f8f9fa',
    borderRadius: 10,
    width: '90%',
  },
  requirementsTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  requirement: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  requirementText: {
    fontSize: 14,
    color: '#666',
    marginLeft: 8,
  },
  requirementMet: {
    color: '#4CAF50',
  },
  buttonContainer: {
    padding: 20,
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
    backgroundColor: '#f5f5f5',
  },
  errorText: {
    color: 'red',
    fontSize: 14,
    marginBottom: 10,
    textAlign: 'center',
  },
});
