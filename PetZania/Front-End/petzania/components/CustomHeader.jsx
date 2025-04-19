import { View, Text } from "react-native";

export default function HeaderTitle({ title, subtitle, alignment }) {
    return (
        <View style={{ alignItems: alignment }}>
            <Text style={{ fontSize: 20, fontWeight: "bold", color: "#000" }}>
                {title}
            </Text>
            <Text style={{ fontSize: 15, color: "gray" }}>{subtitle}</Text>
        </View>
    );
}
