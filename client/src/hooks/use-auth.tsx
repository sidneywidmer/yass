import {api} from "@/api/client.ts";
import {usePlayerStore} from "@/store/player.ts";
import {useEffect, useState} from "react";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";

export function useAuth() {
  const isAuthenticated = usePlayerStore(state => state.isAuthenticated)
  const setOryPlayer = usePlayerStore(state => state.setOryPlayer)
  const setAnonPlayer = usePlayerStore(state => state.setAnonPlayer)
  const logout = usePlayerStore(state => state.logout)
  const handleAxiosError = useAxiosErrorHandler()
  const [initialized, setInitialized] = useState(false)

  useEffect(() => {
    if (isAuthenticated) {
      setInitialized(true)
      return
    }

    const init = async () => {
      api.whoami()
        .then(response => {
          if (response.data) {
            if (response.data.isAnon) {
              setAnonPlayer(response.data.uuid, response.data.name)
            } else {
              setOryPlayer(response.data.uuid, response.data.name)
            }
          }
        })
        .catch((error) => {
          logout()
          handleAxiosError(error)
        })
        .finally(() => {
          setInitialized(true)
        })
    }
    init()
  }, []);

  return {isAuthenticated, initialized}
}