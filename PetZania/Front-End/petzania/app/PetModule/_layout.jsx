import { Stack } from "expo-router";
import { View, Text } from "react-native";
import CustomHeader from "@/components/CustomHeader";
import * as Progress from 'react-native-progress';
import { Dimensions } from "react-native";
import { useContext } from "react";

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
                        <Progress.Bar borderRadius={10} progress={(1/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                    </View>
                    ),
                    headerRight: () => (
                    <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                        <Text style={{fontWeight: '500', color:'black'}}>1</Text>/6
                    </Text>
                    ),
                    headerTitleAlign: "center",
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                }}
                />
                <Stack.Screen
                name="AddPet2"
                options={{
                    headerTitle: () => (
                    <View style={{width: screenWidth }}>
                        <CustomHeader title="Add Pet Profile" subtitle="Type" alignment={"center"}/>
                        <Progress.Bar borderRadius={10} progress={(2/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                    </View>
                    ),
                    headerRight: () => (
                    <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                        <Text style={{fontWeight: '500', color:'black'}}>2</Text>/6
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
                        <Progress.Bar borderRadius={10} progress={(3/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                    </View>
                    ),
                    headerRight: () => (
                    <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                        <Text style={{fontWeight: '500', color:'black'}}>3</Text>/6
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
                        <Progress.Bar borderRadius={10} progress={(4/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                    </View>
                    ),
                    headerRight: () => (
                    <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                        <Text style={{fontWeight: '500', color:'black'}}>4</Text>/6
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
                        <Progress.Bar borderRadius={10} progress={(5/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                    </View>
                    ),
                    headerRight: () => (
                    <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                        <Text style={{fontWeight: '500', color:'black'}}>5</Text>/6
                    </Text>
                    ),
                    headerTitleAlign: "center",
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                }}
                />
                <Stack.Screen
                name="AddPet6"
                options={{
                    headerTitle: () => (
                    <View style={{width: screenWidth }}>
                        <CustomHeader title="Add Pet Profile" subtitle="Finish Pets Profiles" alignment={"center"}/>
                        <Progress.Bar borderRadius={10} progress={(6/6)} width={screenWidth * 0.9} style={{margin: 10}} color='#FFC542' borderWidth={0.5} unfilledColor = {'#FFFFFF'}/>
                    </View>
                    ),
                    headerRight: () => (
                    <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                        <Text style={{fontWeight: '500', color:'black'}}>6</Text>/6
                    </Text>
                    ),
                    headerTitleAlign: "center",
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                }}
                />
                <Stack.Screen
                name="[petId]"
                options={{
                    headerTitle: () => (
                    <View style={{width: screenWidth }}>
                        <CustomHeader title="Pet Details" subtitle="Edit Pet Details" alignment={"center"}/>
                    </View>
                    ),
                    headerRight: () => (
                    <Text style={{ fontSize: 16, color: "gray", marginRight: 10 }}>
                        <Text style={{fontWeight: '500', color:'black'}}>6</Text>/6
                    </Text>
                    ),
                    headerTitleAlign: "center",
                    headerBackTitle: "",
                    headerTintColor: "#9188E5",
                    headerStyle: { backgroundColor: "#FFF" },
                }}
                />
        </Stack>
    );
}