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
import { AuthContext } from "@/context/AuthContext";


export default function LoginForm(){
    const {control , handleSubmit , formState:{errors , isSubmitting} , setError} = useAuthForm("login");

    const [displayPassword, setDisplayPassword] = React.useState(false);

    const router = useRouter();

    const {user, setUser} = useContext(UserContext);
    const {setRefreshToken, setAccessToken} = useContext(AuthContext);

    const getUserDataById = async (userId) => {
        try {
            const response = await axios.get(`http://192.168.1.4:8080/api/user/auth/login/${userId}`);
            if (response.status === 200) {
                setUser(response.data);
            } else {
                console.error("Failed to retrieve user data. Status:", response.status);
            }
        } catch (error) {
            console.error("Error retrieving user data:", error.response?.data?.message || error.message);
        }
    };

    const Login = async (data) => {
        try {
            const response = await axios.post("http://192.168.1.4:8080/api/user/auth/login", data);
            console.log(response, data);
            if (response.status === 200) {
                getUserDataById(response.data.userId);
                setRefreshToken(response.data.tokenDTO.refreshToken);
                setAccessToken(response.data.tokenDTO.accessToken);
                // Here we will check for login count from database
                if(user.loginCount === 1){
                    router.push('/RegisterModule/ProfileSetUp1');
                }
                else{
                    router.dismissAll();
                    router.replace('(tabs)');
                }
            }
        }
        catch (error) {
            const status = error.response?.status;
            const errorMsg = error.response?.data?.message || error.message;

            console.log("Login error:", errorMsg);
            console.log("Status code:", status);

            const showBothFieldsError = (message) => {
                setError("email", {
                    type: "manual",
                    message,
                });
                setError("password", {
                    type: "manual",
                    message,
                });
            };

            if (status === 400 || errorMsg === "Email is incorrect") {
                showBothFieldsError("Invalid email or password.");
                return;
            }

            else if (status === 401 && errorMsg === "User is not verified") {
                try {
                    const response = await axios.post("http://192.168.1.4:8080/api/user/auth/resendOTP", {
                        email: data.email,
                    });
                    if (response.status === 200) {
                        console.log("New OTP requested successfully:", response.data);
                    } else {
                        console.error("Failed to request new OTP. Status:", response.status);
                    }
                }
                catch (error) {
                    console.error("Error requesting new OTP:", error.response?.data?.message || error.message);
                }
                router.push({
                    pathname: "/RegisterModule/OTPVerificationScreen",
                    params: { isRegister: true, email: data.email },
                });
                return;
            }

            if (status === 429) {
                setError("email", {
                    type: "manual",
                    message: "Too many login attempts. Please try again later.",
                });
                setError("password", {
                    type: "manual",
                    message: "Too many login attempts. Please try again later.",
                });
                return;
            }

            // Fallback error handling
            const field = errorMsg.toLowerCase().includes("email")
                ? "email"
                : errorMsg.toLowerCase().includes("password")
                ? "password"
                : null;

            if (field) {
                setError(field, { type: "manual", message: errorMsg });
            } else {
                // For general error with unknown field, show on both
                showBothFieldsError(errorMsg);
            }
        }

    }
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
                width={styles.button.width}
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
      width: responsive.wp('80%'),
    },
    link: {
      color: '#9188E5',
      fontWeight: 'bold',
      alignSelf: 'flex-end',
      fontSize: responsive.fonts.small,
    },
    button: {
      width: responsive.buttons.width.primary,
      borderRadius: responsive.buttons.radius,
    },
  });