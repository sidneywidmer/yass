import { Button } from "@/components/ui/button.tsx";
import { useNavigate } from "react-router-dom";
import { usePlayerStore } from "@/store/player.ts";

export function Header() {
  const navigate = useNavigate();
  const isAuthenticated = usePlayerStore(state => state.isAuthenticated);

  return (
    <div className="flex w-full flex-col items-center bg-background p-6 md:p-10 md:pt-16 lg:pt-20 pb-20">
      <div className="flex w-full max-w-2xl flex-col gap-8 text-center">
        {/* Main Heading - Serif */}
        <h1 className="font-serif text-4xl md:text-5xl lg:text-6xl font-medium leading-tight text-foreground">
          A modern webclient to play your favorite game.
        </h1>

        {/* Description - Sans Serif */}
        <p className="font-sans text-lg md:text-xl text-muted-foreground leading-relaxed max-w-xl mx-auto">
          Experience seamless card gaming with a beautiful, intuitive interface. Play anytime, anywhere without installation.
        </p>

        {/* CTA Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center pt-4 pb-12">
          <Button
            size="lg"
            onClick={() => navigate(isAuthenticated ? "/lobby" : "/signup")}
            className="font-semibold px-8"
          >
            {isAuthenticated ? "Lobby" : "Sign Up"}
          </Button>
          <Button
            variant="outline"
            size="lg"
            onClick={() => navigate("/lobby")}
            className="font-normal px-8"
          >
            Instant play
          </Button>
        </div>
      </div>
    </div>
  );
}
