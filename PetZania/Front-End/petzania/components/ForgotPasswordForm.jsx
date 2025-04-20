import Ionicons from '@expo/vector-icons/Ionicons';
import React from "react";
import Button from "@/components/Button";
import { Link, useRouter } from "expo-router";
import FormInput from "@/components/FormInput";
import { useAuthForm } from "@/components/useForm";
import axios from "axios";
import { ScrollView , StyleSheet } from "react-native"
import { responsive } from "@/utilities/responsive";

export default function ForgotPasswordForm() {
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
        <ScrollView contentContainerStyle={styles.container}>
                  
            <FormInput
                control={control}
                name="email"
                errors={errors}
                placeholder="example@gmail.com"
                icon={<Ionicons name="person" size={24} color="#8188E5" />}
            />
            
            
            <Button
                title="Reset Password"
                onPress={handleSubmit(ResetPassword)}
                width={responsive.buttons.width.primary}
                borderRadius={12}
                fontSize={responsive.fonts.body}
                loading={isSubmitting}
            />
            
            <Link href={""} style={styles.link}>Create an account </Link>
        </ScrollView>
    )
}
const styles = StyleSheet.create({

  link: {
    color: '#9188E5',
    fontWeight: 'bold',
    fontSize: responsive.fonts.small,
  },
  container: {
    alignSelf: 'center',
    width: '80%',
    gap: responsive.hp('2%')
  }
});