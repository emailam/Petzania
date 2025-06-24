import api from '@/api/axiosInstance8081';

// Chat Functions
export async function getAllChats(){
    try {
        const response = await api.get('/chats');
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve chats. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function getChatById(chatId) {
    try {
        const response = await api.get(`/chats/${chatId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve chat. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function createChat(userId) {
    try {
        const response = await api.post(`/chats/user/${userId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to create chat. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}


// Message Functions
export async function getMessagesByChatId(chatId, page = 0, size = 20) {
    try {
        const response = await api.get(`/messages/chat/${chatId}`, {
            params: { page, size }
        });
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve messages. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function sendMessage(chatId, content, replyToMessageId = null, file = false) {
    try {
        const response = await api.post('/messages/send', {
            chatId: chatId,
            content: content,
            replyToMessageId: replyToMessageId,
            file: file
        });
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to send message. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error sending message:', error);
        throw error;
    }
}

export async function deleteMessage(messageId) {
    try {
        const response = await api.delete(`/messages/${messageId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to delete message. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function markMessageAsRead(messageId) {
    try {
        const response = await api.put(`/messages/${messageId}/read`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to mark message as read. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}