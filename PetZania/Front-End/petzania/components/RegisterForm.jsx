import Button from '@/components/Button';
import FormInput from '@/components/FormInput';
import PasswordInput from '@/components/PasswordInput';
import TermsInput from '@/components/TermsInput';
import { useAuthForm } from '@/components/useForm';
import axios from 'axios';
import { Alert, View } from 'react-native';
import React from 'react';
import { responsive } from '@/utilities/responsive';
import { useRouter } from 'expo-router';
export default function RegisterForm(){

    const { control, handleSubmit, formState: { errors, isSubmitting }, setError } = useAuthForm("register",{username: '',confirmPassword: '', termsAccepted: false});
    
    const [displayPassword, setDisplayPassword] = React.useState(false);
    const [displayConfirmPassword, setDisplayConfirmPassword] = React.useState(false);
    const router = useRouter();

    const Register = async (data) => {
        try {
          // Send a POST request with the registration data.
          const response = await axios.post("http://localhost:8080/api/user/auth/register", data);
          
          // On success, display a success alert.
          if(response.status === 200) {
            router.push({
              pathname: "/auth/OTPVerificationScreen",
              params: { email: data.email , isRegiser: true}, // Pass the email to the next screen
            });
          }
        } catch (error) {
          // Extract error message from the API response or fallback to error.message.
          const errorMsg = error.response?.data?.message || error.message;
          
          // Determine which field the error message relates to.
          const field = errorMsg.toLowerCase().includes("email")
            ? "email"
            : errorMsg.toLowerCase().includes("username")
            ? "username"
            : errorMsg.toLowerCase().includes("password")
            ? "password"
            : errorMsg.toLowerCase().includes("terms")
            ? "termsAccepted"
            : null;
          
          // If a specific field is identified, set an error on that field.
          if (field) {
            setError(field, { type: "manual", message: errorMsg });
          } else {
            // Otherwise, display a general error alert.
            Alert.alert("Error", errorMsg);
          }
        }
      };
    return(
        <View>
            <FormInput
                control={control}
                name="username"
                errors={errors}
                placeholder="Username"
                autoCapitalize="none"
            />
    
            <FormInput
                control={control}
                name="email"
                errors={errors}
                placeholder="Email"
                keyboardType="email-address"
                autoCapitalize="none"
            />
    
            <PasswordInput
                control={control}
                name="password"
                errors={errors}
                placeholder="Password"
                showPassword={displayPassword}
                toggleShow={() => setDisplayPassword(!displayPassword)}
            />
    
            <PasswordInput
                control={control}
                name="confirmPassword"
                placeholder="Confirm Password"
                errors={errors}
                showPassword={displayConfirmPassword}
                toggleShow={() => setDisplayConfirmPassword(!displayConfirmPassword)}
            />
    
            <TermsInput
                control={control}
                name="termsAccepted"
                errors={errors}
                autoCapitalize="none"
            />
    
            <Button
                title="Sign Up"
                onPress={handleSubmit(Register)}
                width={responsive.buttons.width.primary}
                borderRadius={12}
                fontSize={responsive.fonts.body}
                loading={isSubmitting}
            />
        </View>
    )
}