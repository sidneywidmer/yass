import {useNavigate} from "react-router-dom"
import {usePlayerStore} from "@/store/player.ts";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {ory} from "@/api/ory.ts";
import {api} from "@/api/client.ts";
import {ErrorMessage, getOryErrorMessage} from "@/api/helpers.ts";
import {UiNode, UiText} from "@ory/client";
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

  const getCsrfToken = (flow: { ui: { nodes: UiNode[] } }): string => {
    const csrfNode = flow.ui.nodes.find(
      (node: UiNode) => (node.attributes as { name?: string }).name === "csrf_token"
    )
    return (csrfNode?.attributes as { value?: string })?.value || ""
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
        const oryUuid = response.data.session.identity?.id
        const username = response.data.session.identity?.traits.name
        setOryPlayer(oryUuid!, username)
        navigate(redirectTo || '/')
      })
      .catch(error => {
        setLoginError(getOryErrorMessage(error.response.data, t));
        handleError(error)
        logout()
      })

  const register = (credentials: { email: string; password: string; username: string }) =>
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

  const signup = (credentials: { email: string; password: string; username: string }, redirectTo?: string) =>
    register(credentials)
      .then(response => {
        const oryUuid = response.data.identity.id
        const username = response.data.identity.traits.name
        setOryPlayer(oryUuid!, username)
        navigate(redirectTo || '/')
      })
      .catch((error) => {
        setSignupError(getOryErrorMessage(error.response.data, t));
        handleError(error)
      })

  const currentIdentity = () =>
    ory.toSession().then(response => response.data.identity).catch(() => undefined)

  const oryLogout = () =>
    ory.createBrowserLogoutFlow().then(flow => ory.updateLogoutFlow({token: flow.data.logout_token}))

  /**
   * Turns the current guest into a real account. Ory registration gives us the session cookie, the
   * link call then moves the existing player row over to that identity so uuid, name and game
   * history are kept.
   *
   * A guest can still be holding an ory session, since AuthMiddleware prefers the anon token over it.
   * Kratos refuses to register while a session exists, so we either reuse that session when it is the
   * account being created (an upgrade that registered but never linked) or drop it and start over.
   */
  const upgradeGuest = async (credentials: { email: string; password: string; username: string }, redirectTo?: string) => {
    try {
      let identity = await currentIdentity()

      if (identity?.traits.email !== credentials.email) {
        if (identity) await oryLogout()
        identity = (await register(credentials)).data.identity
      }

      await api.anonLink()
      setOryPlayer(identity!.id, identity!.traits.name)
      navigate(redirectTo || '/')
    } catch (error) {
      const response = (error as { response?: { status?: number, data?: { ui?: { messages?: UiText[], nodes?: UiNode[] } } } }).response

      // The server answers 409 when the ory identity already belongs to another player
      if (response?.status === 409) {
        setSignupError({id: 0, text: t("errors.upgradeExists")})
        return
      }

      if (response?.data?.ui) {
        setSignupError(getOryErrorMessage(response.data, t))
      } else {
        setSignupError({id: 0, text: t("errors.upgradeFailed")})
      }
      handleError(error)
    }
  }

  return {login, loginError, signup, upgradeGuest, signupError}
}
