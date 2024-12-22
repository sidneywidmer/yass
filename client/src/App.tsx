import '@/App.css'
import Root from "@/routes/root.tsx";
import Login from "@/routes/login.tsx";
import {createBrowserRouter, RouterProvider} from "react-router-dom";
import {AuthGuard} from "@/auth/auth-guard.tsx";
import {AuthRedirect} from "@/auth/auth-redirect.tsx";
import '@/i18n';

const router = createBrowserRouter([
  {
    path: "/",
    element: <AuthGuard><Root/></AuthGuard>,
  },
  {
    path: "/login",
    element: <AuthRedirect><Login/></AuthRedirect>,
  }
]);


export default function App() {
  return (
    <RouterProvider router={router}/>
  );
}
