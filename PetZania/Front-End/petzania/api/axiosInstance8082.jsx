import axios from 'axios';
import { getToken, saveToken, clearAllTokens } from '../storage/tokenStorage';


const BASE_URL = "http://10.0.2.2:8082/api";
//const BASE_URL = "http://192.168.1.6:8082/api";

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
        }
        return config;
    },
    (error) => Promise.reject(error)
);

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                })
                .then((token) => {
                    originalRequest.headers.Authorization = 'Bearer ' + token;
                    return axios(originalRequest);
                })
                .catch((err) => Promise.reject(err));
            }

            isRefreshing = true;

            try {
                const refreshToken = await getToken('refreshToken');

                const response = await axios.post(`http://10.0.2.2:8080/api/user/auth/refresh-token`, {
                    refreshToken,
                });

                console.log('New access token:', response.data);
                const newAccessToken = response.data.accessToken;
                await saveToken('accessToken', newAccessToken);
                processQueue(null, newAccessToken);

                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                return api(originalRequest);
            } catch (err) {
                processQueue(err, null);
                await clearAllTokens();
                return Promise.reject(err);
            } finally {
                isRefreshing = false;
            }
        }

        return Promise.reject(error);
    }
);

export default api;