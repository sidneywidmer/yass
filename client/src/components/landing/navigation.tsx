import {useState, useEffect} from "react";
import {useNavigate} from "react-router-dom";
import Settings from "@/components/settings.tsx";
import {usePlayerStore} from "@/store/player.ts";
import {useTranslation} from "react-i18next";

export function Navigation() {
  const navigate = useNavigate();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);
  const isAuthenticated = usePlayerStore(state => state.isAuthenticated);
  const { t } = useTranslation();

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 0);
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <>
    <nav className="sticky top-0 z-50 bg-background/80 backdrop-blur-sm transition-all duration-300" style={{paddingTop: isScrolled ? "0" : "10px", paddingBottom: isScrolled ? "0" : "10px"}}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className={`flex justify-between items-center transition-all duration-300 ${isScrolled ? "h-16" : "h-20"}`}>
          {/* Logo */}
          <div className="flex-shrink-0 flex items-center gap-2">
            <button
              onClick={() => navigate("/")}
              className="flex items-center gap-2 font-semibold hover:opacity-80 transition-opacity no-underline"
            >
              <img
                src={isScrolled ? "/assets/logo-abstract.png" : "/assets/logo.png"}
                alt="Yass"
                className={`w-auto transition-all duration-300 ${isScrolled ? "h-6" : "h-16"}`}
              />
              {!isScrolled && <span>Yass</span>}
            </button>
          </div>

          {/* Navigation Links - Always Visible */}
          <div className="flex items-center gap-6">
            {!isAuthenticated && (
              <button
                onClick={() => navigate("/login")}
                className="text-sm text-muted-foreground hover:text-foreground transition-colors no-underline"
              >
                {t("landing.navigation.login")}
              </button>
            )}
            {isAuthenticated && (
              <button
                onClick={() => navigate("/lobby")}
                className="text-sm text-muted-foreground hover:text-foreground transition-colors no-underline"
              >
                {t("landing.navigation.lobby")}
              </button>
            )}
            <button
              onClick={() => setSettingsOpen(true)}
              className="text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
              {t("landing.navigation.settings")}
            </button>
            <a
              href="https://github.com/sidneywidmer/yass/"
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm text-muted-foreground hover:text-foreground transition-colors no-underline"
            >
              GitHub
            </a>
          </div>
        </div>
      </div>
    </nav>
    <Settings
      triggerVariant="none"
      open={settingsOpen}
      onOpenChange={setSettingsOpen}
    />
    </>
  );
}
