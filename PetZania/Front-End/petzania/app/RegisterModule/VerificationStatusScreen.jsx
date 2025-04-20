import {View, Text, StyleSheet} from 'react-native';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import Button from '@/components/Button';
export default function VerificationStatus({title, text}){
    return(
        <View style = {styles.container}>
            <MaterialIcons name="verified-user" size={200} color="#9188E5" />
            <Text style = {styles.title}>{title}</Text>
            <Text style = {styles.text}>{text}</Text>
            <Button
                      title="Back To Login"
                      borderRadius={10}
                      width="60%"
                      fontSize={18}
                      onPress={()=>{console.log()}}
                    />
        </View>
    )
}

const styles = StyleSheet.create({
    container:{
        flexDirection: "column",
        alignItems: "center",
        flex: 1,
        backgroundColor: "#fff",
        marginTop : "30%",
        height: "100%",
    },
    title:{
        fontSize: 36,
        fontWeight: "bold",
        color: "#000",
        marginVertical:"2%"
    },
    text:{
        fontSize: 20,
        color: "gray",
        marginBottom:"40%"
    }
})