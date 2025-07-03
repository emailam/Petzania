import axios from 'axios';
import { getToken, saveToken, clearAllTokens } from '../storage/tokenStorage';
import Constants from 'expo-constants';

const BASE_URL = Constants.expoConfig.extra.API_BASE_URL_8080;
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

const PUBLIC_ENDPOINTS = [
    '/user/auth/signup',
    '/user/auth/login',
    '/user/auth/send-reset-password-otp',
    '/user/auth/reset-password',
    '/user/auth/verify',
    '/user/auth/verify-reset-otp',
    '/user/auth/resend-otp',
    '/user/auth/refresh-token'
];

const isPublicEndpoint = (url) => {
    return PUBLIC_ENDPOINTS.some(endpoint => url.includes(endpoint));
};

api.interceptors.request.use(
    async (config) => {
        // Skip token attachment for public endpoints
        if (isPublicEndpoint(config.url)) {
            return config;
        }

        const token = await getToken('accessToken');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
            console.log('📤 Authenticated API Request:', {
                method: config.method?.toUpperCase(),
                url: config.url,
                hasToken: !!token
            });
        } else {
            console.log('⚠️ No access token found for protected request:', config.url);
        }
        return config;
    },
    (error) => Promise.reject(error)
);

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        console.log('🔴 HTTP Error:', {
            status: error.response?.status,
            url: error.config?.url,
            method: error.config?.method,
            message: error.message
        });
        
        // Don't attempt token refresh for public endpoints
        if (isPublicEndpoint(originalRequest.url)) {
            console.log('🔓 Public endpoint failed, not attempting token refresh');
            return Promise.reject(error);
        }
        
        if (error.response?.status === 401 && !originalRequest._retry) {
            console.log('🔄 Starting token refresh process...');
            originalRequest._retry = true;

            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                })
                .then((token) => {
                    originalRequest.headers.Authorization = 'Bearer ' + token;
                    return api(originalRequest);
                })
                .catch((err) => Promise.reject(err));
            }

            isRefreshing = true;

            try {
                const refreshToken = await getToken('refreshToken');
                console.log('🔑 Fetched refresh token:', refreshToken ? 'Present' : 'Missing');

                if (!refreshToken) {
                    throw new Error('No refresh token available');
                }

                console.log('📡 Calling refresh endpoint:', REFRESH_URL);
                const response = await axios.post(REFRESH_URL, {
                    refreshToken,
                });

                console.log('✅ Token refresh successful:', {
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
                console.log('🔄 Retrying original request with new token');
                return api(originalRequest);
            } catch (err) {
                console.log('❌ Token refresh failed:', err.message);
                processQueue(err, null);
                await clearAllTokens();
                console.log('🗑️ Cleared all tokens due to refresh failure');
                return Promise.reject(err);
            } finally {
                isRefreshing = false;
            }
        }

        return Promise.reject(error);
    }
);

export default api;