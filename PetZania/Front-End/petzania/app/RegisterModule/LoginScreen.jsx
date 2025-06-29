import React, { useContext, useEffect }from "react";
import { ScrollView, Text , StyleSheet, View } from 'react-native';
import { SafeAreaView } from "react-native-safe-area-context";
import { useRouter } from "expo-router";
import { responsive } from "@/utilities/responsive";
import LoginForm from "@/components/LoginForm";

import { UserContext } from "@/context/UserContext";
import { PetContext } from "@/context/PetContext";

export default function LoginScreen() {
  const { setUser, user } = useContext(UserContext);
  const { setPets } = useContext(PetContext);
  const router = useRouter();

  useEffect(() => {
    setUser(null);
    setPets([]);
  }
  ,[]);

  return (
    <SafeAreaView style={styles.safeArea}>

      <Text style={styles.title}>Petzania</Text>

      <ScrollView contentContainerStyle={styles.container}>

          <LoginForm/>
          <View style={styles.textContainer}>
            <Text style={styles.text}> Don't have an account?{" "}</Text>
            <Text style={styles.link} onPress={() => router.replace("/RegisterModule/RegisterScreen")}> Sign up now </Text>
          </View>

      </ScrollView>

    </SafeAreaView>
  );
}


const styles = StyleSheet.create({
  safeArea: {
    backgroundColor: 'white',
    gap: responsive.hp('2%'),
    paddingTop: responsive.hp('10%'),
    height: responsive.hp('100%'),
    width: responsive.wp('100%'),
    paddingHorizontal: '5%',
  },
  title: {
    fontSize: responsive.fonts.title,
    color: '#9188E5',
    fontWeight: 'bold',
    paddingHorizontal: '5%',
  },
  container: {
    alignItems: 'center',
    gap: responsive.hp('2%'),
  },
  link: {
    color: '#9188E5',
    fontWeight: 'bold',
  },
  text: {
    color: "gray",
    fontSize: 14,
    marginBottom: "2%",
  },
  textContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    flexWrap: 'wrap',
  },
  button: {
    flex:1,
    width: responsive.wp('50%'),
  }
});
