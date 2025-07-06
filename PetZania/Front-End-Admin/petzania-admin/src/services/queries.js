import { useQuery } from '@tanstack/react-query';
import { getAllUsers, getUserById } from './services';
import { getAllAdmins, getAdminById } from './services';


export const useAllUsers = (options = {}) => {
  return useQuery({
    queryKey: ['users'],
    queryFn: async () => {
      console.log('Fetching all users...'); // Debugging log
      const response = await getAllUsers();
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // Data considered fresh for 5 minutes
    refetchOnWindowFocus: true, // Refetch when window regains focus
    retry: 1, // Retry failed requests twice
    ...options, // Allow overriding default options
  });
};


export const useUser = (userId, options = {}) => {
  return useQuery({
    queryKey: ['user', userId],
    queryFn: async () => {
      const response = await getUserById(userId);
      return response.data;
    },
    enabled: !!userId, // Only run if userId exists
    ...options,
  });
};

/**
 * Custom hook to fetch all admins
 * @param {Object} options - Additional options for the query
 * @returns {Object} React Query result object
 */
export const useAllAdmins = (options = {}) => {
  return useQuery({
    queryKey: ['admins'],
    queryFn: async () => {
      const response = await getAllAdmins();
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // Data considered fresh for 5 minutes
    refetchOnWindowFocus: true, // Refetch when window regains focus
    retry: 2, // Retry failed requests twice
    ...options, // Allow overriding default options
    refetchOnMount: true, // Refetch when component mounts
  });
};

/**
 * Custom hook to fetch a single admin by ID
 * @param {string} adminId - The ID of the admin to fetch
 * @param {Object} options - Additional options for the query
 * @returns {Object} React Query result object
 */
export const useAdmin = (adminId, options = {}) => {
  return useQuery({
    queryKey: ['admin', adminId],
    queryFn: async () => {
      const response = await getAdminById(adminId);
      return response.data;
    },
    enabled: !!adminId, // Only run if adminId exists
    ...options,
  });
};