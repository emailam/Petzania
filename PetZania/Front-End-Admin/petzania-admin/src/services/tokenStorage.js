
export const saveToken = async (key,value) => {
    await sessionStorage.setItem(key, value);
}
export const getToken = async (key) => {
    const token = await sessionStorage.getItem(key);
    return token;
}
export const removeToken = async (key) => {
    await sessionStorage.removeItem(key);
}
export const clearAllTokens = async () => {
    await sessionStorage.removeItem('accessToken');
    await sessionStorage.removeItem('refreshToken');
}