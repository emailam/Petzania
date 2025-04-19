import React from "react";
import { Pressable, StyleSheet, Text  } from "react-native";

export default function Button({ title ,onPress, disabled }) {
  

  return (
    <Pressable onPress={onPress} style={styles.button} disabled={disabled}>
      {React.isValidElement(title) ? (
        title
      ) : (
        <Text style={styles.text}>{title}</Text>
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
    paddingVertical: 12,    
  },
  text: {
    color: "#ffffff",
    fontWeight: "bold",
    fontSize: 20,
  },
});
