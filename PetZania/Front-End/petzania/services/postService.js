// postService.js - Service functions for post-related operations

/**
 * Initialize likes state from posts data
 * @param {Array} posts - Array of post objects
 * @returns {Object} Initial likes state object
 */
export const initializeLikesState = (posts) => {
  if (!posts || posts.length === 0) return {};
  
  const initialLikesState = {};
  posts.forEach((post) => {
    // Use postId if available, otherwise fall back to index
    const key = post.postId;
    initialLikesState[key] = {
      isLiked: false,
      likes: post.reacts || 0,
    };
  });
  
  return initialLikesState;
};

/**
 * Toggle like state for a specific post
 * @param {Object} currentLikesState - Current likes state
 * @param {string|number} postKey - Post identifier
 * @returns {Object} Updated likes state
 */
export const togglePostLike = (currentLikesState, postKey) => {
  const currentState = currentLikesState[postKey];
  if (!currentState) return currentLikesState;
  
  return {
    ...currentLikesState,
    [postKey]: {
      isLiked: !currentState.isLiked,
      likes: currentState.isLiked 
        ? currentState.likes - 1 
        : currentState.likes + 1,
    }
  };
};

/**
 * Get post key for consistent identification
 * @param {Object} post - Post object
 * @param {number} index - Fallback index
 * @returns {string|number} Post key
 */
export const getPostKey = (post, index) => {
  return post.postId || post.id || index;
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