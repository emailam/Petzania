import axios from 'axios';
import { getToken, saveToken, clearAllTokens } from './tokenStorage';

const API_URL = 'http://localhost:8080/api';

// Axios instance
const api = axios.create({
  baseURL: API_URL,
});

// Check token expiration
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

api.interceptors.request.use(
  async (config) => {
    const token = await getToken('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // Important: check if this is a 401 error AND not already a retry attempt
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest); // Using api instance instead of axios
          })
          .catch((err) => Promise.reject(err));
      }

      isRefreshing = true;

      try {
        const refreshToken = await getToken('refreshToken');
        if (!refreshToken) {
          throw new Error('No refresh token available');
        }

        // Using the same base URL as your API instance
        const response = await axios.post(`${API_URL}/admin/refresh-token`, {
          refreshToken,
        });
        
        if (!response.data?.accessToken) {
          throw new Error('Invalid token response');
        }
        
        const newAccessToken = response.data.accessToken;
        console.log('Token refreshed successfully');
        
        // If the response also includes a new refresh token, save it
        if (response.data.refreshToken) {
          await saveToken('refreshToken', response.data.refreshToken);
        }
        
        await saveToken('accessToken', newAccessToken);
        
        // Set the new token in the original request
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        
        // Process the queue with the new token
        processQueue(null, newAccessToken);
        
        // Retry the original request with the api instance
        return api(originalRequest);
      } catch (err) {
        console.error('Token refresh failed:', err);
        processQueue(err, null);
        await clearAllTokens();
        // Consider redirecting to login here
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;