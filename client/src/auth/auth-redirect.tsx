import {Navigate} from "react-router";
import React from "react";
import {useAuth} from "@/hooks/use-auth.tsx";

interface AuthRedirectProps {
  children: React.ReactNode;
}

export function AuthRedirect({children}: AuthRedirectProps) {
  const {isAuthenticated, initialized} = useAuth()

  if (!initialized) {
    return null
  }

  return isAuthenticated
    ? <Navigate to="/" replace/>
    : children;
}
