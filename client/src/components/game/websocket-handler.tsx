import {useEffect} from 'react'
import {Centrifuge, TransportName} from 'centrifuge'
import {useGameStateStore} from '@/store/game-state'
import useGameActions from "@/hooks/use-game-actions.tsx";

export function WebSocketHandler() {
  const gameUuid = useGameStateStore(state => state.gameUuid)
  const uuid = useGameStateStore(state => state.uuid)
  const {addActions} = useGameActions()

  useEffect(() => {
    if (!gameUuid) return

    const transports = [
      {
        transport: 'websocket' as TransportName,
        endpoint: import.meta.env.VITE_CENTRIFUGO_API_URL_WS + '/connection/websocket',
      },
    ];

    const centrifuge = new Centrifuge(transports, {});

    const connectToWs = async () => {
      try {
        console.log("Connecting to Centrifugo...")
        centrifuge.connect()

        const sub = centrifuge.newSubscription(`seat:#${uuid}`)
        sub.on('publication', (ctx) => {
          addActions(ctx.data)
        })

        sub.subscribe()
        console.log("Connected and subscribed to seat:#" + uuid)

        return () => {
          console.log("Cleaning up connection...")
          sub.unsubscribe()
          centrifuge.disconnect()
        }
      } catch (error) {
        console.error("WebSocket connection error:", error)
        return () => {
          centrifuge.disconnect()
        }
      }
    }

    let cleanup: (() => void) | undefined

    connectToWs().then(cleanupFn => {
      cleanup = cleanupFn
    }).catch(error => {
      console.error("Failed to connect to WebSocket:", error)
    })

    return () => {
      if (cleanup) {
        cleanup()
      }
    }
  }, [gameUuid, uuid, addActions])

  return null
}