import {
  AlertDialog,
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
import {useState} from "react";
import {api} from "@/api/client";
import {useAsyncAction} from "@/hooks/use-async-action";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler";
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert.tsx";
import {AlertCircle, Loader2} from "lucide-react";
import {Button} from "@/components/ui/button.tsx";

interface MessageDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  message: string;
  onMessageChange: (message: string) => void;
  userName: string;
  path: string;
  gameUuid: string;
  onSubmit?: () => void;
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
  const handleAxiosError = useAxiosErrorHandler();
  const [error, setError] = useState<string | null>(null);

  const {
    execute: executeSubmitMessage,
    isLoading,
  } = useAsyncAction(async (messageData: {message: string; path: string; gameUuid?: string}) => {
    return api.submitMessage(messageData);
  });

  const handleSubmitMessage = async () => {
    setError(null);
    try {
      await executeSubmitMessage({
        message,
        path,
        gameUuid: gameUuid || undefined,
      });
      onOpenChange(false);
      onSubmit?.();
    } catch (err: any) {
      if (err.response?.status === 422) {
        return setError(t('errors.validation.description'));
      }
      handleAxiosError(err);
    }
  };

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
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>{t("errors.title")}</AlertTitle>
              <AlertDescription>
                {error}
              </AlertDescription>
            </Alert>
          )}

          <div className="space-y-2">
            <label className="text-sm font-medium">{t("settings.message.message")}</label>
            <Textarea
              placeholder={t("settings.message.messagePlaceholder")}
              value={message}
              onChange={(e) => onMessageChange(e.target.value.slice(0, 1000))}
              maxLength={1000}
              rows={4}
              className="resize-none"
              disabled={isLoading}
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
          <AlertDialogCancel disabled={isLoading}>{t("settings.message.cancel")}</AlertDialogCancel>
          <Button
            onClick={handleSubmitMessage}
            disabled={!message.trim() || isLoading}
          >
            {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {t("settings.message.submit")}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
};

export default MessageDialog;
