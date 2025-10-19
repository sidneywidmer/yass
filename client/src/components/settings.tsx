import {Languages, LogOut, SettingsIcon, Spade, User} from 'lucide-react';
import {useSettingsStore} from "@/store/settings.ts";
import {usePlayerStore} from "@/store/player.ts";
import {useGameStateStore} from "@/store/game-state.ts";
import {Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger} from "@/components/ui/sheet.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Separator} from "@/components/ui/separator.tsx";
import {useTranslation} from "react-i18next";
import Logout from "@/components/logout.tsx";
import {SupportedLanguage} from "@/types/language.ts";
import {CardDeck} from "@/types/card-deck.ts";
import {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog.tsx";
import CodeWithCopy from "@/components/code-with-copy.tsx";

const Settings = () => {
  const {language, setLanguage, cardDeck, setCardDeck} = useSettingsStore();
  const isAuthenticated = usePlayerStore(state => state.isAuthenticated)
  const name = usePlayerStore(state => state.name)
  const gameUuid = useGameStateStore(state => state.gameUuid)
  const code = useGameStateStore(state => state.code)
  const {t} = useTranslation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [showLeaveDialog, setShowLeaveDialog] = useState(false);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        setOpen(prev => !prev);
      }
    };

    window.addEventListener('keydown', handleKeyDown, { capture: true });
    return () => window.removeEventListener('keydown', handleKeyDown, { capture: true });
  }, []);

  const handleOpenLeaveDialog = () => {
    setShowLeaveDialog(true);
    setOpen(false); // Close the settings sheet
  };

  const handleLeaveGame = () => {
    navigate('/');
    setShowLeaveDialog(false);
  };

  return (
    <>
    <Sheet open={open} onOpenChange={setOpen} modal={false}>
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
          <SheetDescription></SheetDescription>
        </SheetHeader>
        <div className="py-4">
          <Separator/>
        </div>
        <div className="space-y-4">
          {/* Leave Game Section */}
          {gameUuid && (
            <div>
              <Button
                variant="destructive"
                className="w-full"
                onClick={handleOpenLeaveDialog}
              >
                <LogOut className="mr-2 h-4 w-4"/>
                {t("settings.leaveGame")}
              </Button>
            </div>
          )}

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
                variant={language === SupportedLanguage.EN ? "secondary" : "outline"}
                onClick={() => setLanguage(SupportedLanguage.EN)}
              >
                <img
                  src="https://flagcdn.com/w20/gb.png"
                  width="20"
                  className="inline-block"
                  alt="english"/>
              </Button>
              <Button
                variant={language === SupportedLanguage.DE ? "secondary" : "outline"}
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

          {/* Card Deck Switch Section */}
          <div>
            <h3 className="text-sm font-medium mb-2 flex items-center">
              <Spade className="mr-2 h-5 w-5 text-muted-foreground"/>
              {t("settings.deck")}
            </h3>
            <div className="flex space-x-2">
              <Button
                variant={cardDeck === CardDeck.SWISS ? "secondary" : "outline"}
                onClick={() => setCardDeck(CardDeck.SWISS)}
              >
                <img
                  src="/trumps/swiss/HEARTS.svg"
                  width="16"
                  height="16"
                  className="mr-2"
                  alt="swiss hearts"/>
                {t("settings.deckSwiss")}
              </Button>
              <Button
                variant={cardDeck === CardDeck.FRENCH ? "secondary" : "outline"}
                onClick={() => setCardDeck(CardDeck.FRENCH)}
              >
                <img
                  src="/trumps/french/HEARTS.svg"
                  width="16"
                  height="16"
                  className="mr-2"
                  alt="french hearts"/>
                {t("settings.deckFrench")}
              </Button>
            </div>
          </div>
        </div>
      </SheetContent>
    </Sheet>

    <AlertDialog open={showLeaveDialog} onOpenChange={setShowLeaveDialog}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{t("settings.leaveGameConfirm.title")}</AlertDialogTitle>
          <AlertDialogDescription>
            {t("settings.leaveGameConfirm.description")}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <div className="space-y-2">
          <div className="text-sm text-muted-foreground">{t("settings.leaveGameConfirm.rejoinInfo")}</div>
          <CodeWithCopy code={code!}/>
        </div>
        <AlertDialogFooter>
          <AlertDialogCancel>{t("settings.leaveGameConfirm.cancel")}</AlertDialogCancel>
          <AlertDialogAction onClick={handleLeaveGame}>
            {t("settings.leaveGameConfirm.confirm")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
    </>
  );
};

export default Settings;