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
        console.error('Error fetching all chats:', error);
        throw error;
    }
}

export async function getUserChats(){
    try {
        const response = await api.get('/chats/user');
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve user chats. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error fetching user chats:', error);
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
        console.error('Error fetching chat by ID:', error);
        throw error;
    }
}

export async function createChat(userId) {
    console.log('Creating chat with user ID:', userId);
    try {
        const response = await api.post(`/chats/user/${userId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to create chat. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error creating chat:', error);
        throw error;
    }
}

export async function deleteChat(chatId) {
    console.log('Deleting chat with ID:', chatId);
    try {
        const response = await api.delete(`/chats/user/${chatId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to delete chat. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error deleting chat:', error);
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
        console.error('Error fetching messages:', error);
        throw error;
    }
}

export async function sendMessage(chatId, content, replyToMessageId = null, file = false) {
    try {
        const messageData = {
            chatId: chatId,
            content: content,
            replyToMessageId: replyToMessageId,
            file: file
        };
        
        console.log('Sending message:', messageData);
        const response = await api.post('/messages/send', messageData);
        
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
        console.error('Error deleting message:', error);
        throw error;
    }
}

export async function editMessage(messageId, content) {
    try {
        const response = await api.patch(`/messages/${messageId}/content`, {
            content: content
        });
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to edit message. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error editing message:', error);
        throw error;
    }
}

export async function updateMessageStatus(messageId, status) {
    try {
        const response = await api.patch(`/messages/${messageId}/status`, {
            messageStatus: status
        });
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to update message status. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error updating message status:', error);
        throw error;
    }
}

export async function reactToMessage(messageId, reaction) {
    console.log('Reacting to message with ID:', messageId, 'Reaction:', reaction);
    try {
        const response = await api.put(`/messages/${messageId}/reaction`, {
            messageReact: reaction
        });
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to react to message. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error reacting to message:', error);
        throw error;
    }
}

export async function removeReactionFromMessage(messageId) {
    try {
        const response = await api.delete(`/messages/${messageId}/reaction`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to remove reaction from message. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error removing reaction from message:', error);
        throw error;
    }
}

export async function getReactionsForMessage(messageId) {
    try {
        const response = await api.get(`/messages/${messageId}/reactions`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve message reactions. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error fetching message reactions:', error);
        throw error;
    }
}

// UserChat Functions
export async function getUserChatByChatId(chatId) {
    try {
        const response = await api.get(`/chats/${chatId}/user-chat`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve user chat. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error fetching user chat by chat ID:', error);
        // Return null if user chat doesn't exist (this is expected for new chats)
        if (error.response?.status === 404) {
            return null;
        }
        throw error;
    }
}

export async function partialUpdateUserChat(chatId, updates) {
    try {
        console.log('Updating user chat:', chatId, updates);
        const response = await api.patch(`/chats/${chatId}`, updates);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to update user chat. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error updating user chat:', error);
        throw error;
    }
}

export async function deleteUserChat(userChatId) {
    try {
        const response = await api.delete(`/chats/user/${userChatId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to delete user chat. Please try again later.');
        }
        return response.data;
    } catch (error) {
        console.error('Error deleting user chat:', error);
        throw error;
    }
}