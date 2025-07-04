import {
  CodeField,
  Cursor,
  useBlurOnFulfill,
  useClearByFocusCell,
} from "react-native-confirmation-code-field";
import Button from "@/components/Button";
import { useState } from "react";
import { View, Text, StyleSheet } from "react-native";
import { responsive } from "@/utilities/responsive";

import { useRouter } from "expo-router";

import { verifyOTP, verifyResetOTP } from "@/services/userService";

import Toast from 'react-native-toast-message';

const showToastSuccess = (message) => {
  Toast.show({
    type: 'success',
    text1: message,
    position: 'top',
    visibilityTime: 3000,
    autoHide: true,
    topOffset: 30,
    swipeable: true,
  });
};

const showToastError = (message) => {
  Toast.show({
    type: 'error',
    text1: message,
    position: 'top',
    visibilityTime: 3000,
    autoHide: true,
    topOffset: 30,
  });
};

export default function OTPForm({ CELL_COUNT, isRegister, email }) {
  const [value, setValue] = useState("");
  const ref = useBlurOnFulfill({ value, cellCount: CELL_COUNT });
  const [props, getCellOnLayoutHandler] = useClearByFocusCell({ value, setValue });
  const [errorMessage, setErrorMessage] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const [isLoading, setIsLoading] = useState(false);

  const router = useRouter();

  // ✅ Case 1: Register - verify OTP and go to Login
  const handleVerifyAccount = async () => {
    setIsLoading(true);
    try {
      const response = await verifyOTP(email, value);

      if (response) {
        showToastSuccess(response.message || "Account verified successfully!");
        setErrorMessage(null);
        if(isRegister === "true"){
          router.replace("/RegisterModule/LoginScreen");
          return;
        }
        router.replace("/RegisterModule/ProfileSetUp1");
      } else {
        showToastError(response.message || "Verification failed.");
        setSuccessMessage(null);
      }
    } catch (error) {
      const errMsg = error.response?.data?.message || error.message;
      showToastError(errMsg || "Verification failed.");
      setErrorMessage(errMsg);
      setSuccessMessage(null);
    } finally {
      setIsLoading(false);
    }
  };

  // ✅ Case 2: Reset Password - verify OTP and go to Reset Password Screen
  const handleVerifyResetPassword = async () => {
    setIsLoading(true);
    try {
      const response = await verifyResetOTP(email, value);

      if (response) {
        showToastSuccess(response.message || "OTP verified successfully!");
        setErrorMessage(null);
        router.replace({
          pathname: "/RegisterModule/ResetPasswordScreen",
          params: { email, otp: value },
        });
      } else {
        showToastError(response.message || "OTP verification failed.");
        setSuccessMessage(null);
      }
    } catch (error) {
      const errMsg = error.response?.data?.message || error.message;
      showToastError(errMsg || "OTP verification failed.");
      setErrorMessage(errMsg);
      setSuccessMessage(null);
    } finally {
      setIsLoading(false);
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
        disabled={value.length !== CELL_COUNT}
        loading={isLoading}
        onPress={() => {
          if(isRegister === "true") {
            handleVerifyAccount();
          } else {
            handleVerifyResetPassword();
          }
      }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  codeFieldRoot: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: responsive.hp("3%"),
  },
  cellRoot: {
    width: responsive.wp("12%"),
    height: responsive.hp("6%"),
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
    marginBottom: responsive.hp("2%"),
  },
  successText: {
    color: "green",
    textAlign: "center",
    marginBottom: responsive.hp("2%"),
  },
});