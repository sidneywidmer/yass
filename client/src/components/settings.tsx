import {Languages, SettingsIcon, User} from 'lucide-react';
import {useLangStore} from "@/store/language.ts";
import {usePlayerStore} from "@/store/player.ts";
import {Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger} from "@/components/ui/sheet.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Separator} from "@/components/ui/separator.tsx";
import {useTranslation} from "react-i18next";
import Logout from "@/components/logout.tsx";
import {SupportedLanguage} from "@/types/language.ts";

const Settings = () => {
  const {setLanguage} = useLangStore();
  const isAuthenticated = usePlayerStore(state => state.isAuthenticated)
  const name = usePlayerStore(state => state.name)
  const {t} = useTranslation();

  return (
    <Sheet>
      <SheetTrigger asChild>
        <div className="fixed top-4 right-4 z-10">
          <Button variant="outline" size="icon">
            <SettingsIcon className="h-4 w-4"/>
          </Button>
        </div>
      </SheetTrigger>
      <SheetContent>
        <SheetHeader>
          <SheetTitle>{t("settings.title")}</SheetTitle>
        </SheetHeader>
        <div className="py-4">
          <Separator/>
        </div>
        <div className="space-y-4">
          {/* User Profile Section */}
          {isAuthenticated && (
            <div>
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <User className="h-5 w-5 text-muted-foreground"/>
                  <span className="font-medium">{name}</span>
                </div>
                <Logout/>
              </div>
            </div>
          )}

          {/* Language Switch Section */}
          <div>
            <h3 className="text-sm font-medium mb-2 flex items-center">
              <Languages className="mr-2 h-5 w-5 text-muted-foreground"/>
              {t("settings.language")}
            </h3>
            <div className="flex space-x-2">
              <Button
                variant="outline"
                onClick={() => setLanguage(SupportedLanguage.EN)}
              >
                <img
                  src="https://flagcdn.com/w20/gb.png"
                  width="20"
                  className="inline-block"
                  alt="english"/>
              </Button>
              <Button
                variant="outline"
                onClick={() => setLanguage(SupportedLanguage.DE)}
              >
                <img
                  src="https://flagcdn.com/w20/de.png"
                  width="20"
                  className="inline-block"
                  alt="german"/>
              </Button>
            </div>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
};

export default Settings;