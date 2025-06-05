import { createContext } from "react";
export const UserContext = createContext();
import { useState } from "react";
export const UserProvider = ({ children }) => {
    const [user, setUser] = useState();
    
    return (
        <UserContext.Provider
        value={{
            user,
            setUser,
        }}
        >
        {children}
        </UserContext.Provider>
    );
    }
