import { View, Text } from 'react-native';
import AdoptionBreedingForm from './AdoptionBreedingPost';
export default function ServicePostsScreen() {
    return(
        <View >
            <Text style = {{fontSize : 32 , fontWeight : 400 , marginTop:16 , marginLeft: 16 }}>New Post</Text>
            <AdoptionBreedingForm/>
        </View>
    )
}
    
    