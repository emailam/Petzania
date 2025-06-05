import * as SecureStore from 'expo-secure-store';

export const saveUserId = async (key, value) => {
    await SecureStore.setItemAsync(key, value);
};

export const getUserId = async (key) => {
    return await SecureStore.getItemAsync(key);
};