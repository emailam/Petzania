import { Stack } from "expo-router";
import { TouchableOpacity, View } from "react-native";
import Ionicons from "react-native-vector-icons/Ionicons";
import { useRouter } from "expo-router";

const FriendsLayout = () => {
  const router = useRouter();

  const goBack = () => {
    console.log('Back button pressed');
    router.back(); // make it actually go back
  };

  return (
    <Stack>
      <Stack.Screen
        name="index"
        options={{
          title: 'Friends',
          headerTitleAlign: 'center',
        }}
      />
    </Stack>
  );
};

export default FriendsLayout;
