import { Text , StyleSheet } from "react-native"
import { responsive } from "@/utilities/responsive";
import { SafeAreaView } from "react-native-safe-area-context";
import ForgotPasswordForm from "@/components/ForgotPasswordForm";
export default function ForgotPasswordScreen(){
    
   
    return(
      <SafeAreaView style={styles.safeArea}>
        <Text style={styles.title}>Forgot Password?</Text>
        <Text style = {styles.text}>Enter your email address to get the password reset link</Text>

        <ForgotPasswordForm />
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
  title: {
    fontSize: responsive.fonts.title,
    color: '#9188E5',
    fontWeight: 'bold',
    marginLeft: responsive.margins.screenEdge + 20,
    marginBottom: responsive.hp('1%'),
  },
  text: {
    color: "gray",
    fontSize: responsive.fonts.small,
    marginBottom: responsive.hp('2%'),
    marginLeft: responsive.margins.screenEdge + 25,
    width : "80%",

  },
});