import api from '@/api/axiosInstance8080';

import { getToken, clearAllTokens, saveToken} from '../storage/tokenStorage';
import { saveUserId } from '../storage/userStorage';

export async function getUserById(userId) {
    console.log('Fetching user by ID:', userId);
    try {
        const response = await api.get(`/user/auth/${userId}`);

        if (response.status !== 200) {
            throw new Error('Failed to fetch user data. Please try again later.');
        }

        return response.data;
    } catch (error) {
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
        throw error;
    }
}

export async function getUserProfilePicture(userId){
    try {
        const response = await api.get(`/user/auth/profile-picture-url/${userId}`);

        if (response.status !== 200) {
            throw new Error('Failed to fetch profile picture. Please try again later.');
        }

        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function signup(data) {
    try {
        const response = await api.post('/user/auth/signup', data);

        if (response.status !== 201) {
            throw new Error('Failed to register user. Please check your details and try again.');
        }

        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function sendResetPasswordOTP(email){
    try {
        const response = await api.put('/user/auth/send-reset-password-otp', { email });

        if (response.status !== 200) {
            throw new Error('Failed to send reset password request. Please try again later.');
        }

        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function resetPassword(email, password, otp) {
    try {
        const response = await api.put('/user/auth/reset-password', { email, password, otp });
        if (response.status !== 200) {
            throw new Error('Failed to reset password. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function changePassword(email, newPassword) {
    try {
        const response = await api.put('/user/auth/change-password', { email, newPassword });

        if (response.status !== 200) {
            throw new Error('Failed to change password. Please try again later.');
        }

        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function resendOTP(email) {
    try {
        const response = await api.post('/user/auth/resend-otp', { email });

        if (response.status !== 200) {
            throw new Error('Failed to resend OTP. Please try again later.');
        }

        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function verifyOTP(email, otp) {
    try {
        const response = await api.put('/user/auth/verify', { email, otp });

        if (response.status !== 200) {
            throw new Error('Failed to verify OTP. Please try again later.');
        }

        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function verifyResetOTP(email, otp) {
    try {
        const response = await api.put('/user/auth/verify-reset-otp', { email, otp });

        if (response.status !== 200) {
            throw new Error('Failed to verify reset OTP. Please try again later.');
        }

        return response.data;
    } catch (error) {
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
        throw error;
    }
}

export async function deleteUser(email) {
    try {
        const response = await api.delete(`/user/auth/delete`, {
            data: {
                email: email
            }
        });

        if (response.status !== 200) {
            throw new Error('Failed to delete user. Please try again later.');
        }

        await clearAllTokens();
        return response.data;
    } catch (error) {
        throw error;
    }
}