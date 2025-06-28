import { Text, TouchableOpacity, StyleSheet } from "react-native";
import { Image } from 'expo-image';
import Onboarding from "react-native-onboarding-swiper";
import { useRouter } from "expo-router";
import { AntDesign } from "@expo/vector-icons";

export default function OnboardingScreen() {
  const router = useRouter();

  const handleDone = async () => {
    router.push("/RegisterModule/RegisterScreen");
  };

  const SkipButton = ({ ...props }) => (
    <TouchableOpacity style={styles.button} {...props}>
      <Text style={styles.skipButtonText}>Skip</Text>
    </TouchableOpacity>
  );

  const NextButton = ({ ...props }) => (
    <TouchableOpacity style={styles.nextButton} {...props}>
      <AntDesign name="arrowright" size={30} color="white" />
    </TouchableOpacity>
  );

  return (
    <Onboarding
      onDone={handleDone}
      onSkip={handleDone}
      bottomBarHighlight={false}
      showDone={true}
      SkipButtonComponent={SkipButton}
      NextButtonComponent={NextButton}
      DoneButtonComponent={NextButton}
      bottomBarHeight={100}
      titleStyles={{ fontFamily: "Inter-Bold", fontSize: 26 }}
      subTitleStyles={{ fontFamily: "Inter-Medium", fontSize: 16 }}
      pages={[
        {
          backgroundColor: "#fff",
          image: <Image source={require("../../assets/images/Onboarding/Connected World.png")} style={styles.image} contentFit="contain" />,
          title: "Connect, Share, and Care",
          subtitle: "Your pet's social hub to meet, share, and explore!",
        },
        {
          backgroundColor: "#fff",
          image: <Image source={require("../../assets/images/Onboarding/Pet needs.png")} style={styles.image} contentFit="contain"/>,
          title: "Everything Your Pet Needs",
          subtitle: "Find trusted vets, pet stores, and adoption posts near you!",
        },
        {
          backgroundColor: "#fff",
          image: <Image source={require("../../assets/images/Onboarding/Friends.png")} style={styles.image} contentFit="contain"/>,
          title: "Join, Chat & Make Friends",
          subtitle: "Connect with pet lovers, join groups, and share your journey!",
        },
        {
          backgroundColor: "#fff",
          image: <Image source={require("../../assets/images/Onboarding/Pet profile.png")} style={styles.image} contentFit="contain" />,
          title: "Your Pet's Digital Profile",
          subtitle: "Create and manage your pet's profile—because every pet matters!",
        },
      ]}
    />
  );
}

const styles = StyleSheet.create({
  image: {
    width: 350,
    height: 350,
  },
  button: {
    marginHorizontal: 20,
  },
  skipButtonText: {
    fontSize: 18,
    fontFamily: "Inter-Medium",
    color: "#9188E5",
  },
  nextButton: {
    backgroundColor: "#9188E5",
    width: 50,
    height: 50,
    borderRadius: 15,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 20,
  },
});
