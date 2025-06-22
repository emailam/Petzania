// postService.js - Complete service functions for post-related operations
// import { api } from './api'; // Your API configuration - uncomment when ready

/**
 * Initialize likes state from posts data
 * @param {Array} posts - Array of post objects
 * @returns {Object} Initial likes state object
 */
export const initializeLikesState = (posts) => {
  if (!posts || posts.length === 0) return {};
  
  const initialLikesState = {};
  posts.forEach((post, index) => {
    // Use postId if available, otherwise fall back to index
    const key = getPostKey(post, index);
    initialLikesState[key] = {
      isLiked: false,
      likes: post.reacts || post.likeCount || 0,
    };
  });
  
  return initialLikesState;
};

/**
 * Universal toggle like state for a specific post
 * This function works with both local state and API calls
 * @param {Object} currentLikesState - Current likes state
 * @param {string|number} postKey - Post identifier
 * @param {Object} options - Optional configuration
 * @param {boolean} options.apiCall - Whether to make an API call
 * @param {Function} options.onApiSuccess - Callback for successful API call
 * @param {Function} options.onApiError - Callback for API error
 * @returns {Object} Updated likes state
 */
export const togglePostLike = (currentLikesState, postKey, options = {}) => {
  const { apiCall = false, onApiSuccess, onApiError } = options;
  
  const currentState = currentLikesState[postKey];
  if (!currentState) return currentLikesState;
  
  const newState = {
    ...currentLikesState,
    [postKey]: {
      isLiked: !currentState.isLiked,
      likes: currentState.isLiked 
        ? currentState.likes - 1 
        : currentState.likes + 1,
    }
  };

  // If API call is requested, make the call
  if (apiCall) {
    togglePostLikeAPI(postKey, currentState.isLiked)
      .then((result) => {
        if (onApiSuccess) onApiSuccess(result);
      })
      .catch((error) => {
        if (onApiError) onApiError(error);
        console.error('API call failed for like toggle:', error);
      });
  }

  return newState;
};

/**
 * API call to toggle like status on server
 * @param {string|number} postId - Post identifier
 * @param {boolean} currentlyLiked - Current like status
 * @returns {Promise} API response
 */
export const togglePostLikeAPI = async (postId, currentlyLiked) => {
  try {
    // Replace this with your actual API call
    // const response = await api.request({
    //   method: currentlyLiked ? 'DELETE' : 'POST',
    //   url: `/posts/${postId}/like`,
    //   headers: { 'Content-Type': 'application/json' }
    // });
    // return response.data;
    
    // Mock implementation for now
    console.log(`${currentlyLiked ? 'Unliking' : 'Liking'} post ${postId}`);
    return { 
      success: true, 
      isLiked: !currentlyLiked,
      postId: postId,
      newLikeCount: currentlyLiked ? -1 : 1 // relative change
    };
  } catch (error) {
    console.error('Error toggling post like:', error);
    throw new Error('Failed to toggle post like');
  }
};

/**
 * Fetch all posts created by a specific user
 * @param {string} userId - The user's ID
 * @returns {Promise<Array>} Array of user's posts
 */
export const fetchUserPosts = async (userId) => {
  try {
    // const response = await api.get(`/posts/user/${userId}`);
    // return response.data;
    
    // Mock implementation for now
    console.log(`Fetching posts for user ${userId}`);
    return [];
  } catch (error) {
    console.error('Error fetching user posts:', error);
    throw error;
  }
};

/**
 * Delete a post
 * @param {string} postId - The post ID to delete
 * @returns {Promise<void>}
 */
export const deletePost = async (postId) => {
  try {
    // await api.delete(`/posts/${postId}`);
    
    // Mock implementation for now
    console.log(`Deleting post ${postId}`);
  } catch (error) {
    console.error('Error deleting post:', error);
    throw error;
  }
};

/**
 * Update a post with new data
 * @param {string} postId - The post ID to update
 * @param {Object} postData - Updated post data
 * @returns {Promise<Object>} Updated post object
 */
export const updatePost = async (postId, postData) => {
  try {
    // const response = await api.put(`/posts/${postId}`, postData);
    // return response.data;
    
    // Mock implementation for now
    console.log(`Updating post ${postId}:`, postData);
    return { ...postData, postId, updatedAt: new Date().toISOString() };
  } catch (error) {
    console.error('Error updating post:', error);
    throw error;
  }
};

