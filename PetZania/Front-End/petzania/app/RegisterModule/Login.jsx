import React from "react";
import {
  TextInput,
  Text,
  ScrollView,
  StyleSheet,
  useWindowDimensions,
  TouchableOpacity,
  View,
  Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import Ionicons from "@expo/vector-icons/Ionicons";
import FontAwesome5 from "@expo/vector-icons/FontAwesome5";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import FontAwesome from "@expo/vector-icons/FontAwesome";
import { Link } from "expo-router";
import Button from "@/components/Button";
import axios from "axios";

export default function Login() {
  const { width } = useWindowDimensions();
  const leftEdge = width * 0.1;

  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");

  const [showPassword, setShowPassword] = React.useState(false);

  const [errors, setErrors] = React.useState({
    email: "",
    password: "",
  });

  const emailRef = React.useRef(null);
  const passwordRef = React.useRef(null);

  async function handleSubmit() {
    setErrors({
      email: "",
      password: "",
    });

    if (email.trim() === "") {
      setErrors((prev) => ({ ...prev, email: "Email cannot be empty" }));
      emailRef.current?.focus();
      return;
    }
    if (password === "") {
      setErrors((prev) => ({ ...prev, password: "Password cannot be empty" }));
      passwordRef.current?.focus();
      return;
    }

    const user = {
      email: email,
      password: password,
    };
    try {
      const response = await axios.post("YOUR_BACKEND_URL", user);
      Alert.alert("Success", "Account created successfully");
      // Handle success (e.g., navigate to another screen)
    } catch (error) {
      if (error.response && error.response.data) {
        setErrors((prev) => ({ ...prev, ...error.response.data }));
        // Focus on the first field with an error returned from backend
        if (error.response.data.email) {
          emailRef.current?.focus();
        } else if (error.response.data.password) {
          passwordRef.current?.focus();
        }
      } else {
        Alert.alert("Error", "An unexpected error occurred");
      }
    }
  }

  return (
    <SafeAreaView style={{ marginTop: "20%" }}>
      <Text style={[styles.title, { marginLeft: leftEdge }]}>Welcome Back</Text>

      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.emptySpace}>
          <View style={styles.inputWrapper}>
            <View
              style={[styles.inputContainer, errors.email && styles.errorInput]}
            >
              <MaterialIcons
                name="email"
                size={24}
                width={24}
                color="#8188E5"
              />
              <TextInput
                ref={emailRef}
                placeholder="Email"
                placeholderTextColor="#989898"
                onChangeText={setEmail}
                value={email}
                style={styles.inputField}
              />
            </View>
            {errors.email ? (
              <Text style={styles.errorText}>{errors.email}</Text>
            ) : null}
          </View>
          <View style={styles.inputWrapper}>
            <View
              style={[
                styles.inputContainer,
                errors.password && styles.errorInput,
              ]}
            >
              <FontAwesome
                name="lock"
                size={24}
                width={18}
                marginLeft={5}
                color="#9188E5"
              />
              <TextInput
                ref={passwordRef}
                placeholder="Password"
                placeholderTextColor="#989898"
                onChangeText={setPassword}
                value={password}
                secureTextEntry={!showPassword}
                style={styles.inputField}
              />
              <TouchableOpacity
                onPress={() => setShowPassword((prev) => !prev)}
              >
                <Ionicons
                  name={showPassword ? "eye-off-sharp" : "eye-sharp"}
                  size={24}
                  color="#9188E5"
                  style={styles.toggleText}
                />
              </TouchableOpacity>
            </View>
            <View style={styles.linkContainer}>
              {errors.password ? (
                <Text style={styles.errorText}>{errors.password}</Text>
              ) : <Text></Text>}
              <Link href={""} style={styles.link}>
                Forgot password?
              </Link>
            </View>
          </View>
        </View>
        <Button
          title="Log in"
          borderRadius={10}
          width="60%"
          fontSize={18}
          onPress={handleSubmit}
        />

        <Text style={styles.text}>
          Don't have an account?{" "}
          <Link href={""} style={styles.LinkText}>
            Sign up now
          </Link>
        </Text>

        <View style={styles.methodsContainer}>
          <Text style={styles.text}>Or sign in with</Text>
          <Button
            title={<FontAwesome5 name="google" size={24} color="white" />}
            borderRadius={8}
            width="35%"
            fontSize={18}
            onPress={() => {
              console.log("Google sign up");
            }}
          />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: "column",
    justifyContent: "center",
    alignItems: "center",
    marginTop: "5%",
    width: "100%",
  },
  emptySpace: {
    marginBottom: "8%",
  },

  linkContainer: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  link: {
    color: "#9188E5",
    fontWeight: "bold",
    fontSize: 16,
    marginTop: 4,
  },
  title: {
    color: "#9188E5",
    fontWeight: "bold",
    fontSize: 48,
  },
  inputWrapper: {
    width: "80%",
    height: 80,
  },
  input: {
    borderWidth: 1,
    borderColor: "gray",
    borderRadius: 10,
    padding: 12,
    width: "100%",
  },
  inputContainer: {
    flexDirection: "row",
    alignItems: "center",
    borderWidth: 1,
    borderColor: "gray",
    borderRadius: 10,
    paddingHorizontal: 12,
    width: "100%",
  },
  inputField: {
    flex: 1,
    paddingVertical: 12,
  },
  toggleText: {
    fontWeight: "bold",
    paddingHorizontal: 10,
  },
  LinkText: {
    color: "black",
    fontSize: 18,
    fontWeight: "bold",
  },
  text: {
    color: "gray",
    fontSize: 18,
    padding: 5,
    marginTop: "3%",
    marginBottom: "2%",
  },
  methodsContainer: {
    flexDirection: "column",
    alignItems: "center",
    width: "100%",
    margin: "3%",
  },
  errorInput: {
    borderColor: "red",
  },
  errorText: {
    color: "red",
    fontSize: 12,
    marginTop: 4,
    marginLeft: 4,
  },
});
