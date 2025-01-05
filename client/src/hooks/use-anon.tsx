import {useNavigate} from "react-router-dom"
import {usePlayerStore} from "@/store/player.ts";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {ErrorMessage} from "@/api/helpers.ts";
import {useState} from "react";
import {useTranslation} from "react-i18next";
import {TFunction} from "i18next";
import {api} from "@/api/client.ts";

export const useAnon = () => {
  const navigate = useNavigate()
  const handleError = useAxiosErrorHandler()
  const {setAnonPlayer} = usePlayerStore()
  const [anonSignupError, setAnonSignupError] = useState<ErrorMessage | null>(null)
  const {t}: { t: TFunction } = useTranslation()


  const anonSignup = (username: string) => {
    api.anonSignup({name: username})
      .then((response) => {
        setAnonPlayer(response.data!!.name)
        navigate('/')
      })
      .catch((error) => {
        handleError(error)
        if (error.status == 422) {
          setAnonSignupError({id: 0, text: t("errors.anonSignup")})
        }
      })

  }

  return {anonSignup, anonSignupError}
}