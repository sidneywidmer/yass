import '@/App.css'
import Landing from "@/routes/landing.tsx";
import Lobby from "@/routes/lobby.tsx";
import Login from "@/routes/login.tsx";
import {createBrowserRouter, RouterProvider} from "react-router-dom";
import {AuthGuard} from "@/auth/auth-guard.tsx";
import '@/i18n';
import {MainWrapper} from "@/components/main-wrapper.tsx";
import Signup from "@/routes/signup.tsx";
import Game from "@/routes/game.tsx";
import Analyze from "@/routes/analyze.tsx";
import BetaRoadmapBlog from "@/routes/blog/beta-roadmap.tsx";
import { useEffect } from 'react';
import {preloadAssets} from "@/lib/utils.ts";

const router = createBrowserRouter([
  {
    path: "/",
    element: <MainWrapper><Landing/></MainWrapper>,
  },
  {
    path: "/blog/beta-roadmap",
    element: <MainWrapper><BetaRoadmapBlog/></MainWrapper>,
  },
  {
    path: "/lobby",
    element: <AuthGuard><MainWrapper><Lobby/></MainWrapper></AuthGuard>,
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
