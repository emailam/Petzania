import React, { useContext, useEffect }from "react";
import { ScrollView, Text , StyleSheet, View, Image } from 'react-native';
import { SafeAreaView } from "react-native-safe-area-context";
import { Link } from "expo-router";
import { responsive } from "@/utilities/responsive";
import ExternalSignIn from "@/components/ExternalSignIn";
import LoginForm from "@/components/LoginForm";

import { UserContext } from "@/context/UserContext";
import { PetContext } from "@/context/PetContext";

export default function LoginScreen() {
  const { setUser, user } = useContext(UserContext);
  const { setPets } = useContext(PetContext);

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
          <Text style={styles.text}> Don't have an account?{" "}
            <Link href={"/RegisterModule/RegisterScreen"} style={styles.link}> Sign up now </Link>
          </Text>

          <View style = {styles.button}>
            <ExternalSignIn/>
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
    alignSelf: 'flex-end',
    marginRight: responsive.margins.screenEdge,
    fontSize: responsive.fonts.small,
  },
  text: {
    color: "gray",
    fontSize: 18,
    padding: 5,
    marginBottom: "2%",
  },
  button: {
    flex:1,
    width: responsive.wp('50%'),
  }
});
