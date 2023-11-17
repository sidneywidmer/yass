import * as React from "react";
import * as ReactDOM from "react-dom/client";
import "./index.css";

import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';
import {CssBaseline} from "@mui/material";
import App from "./components/App.jsx";
import GlobalMessageHandling from "./components/GlobalMessageHandling.jsx";


ReactDOM.createRoot(document.getElementById("root")).render(
    <>
        <CssBaseline/>
        <App/>
        <GlobalMessageHandling/>
    </>
);

