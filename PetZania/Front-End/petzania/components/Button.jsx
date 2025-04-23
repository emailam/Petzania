import React from "react";
import { Pressable, StyleSheet, Text  } from "react-native";

export default function Button({ title ,onPress, disabled, fontSize }) {
  const textStyle = React.useMemo(
    () => [styles.text, { fontSize }],
    [fontSize]
  );
  return (
    <Pressable onPress={onPress} style={styles.button} disabled={disabled}>
      {React.isValidElement(title) ? (
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
  text: {
    color: "#ffffff",
    fontFamily: 'Inter-Bold'
  },
});
