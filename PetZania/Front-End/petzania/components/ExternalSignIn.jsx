import Button from "./Button";
import { View, Text , StyleSheet} from "react-native";
import { FontAwesome5 , FontAwesome } from '@expo/vector-icons';
import { responsive } from "@/utilities/responsive";
export default function ExternalSignIn() {
    return(
        <View style={styles.methodsContainer}>
            <Text style={styles.text}>Or sign in with</Text>
            <Button
                title={<FontAwesome5 name="google" size={24} color="white" />}
                borderRadius={8}
                width="35%"
                fontSize={18}
                onPress={() => {
                console.log("Google sign up");
                }}
            />
            <Button
                title={<FontAwesome name="apple" size={24} color="white" />}
                borderRadius={8}
                width="35%"
                fontSize={18}
                onPress={() => {
                console.log("Facebook sign up");
                }}
            />
        </View>
    );
}
const styles = StyleSheet.create({
    text: {
        color: "gray",
        fontSize: 18,
        padding: 5,
        marginTop: "3%",
        marginBottom: "2%",
      },
      methodsContainer: {
        flexDirection: "column",
        alignItems: "center",
        width: responsive.wp('100%'),
        margin: "3%",
        gap: 15,
      },
})