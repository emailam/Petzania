import React, {useContext} from "react";
import { View , StyleSheet } from 'react-native';
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
    const {control , handleSubmit , formState:{errors , isSubmitting} , setError} = useAuthForm("login");

    const [displayPassword, setDisplayPassword] = React.useState(false);

    const router = useRouter();

    const { setUser } = useContext(UserContext);
    const { setPets } = useContext(PetContext);

    const Login = async (data) => {
        try {
            const response = await loginUser(data);
            console.log("Login response:", response);

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
            console.log("Error status code:", status);
            console.log("Error response data:", error.response?.data);
            const errorMsg = error.response?.data?.error || error.message;

            console.log("Login error:", errorMsg);
            console.log("Status code:", status);

            const showBothFieldsError = (message) => {
                setError("email", { type: "manual", message });
                setError("password", { type: "manual", message });
            };

            if (status === 400 || errorMsg === "Email is incorrect") {
                showBothFieldsError("Invalid email or password.");
            }

            else if (status === 401) {
                // Check if it's a verification issue vs authentication issue
                if (errorMsg === "Unauthorized" && error.response?.data?.message === "There is no refresh token sent") {
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
                    showBothFieldsError("Invalid email or password.");
                }
            } else if (status === 429) {
                showBothFieldsError("Too many login attempts. Please try again later.");
            } else {
                const field = errorMsg.toLowerCase().includes("email")
                    ? "email"
                    : errorMsg.toLowerCase().includes("password")
                    ? "password"
                    : null;

                if (field) {
                    setError(field, { type: "manual", message: errorMsg });
                } else {
                    showBothFieldsError(errorMsg);
                }
            }
        }
    };

    return(
        <View style = {styles.container}>
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
                icon={<FontAwesome name="lock" size={24} color="#9188E5"/>}
            />

            <Link href="/RegisterModule/ForgotPasswordScreen" style={styles.link}> Forgot Password? </Link>

            <Button
                title="Login"
                onPress={handleSubmit(Login)}
                borderRadius={12}
                fontSize={responsive.fonts.body}
                loading={isSubmitting}
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