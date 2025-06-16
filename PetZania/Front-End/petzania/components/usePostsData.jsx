import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import PostsData from '../pets.json';

// Helper function to extract age in months from age string or date of birth
const getAgeInMonths = (pet) => {
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

// Helper function to map species to category
const getCategory = (species) => {
  if (!species) return 'All';
  const speciesLower = species.toLowerCase();
  if (speciesLower === 'dog') return 'Dog';
  if (speciesLower === 'cat') return 'Cat';
  return 'All';
};

// Custom hook for fetching and filtering pets data
const usePostsData = (filters = {}) => {
  const {
    category = 'All',
    breedFilter = '',
    genderFilter = 'All',
    ageMin = '',
    ageMax = '',
    sortOrder = 'desc',
    sortBy = 'date'
  } = filters;

  // Simulate API call - in real app, this would be an actual API endpoint
  const fetchPosts = async () => {
    // Simulate network delay
    await new Promise(resolve => setTimeout(resolve, 100));
    return PostsData || [];
  };

  // Use React Query to fetch data
  const {
    data: posts = [],
    isLoading,
    error,
    refetch
  } = useQuery({
    queryKey: ['posts'],
    queryFn: fetchPosts,
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
        const aDate = new Date(a.createdAt  || '2023-01-01');
        const bDate = new Date(b.createdAt  || '2023-01-01');
        
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
};

export default usePostsData;