import * as React from "react";
import * as ReactDOM from "react-dom/client";
import "./index.css";

import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';
import {CssBaseline} from "@mui/material";
import App from "./components/App.jsx";
import GlobalErrorHandling from "./components/GlobalErrorHandling.jsx";
import ErrorProvider from "./contexts/Error.jsx";
import LoadingProvider from "./contexts/Loading.jsx";


ReactDOM.createRoot(document.getElementById("root")).render(
    <React.StrictMode>
        <CssBaseline/>
        <ErrorProvider>
            <LoadingProvider>
                <App/>
                <GlobalErrorHandling/>
            </LoadingProvider>
        </ErrorProvider>
    </React.StrictMode>
);

