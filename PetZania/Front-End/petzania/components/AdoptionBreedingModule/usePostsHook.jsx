import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { fetchAllPosts, getAgeInMonths, getCategory } from '../../services/postService';

// Custom hook for fetching and filtering pets data
export default function usePostsHook(filters = {}, postType = "") {
  const {
    category = 'All', breedFilter = '', genderFilter = 'All', ageMin = '', ageMax = '', sortOrder = 'desc', sortBy = 'date'
  } = filters;

  // Use React Query to fetch data using the service function
  const {
    data: posts = [], isLoading, error, refetch
  } = useQuery({
    queryKey: [postType],
    queryFn: fetchAllPosts,
    staleTime: 5 * 60 * 1000, // 5 minutes
    cacheTime: 10 * 60 * 1000, // 10 minutes
  });

  // Memoized filtered data
  const filteredPosts = useMemo(() => {
    let data = [...posts];

    // Apply category filter
    if (category !== 'All') {
      data = data.filter(p => {
        const petCategory = getCategory(p.petDTO?.species);
        return petCategory === category;
      });
    }

    // Apply breed filter
    if (breedFilter.trim()) {
      data = data.filter(p => {
        const breed = p.petDTO?.breed || '';
        return breed.toLowerCase().includes(breedFilter.toLowerCase());
      });
    }

    // Apply gender filter
    if (genderFilter !== 'All') {
      data = data.filter(p => {
        const gender = p.petDTO?.gender || '';
        return gender.toLowerCase() === genderFilter.toLowerCase();
      });
    }

    // Apply age range filter
    const min = parseFloat(ageMin) || 0;
    const max = parseFloat(ageMax) || Infinity;
    data = data.filter(p => {
      const ageInMonths = getAgeInMonths(p);
      return ageInMonths >= min && ageInMonths <= max;
    });

    // Apply sorting based on sortBy and sortOrder
    data.sort((a, b) => {
      if (sortBy === 'likes') {
        // Sort by likes/reacts
        const aLikes = a.reacts || 0;
        const bLikes = b.reacts || 0;
        return sortOrder === 'desc' ? bLikes - aLikes : aLikes - bLikes;
      } else {
        // Sort by date
        const aDate = new Date(a.createdAt || '2023-01-01');
        const bDate = new Date(b.createdAt || '2023-01-01');

        // For createdAt, newer dates are "larger", so desc means newer first
        return sortOrder === 'desc' ? bDate - aDate : aDate - bDate;
      }
    });

    return data;
  }, [posts, category, breedFilter, genderFilter, ageMin, ageMax, sortOrder, sortBy]);

  return {
    posts: filteredPosts,
    isLoading,
    error,
    refetch,
    totalCount: posts.length,
    filteredCount: filteredPosts.length
  };
}

