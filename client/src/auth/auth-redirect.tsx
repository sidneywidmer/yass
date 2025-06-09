import {Navigate} from "react-router";
import React from "react";
import {useAuth} from "@/hooks/use-auth.tsx";
import {Loader2} from "lucide-react";

interface AuthRedirectProps {
  children: React.ReactNode;
}

export function AuthRedirect({children}: AuthRedirectProps) {
  const {isAuthenticated, initialized} = useAuth()

  if (!initialized) {
    // Show a loading state instead of null to prevent routing issues
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    )
  }

  return isAuthenticated
    ? <Navigate to="/" replace/>
    : children;
}
