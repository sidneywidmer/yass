import {useCallback, useEffect, useRef, useState} from 'react'
import {AxiosError} from 'axios'
import {api} from '@/api/client'
import {useGameStateStore} from '@/store/game-state'
import {Dialog, DialogContent, DialogTitle} from '@/components/ui/dialog'
import {DialogDescription} from '@radix-ui/react-dialog'
import {Button} from '@/components/ui/button'
import {useTranslation} from 'react-i18next'
import {WifiOff} from 'lucide-react'

const PING_INTERVAL_MS = 5000
const RETRY_COUNTDOWN_SECONDS = 5
// Hidden longer than this and websocket updates were likely missed, so refetch on return
const REFRESH_AFTER_HIDDEN_MS = 10000

export function ConnectionHandler() {
  const uuid = useGameStateStore(state => state.uuid)
  const code = useGameStateStore(state => state.code)
  const setGameState = useGameStateStore(state => state.setGameState)
  const {t} = useTranslation()

  const [offline, setOffline] = useState(false)
  const [countdown, setCountdown] = useState(RETRY_COUNTDOWN_SECONDS)
  const [reconnecting, setReconnecting] = useState(false)
  const reconnectingRef = useRef(false)
  const hiddenAtRef = useRef<number | undefined>(undefined)

  const goOffline = useCallback(() => {
    setCountdown(RETRY_COUNTDOWN_SECONDS)
    setOffline(true)
  }, [])

  const refreshGameState = useCallback(async () => {
    const response = await api.joinGame({code: code!})
    setGameState(response.data!)
  }, [code, setGameState])

  const reconnect = useCallback(async () => {
    if (reconnectingRef.current) return
    reconnectingRef.current = true
    setReconnecting(true)
    try {
      await api.pingSeat({seat: uuid})
      await refreshGameState()
      setOffline(false)
    } catch {
      setCountdown(RETRY_COUNTDOWN_SECONDS)
    } finally {
      reconnectingRef.current = false
      setReconnecting(false)
    }
  }, [uuid, refreshGameState])

  useEffect(() => {
    if (!uuid || offline) return

    const interval = setInterval(() => {
      if (document.hidden) return
      api.pingSeat({seat: uuid}).catch((error: AxiosError) => {
        if (!error.response || error.response.status >= 500) goOffline()
      })
    }, PING_INTERVAL_MS)

    return () => clearInterval(interval)
  }, [uuid, offline, goOffline])

  useEffect(() => {
    if (!offline || reconnecting) return
    const timeout = setTimeout(() => {
      if (countdown <= 1) {
        setCountdown(0)
        void reconnect()
      } else {
        setCountdown(c => c - 1)
      }
    }, 1000)
    return () => clearTimeout(timeout)
  }, [offline, reconnecting, countdown, reconnect])

  useEffect(() => {
    if (!offline) return
    const onOnline = () => void reconnect()
    window.addEventListener('online', onOnline)
    return () => window.removeEventListener('online', onOnline)
  }, [offline, reconnect])

  useEffect(() => {
    if (!uuid) return

    const onVisibilityChange = () => {
      if (document.hidden) {
        hiddenAtRef.current = Date.now()
        return
      }
      const hiddenFor = Date.now() - (hiddenAtRef.current ?? Date.now())
      hiddenAtRef.current = undefined
      if (offline) {
        void reconnect()
      } else if (hiddenFor > REFRESH_AFTER_HIDDEN_MS) {
        refreshGameState().catch(goOffline)
      }
    }

    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => document.removeEventListener('visibilitychange', onVisibilityChange)
  }, [uuid, offline, reconnect, refreshGameState, goOffline])

  return (
    <Dialog open={offline} modal>
      <DialogContent className="sm:max-w-md focus:outline-none focus-visible:outline-none" disableClose={true}
                     tabIndex={-1}>
        <DialogTitle className="text-center">{t('connection.title')}</DialogTitle>
        <DialogDescription></DialogDescription>
        <div className="flex flex-col items-center gap-4">
          <WifiOff className="h-10 w-10 text-muted-foreground"/>
          <p className="text-sm text-muted-foreground text-center">{t('connection.description')}</p>
          <p className="text-sm text-center">
            {reconnecting ? t('connection.reconnecting') : t('connection.retrying', {seconds: countdown})}
          </p>
          <Button onClick={() => void reconnect()} disabled={reconnecting}>
            {t('connection.refresh')}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
