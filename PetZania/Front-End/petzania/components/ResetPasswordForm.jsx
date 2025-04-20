import { ScrollView } from "react-native";
import PasswordInput from "@/components/PasswordInput";
import axios from "axios";
import { useRouter , useLocalSearchParams } from "expo-router";
import Button from "@/components/Button";
import { useAuthForm } from "@/components/useForm";
import {StyleSheet} from "react-native"
import React from "react";
import { responsive } from "@/utilities/responsive";
export default function ResetPasswordForm(){
    const {control , handleSubmit , formState:{errors , isSubmitting} , setError} = useAuthForm("resetPassword");

    const [displayPassword, setDisplayPassword] = React.useState(false);
    const [displayConfirmPassword, setDisplayConfirmPassword] = React.useState(false);
    const {email} = useLocalSearchParams();
    const router = useRouter();
    const setNewPassword = async (data) => {
      try {
        const response = await axios.post("http://localhost:8080/api/user/auth/reset-password", {email: email, password: data.password});
        
        if (response.status === 200) {
          // Show Sucess Message
          router.replace("/RegisterModule/LoginScreen");
        } else {
          // If response status is not 200, treat it as an error.
          console.error("Unexpected response status:", response.status);
        }
      } catch (error) {
        // Extract error message from the API response or fallback to error.message.
        const errorMsg = error.response?.data?.message || error.message;
        
        // Determine if the error relates to the password field.
        const field = errorMsg.toLowerCase().includes("password") ? "password" : null;
        
        if (field) {
          setError(field, { type: "manual", message: errorMsg });
        } else {
          // Log error if it's not field-specific.
          console.error("Error occurred:", errorMsg);
        }
      }
    };
    return(
        <ScrollView contentContainerStyle={styles.container}>
                  
            <PasswordInput
                    control={control}
                    name="password"
                    errors={errors}
                    showPassword={displayPassword}
                    placeholder="Enter your new password"
                    toggleShow={() => setDisplayPassword(!displayPassword)}
                />
    
            <PasswordInput
                    control={control}
                    name="confirmPassword"
                    errors={errors}
                    placeholder = "Confirm your new password"
                    showPassword={displayConfirmPassword}
                    toggleShow={() => setDisplayConfirmPassword(!displayPassword)}
                />
                
            <Button
                title="Reset Password"
                onPress={handleSubmit(setNewPassword)}
                borderRadius={12}
                fontSize={responsive.fonts.body}
                loading={isSubmitting}
            />
        </ScrollView>
    )
}
const styles = StyleSheet.create({
  container: {
    width: "80%",
    alignSelf: "center",
    gap: responsive.hp("2%"),
  }
});