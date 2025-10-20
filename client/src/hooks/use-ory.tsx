import {useNavigate} from "react-router-dom"
import {usePlayerStore} from "@/store/player.ts";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {ory} from "@/api/ory.ts";
import {ErrorMessage, getOryErrorMessage} from "@/api/helpers.ts";
import {useState} from "react";
import {useTranslation} from "react-i18next";

export const useOry = () => {
  const navigate = useNavigate()
  const handleError = useAxiosErrorHandler()
  const setOryPlayer = usePlayerStore(state => state.setOryPlayer)
  const logout = usePlayerStore(state => state.logout)
  const [loginError, setLoginError] = useState<ErrorMessage | null>(null)
  const [signupError, setSignupError] = useState<ErrorMessage | null>(null)
  const {t} = useTranslation();

  const getCsrfToken = (flow: any): string => {
    let csrfNode = flow.ui.nodes.find(
      (node: any) => node.attributes.name === "csrf_token"
    )
    return csrfNode?.attributes.value || ""
  }

  const login = (credentials: { email: string; password: string }, redirectTo?: string) =>
    ory.createBrowserLoginFlow()
      .then(flow => ory.updateLoginFlow({
        flow: flow.data.id,
        updateLoginFlowBody: {
          method: 'password',
          identifier: credentials.email,
          password: credentials.password,
          csrf_token: getCsrfToken(flow.data)
        }
      }))
      .then(response => {
        let oryUuid = response.data.session.identity?.id
        let username = response.data.session.identity?.traits.name
        setOryPlayer(oryUuid!!, username)
        navigate(redirectTo || '/')
      })
      .catch(error => {
        setLoginError(getOryErrorMessage(error.response.data, t));
        handleError(error)
        logout()
      })

  const signup = (credentials: { email: string; password: string; username: string }, redirectTo?: string) =>
    ory.createBrowserRegistrationFlow()
      .then(flow => ory.updateRegistrationFlow({
        flow: flow.data.id,
        updateRegistrationFlowBody: {
          method: 'password',
          traits: {
            email: credentials.email,
            name: credentials.username
          },
          password: credentials.password,
          csrf_token: getCsrfToken(flow.data)
        }
      }))
      .then(response => {
        let oryUuid = response.data.identity.id
        let username = response.data.identity.traits.name
        setOryPlayer(oryUuid!!, username)
        navigate(redirectTo || '/')
      })
      .catch((error) => {
        setSignupError(getOryErrorMessage(error.response.data, t));
        handleError(error)
      })

  return {login, loginError, signup, signupError}
}