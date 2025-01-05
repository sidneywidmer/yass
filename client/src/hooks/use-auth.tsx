import {api} from "@/api/client.ts";
import {usePlayerStore} from "@/store/player.ts";
import {useEffect, useState} from "react";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";

export function useAuth() {
  const {isAuthenticated, setOryPlayer, setAnonPlayer, logout} = usePlayerStore()
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
              setAnonPlayer(response.data.name)
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