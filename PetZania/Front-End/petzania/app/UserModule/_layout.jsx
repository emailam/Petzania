import { Stack } from 'expo-router';

export default function UserModuleLayout() {
  return (
    <Stack>
      <Stack.Screen
        name="[userid]"
        options={{
            headerShown: true,
            headerTitle: '',
            headerBackTitleVisible: false,
            headerTintColor: '#9188E5',
            headerStyle: { backgroundColor: '#FFF' },
        }}
      />
      <Stack.Screen
        name="EditProfile"
        options={{
            headerShown: true,
            headerTitle: 'Edit Profile',
            headerTitleStyle: { color: '#000' },
            headerTitleAlign: 'center',
            headerBackTitleVisible: false,
            headerTintColor: '#9188E5',
            headerStyle: { backgroundColor: '#FFF' },
        }}
      />
    </Stack>
  );
}