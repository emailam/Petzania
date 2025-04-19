import React from "react";
import {  View , StyleSheet } from 'react-native';
import { Link } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import FontAwesome from "@expo/vector-icons/FontAwesome";

import Button from "@/components/Button";
import FormInput from "@/components/FormInput";
import PasswordInput from "@/components/PasswordInput";
import { responsive } from "@/utilities/responsive";
import {useAuthForm} from "@/components/useForm";
import axios from "axios";
export default function LoginForm(){
    const {control , handleSubmit , formState:{errors , isSubmitting} , setError} = useAuthForm("login");

    const [displayPassword, setDisplayPassword] = React.useState(false);

    const Login = async (data) => {
        try {

        const response = await axios.post("http://localhost:8080/api/user/auth/login", data);

        if (response.ok) {
            // Redirect to the HomePage screen.
            // To be implemented
        } 
        } catch (error) {
        const errorMsg = error.response?.data?.message || error.message;
        
        const field = errorMsg.toLowerCase().includes('email') ? 'email' :
                        errorMsg.toLowerCase().includes('password') ? 'password' : null;
        
        if (field) {
            setError(field, { type: 'manual', message: errorMsg });
        } else {
            Alert.alert('Error', errorMsg);
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
                icon={  
                <FontAwesome name="lock" size={24} color="#9188E5"/>}
            />
            
            <Link href="/forgot-password" style={styles.link}> Forgot Password? </Link>
              
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