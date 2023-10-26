import * as React from 'react';
import Container from '@mui/material/Container';
import {createBrowserRouter, RouterProvider} from "react-router-dom";
import Root from "../routes/Root.jsx";
import Analyze from "../routes/Analyze.jsx";
import Play from "../routes/Play.jsx";

const router = createBrowserRouter([
    {
        path: "/",
        element: <Root/>,
    },
    {
        path: "/analyze/:gameCode",
        element: <Analyze/>,
    },
    {
        path: "/play/:gameCode",
        element: <Play/>,
    }
]);


export default function App() {
    return (
        <Container maxWidth="md">
            <RouterProvider router={router}/>
        </Container>
    );
}