import { Button } from "@/components/ui/button.tsx";
import { useNavigate } from "react-router-dom";
import { usePlayerStore } from "@/store/player.ts";
import { useTranslation } from "react-i18next";

export function Header() {
  const navigate = useNavigate();
  const isAuthenticated = usePlayerStore(state => state.isAuthenticated);
  const { t } = useTranslation();

  return (
    <div className="relative flex w-full flex-col items-center bg-background p-6 md:p-10 md:pt-16 lg:pt-20 pb-8 md:pb-20 overflow-hidden">
      <div className="relative flex w-full max-w-6xl flex-col lg:flex-row gap-8 lg:gap-12 items-center">
        {/* Subtle oval glow behind text */}
        <div className="absolute top-1/3 left-1/2 lg:left-1/4 -translate-x-1/2 -translate-y-1/2 w-[50vw] h-64 bg-gradient-to-br from-yellow-400/25 to-emerald-400/25 rounded-full blur-3xl pointer-events-none" />

        {/* Left Column - Text Content */}
        <div className="relative flex w-full lg:w-1/2 flex-col gap-8 text-center lg:text-left">
          {/* Main Heading - Serif */}
          <h1 className="relative z-10 font-serif text-4xl md:text-5xl lg:text-6xl font-medium leading-tight text-foreground">
            {t("landing.header.title")}
          </h1>

          {/* Description - Sans Serif */}
          <p className="font-sans z-10 text-lg md:text-xl text-muted-foreground leading-relaxed">
            {t("landing.header.description")}
          </p>

          {/* CTA Buttons */}
          <div className="flex flex-row z-10 gap-2 md:gap-4 justify-center lg:justify-start pt-4 flex-wrap">
            <Button
              size="lg"
              onClick={() => navigate(isAuthenticated ? "/lobby" : "/signup")}
              className="font-semibold px-8"
            >
              {isAuthenticated ? t("landing.navigation.lobby") : t("landing.header.signUp")}
            </Button>
            {!isAuthenticated && (
              <Button
                variant="outline"
                size="lg"
                onClick={() => navigate("/signup", { state: { isGuest: true } })}
                className="font-normal px-8"
              >
                {t("landing.header.playAsGuest")}
              </Button>
            )}
          </div>
        </div>

        {/* Right Column - Game Showcase */}
        <div className="relative w-full lg:w-1/2 flex items-center justify-center lg:justify-end">
          <video
            autoPlay
            loop
            muted
            className="w-full max-w-[500px] rounded-lg shadow-lg border border-foreground/20"
          >
            <source src="/assets/demo.mp4" type="video/mp4" />
            <img
              src="/assets/demo.png"
              className="w-full max-w-[500px] rounded-lg shadow-lg border border-foreground/20"
            />
          </video>
        </div>
      </div>
    </div>
  );
}
