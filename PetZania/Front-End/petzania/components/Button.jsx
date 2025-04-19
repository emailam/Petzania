import React from "react";
import { Pressable, StyleSheet, Text } from "react-native";

export default function Button({ title, borderRadius, width, fontSize, onPress }) {
  const buttonStyle = React.useMemo(
    () => [styles.container, { borderRadius, width }],
    [borderRadius, width]
  );
  const textStyle = React.useMemo(
    () => [styles.text, { fontSize }],
    [fontSize]
  );

  return (
    <Pressable onPress={onPress} style={buttonStyle}>
      {React.isValidElement(title) ? (
        title
      ) : (
        <Text style={textStyle}>{title}</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: "#9188E5",
    padding: 10,
    alignItems:'center'
  },
  text: {
    color: "#ffffff",
    fontFamily: 'Inter-Bold'
  },
});
