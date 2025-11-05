import * as SecureStore from 'expo-secure-store';

export const saveOnboardingStatus = async (value) => {
    await SecureStore.setItemAsync('hasSeenOnboarding', value ? 'true' : 'false');
}

export const getOnboardingStatus = async () => {
    const value = await SecureStore.getItemAsync('hasSeenOnboarding');
    return value === 'true';
}