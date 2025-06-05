import { createContext } from "react";
export const AdminContext = createContext();
import { useState } from "react";
export const AdminProvider = ({ children }) => {
    const [admin, setAdmin] = useState(null);
    const [role, setRole] = useState(null);
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    return (
        <AdminContext.Provider
        value={{
            admin,
            setAdmin,
            isLoggedIn,
            setIsLoggedIn,
            role,
            setRole,
        }}
        >
        {children}
        </AdminContext.Provider>
    );
    }
