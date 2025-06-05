import React, {useContext} from "react";
import { View , StyleSheet } from 'react-native';
import { Link } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import FontAwesome from "@expo/vector-icons/FontAwesome";

import Button from "@/components/Button";
import FormInput from "@/components/FormInput";
import PasswordInput from "@/components/PasswordInput";
import { responsive } from "@/utilities/responsive";
import { useAuthForm } from "@/components/useForm";
import axios from "axios";

const { useRouter } = require("expo-router");

import { UserContext } from "@/context/UserContext";

import { getUserById, loginUser } from "@/services/userService";

export default function LoginForm(){
    const {control , handleSubmit , formState:{errors , isSubmitting} , setError} = useAuthForm("login");

    const [displayPassword, setDisplayPassword] = React.useState(false);

    const router = useRouter();

    const { setUser } = useContext(UserContext);

    const Login = async (data) => {
        data.email = data.email.trim();

        try {
            const response = await loginUser(data);

            if (response) {
                const userData = await getUserById(response.userId);
                console.log("User data:", userData);
                console.log("User ID:", response.userId);
                setUser(userData);
                if (userData?.name === null) {
                    router.push("/RegisterModule/ProfileSetUp1");
                } else {
                    router.replace("/RegisterModule/ProfileSetUp1"); // TODO: replace with Home when ready
                }
            }

        } catch (error) {
            const status = error.response?.status;
            const errorMsg = error.response?.data?.message || error.message;

            console.log("Login error:", errorMsg);
            console.log("Status code:", status);

            const showBothFieldsError = (message) => {
                setError("email", { type: "manual", message });
                setError("password", { type: "manual", message });
            };

            if (status === 400 || errorMsg === "Email is incorrect") {
                showBothFieldsError("Invalid email or password.");
            } else if (status === 401 && errorMsg === "User is not verified") {
                try {
                    const otpRes = await axios.post("http://192.168.1.4:8080/api/user/auth/resendOTP", {
                        email: data.email,
                    });

                    if (otpRes.status === 200) {
                        console.log("New OTP sent:", otpRes.data);
                    } else {
                        console.error("Failed to resend OTP. Status:", otpRes.status);
                    }
                } catch (otpError) {
                    console.error("OTP resend error:", otpError.response?.data?.message || otpError.message);
                }

                router.push({
                    pathname: "/RegisterModule/OTPVerificationScreen",
                    params: { isRegister: true, email: data.email },
                });
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