import api from '@/api/axiosInstance';

export async function addFriend(receiverId) {
    try {
        const response = await api.post(`/friends/send-request/${receiverId}`);
        if (response.status !== 200) {
            throw new Error('Failed to add friend. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error adding friend:', error.response?.data?.message || error.message);
        throw error;
    }
}
