import { GalleryVerticalEnd } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import Settings from "@/components/settings.tsx";
import { usePlayerStore } from "@/store/player.ts";
import { useTranslation } from "react-i18next";

export function Navigation() {
  const navigate = useNavigate();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const isAuthenticated = usePlayerStore(state => state.isAuthenticated);
  const { t } = useTranslation();

  return (
    <>
    <nav className="sticky top-0 z-50 bg-background/80 backdrop-blur-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo */}
          <div className="flex-shrink-0 flex items-center gap-2">
            <button
              onClick={() => navigate("/")}
              className="flex items-center gap-2 font-semibold hover:opacity-80 transition-opacity"
            >
              <div className="flex h-6 w-6 items-center justify-center rounded-md bg-primary text-primary-foreground">
                <GalleryVerticalEnd className="size-4" />
              </div>
              <span>Yass</span>
            </button>
          </div>

          {/* Navigation Links - Always Visible */}
          <div className="flex items-center gap-6">
            {!isAuthenticated && (
              <button
                onClick={() => navigate("/login")}
                className="text-sm text-muted-foreground hover:text-foreground transition-colors"
              >
                {t("landing.navigation.login")}
              </button>
            )}
            {isAuthenticated && (
              <button
                onClick={() => navigate("/lobby")}
                className="text-sm text-muted-foreground hover:text-foreground transition-colors"
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
