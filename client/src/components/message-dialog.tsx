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
import {Input} from "@/components/ui/input.tsx";
import {Textarea} from "@/components/ui/textarea.tsx";
import {useTranslation} from "react-i18next";

interface MessageDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  message: string;
  onMessageChange: (message: string) => void;
  userName: string;
  path: string;
  gameUuid: string;
  onSubmit: () => void;
}

const MessageDialog = ({
  open,
  onOpenChange,
  message,
  onMessageChange,
  userName,
  path,
  gameUuid,
  onSubmit,
}: MessageDialogProps) => {
  const {t} = useTranslation();

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent className="max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle>{t("settings.message.title")}</AlertDialogTitle>
          <AlertDialogDescription>
            {t("settings.message.description")}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <div className="space-y-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">{t("settings.message.message")}</label>
            <Textarea
              placeholder={t("settings.message.messagePlaceholder")}
              value={message}
              onChange={(e) => onMessageChange(e.target.value.slice(0, 1000))}
              maxLength={1000}
              rows={4}
              className="resize-none"
            />
            <div className="text-xs text-muted-foreground text-right">
              {message.length}/1000
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium">{t("settings.message.user")}</label>
            <Input
              value={userName || ""}
              disabled
              className="bg-muted"
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium">{t("settings.message.path")}</label>
            <Input
              value={path}
              disabled
              className="bg-muted"
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium">{t("settings.message.game")}</label>
            <Input
              value={gameUuid || ""}
              disabled
              className="bg-muted"
            />
          </div>
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel>{t("settings.message.cancel")}</AlertDialogCancel>
          <AlertDialogAction onClick={onSubmit} disabled={!message.trim()}>
            {t("settings.message.submit")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
};

export default MessageDialog;
