import {LogOut} from 'lucide-react';
import {Button} from "@/components/ui/button.tsx";
import {usePlayerStore} from "@/store/player.ts";
import {useState} from "react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle
} from "@/components/ui/alert-dialog.tsx";
import {useTranslation} from "react-i18next";
import {useAnonTokenStore} from "@/store/anon-token.ts";
import {ory} from "@/api/ory.ts";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {useNavigate} from "react-router-dom";

const Logout = () => {
  const {t} = useTranslation()
  const {isAnon, logout} = usePlayerStore()
  const [showAlert, setShowAlert] = useState(false)
  const {clearToken} = useAnonTokenStore()
  const navigate = useNavigate()
  const handleError = useAxiosErrorHandler()

  const handleMaybeLogout = () => {
    if (isAnon()) {
      setShowAlert(true)
      return
    }
    handleRealLogout()
  }

  const handleRealLogout = () => {
    setShowAlert(false)

    if (isAnon()) {
      logout()
      clearToken()
      navigate('/login')
      return
    }

    ory.createBrowserLogoutFlow()
      .then(flow => ory.updateLogoutFlow({
        token: flow.data.logout_token
      }))
      .catch(handleError)
      .finally(() => {
        logout()
        navigate('/login')
      })
  }

  return (
    <>
      <div className="fixed top-4 right-16">
        <Button
          variant="outline"
          size="icon"
          onClick={handleMaybeLogout}
        >
          <LogOut className="h-4 w-4"/>
        </Button>
      </div>
      <AlertDialog open={showAlert} onOpenChange={setShowAlert}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t("logout.prompt.title")}</AlertDialogTitle>
            <AlertDialogDescription>{t("logout.prompt.text")}</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t("logout.prompt.cancel")}</AlertDialogCancel>
            <AlertDialogAction onClick={handleRealLogout}>{t("logout.prompt.title")}</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
};

export default Logout;