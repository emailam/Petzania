import Ionicons from '@expo/vector-icons/Ionicons';
import React from "react";
import Button from "@/components/Button";
import { useRouter } from "expo-router";
import FormInput from "@/components/FormInput";
import { useAuthForm } from "@/components/useForm";
import { ScrollView , StyleSheet, Text } from "react-native"
import { responsive } from "@/utilities/responsive";

import { sendResetPasswordOTP } from '@/services/userService';

export default function ForgotPasswordForm() {
    const {control , handleSubmit , formState:{errors , isSubmitting} , setError} = useAuthForm("forgotPassword");
    const router = useRouter();

    const goToRegisterScreen = () => {
      router.replace('/RegisterModule/RegisterScreen');
    }

    const SendResetPasswordOTP = async (data) => {
      try {
        const response = await sendResetPasswordOTP(data.email);

        if (response) {
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
                onPress={handleSubmit(SendResetPasswordOTP)}
                width={responsive.buttons.width.primary}
                borderRadius={12}
                fontSize={responsive.fonts.body}
                loading={isSubmitting}
            />

            <Text style={styles.link} onPress={goToRegisterScreen}>Create an account</Text>
        </ScrollView>
    )
}
const styles = StyleSheet.create({
  link: {
    color: '#9188E5',
    fontWeight: 'bold',
    fontSize: 14,
  },
  container: {
    alignSelf: 'center',
    width: '100%',
    gap: 16,
    paddingHorizontal: 20
  }
});