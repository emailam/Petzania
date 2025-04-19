import React from "react";
import { ScrollView, View, Text, Alert, StyleSheet } from 'react-native';
import { SafeAreaView } from "react-native-safe-area-context";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import FontAwesome from "@expo/vector-icons/FontAwesome";
import { Link } from "expo-router";
import Button from "@/components/Button";
import FormInput from "@/components/FormInput";
import PasswordInput from "@/components/PasswordInput";
import { responsive } from "@/utilities/responsive";
import {useAuthForm} from "@/components/useForm";
import ExternalSignIn from "@/components/ExternalSignIn";
import axios from "axios";
export default function LoginScreen() {

  const {control , handleSubmit , formState:{errors , isSubmitting} , setError} = useAuthForm("login");

  const [displayPassword, setDisplayPassword] = React.useState(false);

  const Login = async (data) => {
    try {

      const response = await axios.post("http://localhost:8080/api/user/auth/login", data);

      if (response.ok) {
        // Redirect to the HomePage screen.
        // To be implemented
      } 
    } catch (error) {
      const errorMsg = error.response?.data?.message || error.message;
      
      const field = errorMsg.toLowerCase().includes('email') ? 'email' :
                    errorMsg.toLowerCase().includes('password') ? 'password' : null;
      
      if (field) {
        setError(field, { type: 'manual', message: errorMsg });
      } else {
        Alert.alert('Error', errorMsg);
      }
    }
  }
    return (
      <SafeAreaView style={styles.safeArea}>
        <Text style={styles.title}>PetZania</Text>
  
        <ScrollView contentContainerStyle={styles.container}>
          
            <FormInput
              control={control}
              name="email"
              errors={errors}
              placeholder="Email"
              icon={<MaterialIcons name="email" size={24} color="#8188E5" />}
            />
          
            <PasswordInput
              control={control}
              name="password"
              errors={errors}
              showPassword={displayPassword}
              toggleShow={() => setDisplayPassword(!displayPassword)}
              placeholder="Password"
              icon={  
                <FontAwesome name="lock" size={24} color="#9188E5"/>}
            />
          
          <Link href="/forgot-password" style={styles.link}> Forgot Password? </Link>
          
          <View style = {styles.subContainer}>
            <Button
                title="Login"
                onPress={handleSubmit(Login)}
                width={styles.button.width}
                borderRadius={12}
                fontSize={responsive.fonts.body}
                loading={isSubmitting}
            />

            <Text style={styles.text}> Don't have an account?{" "}
              <Link href={""} style={styles.link}> Sign up now </Link>
            </Text>

          <ExternalSignIn/>
        </View>
        </ScrollView>
      </SafeAreaView>
    );
}


const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    paddingTop: responsive.hp('12%'),
    backgroundColor: 'white',
  },
  title: {
    fontSize: responsive.fonts.title,
    color: '#9188E5',
    fontWeight: 'bold',
    marginLeft: responsive.margins.screenEdge + 20,
    marginBottom: responsive.hp('1%'),
  },
  container: {
    flexGrow: 1,
    alignItems: 'center',
    width: responsive.wp('100%'),  
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 12,
    paddingHorizontal: 12,
    width: responsive.wp('100%'),
    marginVertical: responsive.hp('1%'),
    marginLeft: responsive.margins.screenEdge ,
  },
  link: {
    color: '#9188E5',
    fontWeight: 'bold',
    alignSelf: 'flex-end',
    marginRight: responsive.margins.screenEdge + 30,
    fontSize: responsive.fonts.small,
  },
  icon: {
    marginRight: responsive.wp('3%'),
  },
  button: {
    marginTop: responsive.hp('3%'),
    width: responsive.buttons.width.primary,
    borderRadius: responsive.buttons.radius,
  },
  subContainer: {
    marginTop: responsive.hp('5%'),
    width: responsive.wp('100%'),
    alignItems: 'center',
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
    width: responsive.wp('100%'),
    margin: "3%",
  },
});
