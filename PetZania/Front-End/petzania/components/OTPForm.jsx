import { CodeField, Cursor, useBlurOnFulfill, useClearByFocusCell } from "react-native-confirmation-code-field";
import Button from "@/components/Button";
import { useState } from "react";
import { View, Text, StyleSheet } from "react-native";
import { responsive } from '@/utilities/responsive';
import axios from "axios";
import { useRouter } from "expo-router";
export default function OTPForm({ CELL_COUNT , isRegister , email }) {
  const [value, setValue] = useState("");
  const ref = useBlurOnFulfill({ value, cellCount: CELL_COUNT });
  const [props, getCellOnLayoutHandler] = useClearByFocusCell({ value, setValue });
  const [errorMessage, setErrorMessage] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const router = useRouter();
  const handleVerify = async () => {

    console.log("OTP entered:", value);
    try {
      // Replace the URL below with your actual OTP verification endpoint.
      const response = await axios.post("/api/verify-otp", { otp: value });
      
      // Assuming a response structure like { success: true, message: "OTP verified" }
      if (response.data.success) {
        setSuccessMessage("OTP verified successfully!");
        setErrorMessage(null);
        if(isRegister) {
        router.replace("/RegisterModule/LoginScreen");
        }
        else {
          router.replace("/RegisterModule/ResetPasswordScreen", { email: email });
        }
      } else {
        setErrorMessage(response.data.message || "OTP verification failed.");
        setSuccessMessage(null);
      }
    } catch (error) {
      // Handle any unexpected errors
      const errMsg = error.response?.data?.message || error.message;
      setErrorMessage(errMsg);
      setSuccessMessage(null);
    }
  };

  return (
    <View>
      <CodeField
        ref={ref}
        {...props}
        value={value}
        onChangeText={setValue}
        cellCount={CELL_COUNT}
        rootStyle={styles.codeFieldRoot}
        keyboardType="numeric"
        textContentType="oneTimeCode"
        renderCell={({ index, symbol, isFocused }) => (
          <View
            key={index}
            onLayout={getCellOnLayoutHandler(index)}
            style={[styles.cellRoot, isFocused && styles.focusCell]}
          >
            <Text style={styles.cellText}>
              {symbol || (isFocused ? <Cursor /> : null)}
            </Text>
          </View>
        )}
      />
      {errorMessage && <Text style={styles.errorText}>{errorMessage}</Text>}
      {successMessage && <Text style={styles.successText}>{successMessage}</Text>}
      <Button
        title="Verify"
        onPress={handleVerify}
        disabled={value.length !== CELL_COUNT}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  codeFieldRoot: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: responsive.hp('3%'),
  },
  cellRoot: {
    width: responsive.wp('12%'),
    height: responsive.hp('6%'),
    backgroundColor: "#F3F3F3",
    justifyContent: "center",
    alignItems: "center",
    borderWidth: 1,
    borderColor: "#E0E0E0",
    borderRadius: 12,
  },
  cellText: {
    fontSize: responsive.fonts.body,
    color: "#333",
  },
  focusCell: {
    borderColor: "#8B73FF",
    borderWidth: 2,
  },
  errorText: {
    color: "red",
    textAlign: "center",
    marginBottom: responsive.hp('2%'),
  },
  successText: {
    color: "green",
    textAlign: "center",
    marginBottom: responsive.hp('2%'),
  },
});
