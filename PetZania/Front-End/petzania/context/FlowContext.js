import React, { createContext, useState } from 'react';

export const FlowContext = createContext();

export const FlowProvider = ({ children }) => {
    const [fromPage, setFromPage] = useState(null);

    return (
        <FlowContext.Provider value={{ fromPage, setFromPage }}>
            {children}
        </FlowContext.Provider>
    );
};
