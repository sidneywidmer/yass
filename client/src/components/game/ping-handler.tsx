import {useEffect, useRef} from 'react'
import {api} from '@/api/client'
import {useGameStateStore} from '@/store/game-state'
import {useAxiosErrorHandler} from '@/hooks/use-axios-error-handler'

export function PingHandler() {
  const uuid = useGameStateStore(state => state.uuid)
  const intervalRef = useRef<NodeJS.Timeout>()
  const handleError = useAxiosErrorHandler()

  useEffect(() => {
    if (!uuid) return

    const ping = async () => {
      await api.pingSeat({seat: uuid!!})
        .catch(handleError)
    }

    intervalRef.current = setInterval(ping, 5000)
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
      }
    }
  }, [uuid])

  return null
}