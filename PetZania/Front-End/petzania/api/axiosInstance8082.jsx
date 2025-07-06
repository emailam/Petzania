import axios from 'axios';
import { getToken, saveToken, clearAllTokens } from '../storage/tokenStorage';
import Constants from 'expo-constants';

const BASE_URL = Constants.expoConfig.extra.API_BASE_URL_8082;
const REFRESH_URL = `${Constants.expoConfig.extra.API_BASE_URL_8080}/user/auth/refresh-token`;

const api = axios.create({
    baseURL: BASE_URL,
});

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
            console.log('📤 Authenticated API Request (Service 8082):', {
                method: config.method?.toUpperCase(),
                url: config.url,
                hasToken: !!token
            });
        } else {
            console.log('⚠️ No access token found for request (Service 8082):', config.url);
        }
        return config;
    },
    (error) => Promise.reject(error)
);

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        console.log('🔴 HTTP Error (Service 8082):', {
            status: error.response?.status,
            url: error.config?.url,
            method: error.config?.method,
            message: error.message
        });

        // Handle other client errors that shouldn't trigger token refresh
        if (error.response?.status >= 400 && error.response?.status < 500 && error.response?.status !== 401 && error.response?.status !== 403) {
            console.log(`🔴 Client error (${error.response.status}) (Service 8082):`, {
                url: originalRequest.url,
                method: originalRequest.method,
                status: error.response.status,
                message: error.response?.data?.message || error.message
            });
            return Promise.reject(error);
        }

        // Treat 401 and 403 as triggers for token refresh
        if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
            console.log('🔄 Starting token refresh process (Service 8082)...');
            originalRequest._retry = true;

            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                })
                .then((token) => {
                    if (token) {
                        originalRequest.headers.Authorization = 'Bearer ' + token;
                        return api(originalRequest);
                    } else {
                        return Promise.reject(new Error('Authentication failed. Please log in again.'));
                    }
                })
                .catch((err) => Promise.reject(err));
            }

            isRefreshing = true;

            try {
                const refreshToken = await getToken('refreshToken');
                console.log('🔑 Fetched refresh token (Service 8082):', refreshToken ? 'Present' : 'Missing');

                if (!refreshToken) {
                    console.log('🚫 No refresh token available, cannot refresh (Service 8082)');
                    throw new Error('No refresh token available');
                }

                console.log('📡 Calling refresh endpoint (Service 8082):', REFRESH_URL);
                const response = await axios.post(REFRESH_URL, {
                    refreshToken,
                });

                console.log('✅ Token refresh successful (Service 8082):', {
                    hasNewAccessToken: !!response.data.accessToken,
                    hasNewRefreshToken: !!response.data.refreshToken
                });

                const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data;
                if (newAccessToken) {
                    await saveToken('accessToken', newAccessToken);
                }
                if (newRefreshToken) {
                    await saveToken('refreshToken', newRefreshToken);
                }

                processQueue(null, newAccessToken);

                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                console.log('🔄 Retrying original request with new token (Service 8082)');
                return api(originalRequest);
            } catch (err) {
                console.log('❌ Token refresh failed (Service 8082):', {
                    status: err.response?.status,
                    message: err.message,
                    data: err.response?.data
                });
                // Clear tokens and queue
                processQueue(err, null);
                await clearAllTokens();
                console.log('🗑️ Cleared all tokens due to refresh failure (Service 8082)');
                // If refresh token is invalid (400/401), don't retry the original request
                if (err.response?.status === 400 || err.response?.status === 401) {
                    console.log('🚫 Refresh token invalid, not retrying original request (Service 8082)');
                    return Promise.reject(new Error('Authentication failed. Please log in again.'));
                }
                // Catch-all: reject all failed requests
                return Promise.reject(err);
            } finally {
                isRefreshing = false;
            }
        }

        return Promise.reject(error);
    }
);

export default api;