import Button from '@/components/Button';
import FormInput from '@/components/FormInput';
import PasswordInput from '@/components/PasswordInput';
import TermsInput from '@/components/TermsInput';
import { useAuthForm } from '@/components/useForm';

import { Alert, View } from 'react-native';
import React, {useContext, useState} from 'react';
import { responsive } from '@/utilities/responsive';
import { useRouter } from 'expo-router';
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import FontAwesome from "@expo/vector-icons/FontAwesome";

import { UserContext } from '@/context/UserContext';

import { registerUser } from '@/services/userService';


export default function RegisterForm(){
    const { control, handleSubmit, formState: { errors, isSubmitting }, setError } = useAuthForm("register",{username: '',confirmPassword: '', termsAccepted: false});

    const [displayPassword, setDisplayPassword] = useState(false);
    const [displayConfirmPassword, setDisplayConfirmPassword] = useState(false);
    const router = useRouter();

    const {setUser} = useContext(UserContext);

    const Register = async (data) => {
      try {
        const response = await registerUser(data);

        if (response) {
          setUser({
            ...response.userProfileDTO,
            email: data.email
          });
          router.push({
            pathname: "/RegisterModule/OTPVerificationScreen",
            params: { isRegister: true, email: data.email },
          });
        }
      } catch (error) {
        const errorMsg = error.response?.data?.message || error.message;

        const field = errorMsg.toLowerCase().includes("email")
          ? "email"
          : errorMsg.toLowerCase().includes("username")
          ? "username"
          : errorMsg.toLowerCase().includes("password")
          ? "password"
          : errorMsg.toLowerCase().includes("terms")
          ? "termsAccepted"
          : null;

        if (field) {
          setError(field, { type: "manual", message: errorMsg });
        } else {
          Alert.alert("Error", errorMsg);
        }
      }
    };

    return(
      <View style = {{width: '100%', gap: 16, paddingHorizontal: 20}}>
        <FormInput
            control={control}
            name="username"
            errors={errors}
            placeholder="Username"
            autoCapitalize="none"
            icon={<FontAwesome name="user" size={24} color="#9188E5" />}
        />

        <FormInput
          control={control}
          name="email"
          errors={errors}
          placeholder="Email"
          keyboardType="email-address"
          autoCapitalize="none"
          icon={<MaterialIcons name="email" size={24} color="#9188E5" />}
        />

        <PasswordInput
          control={control}
          name="password"
          errors={errors}
          placeholder="Password"
          showPassword={displayPassword}
          toggleShow={() => setDisplayPassword(!displayPassword)}
          icon={<FontAwesome name="lock" size={24} color="#9188E5"/>}
        />

        <PasswordInput
          control={control}
          name="confirmPassword"
          placeholder="Confirm Password"
          errors={errors}
          showPassword={displayConfirmPassword}
          toggleShow={() => setDisplayConfirmPassword(!displayConfirmPassword)}
          icon={<FontAwesome name="lock" size={24} color="#9188E5"/>}
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