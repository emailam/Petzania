import React from "react";
import { Pressable, StyleSheet, Text, ActivityIndicator } from "react-native";

export default function Button({ title, onPress, disabled, loading, fontSize }) {
  const isDisabled = disabled || loading;

  const textStyle = React.useMemo(
    () => [styles.text, { fontSize }],
    [fontSize]
  );

  return (
    <Pressable
      onPress={onPress}
      style={[styles.button, isDisabled && styles.disabledButton]}
      disabled={isDisabled}
    >
      {loading ? (
        <ActivityIndicator color="#ffffff" />
      ) : React.isValidElement(title) ? (
        title
      ) : (
        <Text style={textStyle}>{title}</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    backgroundColor: "#9188E5",
    borderRadius: 14,
    justifyContent: "center",
    alignItems: "center",
    padding: 12,
    width: "100%",
  },
  disabledButton: {
    backgroundColor: "#BDB9F1", // lighter version of the purple when disabled
    opacity: 0.8,
  },
  text: {
    color: "#ffffff",
    fontFamily: "Inter-Bold",
  },
});