/**
 * Upload an image file
 * @param {string} imageUri - Local image URI
 * @returns {Promise<string>} URL of uploaded image
 */
export const uploadImage = async (imageUri) => {
  try {
    // const formData = new FormData();
    // formData.append('image', {
    //   uri: imageUri,
    //   type: 'image/jpeg',
    //   name: `image_${Date.now()}.jpg`,
    // });
    // 
    // const response = await api.post('/upload/image', formData, {
    //   headers: { 'Content-Type': 'multipart/form-data' }
    // });
    // return response.data.url;
    
    // Mock implementation for now
    console.log(`Uploading image: ${imageUri}`);
    return `https://mock-storage.com/images/image_${Date.now()}.jpg`;
  } catch (error) {
    console.error('Error uploading image:', error);
    throw error;
  }
};

/**
 * Record user's interest response to a post
 * @param {string} postId - The post ID
 * @param {string} userId - The user's ID
 * @param {string} response - 'interested' or 'not interested'
 * @returns {Promise<void>}
 */
export const recordInterestResponse = async (postId, userId, response) => {
  try {
    // await api.post(`/posts/${postId}/interest`, {
    //   userId,
    //   response,
    //   timestamp: new Date().toISOString(),
    // });
    
    // Mock implementation for now
    console.log(`Recording interest for post ${postId}: ${response} by user ${userId}`);
  } catch (error) {
    console.error('Error recording interest response:', error);
    throw error;
  }
};

/**
 * Get post statistics for a user's posts
 * @param {string} userId - The user's ID
 * @returns {Promise<Object>} Statistics object
 */
export const getUserPostStats = async (userId) => {
  try {
    // const response = await api.get(`/posts/user/${userId}/stats`);
    // return response.data;
    
    // Mock implementation for now
    console.log(`Fetching stats for user ${userId}`);
    return {
      totalPosts: 0,
      totalLikes: 0,
      totalInterested: 0,
      completedPosts: 0
    };
  } catch (error) {
    console.error('Error fetching post stats:', error);
    throw error;
  }
};

/**
 * Update post status (pending, completed, expired)
 * @param {string} postId - The post ID
 * @param {string} status - New status
 * @returns {Promise<Object>} Updated post
 */
export const updatePostStatus = async (postId, status) => {
  try {
    // const response = await api.patch(`/posts/${postId}/status`, { status });
    // return response.data;
    
    // Mock implementation for now
    console.log(`Updating post ${postId} to status: ${status}`);
    return { success: true, postId, status };
  } catch (error) {
    console.error('Error updating post status:', error);
    throw error;
  }
};

/**
 * Get interested users for a post
 * @param {string} postId - The post ID
 * @returns {Promise<Array>} Array of interested users
 */
export const getInterestedUsers = async (postId) => {
  try {
    // const response = await api.get(`/posts/${postId}/interested-users`);
    // return response.data;
    
    // Mock implementation for now
    console.log(`Fetching interested users for post ${postId}`);
    return [];
  } catch (error) {
    console.error('Error fetching interested users:', error);
    throw error;
  }
};

/**
 * Send a message to pet owner
 * @param {string} ownerId - The owner's user ID
 * @param {string} message - Message content
 * @returns {Promise<Object>} Response object
 */
export const sendMessageToOwner = async (ownerId, message) => {
  try {
    // await api.post('/messages', {
    //   recipientId: ownerId,
    //   message,
    //   timestamp: new Date().toISOString(),
    // });
    
    // Mock implementation for now
    console.log(`Sending message to owner ${ownerId}:`, message);
    return { success: true, messageId: `mock-message-${Date.now()}` };
  } catch (error) {
    console.error('Error sending message:', error);
    throw error;
  }
};

/**
 * Fetch post details with additional metadata
 * @param {string} postId - The post ID
 * @returns {Promise<Object>} Detailed post object
 */
