import { useState, useEffect } from 'react';
import { Pressable, StyleSheet, Text } from 'react-native';
import { responsive } from '@/utilities/responsive';

import { resendOTP, sendResetPasswordOTP } from '@/services/userService';
import Toast from 'react-native-toast-message';

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

  const showToast = (type, message) => {
    Toast.show({
      type,
      text1: message,
      position: 'top',
      visibilityTime: 3000,
      autoHide: true,
      topOffset: 30,
    });
  };

  const handleRequestNewCode = async () => {
    if (!resendActive) return;

    try {
      let response;

      if (isRegister === "true") {
        response = await resendOTP(email);
      } else {
        response = await sendResetPasswordOTP(email);
      }

      if (response && response.status === 200) {
        showToast('success', response.data.message || 'OTP sent successfully');
      } else {
        showToast('error', 'Failed to send OTP. Please try again.');
      }
    } catch (error) {
      const errorMsg = error.response?.data?.message || error.message;
      showToast('error', errorMsg || 'Error sending OTP.');
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