import React, { createContext, useState, useEffect } from 'react';
import { saveToken, getToken, clearAllTokens } from '../storage/tokenStorage';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [accessToken, setAccessToken] = useState(null);
    const [refreshToken, setRefreshToken] = useState(null);

    useEffect(() => {
        const loadTokens = async () => {
            const storedAccess = await getToken('accessToken');
            const storedRefresh = await getToken('refreshToken');
            if (storedAccess) setAccessToken(storedAccess);
            if (storedRefresh) setRefreshToken(storedRefresh);
        };
        loadTokens();
    }, []);

    const saveTokens = async (access, refresh) => {
        setAccessToken(access);
        setRefreshToken(refresh);
        await saveToken('accessToken', access);
        await saveToken('refreshToken', refresh);
    };

    const logout = async () => {
        setAccessToken(null);
        setRefreshToken(null);
        await clearAllTokens();
    };

    return (
        <AuthContext.Provider value={{ accessToken, refreshToken, setAccessToken, setRefreshToken, saveTokens, logout }}>
            {children}
        </AuthContext.Provider>
    );
};