export const fetchPostDetails = async (postId) => {
  try {
    // const response = await api.get(`/posts/${postId}/details`);
    // return response.data;
    
    // Mock implementation for now
    console.log(`Fetching details for post ${postId}`);
    return {
      "postId": postId,
      "ownerId": "8e2a7ad4-4a2f-4b3d-b5f1-1a2c3e4d5f6a",
      "petDTO": {
        "petId": "1c2d3e4f-5a6b-7c8d-9e0f-1234567890ab",
        "name": "Bella",
        "description": "A friendly Maltipoo looking for a loving home.",
        "gender": "FEMALE",
        "dateOfBirth": "2023-03-15",
        "age": "1 year",
        "breed": "Maltipoo",
        "species": "DOG",
        "myVaccinesURLs": [],
        "myPicturesURLs": [
          "https://hips.hearstapps.com/hmg-prod/images/small-fluffy-dog-breeds-maltipoo-66300ad363389.jpg?crop=0.668xw:1.00xh;0.151xw,0&resize=640:*",
          "https://example.com/image2.jpg",
          "https://example.com/image3.jpg"
        ]
      },
      "postStatus": "PENDING",
      "reactedUsersIds": [
        "11111111-2222-3333-4444-555555555555",
        "66666666-7777-8888-9999-000000000000"
      ]
    };
  } catch (error) {
    console.error('Error fetching post details:', error);
    throw error;
  }
};

/**
 * Fetch owner details
 * @param {string} ownerId - The owner's user ID
 * @returns {Promise<Object>} Owner details
 */
export const fetchOwnerDetails = async (ownerId) => {
  try {
    // const response = await api.get(`/users/${ownerId}`);
    // return response.data;
    
    // Mock implementation for now
    console.log(`Fetching owner details for ${ownerId}`);
    return {
      "name": "Shawky Ibrahim",
      "profilePictureURL": "https://hips.hearstapps.com/hmg-prod/images/small-fluffy-dog-breeds-maltipoo-66300ad363389.jpg?crop=0.668xw:1.00xh;0.151xw,0&resize=640:*",
    };
  } catch (error) {
    console.error('Error fetching owner details:', error);
    throw error;
  }
};

/**
 * Function to fetch all posts data - to be used in the usePostsData hook
 * @returns {Promise<Array>} Array of all posts
 */
export const fetchAllPosts = async () => {
  try {
    // const response = await api.get('/posts');
    // return response.data;
    
    // For now, we'll import the JSON data
    // Note: In a real implementation, this would be an API call
    const PostsData = await import('../pets.json');
    return PostsData.default || PostsData;
  } catch (error) {
    console.error('Error fetching posts:', error);
    throw error;
  }
};

/**
 * Function to get users who reacted to a post
 * @param {string} postId - Post identifier
 * @param {Array} reactedUsersIds - Array of user IDs who reacted
 * @returns {Promise<Array>} Array of user objects
 */
export const getPostReactedUsers = async (postId, reactedUsersIds) => {
  try {
    // const response = await api.get(`/posts/${postId}/reacted-users`);
    // return response.data;
    
    // Mock implementation for now
    console.log(`Fetching reacted users for post ${postId}:`, reactedUsersIds);
    
    // Mock user data based on the IDs
    const mockUsers = reactedUsersIds.map((id, index) => ({
      id,
      name: `User ${index + 1}`,
      profilePictureURL: `https://via.placeholder.com/40x40?text=U${index + 1}`,
    }));
    
    return mockUsers;
  } catch (error) {
    console.error('Error fetching reacted users:', error);
    throw error;
  }
};

// ==================== UTILITY FUNCTIONS ====================

/**
 * Get post key for consistent identification
 * @param {Object} post - Post object
 * @param {number} index - Fallback index
 * @returns {string|number} Post key
 */
export const getPostKey = (post, index) => {
  return post?.postId || post?.id || `post_${index}`;
};

/**
 * Get default filter configuration
 * @returns {Object} Default filters object
 */
export const getDefaultFilters = () => ({
  category: 'All',
  breedFilter: '',
  genderFilter: 'All',
  ageMin: '',
  ageMax: '',
  sortOrder: 'desc',
  sortBy: 'date'
});

/**
 * Format reacts data for display
 * @param {string} postId - Post identifier
 * @param {Array} reactedUsersIds - Array of user IDs who reacted
 * @returns {Object} Formatted reacts data
 */
export const formatReactsData = (postId, reactedUsersIds = []) => ({
  postId,
  reactedUsersIds,
  count: reactedUsersIds.length,
  hasReactions: reactedUsersIds.length > 0
});

/**
 * Validate post data
 * @param {Object} post - Post object to validate
 * @returns {boolean} Whether post is valid
 */
export const isValidPost = (post) => {
  return post && (post.postId || post.id !== undefined);
};

