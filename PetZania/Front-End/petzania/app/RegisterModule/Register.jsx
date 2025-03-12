import React from 'react';
import { TextInput, Text, ScrollView, StyleSheet, useWindowDimensions, TouchableOpacity, View, Alert } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import Ionicons from '@expo/vector-icons/Ionicons';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import FontAwesome5 from '@expo/vector-icons/FontAwesome5';
import { Link } from 'expo-router';
import Button from '@/components/Button';
import axios from 'axios';

export default function Register() {
  const { width } = useWindowDimensions();
  const leftEdge = width * 0.1;
  

  const [username, setUsername] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [confirmPassword, setConfirmPassword] = React.useState("");
  

  const [showPassword, setShowPassword] = React.useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = React.useState(false);
  const [checkBox, setCheckBox] = React.useState(false);

  const [errors, setErrors] = React.useState({
    username: "",
    email: "",
    password: "",
    confirmPassword: ""
  });
  
  const usernameRef = React.useRef(null);
  const emailRef = React.useRef(null);
  const passwordRef = React.useRef(null);
  const confirmPasswordRef = React.useRef(null);

  async function handleSubmit() {
    setErrors({
      username: "",
      email: "",
      password: "",
      confirmPassword: "",
      checkBox: ""
    });
    
    if(username.trim() === ""){
      setErrors(prev => ({...prev, username: "Username cannot be empty"}));
      usernameRef.current?.focus();
      return;
    }
    if(email.trim() === ""){
      setErrors(prev => ({...prev, email: "Email cannot be empty"}));
      emailRef.current?.focus();
      return;
    }
    if(password === ""){
      setErrors(prev => ({...prev, password: "Password cannot be empty"}));
      passwordRef.current?.focus();
      return;
    }
    if(confirmPassword === ""){
      setErrors(prev => ({...prev, confirmPassword: "Please confirm your password"}));
      confirmPasswordRef.current?.focus();
      return;
    }
    if(password !== confirmPassword){
      setErrors(prev => ({...prev, confirmPassword: "Passwords don't match"}));
      confirmPasswordRef.current?.focus();
      return;
    }
    if(checkBox === false){
      setErrors(prev => ({...prev, checkBox: "Please agree to the terms & conditions"}));
        return;
    }
    const user = { 
        "username":username, 
        "email":email, 
        "password":password };
    try {
      const response = await axios.post("YOUR_BACKEND_URL", user);
      Alert.alert("Success", "Account created successfully");
      // Handle success (e.g., navigate to another screen)
    } catch (error) {
      if(error.response && error.response.data){
        setErrors(prev => ({...prev, ...error.response.data}));
        // Focus on the first field with an error returned from backend
        if(error.response.data.username){
          usernameRef.current?.focus();
        } else if(error.response.data.email){
          emailRef.current?.focus();
        } else if(error.response.data.password){
          passwordRef.current?.focus();
        }
      } else {
        Alert.alert("Error", "An unexpected error occurred");
      }
    }
  }

  return (
    <SafeAreaView style={{ marginTop: '20%' }}>
      <Text style={[styles.title, { marginLeft: leftEdge }]}>Create account</Text>

      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.inputWrapper}>
          <TextInput
            ref={usernameRef}
            placeholder="Username"
            placeholderTextColor="#989898"
            onChangeText={setUsername}
            value={username}
            style={[styles.input, errors.username && styles.errorInput]}
          />
          {errors.username ? <Text style={styles.errorText}>{errors.username}</Text> : null}
        </View>

        <View style={styles.inputWrapper}>
          <TextInput
            ref={emailRef}
            placeholder="Email"
            placeholderTextColor="#989898"
            onChangeText={setEmail}
            value={email}
            style={[styles.input, errors.email && styles.errorInput]}
          />
          {errors.email ? <Text style={styles.errorText}>{errors.email}</Text> : null}
        </View>

        <View style={styles.inputWrapper}>
          <View style={[styles.inputContainer, errors.password && styles.errorInput]}>
            <TextInput
              ref={passwordRef}
              placeholder="Password"
              placeholderTextColor="#989898"
              onChangeText={setPassword}
              value={password}
              secureTextEntry={!showPassword}
              style={styles.inputField}
            />
            <TouchableOpacity onPress={() => setShowPassword(prev => !prev)}>
              <Ionicons 
                name={showPassword ? "eye-off-sharp" : "eye-sharp"} 
                size={24} 
                color="#9188E5" 
                style={styles.toggleText}
              />
            </TouchableOpacity>
          </View>
          {errors.password ? <Text style={styles.errorText}>{errors.password}</Text> : null}
        </View>

        <View style={styles.inputWrapper}>
          <View style={[styles.inputContainer, errors.confirmPassword && styles.errorInput]}>
            <TextInput
              ref={confirmPasswordRef}
              placeholder="Confirm Password"
              placeholderTextColor="#989898"
              onChangeText={setConfirmPassword}
              value={confirmPassword}
              secureTextEntry={!showConfirmPassword}
              style={styles.inputField}
            />
            <TouchableOpacity onPress={() => setShowConfirmPassword(prev => !prev)}>
              <Ionicons 
                name={showConfirmPassword ? "eye-off-sharp" : "eye-sharp"} 
                size={24} 
                color="#9188E5" 
                style={styles.toggleText}
              />
            </TouchableOpacity>
          </View>
          {errors.confirmPassword ? <Text style={styles.errorText}>{errors.confirmPassword}</Text> : null}
        </View>

        <View style={[styles.inputWrapper,{marginBottom: '10%'}]}>
            <View style={styles.checkBoxContainer}>
                <TouchableOpacity onPress={() => setCheckBox(prev => !prev)}>
                    <FontAwesome 
                    name={checkBox ? "check-square" : "square-o"} 
                    size={24} 
                    color="#9188E5"
                    width={24}
                    />
                </TouchableOpacity>
                <Text style={styles.textCheckBox}>
                    I agree to the <Link href={""} style={styles.LinkText}>Terms & Condition </Link>
                    and <Link href={""} style={styles.LinkText}>Privacy Terms</Link>
                </Text>
            </View>
            {errors.checkBox ? <Text style={styles.errorText}>{errors.checkBox}</Text> : null}
        </View>
        

        <Button 
          title="Sign Up" 
          borderRadius={10} 
          width="60%" 
          fontSize={18} 
          onPress={handleSubmit} 
        />

        <Text style={styles.text}>
          Already have an account? <Link href={""} style={styles.LinkText}>Login now</Link>
        </Text>

        <View style={styles.methodsContainer}>
          <Text style={styles.text}>Or sign in with</Text>
          <Button 
            title={<FontAwesome5 name="google" size={24} color="white" />} 
            borderRadius={8} 
            width="35%" 
            fontSize={18} 
            onPress={() => {console.log("Google sign up")}} 
          />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: '5%',
    width: '100%',
  },
  title: {
    color: '#9188E5',
    fontWeight: 'bold',
    fontSize: 48,
  },
  inputWrapper: {
    width: '80%',
    margin: 10,
  },
  input: {
    borderWidth: 1,
    borderColor: 'gray',
    borderRadius: 10,
    padding: 12,
    width: '100%',
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'gray',
    borderRadius: 10,
    paddingHorizontal: 12,
    width: '100%',
  },
  inputField: {
    flex: 1,
    paddingVertical: 12,
  },
  toggleText: {
    fontWeight: 'bold',
    paddingHorizontal: 10,
  },
  checkBoxContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    width: '80%',
  },
  textCheckBox: {
    color: 'gray',
    fontSize: 18,
    marginLeft: 5,

  },
  LinkText: {
    color: 'black',
    fontSize: 18,
    fontWeight: 'bold'
  },
  text: {
    color: 'gray',
    fontSize: 18,
    padding: 5,
    marginTop: '3%',
    marginBottom: '2%'
  },
  methodsContainer: {
    flexDirection: 'column',
    alignItems: 'center',
    width: '100%',
    margin: '3%'
  },
  errorInput: {
    borderColor: 'red'
  },
  errorText: {
    color: 'red',
    fontSize: 12,
    marginTop: 4,
    marginLeft: 4,
  },
});
