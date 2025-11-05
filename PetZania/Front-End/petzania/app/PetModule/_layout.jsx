import { Stack } from "expo-router";
import { View, Text } from "react-native";
import CustomHeader from "@/components/CustomHeader";
import * as Progress from 'react-native-progress';
import { Dimensions } from "react-native";

export default function PetModuleLayout() {
    const screenWidth = Dimensions.get("window").width;

    return (
        <Stack>
            <Stack.Screen
                name="AddPet1"
                options={{
                    headerTitle: () => (
                        <View style={{width: screenWidth }}>
                            <CustomHeader title="Add Pet Profile" subtitle="Name" alignment={"center"}/>
                            <Progress.Bar borderRadius={10} progress={(1/5)} width={screenWidth * 0.9} style={{margin: 10}} color='#9188E5' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                        </View>
                    ),
                    headerRight: () => (
                        <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                            <Text style={{fontWeight: '500', color:'black'}}>1</Text>/5
                        </Text>
                    ),
                    headerTitleAlign: "center",
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF"},
                }}
            />
            <Stack.Screen
                name="AddPet2"
                options={{
                    headerTitle: () => (
                        <View style={{width: screenWidth }}>
                            <CustomHeader title="Add Pet Profile" subtitle="Type" alignment={"center"}/>
                            <Progress.Bar borderRadius={10} progress={(2/5)} width={screenWidth * 0.9} style={{margin: 10}} color='#9188E5' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                        </View>
                    ),
                    headerRight: () => (
                    <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                        <Text style={{fontWeight: '500', color:'black'}}>2</Text>/5
                    </Text>
                    ),
                    headerTitleAlign: "center",
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                }}
            />
            <Stack.Screen
                name="AddPet3"
                options={{
                    headerTitle: () => (
                        <View style={{width: screenWidth }}>
                            <CustomHeader title="Add Pet Profile" subtitle="Breed" alignment={"center"}/>
                            <Progress.Bar borderRadius={10} progress={(3/5)} width={screenWidth * 0.9} style={{margin: 10}} color='#9188E5' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                        </View>
                    ),
                    headerRight: () => (
                        <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                            <Text style={{fontWeight: '500', color:'black'}}>3</Text>/5
                        </Text>
                    ),
                    headerTitleAlign: "center",
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                }}
            />
            <Stack.Screen
                name="AddPet4"
                options={{
                    headerTitle: () => (
                        <View style={{width: screenWidth }}>
                            <CustomHeader title="Add Pet Profile" subtitle="General Information" alignment={"center"}/>
                            <Progress.Bar borderRadius={10} progress={(4/5)} width={screenWidth * 0.9} style={{margin: 10}} color='#9188E5' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                        </View>
                    ),
                    headerRight: () => (
                        <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                            <Text style={{fontWeight: '500', color:'black'}}>4</Text>/5
                        </Text>
                    ),
                    headerTitleAlign: "center",
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                }}
            />
            <Stack.Screen
                name="AddPet5"
                options={{
                    headerTitle: () => (
                        <View style={{width: screenWidth }}>
                            <CustomHeader title="Add Pet Profile" subtitle="Health Conditions" alignment={"center"}/>
                            <Progress.Bar borderRadius={10} progress={(5/5)} width={screenWidth * 0.9} style={{margin: 10}} color='#9188E5' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                        </View>
                    ),
                    headerRight: () => (
                        <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                            <Text style={{fontWeight: '500', color:'black'}}>5</Text>/5
                        </Text>
                    ),
                    headerTitleAlign: "center",
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                }}
            />
            <Stack.Screen
                name="AllPets"
                options={{
                    headerTitle: () => (
                        <CustomHeader title="All Pets" subtitle="Manage Your Pets" alignment="center" />
                    ),
                    headerRight: () => null,
                    headerTitleAlign: "center",
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                }}
            />
            <Stack.Screen
                name="[petId]"
                options={{
                    headerTitle: "Pet Details",
                    headerRight: () => null,
                    headerTitleAlign: "center",
                    headerTitleStyle: { color: "#000" },
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                }}
            />
        </Stack>
    );
}