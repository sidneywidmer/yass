import {useNavigate} from "react-router-dom"
import {usePlayerStore} from "@/store/player.ts";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {ErrorMessage, generateAnonToken} from "@/api/helpers.ts";
import {useState} from "react";
import {useTranslation} from "react-i18next";
import {TFunction} from "i18next";
import {api} from "@/api/client.ts";
import {useAnonTokenStore} from "@/store/anon-token.ts";

export const useAnon = () => {
  const navigate = useNavigate()
  const handleError = useAxiosErrorHandler()
  const {setAnonPlayer} = usePlayerStore()
  const [anonSignupError, setAnonSignupError] = useState<ErrorMessage | null>(null)
  const {t}: { t: TFunction } = useTranslation()
  const {setToken} = useAnonTokenStore()


  const anonSignup = (username: string) => {
    const token = generateAnonToken()
    setToken(token)

    api.anonSignup({
      name: username,
      anonToken: token
    })
      .then((response) => {
        setAnonPlayer(token, response.data!!.name)
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