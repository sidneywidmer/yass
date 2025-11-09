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
import {useNavigate, useLocation} from "react-router-dom";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog.tsx"; // For leave game dialog
import CodeWithCopy from "@/components/code-with-copy.tsx";
import MessageDialog from "@/components/message-dialog.tsx";

interface SettingsProps {
  triggerVariant?: 'fixed' | 'inline' | 'none';
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
}

const Settings = ({ triggerVariant = 'fixed', open: controlledOpen, onOpenChange }: SettingsProps) => {
  const {language, setLanguage, cardDeck, setCardDeck} = useSettingsStore();
  const isAuthenticated = usePlayerStore(state => state.isAuthenticated)
  const name = usePlayerStore(state => state.name)
  const gameUuid = useGameStateStore(state => state.gameUuid)
  const code = useGameStateStore(state => state.code)
  const {t} = useTranslation();
  const navigate = useNavigate();
  const [internalOpen, setInternalOpen] = useState(false);
  const [showLeaveDialog, setShowLeaveDialog] = useState(false);
  const [showMessageDialog, setShowMessageDialog] = useState(false);
  const [messageText, setMessageText] = useState("");
  const location = useLocation();

  // Use controlled state if provided, otherwise use internal state
  const open = controlledOpen !== undefined ? controlledOpen : internalOpen;

  const setOpen = (value: boolean | ((prev: boolean) => boolean)) => {
    const newValue = typeof value === 'function' ? value(open) : value;
    if (onOpenChange) {
      onOpenChange(newValue);
    } else {
      setInternalOpen(newValue);
    }
  };

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        setOpen(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown, { capture: true });
    return () => window.removeEventListener('keydown', handleKeyDown, { capture: true });
  }, [open, onOpenChange]);

  const handleOpenLeaveDialog = () => {
    setShowLeaveDialog(true);
    setOpen(false); // Close the settings sheet
  };

  const handleLeaveGame = () => {
    navigate('/lobby');
    setShowLeaveDialog(false);
  };

  const handleOpenMessageDialog = () => {
    setMessageText("");
    setShowMessageDialog(true);
    setOpen(false); // Close the settings sheet
  };

  const triggerClasses = triggerVariant === 'fixed'
    ? "fixed top-4 right-4 z-10"
    : "";

  return (
    <>
    <Sheet open={open} onOpenChange={setOpen} modal={false}>
      {triggerVariant !== 'none' && (
        <SheetTrigger asChild>
          <div className={triggerClasses}>
            <Button variant="outline" size="icon">
              <SettingsIcon className="h-4 w-4"/>
            </Button>
          </div>
        </SheetTrigger>
      )}
      <SheetContent>
        <SheetHeader>
          <SheetTitle>{t("settings.title")}</SheetTitle>
          <SheetDescription></SheetDescription>
        </SheetHeader>
        <div className="space-y-4">
          {/* User Profile Section */}
          {isAuthenticated && (
            <>
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <User className="h-5 w-5 text-muted-foreground"/>
                  <span className="font-medium">{name}</span>
                </div>
                <Logout/>
              </div>
              <Separator/>
            </>
          )}

          {/* Game Section */}
          <div className="space-y-4">
            {gameUuid && (
              <div>
                <h3 className="text-sm font-medium mb-3">{t("settings.game")}</h3>
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

            {/* Language Switch */}
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

            {/* Card Deck Switch */}
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

          {/* Feedback Section */}
          {isAuthenticated && (
            <>
              <Separator/>
              <div>
                <h3 className="text-sm font-medium mb-3">{t("settings.feedback")}</h3>
                <Button
                  variant="outline"
                  className="w-full"
                  onClick={handleOpenMessageDialog}
                >
                  {t("settings.sendMessage")}
                </Button>
              </div>
            </>
          )}
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

    <MessageDialog
      open={showMessageDialog}
      onOpenChange={setShowMessageDialog}
      message={messageText}
      onMessageChange={setMessageText}
      userName={name || ""}
      path={location.pathname}
      gameUuid={gameUuid || ""}
    />
    </>
  );
};

export default Settings;