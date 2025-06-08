import React from "react";
import {Toaster} from "@/components/ui/sonner.tsx";
import Settings from "@/components/settings.tsx";

interface WrapperProps {
  children: React.ReactNode;
}

export function MainWrapper({children}: WrapperProps) {
  return (
    <>
      <Toaster richColors theme="light" duration={10000} closeButton={true}/>
      <Settings/>
      {children}
    </>
  )
}
