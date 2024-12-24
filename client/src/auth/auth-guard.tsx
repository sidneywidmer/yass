import {Navigate, useLocation} from "react-router";
import React from "react";
import {useAuth} from "@/hooks/use-auth.tsx";

interface AuthGuardProps {
  children: React.ReactNode;
}

export function AuthGuard({children}: AuthGuardProps) {
  const {isAuthenticated, initialized} = useAuth()
  const location = useLocation()

  if (!initialized) {
    return null
  }
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{from: location}} replace/>
  }

  return children
}