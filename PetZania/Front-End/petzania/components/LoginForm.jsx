import React, {useContext, useState, useEffect, useRef} from "react";
import { View , StyleSheet, Text } from 'react-native';
import { Link } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import FontAwesome from "@expo/vector-icons/FontAwesome";
import Toast from 'react-native-toast-message';

import Button from "@/components/Button";
import FormInput from "@/components/FormInput";
import PasswordInput from "@/components/PasswordInput";
import { responsive } from "@/utilities/responsive";
import { useAuthForm } from "@/components/useForm";


const { useRouter } = require("expo-router");

import { UserContext } from "@/context/UserContext";
import { PetContext } from "@/context/PetContext";

import { getUserById, loginUser, resendOTP } from "@/services/userService";

export default function LoginForm(){
    const {control , handleSubmit , formState:{errors , isSubmitting, isValid} , setError, clearErrors, watch} = useAuthForm("login");

    const [displayPassword, setDisplayPassword] = React.useState(false);
    const [generalError, setGeneralError] = useState("");
    const [isFormComplete, setIsFormComplete] = useState(false);

    // Create ref for password input navigation
    const passwordRef = useRef(null);

    const router = useRouter();

    const { setUser } = useContext(UserContext);
    const { setPets } = useContext(PetContext);

    // Watch form values to enable/disable submit button
    const watchedValues = watch();
    
    useEffect(() => {
        const { email, password } = watchedValues;
        
        // Check if all required fields are filled and valid
        const allFieldsFilled = 
            email && email.trim().length > 0 &&
            password && password.length > 0;
        
        // Form is complete when all fields are filled AND form is valid (no errors)
        setIsFormComplete(allFieldsFilled && isValid);
    }, [watchedValues, isValid]);

    const Login = async (data) => {
        data.email = data.email.toLowerCase();

        setGeneralError("");
        clearErrors();
        try {
            const response = await loginUser(data);

            if (response) {
                const userData = await getUserById(response.userId);
                setUser(userData);
                setPets(userData.myPets || []);
                if (userData.name === null) {
                    router.push("/RegisterModule/ProfileSetUp1");
                } else {
                    router.replace("/Home");
                }
            }
        } catch (error) {
            const status = error.response?.status;
            const errorMsg = error.response?.data?.error || error.message;

            if (status === 400 || errorMsg === "Email is incorrect") {
                setGeneralError("Invalid email or password. Please check your credentials and try again.");
            }
            else if (status === 401) {
                // Check if it's a verification issue vs authentication issue
                if (errorMsg === "Unauthorized" && (error.response?.data?.message === "There is no refresh token sent" || error.response?.data?.message === "User is not verified")) {
                    try {
                        await resendOTP(data.email);

                        Toast.show({
                            type: "success",
                            text1: "OTP sent",
                            text2: `A verification code has been sent to ${data.email}`,
                        });

                        router.push({
                            pathname: "/RegisterModule/OTPVerificationScreen",
                            params: { isRegister: true, email: data.email },
                        });
                    } catch (otpError) {
                        setError("email", {
                            type: "manual",
                            message: otpError.response?.data?.message || "Failed to send OTP. Please try again later.",
                        });
                    }
                } else {
                    // General unauthorized error
                    setGeneralError("Invalid email or password. Please check your credentials and try again.");
                }
            } else if (status === 429) {
                setGeneralError("Too many login attempts. Please wait a moment before trying again.");
            } else {
                const field = errorMsg.toLowerCase().includes("email")
                    ? "email"
                    : errorMsg.toLowerCase().includes("password")
                    ? "password"
                    : null;

                if (field) {
                    setError(field, { type: "manual", message: errorMsg });
                } else {
                    setGeneralError(errorMsg || "An unexpected error occurred. Please try again.");
                }
            }
        }
    };

    return(
        <View style = {styles.container}>
            {/* General error message displayed at the top */}
            {generalError ? (
                <View style={styles.errorContainer}>
                    <Text style={styles.generalErrorText}>{generalError}</Text>
                </View>
            ) : null}

            <FormInput
                control={control}
                name="email"
                maxLength={100}
                errors={errors}
                label={"Email"}
                placeholder={"example@example.com"}
                keyboardType="email-address"
                autoCapitalize="none"
                icon={<MaterialIcons name="email" size={24} color="#8188E5" />}
                returnKeyType="next"
                onSubmitEditing={() => passwordRef.current?.focus()}
            />
            <PasswordInput
                control={control}
                name="password"
                maxLength={32}
                errors={errors}
                showPassword={displayPassword}
                toggleShow={() => setDisplayPassword(!displayPassword)}
                label={"Password"}
                icon={<FontAwesome name="lock" size={24} color="#9188E5"/>}
                returnKeyType="done"
                inputRef={passwordRef}
                onSubmitEditing={() => {
                    if (isFormComplete) {
                        handleSubmit(Login)();
                    }
                }}
            />

            <Link href="/RegisterModule/ForgotPasswordScreen" style={styles.link}> Forgot Password? </Link>

            <Button
                title="Login"
                onPress={handleSubmit(Login)}
                borderRadius={12}
                fontSize={responsive.fonts.body}
                loading={isSubmitting}
                disabled={!isFormComplete || isSubmitting}
            />
        </View>
    )
}
const styles = StyleSheet.create({
    container: {
        gap: responsive.hp('2%'),
        width: responsive.wp('100%'),
        paddingHorizontal: '5%',
    },
    errorContainer: {
        backgroundColor: '#FFEBEE',
        borderLeftWidth: 4,
        borderLeftColor: '#F44336',
        paddingHorizontal: 12,
        paddingVertical: 8,
        borderRadius: 6,
        marginBottom: 8,
    },
    generalErrorText: {
        color: '#C62828',
        fontSize: 14,
        fontFamily: 'Inter-Medium',
    },
    link: {
      color: '#9188E5',
      fontWeight: 'bold',
      alignSelf: 'flex-end',
      fontSize: responsive.fonts.small,
    },
    button: {
      borderRadius: responsive.buttons.radius,
    },
  });