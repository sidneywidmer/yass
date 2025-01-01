import {useEffect} from 'react'
import {Centrifuge} from 'centrifuge'
import {useGameStateStore} from '@/store/game-state'
import useGameActions from "@/hooks/use-game-actions.tsx";


export function WebSocketHandler() {
  const {gameUuid, uuid} = useGameStateStore()
  const {addActions} = useGameActions()

  const centrifuge = new Centrifuge(import.meta.env.VITE_CENTRIFUGO_API_URL + '/connection/websocket', {});

  useEffect(() => {
    if (!gameUuid) return

    const connectToWs = async () => {
      try {
        centrifuge.on('connected', function (ctx) {
          console.log("connected ", ctx);
        });
        centrifuge.on('disconnected', function (ctx) {
          console.log("disconnected ", ctx);
        });

        const sub = centrifuge.newSubscription(`seat:#${uuid}`)
        sub.on('subscribed', function (ctx) {
          console.log('subscribed ', ctx);
        });

        sub.on('publication', (ctx) => {
          addActions(ctx.data)
        })

        sub.subscribe()
        centrifuge.connect()

        return () => {
          sub.unsubscribe()
          centrifuge.disconnect()
        }
      } catch (error) {
        console.log(error)
      }
    }

    connectToWs()
  }, [gameUuid])

  return null
}