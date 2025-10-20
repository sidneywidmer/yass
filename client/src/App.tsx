import '@/App.css'
import Root from "@/routes/root.tsx";
import Login from "@/routes/login.tsx";
import {createBrowserRouter, RouterProvider} from "react-router-dom";
import {AuthGuard} from "@/auth/auth-guard.tsx";
import '@/i18n';
import {MainWrapper} from "@/components/main-wrapper.tsx";
import Signup from "@/routes/signup.tsx";
import Game from "@/routes/game.tsx";
import Analyze from "@/routes/analyze.tsx";
import { useEffect } from 'react';
import {preloadAssets} from "@/lib/utils.ts";

const router = createBrowserRouter([
  {
    path: "/",
    element: <AuthGuard><MainWrapper><Root/></MainWrapper></AuthGuard>,
  },
  {
    path: "/login",
    element: <MainWrapper><Login/></MainWrapper>,
  },
  {
    path: "/signup",
    element: <MainWrapper><Signup/></MainWrapper>,
  },
  {
    path: "/game/:code",
    element: <AuthGuard><MainWrapper><Game/></MainWrapper></AuthGuard>,
  },
  {
    path: "/game/:code/analyze",
    element: <AuthGuard><MainWrapper><Analyze/></MainWrapper></AuthGuard>,
  }
]);


export default function App() {
  useEffect(() => {
    preloadAssets();
  }, []);
  return (
    <RouterProvider router={router}/>
  );
}
