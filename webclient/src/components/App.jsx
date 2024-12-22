import * as React from 'react';
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
        path: "/admin/analyze/:gameCode",
        element: <Analyze/>,
    },
    {
        path: "/admin/play/:gameCode",
        element: <Play/>,
    }
]);


export default function App() {
    return (
        <RouterProvider router={router}/>
    );
}