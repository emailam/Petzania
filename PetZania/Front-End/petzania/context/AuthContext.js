import { createContext, useState } from "react";

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [accessToken, setAccessToken] = useState(null);
    const [refreshToken, setRefreshToken] = useState(null);

    const logout = () => {
        setAccessToken(null);
        setRefreshToken(null);
    };

    return (
        <AuthContext.Provider
        value={{
            accessToken,
            setAccessToken,
            refreshToken,
            setRefreshToken,
            logout,
        }}
        >
        {children}
        </AuthContext.Provider>
    );
};
