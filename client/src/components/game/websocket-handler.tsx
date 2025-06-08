import {useEffect} from 'react'
import {Centrifuge, TransportName} from 'centrifuge'
import {useGameStateStore} from '@/store/game-state'
import useGameActions from "@/hooks/use-game-actions.tsx";

class CustomEventSource extends EventSource {
  constructor(url: string, _?: EventSourceInit) {
    super(url, {
      withCredentials: true,
    });
  }
}

export function WebSocketHandler() {
  const gameUuid = useGameStateStore(state => state.gameUuid)
  const uuid = useGameStateStore(state => state.uuid)
  const {addActions} = useGameActions()
  const transports = [
    {
      transport: 'sse' as TransportName,
      endpoint: import.meta.env.VITE_CENTRIFUGO_API_URL_SSE + '/connection/sse',
    }
  ];
  const centrifuge = new Centrifuge(transports, {
    debug: true,
    eventsource: CustomEventSource,
    emulationEndpoint: import.meta.env.VITE_CENTRIFUGO_API_URL_SSE + '/emulation',
  });

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