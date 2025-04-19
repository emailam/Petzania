import { View , Text , StyleSheet} from "react-native"
import React from "react";
import Button from "@/components/Button";
import { useAuthForm } from "@/components/useForm";
import { responsive } from "@/utilities/responsive";
import { SafeAreaView } from "react-native-safe-area-context";
import { ScrollView } from "react-native";
import PasswordInput from "@/components/PasswordInput";
import axios from "axios";
import { useRouter , useLocalSearchParams } from "expo-router";
export default function ResetPasswordScreen(){
    
    const {control , handleSubmit , formState:{errors , isSubmitting} , setError} = useAuthForm("resetPassword");

    const [displayPassword, setDisplayPassword] = React.useState(false);
    const [displayConfirmPassword, setDisplayConfirmPassword] = React.useState(false);
    const {email} = useLocalSearchParams();
    const router = useRouter();
    const setNewPassword = async (data) => {
      try {
        const response = await axios.post("http://localhost:8080/api/user/auth/reset-password", {email: email, password: data.password});
        
        if (response.status === 200) {
          // Show Sucess Message
          router.replace("/RegisterModule/LoginScreen");
        } else {
          // If response status is not 200, treat it as an error.
          console.error("Unexpected response status:", response.status);
        }
      } catch (error) {
        // Extract error message from the API response or fallback to error.message.
        const errorMsg = error.response?.data?.message || error.message;
        
        // Determine if the error relates to the password field.
        const field = errorMsg.toLowerCase().includes("password") ? "password" : null;
        
        if (field) {
          setError(field, { type: "manual", message: errorMsg });
        } else {
          // Log error if it's not field-specific.
          console.error("Error occurred:", errorMsg);
        }
      }
    };
    
    return(
      <SafeAreaView style={styles.safeArea}>
        <Text style={styles.title}>Reset Password</Text>
        <Text style = {styles.text}>Create a new password. Ensure it differs from previous ones for security</Text>
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
              placeholder = "Confirm your new password"
              showPassword={displayConfirmPassword}
              toggleShow={() => setDisplayConfirmPassword(!displayPassword)}
            />
          
          <View style = {styles.subContainer}>
            <Button
                title="Reset Password"
                onPress={handleSubmit(setNewPassword)}
                width={styles.button.width}
                borderRadius={12}
                fontSize={responsive.fonts.body}
                loading={isSubmitting}
            />
        </View>
      </ScrollView>
    </SafeAreaView>
    )
}
const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    paddingTop: responsive.hp('12%'),
    backgroundColor: 'white',
    alignItems: 'left',
  },
  link: {
    color: '#9188E5',
    fontWeight: 'bold',
    fontSize: responsive.fonts.small,
    marginTop: responsive.hp('5%'),
  },
  title: {
    fontSize: responsive.fonts.title,
    color: '#9188E5',
    fontWeight: 'bold',
    marginLeft: responsive.margins.screenEdge + 20,
    marginBottom: responsive.hp('1%'),
  },
  container: {
    flexGrow: 1,
    alignItems: 'center',
    width: '100%',    
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 12,
    paddingHorizontal: 12,
    width: responsive.wp('100%'),
    marginVertical: responsive.hp('1%'),
    marginLeft: responsive.margins.screenEdge ,
  },
  icon: {
    marginRight: responsive.wp('3%'),
  },
  button: {
    marginTop: responsive.hp('3%'),
    width: responsive.buttons.width.primary,
    borderRadius: responsive.buttons.radius,
  },
  subContainer: {
    marginTop: responsive.hp('5%'),
    width: '100%',
    alignItems: 'center',
  },
  text: {
    color: "gray",
    fontSize: responsive.fonts.small,
    marginBottom: responsive.hp('2%'),
    marginLeft: responsive.margins.screenEdge + 25,
    width : "80%",

  },
  methodsContainer: {
    flexDirection: "column",
    alignItems: "center",
    width: "100%",
    margin: "3%",
  },
});