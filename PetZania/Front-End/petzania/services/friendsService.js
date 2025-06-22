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

// export async function getSentFriendRequests(page = 0, size = 10, sortBy = 'createdAt', direction = 'desc') {
//     try {
//         const response = await api.get('/friends/sent-requests', {
//             params: { page, size, sortBy, direction }
//         });
//         if (response.status < 200 || response.status >= 300) {
//             throw new Error('Failed to retrieve sent friend requests. Please try again later.');
//         }
//         return response.data;
//     } catch (error) {
//         throw error;
//     }
// }

// export async function getFriendshipStatus(userId) {
//     try {
//         const response = await api.get(`/friends/status/${userId}`);
//         if (response.status < 200 || response.status >= 300) {
//             throw new Error('Failed to retrieve friendship status. Please try again later.');
//         }
//         return response.data;
//     } catch (error) {
//         throw error;
//     }
// }

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
export async function getFriends(page = 0, size = 15, sortBy = 'createdAt', direction = 'asc') {
    try {
        const response = await api.get('/friends/getFriends', {
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

export async function getNumberOfFriends() {
    try {
        const response = await api.get('/friends/getNumberOfFriends');
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve number of friends. Please try again later.');
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

export async function getFollowing(page = 0, size = 10, sortBy = 'createdAt', direction = 'asc') {
    try {
        const response = await api.get('/friends/getFollowing', {
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

export async function getFollowers(page = 0, size = 10, sortBy = 'createdAt', direction = 'asc') {
    try {
        const response = await api.get('/friends/getFollowers', {
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

export async function getNumberOfFollowing() {
    try {
        const response = await api.get('/friends/getNumberOfFollowing');
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve following count. Please try again later.');
        }
        return response.data;
    } catch (error) {
        throw error;
    }
}

export async function getNumberOfFollowers() {
    try {
        const response = await api.get('/friends/getNumberOfFollowers');
        if (response.status < 200 || response.status >= 300) {
            throw new Error('Failed to retrieve followers count. Please try again later.');
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

// export async function getNumberOfFollowersByUserId(userId) {
//     try {
//         const response = await api.get(`/friends/getNumberOfFollowers/${userId}`);
//         if (response.status < 200 || response.status >= 300) {
//             throw new Error('Failed to retrieve followers count for user. Please try again later.');
//         }
//         return response.data;
//     } catch (error) {
//         throw error;
//     }
// }

// export async function getNumberOfFollowingByUserId(userId) {
//     try {
//         const response = await api.get(`/friends/getNumberOfFollowing/${userId}`);
//         if (response.status < 200 || response.status >= 300) {
//             throw new Error('Failed to retrieve following count for user. Please try again later.');
//         }
//         return response.data;
//     } catch (error) {
//         throw error;
//     }
// }

// export async function getNumberOfFriendsByUserId(userId) {
//     try {
//         const response = await api.get(`/friends/getNumberOfFriends/${userId}`);
//         if (response.status < 200 || response.status >= 300) {
//             throw new Error('Failed to retrieve friends count for user. Please try again later.');
//         }
//         return response.data;
//     } catch (error) {
//         throw error;
//     }
// }