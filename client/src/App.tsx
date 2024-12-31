import '@/App.css'
import Root from "@/routes/root.tsx";
import Login from "@/routes/login.tsx";
import {createBrowserRouter, RouterProvider} from "react-router-dom";
import {AuthGuard} from "@/auth/auth-guard.tsx";
import {AuthRedirect} from "@/auth/auth-redirect.tsx";
import '@/i18n';
import {MainWrapper} from "@/components/main-wrapper.tsx";
import Signup from "@/routes/signup.tsx";
import Game from "@/routes/game.tsx";

const router = createBrowserRouter([
  {
    path: "/",
    element: <AuthGuard><MainWrapper><Root/></MainWrapper></AuthGuard>,
  },
  {
    path: "/login",
    element: <AuthRedirect><MainWrapper><Login/></MainWrapper></AuthRedirect>,
  },
  {
    path: "/signup",
    element: <AuthRedirect><MainWrapper><Signup/></MainWrapper></AuthRedirect>,
  },
  {
    path: "/game/:code",
    element: <AuthGuard><MainWrapper><Game/></MainWrapper></AuthGuard>,
  }
]);


export default function App() {
  return (
    <RouterProvider router={router}/>
  );
}
