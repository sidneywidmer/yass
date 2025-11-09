import { useNavigate } from "react-router-dom";
import { Megaphone } from "lucide-react";
import { useTranslation } from "react-i18next";

export function BetaAnnouncement() {
  const navigate = useNavigate();
  const { t } = useTranslation();

  return (
    <div className="w-full bg-gradient-to-r from-yellow-400/10 to-emerald-400/10 border-b border-emerald-400/20">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <button
          onClick={() => navigate("/blog/beta-roadmap")}
          className="w-full flex items-center justify-center gap-3 text-sm md:text-base font-medium text-foreground hover:text-foreground/80 transition-colors group"
        >
          <Megaphone className="h-5 w-5 text-emerald-500 group-hover:scale-110 transition-transform" />
          <span>{t("landing.betaAnnouncement")}</span>
          <span className="text-xs bg-emerald-500/20 text-emerald-600 px-2 py-1 rounded-full ml-2">START BETA</span>
        </button>
      </div>
    </div>
  );
}
