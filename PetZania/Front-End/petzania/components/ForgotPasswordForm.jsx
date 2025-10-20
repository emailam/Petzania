
import React from "react";
import Button from "@/components/Button";
import { useRouter } from "expo-router";
import FormInput from "@/components/FormInput";
import { useAuthForm } from "@/components/useForm";
import { ScrollView , StyleSheet, Text } from "react-native"
import { responsive } from "@/utilities/responsive";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { sendResetPasswordOTP } from '@/services/userService';

export default function ForgotPasswordForm() {
    const {control , handleSubmit , formState:{errors , isSubmitting, isValid}, watch, setError} = useAuthForm("forgotPassword");
    const router = useRouter();

    // Watch the email field to check if it has content
    const email = watch("email");

    // Check if form is complete and valid (using Paper's built-in validation)
    const isComplete = email && email.trim() !== '' && !errors.email && isValid;

    const goToRegisterScreen = () => {
      router.replace('/RegisterModule/RegisterScreen');
    }

    const SendResetPasswordOTP = async (data) => {
      try {
        data.email = data.email.toLowerCase();
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
        if (error.status === 404) {
          setError("email", {
            type: "manual",
            message: "Email not found. Please check your email address or create an account.",
          });
        } else {
          setError("email", {
            type: "manual",
            message: error.message || "Network error. Please try again later.",
          });
        }
      }
    };

    return(
        <ScrollView contentContainerStyle={styles.container}>
            <FormInput
              control={control}
              name="email"
              errors={errors}
              placeholder="example@example.com"
              label={"Email"}
              keyboardType="email-address"
              autoCapitalize="none"
              maxLength={100}
              returnKeyType="done"
              onSubmitEditing={handleSubmit(SendResetPasswordOTP)}
              icon={<MaterialIcons name="email" size={24} color="#9188E5" />}
            />
            <Button
              title="Reset Password"
              onPress={handleSubmit(SendResetPasswordOTP)}
              width={responsive.buttons.width.primary}
              borderRadius={12}
              fontSize={responsive.fonts.body}
              loading={isSubmitting}
              disabled={isSubmitting || !isComplete}
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