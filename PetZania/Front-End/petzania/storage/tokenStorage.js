import * as SecureStore from 'expo-secure-store';

export const saveToken = async (key, value) => {
    await SecureStore.setItemAsync(key, value);
};

export const getToken = async (key) => {
    return await SecureStore.getItemAsync(key);
};

export const deleteToken = async (key) => {
    await SecureStore.deleteItemAsync(key);
};

export const clearAllTokens = async () => {
    await SecureStore.deleteItemAsync('accessToken');
    await SecureStore.deleteItemAsync('refreshToken');
    await SecureStore.deleteItemAsync('userId');
};