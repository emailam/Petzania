import api from '@/api/axiosInstance';

export async function searchByUsername(prefix, page = 0, size = 10) {
    try {
        const response = await api.get(`/user/auth/users/${prefix}?page=${page}&size=${size}`);
        if (response.status !== 200) {
            throw new Error('Failed to search users. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}