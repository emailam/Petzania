import api from "@/api/axiosInstance8082"
import { useMutation , useInfiniteQuery, useQueryClient } from '@tanstack/react-query';
import { getPetById } from './petService';
async function fetchPosts(filters, page = 0, pageSize = 10) {
  try {
    // Put pagination parameters in the URL as query parameters
    // Put filters in the request body
    const response = await api.post(`/pet-posts/filtered?page=${page}&size=${pageSize}`, {
      ...filters,
    });

    const { content, last, number } = response.data;

    return {
      posts: content,
      hasNext: !last,
      currentPage: number, // Include current page for debugging
    };
  } catch (error) {
    console.error('Error fetching posts:', error.response?.data?.message || error.message);
    throw error;
  }
}

export const useFetchPosts = (filters) => {
  const newfilters = { ...filters, petPostStatus: "PENDING" };

  return useInfiniteQuery({
    queryKey: ['posts', newfilters],
    queryFn: ({ pageParam = 0 }) => {
      // Pass pageParam as the page parameter, and include filters
      console.log('Fetching page:', pageParam); // Add this for debugging
      return fetchPosts(newfilters, pageParam);
    },
    getNextPageParam: (lastPage, allPages) => {
      // Since pages are 0-indexed, the next page number should be the current length
      // allPages.length gives us the next page number (0-indexed)
      return lastPage.hasNext ? allPages.length : undefined;
    },
    staleTime: 1000 * 60 * 2,
    placeholderData: previousData => previousData,
  });
};

async function toggleLike({ postId }) {
  try {
    const response = await api.put(`/pet-posts/${postId}/react`);
    return response.data;
  } catch (error) {
    console.error('Error toggling like:', error.response?.data?.message || error.message);
    throw error;
  }
}

export function useToggleLike() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: toggleLike,
    onSuccess: (data, variables) => {
      // Invalidate all posts queries
      queryClient.invalidateQueries({ queryKey: ['posts'] });
      queryClient.invalidateQueries({ queryKey: ['user-posts-infinite'] });
      queryClient.invalidateQueries({ queryKey: ['post', variables.postId] });
    },
    onError: (error) => {
      console.error('Toggle like failed:', error.response?.data?.message || error.message);
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
    const { content, last } = response.data;
    return {
      posts: content,
      hasNext: !last,
    };
  } catch (error) {
    throw error;
  }
}

export function useUserPostsInfinite(userId) {
  return useInfiniteQuery({
    queryKey: ['user-posts-infinite', userId],
    queryFn: ({ pageParam = 0 }) => getUserPostsPaginated({ pageParam, userId }),
    getNextPageParam: (lastPage, allPages) => {
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
    throw error;
  }
}

export function useDeletePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deletePost,
    onSuccess: (data, variables) => {
      // Invalidate all relevant queries
      queryClient.invalidateQueries({ queryKey: ['posts'] });
      queryClient.invalidateQueries({ queryKey: ['user-posts-infinite'] });
      queryClient.removeQueries({ queryKey: ['post', variables] });
    },
    onError: (error) => {
      console.error('Delete post failed:', error.response?.data?.message || error.message);
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
    throw error;
  }
}

export function useCreatePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createPost,
    onSuccess: (data, variables) => {
      // Invalidate posts queries to show the new post
      queryClient.invalidateQueries({ queryKey: ['posts'] });
      queryClient.invalidateQueries({ queryKey: ['user-posts-infinite'] });
    },
    onError: (error) => {
      console.error('Create post failed:', error.response?.data?.message || error.message);
    },
  });
}

async function updatePost(postId, newPostData) {
  try {
    console.log('Updating post with ID:', postId, 'and data:', newPostData);    
    const response = await api.patch(`/pet-posts/${postId}`, newPostData);
    return response.data;
  } catch (error) {
    console.error('Error updating post:', error.response?.data?.message || error.message);
    throw error;
  }
}

export function useUpdatePost(filters, userId) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: ({ postId, newPostData }) =>
      updatePost(postId, newPostData),
    onSuccess: (updated, { postId }) => {
      // 1) Immediately patch the 'posts' infinite list
      qc.setQueryData(
        ['posts', filters],
        old => old && {
          pageParams: old.pageParams,
          pages: old.pages.map(page => ({
            ...page,
            data: page.data.map(p =>
              p.id === postId ? { ...p, ...updated } : p
            )
          }))
        }
      );
      // 2) Patch the user's infinite list
      qc.setQueryData(
        ['user-posts-infinite', userId],
        old => old && {
          pageParams: old.pageParams,
          pages: old.pages.map(page => ({
            ...page,
            posts: page.posts.map(p =>
              p.id === postId ? { ...p, ...updated } : p
            )
          }))
        }
      );
      // 3) Invalidate so any other views refetch
      qc.invalidateQueries(['posts']);
      qc.invalidateQueries(['user-posts-infinite']);
      qc.invalidateQueries(['post', postId]);
    },
  });
}
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