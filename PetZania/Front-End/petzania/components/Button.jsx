import { Pressable , StyleSheet , Text , View} from "react-native";

export default function Button({title,borderRadius,width,fontSize,onPress}) {
    return (
        <View style={[styles.container,{borderRadius: borderRadius, width: width}]}>

            <Pressable onPress={onPress} style={[styles.Button,{borderRadius: borderRadius,width: width}]}>

                <Text style = {[styles.text,{fontSize: fontSize}]}>{title}</Text>

            </Pressable>
        </View>
        
    )
}
const styles = StyleSheet.create({
    container:{
        alignItems: 'center',
        backgroundColor: '#9188E5',
    },
    text:{
        color: '#ffffff',
        textAlign: 'center',
    },
    Button: {
        padding:10
    },
})