/**
 * Get likes count for a post
 * @param {Object} likesState - Current likes state
 * @param {string|number} postKey - Post identifier
 * @param {Object} fallbackPost - Original post data for fallback
 * @returns {number} Likes count
 */
export const getLikesCount = (likesState, postKey, fallbackPost = {}) => {
  return likesState[postKey]?.likes || fallbackPost.reacts || fallbackPost.likeCount || 0;
};

/**
 * Check if post is liked
 * @param {Object} likesState - Current likes state
 * @param {string|number} postKey - Post identifier
 * @returns {boolean} Whether post is liked
 */
export const isPostLiked = (likesState, postKey) => {
  return likesState[postKey]?.isLiked || false;
};

/**
 * Helper function to calculate time ago from createdAt
 * Enhanced version with more granular time units
 * @param {string} createdAt - ISO date string or timestamp
 * @returns {string} Human readable time ago
 */
export const getTimeAgo = (createdAt) => {
  if (!createdAt) return 'Just now';
  
  const now = new Date();
  const created = new Date(createdAt);
  const diffInMs = now - created;
  const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
  
  if (diffInMinutes < 1) return 'Just now';
  if (diffInMinutes < 60) return `${diffInMinutes}m ago`;
  
  const diffInHours = Math.floor(diffInMinutes / 60);
  if (diffInHours < 24) return `${diffInHours}h ago`;
  
  const diffInDays = Math.floor(diffInHours / 24);
  if (diffInDays < 7) return `${diffInDays}d ago`;
  
  const diffInWeeks = Math.floor(diffInDays / 7);
  if (diffInWeeks < 4) return `${diffInWeeks}w ago`;
  
  const diffInMonths = Math.floor(diffInDays / 30);
  return `${diffInMonths}mo ago`;
};

/**
 * Helper function to extract age in months from age string or date of birth
 * @param {Object} pet - Pet object containing age information
 * @returns {number} Age in months
 */
export const getAgeInMonths = (pet) => {
  if (pet.petDTO?.dateOfBirth) {
    const birthDate = new Date(pet.petDTO.dateOfBirth);
    const now = new Date();
    const diffTime = Math.abs(now - birthDate);
    const diffMonths = Math.ceil(diffTime / (1000 * 60 * 60 * 24 * 30.44)); // average days per month
    return diffMonths;
  }
  
  // Fallback to parsing age string
  const ageStr = pet.petDTO?.age || '0';
  const ageMatch = ageStr.match(/(\d+(?:\.\d+)?)/);
  if (!ageMatch) return 0;
  
  const ageValue = parseFloat(ageMatch[1]);
  if (ageStr.includes('year')) {
    return ageValue * 12;
  } else if (ageStr.includes('month')) {
    return ageValue;
  }
  return ageValue; // assume months if no unit specified
};

/**
 * Helper function to map species to category
 * @param {string} species - Pet species
 * @returns {string} Category string
 */
export const getCategory = (species) => {
  if (!species) return 'All';
  const speciesLower = species.toLowerCase();
  if (speciesLower === 'dog') return 'Dog';
  if (speciesLower === 'cat') return 'Cat';
  return 'All';
};

/**
 * Validate post data for creation/update
 * @param {Object} postData - Post data to validate
 * @returns {Array} Array of error messages (empty if valid)
 */
export const validatePostData = (postData) => {
  const errors = [];
  
  if (!postData.petDTO?.name?.trim()) {
    errors.push('Pet name is required');
  }
  
  if (!postData.petDTO?.myPicturesURLs?.length) {
    errors.push('At least one image is required');
  }
  
  if (!postData.location?.trim()) {
    errors.push('Location is required');
  }
  
  return errors;
};

/**
 * Format post data for API submission
 * @param {Object} formData - Form data from UI
 * @param {Object} existingPost - Existing post data (for updates)
 * @returns {Object} Formatted post object
 */
export const formatPostForAPI = (formData, existingPost = null) => {
  return {
    ...existingPost,
    petDTO: {
      ...existingPost?.petDTO,
      name: formData.name,
      gender: formData.gender,
      breed: formData.breed,
      age: formData.age,
      description: formData.description,
      myPicturesURLs: formData.images,
    },
    location: formData.location,
    status: formData.status,
    updatedAt: new Date().toISOString(),
  };
};