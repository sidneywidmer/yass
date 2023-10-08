import React, {createContext, useContext, useState} from 'react';
import axios from "axios";

const ErrorContext = createContext(null);

export const useError = () => {
    return useContext(ErrorContext)
};

const ErrorProvider = ({children}) => {
    const [error, setError] = useState(null);

    const handleError = (error) => {
        if (axios.isAxiosError(error)) {
            setError(error.message)
        } else {
            setError(error)
        }
    };

    const clearError = () => {
        setError(null)
    };

    return (
        <ErrorContext.Provider value={{error, handleError, clearError}}>
            {children}
        </ErrorContext.Provider>
    );
};

export default ErrorProvider