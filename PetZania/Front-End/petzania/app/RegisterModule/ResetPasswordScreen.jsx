import { Text , StyleSheet} from "react-native"
import { responsive } from "@/utilities/responsive";
import { SafeAreaView } from "react-native-safe-area-context";
import ResetPasswordForm from "@/components/ResetPasswordForm";
import { useLocalSearchParams } from "expo-router";

export default function ResetPasswordScreen(){
    const {otp} = useLocalSearchParams();
    return(
      <SafeAreaView style={styles.safeArea}>
        <Text style={styles.title}>Reset Password</Text>
        <Text style = {styles.text}>Create a new password</Text>
        <ResetPasswordForm otp={otp} />
      </SafeAreaView>
    )
}
const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    paddingTop: responsive.hp('10%'),
    backgroundColor: 'white',
    alignItems: 'left',
    height: responsive.hp('100%'),
  },
  title: {
    fontSize: responsive.fonts.title,
    color: '#9188E5',
    fontWeight: 'bold',
    marginLeft: responsive.margins.screenEdge,
    marginBottom: responsive.hp('1%'),
  },
  text: {
    color: "gray",
    fontSize: responsive.fonts.small,
    marginBottom: responsive.hp('2%'),
    marginLeft: responsive.margins.screenEdge,
    width : "80%",
  }
});