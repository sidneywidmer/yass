import { useTranslation } from "react-i18next";
import { TrumpIcon } from "@/components/game/trump-icon.tsx";

export function Footer() {
  const { t } = useTranslation();
  const currentYear = new Date().getFullYear();

  return (
    <footer className="w-full bg-muted">
      <div className="flex w-full flex-col items-center justify-center p-4 md:p-8 md:py-7">
        <div className="flex flex-col items-center gap-2 text-center">
          <div className="flex items-center gap-2 justify-center">
            <p className="font-sans text-base text-foreground">
              {t("landing.footer.madeWith")}
            </p>
            <TrumpIcon trump="HEARTS" className="h-4 w-4" />
            <p className="font-sans text-base text-foreground">
              {t("landing.footer.inSwitzerland")}
            </p>
          </div>
          <p className="font-sans text-sm text-muted-foreground">
            {t("landing.footer.copyright", { year: currentYear })}
          </p>
        </div>
      </div>
    </footer>
  );
}
