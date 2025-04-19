import { View , Text , StyleSheet, ScrollView } from "react-native"
import Ionicons from '@expo/vector-icons/Ionicons';
import React from "react";
import Button from "@/components/Button";
import { Link, useRouter} from "expo-router";
import FormInput from "@/components/FormInput";
import { responsive } from "@/utilities/responsive";
import { SafeAreaView } from "react-native-safe-area-context";
import { useAuthForm } from "@/components/useForm";
import axios from "axios";
export default function ForgotPasswordScreen(){
    
    const {control , handleSubmit , formState:{errors , isSubmitting} , setError} = useAuthForm("forgotPassword");
    const router = useRouter();
    const ResetPassword = async (data) => {
      try {
        const response = await axios.post("https://api.example.com/reset-password", {
          email: data.email,
        });
        
        if (response.ok) {
          // Show sucess Message
          router.push({
            pathname: "/RegisterModule/OTPVerificationScreen",
            params: { email: data.email, isRegister : false }, 
          });
          
        } else {
          setError("email", {
            type: "manual",
            message: response.json().message || "No user found with this email",
          });
        }
      } catch (error) {
        // Handle network or other unexpected errors.
        console.error("Error sending reset password request:", error);
        setError("email", {
          type: "manual",
          message: "Network error. Please try again later.",
        });
      }
    };
    
    return(
      <SafeAreaView style={styles.safeArea}>
        <Text style={styles.title}>Forgot Password?</Text>
        <Text style = {styles.text}>Enter your email address to get the password reset link</Text>
        <ScrollView contentContainerStyle={styles.container}>
          
          <FormInput
              control={control}
              name="email"
              errors={errors}
              placeholder="example@gmail.com"
              icon={<Ionicons name="person" size={24} color="#8188E5" />}
          />
          
          <View style = {styles.subContainer}>
            <Button
                title="Reset Password"
                onPress={handleSubmit(ResetPassword)}
                width={styles.button.width}
                borderRadius={12}
                fontSize={responsive.fonts.body}
                loading={isSubmitting}
            />
        </View>
        <Link href={""} style={styles.link}>Create an account </Link>
      </ScrollView>
    </SafeAreaView>
    )
}
const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    paddingTop: responsive.hp('12%'),
    backgroundColor: 'white',
    alignItems: 'left',
  },
  link: {
    color: '#9188E5',
    fontWeight: 'bold',
    fontSize: responsive.fonts.small,
    marginTop: responsive.hp('5%'),
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
    width: '100%',    
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
    width: '100%',
    alignItems: 'center',
  },
  text: {
    color: "gray",
    fontSize: responsive.fonts.small,
    marginBottom: responsive.hp('2%'),
    marginLeft: responsive.margins.screenEdge + 25,
    width : "60%",

  },
  methodsContainer: {
    flexDirection: "column",
    alignItems: "center",
    width: "100%",
    margin: "3%",
  },
});