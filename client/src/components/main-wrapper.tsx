import React from "react";
import {Toaster} from "@/components/ui/sonner.tsx";
import LanguageSwitcher from "@/components/language-switcher.tsx";
import Logout from "@/components/logout.tsx";
import {usePlayerStore} from "@/store/player.ts";

interface WrapperProps {
  children: React.ReactNode;
}

export function MainWrapper({children}: WrapperProps) {
  const isAuthenticated = usePlayerStore(state => state.isAuthenticated)
  return (
    <>
      <Toaster richColors theme="light" duration={10000} closeButton={true}/>
      <LanguageSwitcher/>
      {isAuthenticated && <Logout/>}
      {children}
    </>
  )
}
