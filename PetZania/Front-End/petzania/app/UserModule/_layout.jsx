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
    </Stack>
  );
}