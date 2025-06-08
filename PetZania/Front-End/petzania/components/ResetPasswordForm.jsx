import { ScrollView, StyleSheet } from "react-native";
import PasswordInput from "@/components/PasswordInput";

import { useRouter, useLocalSearchParams } from "expo-router";
import Button from "@/components/Button";
import { useAuthForm } from "@/components/useForm";
import { responsive } from "@/utilities/responsive";
import React from "react";

import Toast from "react-native-toast-message";

import { resetPassword } from "@/services/userService";

export default function ResetPasswordForm() {
    const { control, handleSubmit, formState: { errors, isSubmitting }, setError, getValues } = useAuthForm("resetPassword");

    const [displayPassword, setDisplayPassword] = React.useState(false);
    const [displayConfirmPassword, setDisplayConfirmPassword] = React.useState(false);
    const { email, otp } = useLocalSearchParams();
    const router = useRouter();

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
                errors={errors}
                showPassword={displayPassword}
                placeholder="Enter your new password"
                toggleShow={() => setDisplayPassword(!displayPassword)}
            />
            <PasswordInput
                control={control}
                name="confirmPassword"
                errors={errors}
                placeholder="Confirm your new password"
                showPassword={displayConfirmPassword}
                toggleShow={() => setDisplayConfirmPassword(!displayConfirmPassword)} 
            />
            <Button
                title="Reset Password"
                onPress={handleSubmit(setNewPassword)}
                borderRadius={12}
                fontSize={responsive.fonts.body}
                loading={isSubmitting}
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