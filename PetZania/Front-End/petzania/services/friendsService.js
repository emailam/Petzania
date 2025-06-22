import api from '@/api/axiosInstance8081.jsx';

// Friend Request Functions
export async function sendFriendRequest(receiverId) {
    try {
        const response = await api.post(`/friends/send-request/${receiverId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to send friend request. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function getReceivedFriendRequests(page = 0, size = 10, sortBy = 'createdAt', direction = 'desc') {
    try {
        const response = await api.get('/friends/received-requests', {
            params: { page, size, sortBy, direction }
        });
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve friend requests. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function isFriendRequestExists(receiverId) {
    try {
        const response = await api.get(`/friends/isFriendRequestExists/${receiverId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to check if friend request exists. Please try again later.');
        }
        // Returns the friend request UUID if exists
        return response.data;
    } catch (error) {
        // Handle 404 as "no friend request exists" - this is expected behavior
        if (error.response && error.response.status === 404) {
            return null; // Return null to indicate no friend request exists
        }
        
        console.error('isFriendRequestExists error:', error);
        throw error;
    }
}

export async function acceptFriendRequest(requestId) {
    try {
        const response = await api.post(`/friends/accept-request/${requestId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to accept friend request. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function cancelFriendRequest(requestId) {
    try {
        const response = await api.put(`/friends/cancel-request/${requestId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to cancel friend request. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

// Friends Functions
export async function getFriendsByUserId(page = 0, size = 15, sortBy = 'createdAt', direction = 'asc', userId) {
    try {
        const response = await api.get(`/friends/getFriends/${userId}`, {
            params: { page, size, sortBy, direction }
        });
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve friends list. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function getNumberOfFriendsByUserId(userId) {
    try {
        const response = await api.get(`/friends/getNumberOfFriends/${userId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve friends count for user. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function removeFriend(friendId) {
    try {
        const response = await api.delete(`/friends/remove/${friendId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to remove friend. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function isFriend(userId) {
    try {
        const response = await api.get(`/friends/isFriend/${userId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to check friendship status. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

// Add missing function to check if user is blocked
export async function isBlockingExists(userId) {
    try {
        const response = await api.get(`/friends/isBlockingExists/${userId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to check blocking status. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

// Follow Functions
export async function followUser(userId) {
    try {
        const response = await api.post(`/friends/follow/${userId}`);
        console.log('Follow response:', response);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to follow user. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function unfollowUser(userId) {
    try {
        const response = await api.put(`/friends/unfollow/${userId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to unfollow user. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function getFollowersByUserId(page = 0, size = 10, sortBy = 'createdAt', direction = 'asc', userId) {
    try {
        const response = await api.get(`/friends/getFollowers/${userId}`, {
            params: { page, size, sortBy, direction }
        });
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve followers list. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function getNumberOfFollowersByUserId(userId) {
    try {
        const response = await api.get(`/friends/getNumberOfFollowers/${userId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve followers count for user. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function getFollowingByUserId(page = 0, size = 10, sortBy = 'createdAt', direction = 'asc', userId) {
    try {
        const response = await api.get(`/friends/getFollowing/${userId}`, {
            params: { page, size, sortBy, direction }
        });
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve following list. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function getNumberOfFollowingByUserId(userId) {
    try {
        const response = await api.get(`/friends/getNumberOfFollowing/${userId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve following count for user. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function isFollowing(userId) {
    try {
        const response = await api.get(`/friends/isFollowing/${userId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to check following status. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

// Block Functions
export async function blockUser(userId) {
    try {
        const response = await api.post(`/friends/block/${userId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to block user. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function unblockUser(userId) {
    try {
        const response = await api.put(`/friends/unblock/${userId}`);
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to unblock user. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function getNumberOfBlockedUsers() {
    try {
        const response = await api.get('/friends/getNumberOfBlockedUsers');
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve blocked users count. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function getBlockedUsers(page = 0, size = 10, sortBy = 'createdAt', direction = 'asc') {
    try {
        const response = await api.get('/friends/getBlockedUsers', {
            params: { page, size, sortBy, direction }
        });
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve blocked users list. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}