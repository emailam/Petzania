import api from '@/api/axiosInstance8083.jsx';

// Notification Functions
export async function getAllNotifications(page = 0, size = 10, sortBy = 'createdAt', direction = 'desc') {
    try {
        const response = await api.get('/notifications', {
            params: { page, size, sortBy, direction }
        });
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve notifications. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function getUnreadNotificationCount() {
    try {
        const response = await api.get('/notifications/unread-count');
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve unread notification count. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function markNotificationAsRead(notificationId) {
    try {
        const response = await api.put(`/notifications/mark-read/${notificationId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to mark notification as read. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function markAllNotificationsAsRead() {
    try {
        const response = await api.put('/notifications/mark-all-read');
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to mark all notifications as read. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function deleteNotification(notificationId) {
    try {
        const response = await api.delete(`/notifications/${notificationId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to delete notification. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}