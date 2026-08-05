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
        endpoint: import.meta.env.VITE_CENTRIFUGO_API_URL + '/connection/websocket',
      },
    ];

    const centrifuge = new Centrifuge(transports, {});

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
  }, [gameUuid, uuid, addActions])

  return null
}