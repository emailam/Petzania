import api from "@/api/axiosInstance8082"
import { useMutation , useInfiniteQuery, useQueryClient } from '@tanstack/react-query';
import { getPetById } from './petService';

async function fetchPosts(filters, page = 0, pageSize = 10) {
  try {
    const { data } = await api.get('/pet-posts/filtered', {
      params: { ...filters, page, pageSize },
    });
    return data;
  } catch (error) {
    console.error('Error fetching posts:', error.response?.data?.message || error.message);
    throw error;
  }
}

export const useFetchPosts = (filters) => {
  const newfilters = {
      ...filters,petPostStatus:"PENDING"}
  console.log('usePosts called with filters:', newfilters);
  return useInfiniteQuery({
    queryKey: ['posts', newfilters],
    queryFn: ({ pageParam = 0 }) => fetchPosts(newfilters, pageParam),
    getNextPageParam: (lastPage) => {
      const next = lastPage.page + 1;
      const max = Math.ceil(lastPage.totalCount / lastPage.pageSize);
      return next < max ? next : undefined;
    },
    staleTime: 1000 * 60 * 2,
    placeholderData: (previousData) => previousData, // keepPreviousData is now placeholderData
  });
};

async function toggleLike({ postId }) {
  try {
    const response = await api.put(`/api/posts/${postId}/react`);
    return response.data;
  } catch (error) {
    // Optionally log, transform, or wrap the error here
    throw error;
  }
}

// Exported React Query mutation hook
export function useToggleLike() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: toggleLike,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries(['posts']);
      queryClient.invalidateQueries(['post', variables.postId]);
    },
  });
}

async function getUserPostsPaginated({ pageParam = 0, userId }) {
  try {
    const response = await api.get(`/pet-posts/user/${userId}`, {
      params: {
        page: pageParam,
        size: 20,
      },
    });
    console.log('Paginated user posts response:', response.data);
    const { content, last } = response.data;
    return {
      posts: content,
      hasNext: !last,
    };
  } catch (error) {
    //console.error('Error fetching paginated user postaaaaaaaaaaaaaaaas:', error.response?.data?.message || error.message);
    throw error;
  }
}

export function useUserPostsInfinite(userId) {
  return useInfiniteQuery({
    queryKey: ['user-posts-infinite', userId],
    queryFn: ({ pageParam = 0 }) => getUserPostsPaginated({ pageParam, userId }),
    getNextPageParam: (lastPage, allPages) => {
      // Customize based on your API response
      return lastPage.hasNext ? allPages.length : undefined;
    },
    enabled: !!userId,
  });
}

async function deletePost(postId) {
  try {
    const response = await api.delete(`/pet-posts/${postId}`);
    return response.data;
  } catch (error) {
    //console.error('Error deleting post:', error.response?.data?.message || error.message);
    throw error;
  }
}

export function useDeletePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deletePost,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['userPosts'] });
    },
    onError: (error) => {
      //console.error('Delete post failed:', error.response?.data?.message || error.message);
    },
  });
}

async function createPost(postData) {
  try {
    console.log('Creating post with data:', postData);
    const { petId, ...rest } = postData;
    const petDTO = await getPetById(petId);
    
    const payload = {
      ...rest,
      petDTO: petDTO,
    };
    console.log('Payload for post creation:', payload);
    const response = await api.post('/pet-posts', payload);
    return response.data;
  } catch (error) {
    console.error('Error creating post:', error.response?.data?.message || error.message);
    throw error; // re-throw to let React Query or caller handle it
  }
}
export function useCreatePost() {
  return useMutation({
    mutationFn: createPost,
  });
}
async function updatePost(postId, newPostData) {
  try {
    console.log('Updating post with ID:', postId, 'and data:', newPostData);    
    //console.log('Payload for post update:', payload);
    const response = await api.patch(`/pet-posts/${postId}`, newPostData);
    //console.log('Post updated successfully:', response.data);
    return response.data;
  } catch (error) {
    console.error('Error updating post:', error.response?.data?.message || error.message);
    throw error; // re-throw to let React Query or caller handle it
  }
}
export function useUpdatePost() {
  return useMutation({
    mutationFn: ({ postId, newPostData }) => updatePost(postId, newPostData),
    onSuccess: (data, variables) => {
      //console.log('Post updated successfully:', data);
      // Optionally invalidate queries or perform other actions
      // queryClient.invalidateQueries({ queryKey: ['posts'] });
    },
    onError: (error) => {
      //console.error('Error updating post:', error.response?.data?.message || error.message);
    },
  })
  };
async function getPostById(postId) {
  try {
    const response = await api.get(`/pet-posts/${postId}`);
    return response.data;
  } catch (error) {
    //console.error('Error fetching post by ID:', error.response?.data?.message || error.message);
    throw error; // rethrow to let React Query or caller handle it
  }
}

// exported useQuery hook
export function useGetPostById(postId) {
  return useQuery({
    queryKey: ['post', postId], // <-- Array with ID!
    queryFn: () => getPostById(postId),
    staleTime: 1000 * 60 * 2, // 2 minutes
    cacheTime: 1000 * 60 * 5, // 5 minutes
    retry: false,
    enabled: !!postId, // <-- Only fetch when postId is truthy
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
}
// ==================== UTILITY FUNCTIONS ====================

export const getPostKey = (post, index) => {
  return post?.postId || post?.id || `post_${index}`;
};

export const getDefaultFilters = () => ({
  category: 'All',
  breedFilter: '',
  genderFilter: 'All',
  ageMin: '',
  ageMax: '',
  sortOrder: '',
  sortBy: ''
});

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

export const getAgeInMonths = (pet) => {
  if (pet.petDTO?.dateOfBirth) {
    const birthDate = new Date(pet.petDTO.dateOfBirth);
    const now = new Date();
    const diffTime = Math.abs(now - birthDate);
    const diffMonths = Math.ceil(diffTime / (1000 * 60 * 60 * 24 * 30.44));
    return diffMonths;
  }
  
  const ageStr = pet.petDTO?.age || '0';
  const ageMatch = ageStr.match(/(\d+(?:\.\d+)?)/);
  if (!ageMatch) return 0;
  
  const ageValue = parseFloat(ageMatch[1]);
  if (ageStr.includes('year')) {
    return ageValue * 12;
  } else if (ageStr.includes('month')) {
    return ageValue;
  }
  return ageValue;
};
// Add these to your postService.js file if they don't exist