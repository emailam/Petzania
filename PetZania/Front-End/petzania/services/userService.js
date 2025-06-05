import api from '@/api/axiosInstance';

import { getToken, clearAllTokens, saveToken} from '../storage/tokenStorage';
import { saveUserId } from '../storage/userStorage';

export async function getUserById(userId) {
    try {
        const response = await api.get(`/user/auth/${userId}`);

        if (response.status !== 200) {
            throw new Error('Failed to fetch user data. Please try again later.');
        }

        return response.data;
    } catch (error) {
        console.error('Error fetching user data:', error.response?.data?.message || error.message);
        throw error;
    }
}

export async function loginUser(data) {
    try {
        const response = await api.post('/user/auth/login', data);

        if (response.status !== 200) {
            throw new Error('Failed to login. Please check your credentials and try again.');
        }

        const { accessToken, refreshToken } = response.data.tokenDTO;
        const userId = response.data.userId;

        await saveToken('accessToken', accessToken);
        await saveToken('refreshToken', refreshToken);
        await saveUserId('userId', userId);

        return response.data;
    } catch (error) {
        console.error('Error logging in:', error.response?.data?.message || error.message);
        throw error;
    }
}

export async function updateUserData(userId, userData) {
    try {
        const response = await api.patch(`/user/auth/${userId}`, userData);

        if (response.status !== 200) {
            throw new Error('Failed to update user data. Please try again later.');
        }

        return response.data;
    } catch (error) {
        console.error('Error updating user data:', error.response?.data?.message || error.message);
        throw error;
    }
}

export async function logout(email) {
    try {
        const refreshToken = await getToken('refreshToken');

        const response = await api.post('/user/auth/logout', {
            email: email,
            refreshToken: refreshToken,
        });

        if (response.status !== 200) {
            throw new Error('Failed to logout. Please try again later.');
        }

        await clearAllTokens();
        return response.data;
    } catch (error) {
        console.error('Error logging out:', error.response?.data?.message || error.message);
        throw error;
    }
}

