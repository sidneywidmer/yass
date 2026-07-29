import {MainScreen} from "@/components/main-screen.tsx";
import {LobbyHeader} from "@/components/lobby/lobby-header.tsx";

export default function Lobby() {
  return (
    <div className="flex min-h-svh flex-col items-center bg-muted p-6 pt-12 md:p-10 md:pt-16">
      <div className="flex w-full max-w-lg flex-col gap-8">
        <LobbyHeader/>
        <MainScreen/>
      </div>
    </div>
  );
}
