import Button from '@/components/Button';
import FormInput from '@/components/FormInput';
import PasswordInput from '@/components/PasswordInput';
import TermsInput from '@/components/TermsInput';
import { useAuthForm } from '@/components/useForm';

import { Alert, View } from 'react-native';
import React, {useContext, useState, useEffect, useRef} from 'react';
import { responsive } from '@/utilities/responsive';
import { useRouter } from 'expo-router';
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import FontAwesome from "@expo/vector-icons/FontAwesome";

import { UserContext } from '@/context/UserContext';

import { signup } from '@/services/userService';


export default function RegisterForm(){
    const { control, handleSubmit, formState: { errors, isSubmitting, isValid }, setError, watch } = useAuthForm("register",{username: '',confirmPassword: '', termsAccepted: false});

    const [displayPassword, setDisplayPassword] = useState(false);
    const [displayConfirmPassword, setDisplayConfirmPassword] = useState(false);
    const [isFormComplete, setIsFormComplete] = useState(false);
    const router = useRouter();

    // Create refs for input navigation
    const emailRef = useRef(null);
    const passwordRef = useRef(null);
    const confirmPasswordRef = useRef(null);

    const {setUser} = useContext(UserContext);

    // Watch all form values to determine if form is complete
    const watchedValues = watch();
    
    useEffect(() => {
        const { username, email, password, confirmPassword, termsAccepted } = watchedValues;
        
        // Check if all required fields have values
        const allFieldsFilled = 
            username && username.trim().length >= 5 && // Username minimum 5 chars
            email && email.trim().length > 0 &&
            password && password.length >= 8 && // Password minimum 8 chars
            confirmPassword && confirmPassword.length > 0 &&
            termsAccepted === true;
        
        // Check if passwords match
        const passwordsMatch = password === confirmPassword;
        
        // Form is complete when all fields are filled, passwords match, AND form is valid (no errors)
        setIsFormComplete(allFieldsFilled && passwordsMatch && isValid);
    }, [watchedValues, isValid]);

    const Register = async (data) => {
      data.email = data.email.toLowerCase();
      try {
        const response = await signup(data);

        if (response) {
          setUser({
            ...response.userProfileDTO,
            email: data.email
          });
          router.push({
            pathname: "/RegisterModule/OTPVerificationScreen",
            params: { isRegister: true, email: data.email, password: data.password },
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
            label={"Username"}
            placeholder={"example"}
            autoCapitalize="none"
            icon={<FontAwesome name="user" size={24} color="#9188E5" />}
            maxLength={32}
            returnKeyType="next"
            onSubmitEditing={() => emailRef.current?.focus()}
        />

        <FormInput
          control={control}
          name="email"
          errors={errors}
          label={"Email"}
          placeholder={"example@example.com"}
          keyboardType="email-address"
          autoCapitalize="none"
          icon={<MaterialIcons name="email" size={24} color="#9188E5" />}
          maxLength={100}
          returnKeyType="next"
          inputRef={emailRef}
          onSubmitEditing={() => passwordRef.current?.focus()}
        />

        <PasswordInput
          control={control}
          name="password"
          errors={errors}
          label={"Password"}
          showPassword={displayPassword}
          toggleShow={() => setDisplayPassword(!displayPassword)}
          icon={<FontAwesome name="lock" size={24} color="#9188E5"/>}
          maxLength={32}
          returnKeyType="next"
          inputRef={passwordRef}
          onSubmitEditing={() => confirmPasswordRef.current?.focus()}
        />

        <PasswordInput
          control={control}
          name="confirmPassword"
          label="Confirm Password"
          errors={errors}
          showPassword={displayConfirmPassword}
          toggleShow={() => setDisplayConfirmPassword(!displayConfirmPassword)}
          icon={<FontAwesome name="lock" size={24} color="#9188E5"/>}
          maxLength={32}
          returnKeyType="done"
          inputRef={confirmPasswordRef}
          onSubmitEditing={() => {
            if (isFormComplete) {
              handleSubmit(Register)();
            }
          }}
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
          disabled={!isFormComplete || isSubmitting}
        />
      </View>
    )
}