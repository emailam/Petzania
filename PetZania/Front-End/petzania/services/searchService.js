import api from '@/api/axiosInstance';

export async function searchByUsername(prefix){
    try {
        const response = await api.get(`/user/auth/users/${prefix}`);
        if (response.status !== 200) {
            throw new Error('Failed to search users. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}