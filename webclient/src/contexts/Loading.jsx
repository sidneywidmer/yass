import React, {createContext, useContext, useState} from 'react';

const LoadingContext = createContext(false);

export const useLoading = () => {
    return useContext(LoadingContext);
};

const LoadingProvider = ({children}) => {
    const [loading, setLoading] = useState(false);

    const startLoading = () => {
        setLoading(true);
    };

    const stopLoading = () => {
        setLoading(false);
    };

    return (
        <LoadingContext.Provider value={{loading, startLoading, stopLoading}}>
            {children}
        </LoadingContext.Provider>
    );
};

export default LoadingProvider