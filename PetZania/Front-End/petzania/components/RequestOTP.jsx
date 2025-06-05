import { useState, useEffect } from 'react';
import { Pressable, StyleSheet, Text } from 'react-native';
import axios from "axios";
import { responsive } from '@/utilities/responsive';


export default function RequestOTP({ RESEND_COOLDOWN, email, isRegister }) {
  const [resendActive, setResendActive] = useState(false);
  const [remainingTime, setRemainingTime] = useState(RESEND_COOLDOWN);

  useEffect(() => {
    let timer;
    if (!resendActive && remainingTime > 0) {
      timer = setInterval(() => {
        setRemainingTime((prev) => prev - 1);
      }, 1000);
    } else if (remainingTime === 0) {
      setResendActive(true);
      setRemainingTime(RESEND_COOLDOWN);
    }
    return () => clearInterval(timer);
  }, [resendActive, remainingTime, RESEND_COOLDOWN]);

  const handleRequestNewCode = async () => {
    if (!resendActive) return;
    try {
      if (isRegister === "true") {
        const response = await axios.post("http://192.168.1.4:8080/api/user/auth/resendOTP", {
          email: email,
        });
        if (response.status === 200) {
          console.log("New OTP requested successfully:", response.data);
        } else {
          console.error("Failed to request new OTP. Status:", response.status);
        }
      }
      else {
        const response = await axios.put("http://192.168.1.4:8080/api/user/auth/sendResetPasswordOTP", {
          email: email,
        });
        if (response.status === 200) {
          console.log("Reset password OTP requested successfully:", response.data);
        } else {
          console.error("Failed to request reset password OTP. Status:", response.status);
        }
      }
    } catch (error) {
      console.error("Error requesting OTP:", error.response?.data?.message || error.message);
    }

    setResendActive(false);
  };

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <Pressable onPress={handleRequestNewCode} disabled={!resendActive}>
      <Text style={[styles.footerLink, !resendActive && styles.disabledLink]}>
        {resendActive ? 'Request new code' : `Request new code in ${formatTime(remainingTime)}`}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  footerLink: {
    fontSize: responsive.fonts.small,
    color: "#8B73FF",
    fontWeight: 'bold',
  },
  disabledLink: {
    color: '#999',
  },
});
