import {MainScreen} from "@/components/main-screen.tsx";
import {useNavigate} from "react-router-dom";

export default function Lobby() {
  const navigate = useNavigate();

  return (
    <>
      <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-muted p-6 md:p-10">
        <div className="flex w-full max-w-sm flex-col gap-6">
          <button onClick={() => navigate("/")} className="self-center hover:opacity-80 transition-opacity">
            <img src="/assets/logo.png" alt="Yass" className="h-24 w-auto" />
          </button>
          <MainScreen/>
        </div>
      </div>
    </>
  );
}
