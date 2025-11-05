import { ScrollView, StyleSheet } from "react-native";
import PasswordInput from "@/components/PasswordInput";

import { useRouter, useLocalSearchParams } from "expo-router";
import Button from "@/components/Button";
import { useAuthForm } from "@/components/useForm";
import { responsive } from "@/utilities/responsive";
import FontAwesome from "@expo/vector-icons/FontAwesome";
import React, { useState, useEffect } from "react";

import Toast from "react-native-toast-message";

import { resetPassword } from "@/services/userService";

export default function ResetPasswordForm() {
    const { control, handleSubmit, formState: { errors, isSubmitting, isValid }, setError, watch } = useAuthForm("resetPassword");

    const [displayPassword, setDisplayPassword] = useState(false);
    const [displayConfirmPassword, setDisplayConfirmPassword] = useState(false);
    const [isFormComplete, setIsFormComplete] = useState(false);
    const { email, otp } = useLocalSearchParams();
    const router = useRouter();

    // Watch form values to determine if form is complete
    const watchedValues = watch();
    
    useEffect(() => {
        const { password, confirmPassword } = watchedValues;
        
        // Check if all required fields have values
        const allFieldsFilled = 
            password && password.length >= 8 && // Password minimum 8 chars
            confirmPassword && confirmPassword.length > 0;
        
        // Check if passwords match
        const passwordsMatch = password === confirmPassword;
        
        // Form is complete when all fields are filled, passwords match, AND form is valid (no errors)
        setIsFormComplete(allFieldsFilled && passwordsMatch && isValid);
    }, [watchedValues, isValid]);

    const showSuccessMessage = (message, description = '') => {
        Toast.show({
            type: 'success',
            text1: message,
            text2: description,
            position: 'top',
            visibilityTime: 3000,
            swipeable: true,
        });
    }

    const showErrorMessage = (message, description = '') => {
        Toast.show({
            type: 'error',
            text1: message,
            text2: description,
            position: 'top',
            visibilityTime: 3000,
            swipeable: true,
        });
    }

    const setNewPassword = async (data) => {
        const { password, confirmPassword } = data;

        // ✅ Check if passwords match BEFORE calling API
        if (password !== confirmPassword) {
            setError("confirmPassword", { type: "manual", message: "Passwords do not match." });
            return;
        }

        try {
            const response = await resetPassword(email, password, otp);

            if (response) {
                showSuccessMessage("Password Reset Successful", "You can now log in with your new password.");
                router.dismissAll();
                router.push("/RegisterModule/LoginScreen");
            }
            else {
                showErrorMessage("Password Reset Failed", "Please try again later.");
            }
        } catch (error) {

            const errorMsg = error.response?.data?.message || error.message;
            const field = errorMsg.toLowerCase().includes("password") ? "password" : null;
            showErrorMessage("Error", errorMsg);

            if (field) {
                setError(field, { type: "manual", message: errorMsg });
            } else {
                console.error("Error occurred:", errorMsg);
            }
        }
    };

    return (
        <ScrollView contentContainerStyle={styles.container}>
            <PasswordInput
                control={control}
                name="password"
                label={"Password"}
                icon={<FontAwesome name="lock" size={24} color="#9188E5"/>}
                errors={errors}
                maxLength={32}
                showPassword={displayPassword}
                toggleShow={() => setDisplayPassword(!displayPassword)}
            />
            <PasswordInput
                control={control}
                name="confirmPassword"
                label={"Confirm Password"}
                errors={errors}
                maxLength={32}
                icon={<FontAwesome name="lock" size={24} color="#9188E5"/>}
                showPassword={displayConfirmPassword}
                toggleShow={() => setDisplayConfirmPassword(!displayConfirmPassword)} 
            />
            <Button
                title="Reset Password"
                onPress={handleSubmit(setNewPassword)}
                borderRadius={12}
                fontSize={responsive.fonts.body}
                loading={isSubmitting}
                disabled={!isFormComplete || isSubmitting}
            />
        </ScrollView>
    );
}

const styles = StyleSheet.create({
    container: {
        alignSelf: 'center',
        width: '100%',
        gap: 16,
        paddingHorizontal: 20
    }
});