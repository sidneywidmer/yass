import {api} from "@/api/client.tsx";
import {usePlayerStore} from "@/store/player.ts";
import {useEffect, useState} from "react";

export function useAuth() {
  const {isAuthenticated, setPlayer, wipe} = usePlayerStore()
  const [initialized, setInitialized] = useState(false)

  useEffect(() => {
    if (isAuthenticated) {
      setInitialized(true)
      return
    }

    const init = async () => {
      api.whoami()
        .then(response => {
          if (response.status === 200 && response.data) {
            setPlayer(response.data.name, response.data.uuid)
          }
        })
        .catch(() => {
          wipe()
        })
        .finally(() => {
          setInitialized(true)
        })
    }
    init()
  }, []);

  return {isAuthenticated, initialized}
}