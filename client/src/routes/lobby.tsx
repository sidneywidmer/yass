import {MainScreen} from "@/components/main-screen.tsx";
import {Logo} from "@/components/logo.tsx";

export default function Lobby() {
  return (
    <>
      <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-muted p-6 md:p-10">
        <div className="flex w-full max-w-sm flex-col gap-6">
          <Logo/>
          <MainScreen/>
        </div>
      </div>
    </>
  );
}
