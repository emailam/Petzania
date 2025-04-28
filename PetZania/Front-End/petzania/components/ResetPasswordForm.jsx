import { ScrollView, StyleSheet } from "react-native";
import PasswordInput from "@/components/PasswordInput";
import axios from "axios";
import { useRouter, useLocalSearchParams } from "expo-router";
import Button from "@/components/Button";
import { useAuthForm } from "@/components/useForm";
import { responsive } from "@/utilities/responsive";
import React from "react";

export default function ResetPasswordForm() {
    const { control, handleSubmit, formState: { errors, isSubmitting }, setError, getValues } = useAuthForm("resetPassword");

    const [displayPassword, setDisplayPassword] = React.useState(false);
    const [displayConfirmPassword, setDisplayConfirmPassword] = React.useState(false);
    const { email, otp } = useLocalSearchParams();
    const router = useRouter();

    const setNewPassword = async (data) => {
        const { password, confirmPassword } = data;

        // ✅ Check if passwords match BEFORE calling API
        if (password !== confirmPassword) {
            setError("confirmPassword", { type: "manual", message: "Passwords do not match." });
            return;
        }

        try {
            const response = await axios.put("http://192.168.1.4:8080/api/user/auth/resetPassword", {
                email,
                password,
                otp
            });

            if (response.status === 200) {
                router.replace("/RegisterModule/LoginScreen");
            } else {
                console.error("Unexpected response status:", response.status);
            }
        } catch (error) {
            const errorMsg = error.response?.data?.message || error.message;
            const field = errorMsg.toLowerCase().includes("password") ? "password" : null;

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
                toggleShow={() => setDisplayConfirmPassword(!displayConfirmPassword)} // 🔥 Fixed this line
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
        width: "80%",
        alignSelf: "center",
        gap: responsive.hp("2%"),
    },
});