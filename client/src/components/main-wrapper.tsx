import React from "react";
import {Toaster} from "@/components/ui/sonner.tsx";
import Settings from "@/components/settings.tsx";
import {useAuth} from "@/hooks/use-auth.tsx";
import { useLocation } from "react-router-dom";

interface WrapperProps {
  children: React.ReactNode;
}

export function MainWrapper({children}: WrapperProps) {
  useAuth();
  const location = useLocation();
  const isLandingPage = location.pathname === "/";
  const isBlogPage = location.pathname.startsWith("/blog");

  return (
    <>
      <Toaster richColors theme="light" duration={10000} closeButton={true}/>
      {!isLandingPage && !isBlogPage && <Settings triggerVariant="fixed"/>}
      {children}
    </>
  )
}
