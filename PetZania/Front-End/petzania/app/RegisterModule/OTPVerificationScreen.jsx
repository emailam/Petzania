import React, { useContext } from "react";
import { View, Text, StyleSheet,SafeAreaView } from "react-native";
import { responsive } from "@/utilities/responsive";
import OTPForm from "@/components/OTPForm";
import RequestOTP from "@/components/RequestOTP";
import { useLocalSearchParams } from "expo-router";


export default function OTPVerificationScreen() {
  const { isRegister, email } = useLocalSearchParams();
  return (
    <SafeAreaView style={styles.safeArea}>

      <View style={styles.container}>

        <Text style={styles.title}>Almost there</Text>
        <Text style={styles.subtitle}>Please enter the 6-digit code sent to {""}
          <Text style={styles.emailText}>{email}</Text> for verification.
        </Text>

        <OTPForm CELL_COUNT={6} isRegister={isRegister} email={email} />

        <Text style={styles.footerText}>Didn't receive any code?</Text>

        <RequestOTP RESEND_COOLDOWN = {60} email = {email} isRegister = {isRegister}/>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    paddingTop: responsive.hp('10%'),
    backgroundColor: "#FFFFFF",
  },
  container: {
    flex: 1,
    paddingHorizontal: responsive.margins.screenEdge,
  },
  title: {
    fontSize: responsive.fonts.title,
    color: "#8B73FF",
    fontWeight: "600",
  },
  subtitle: {
    fontSize: responsive.fonts.small,
    color: "#333",
    paddingVertical: responsive.hp('2%'),
    
  },
  emailText: {
    fontWeight: "bold",
  },

  footerText: {
    fontSize: responsive.fonts.small,
    color: "#333",
    paddingTop: responsive.hp('2%'),
    paddingBottom: responsive.hp('1%'),
  },
  
});