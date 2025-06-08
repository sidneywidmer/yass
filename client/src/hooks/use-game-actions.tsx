import {useCallback, useEffect, useRef, useState} from 'react'
import {useGameStateStore} from '@/store/game-state'

interface GameAction {
  type: string

  [key: string]: any
}

interface ActionHandler {
  handle: (action: GameAction) => Promise<void>
}

const useGameActions = () => {
  const [actionQueue, setActionQueue] = useState<GameAction[]>([])
  const processingRef = useRef(false)
  const playCard = useGameStateStore(state => state.playCard)
  const addCardsToHand = useGameStateStore(state => state.addCardsToHand)
  const position = useGameStateStore(state => state.position)
  const clearCards = useGameStateStore(state => state.clearCards)
  const addWeis = useGameStateStore(state => state.addWeis)

  const actionHandlers: Record<string, ActionHandler> = {
    CardPlayed: {
      handle: async (action) => {
        if (position == action.card.position) {
          return
        }
        playCard(action.card)
      }
    },
    UpdateHand: {
      handle: async (action) => {
        addCardsToHand(action.cards)
      }
    },
    ClearPlayedCards: {
      handle: async (action) => {
        // Allow enough time the players to register what card was played
        await new Promise(resolve => setTimeout(resolve, 1000))
        await clearCards(action.position)
      }
    },
    UpdateState: {
      handle: async (action) => useGameStateStore.setState({state: action.state}),
    },
    UpdateTrump: {
      handle: async (action) => useGameStateStore.setState({trump: action.trump}),
    },
    UpdateActive: {
      handle: async (action) => useGameStateStore.setState({activePosition: action.position}),
    },
    UpdatePossibleWeise: {
      handle: async (action) => useGameStateStore.setState({weise: action.weise}),
    },
    GameFinished: {
      handle: async (action) => useGameStateStore.setState({
        finished: {
          winners: action.winners,
          losers: action.losers,
          winnerPoints: action.winnerPoints,
          loserPoints: action.loserPoints
        }
      }),
    },
    UpdatePoints: {
      handle: async (action) => {
        useGameStateStore.setState({points: action.points})
      },
    },
    ShowWeis: {
      handle: async (action) => addWeis(action.position, action.weis)
    },
    PlayerJoined: {
      handle: async (action) => useGameStateStore.setState((state) => {
        const existingPlayerIndex = state.otherPlayers!!.findIndex(p => p.uuid === action.player.uuid)

        if (existingPlayerIndex !== -1) {
          return {
            otherPlayers: state.otherPlayers!!.map((p, i) =>
              i === existingPlayerIndex ? {...p, status: "CONNECTED"} : p
            )
          }
        }

        return {
          otherPlayers: [...state.otherPlayers!!, action.player]
        }
      })
    },
    PlayerDisconnected: {
      handle: async (action) => useGameStateStore.setState((state) => ({
        otherPlayers: state.otherPlayers!!.map(player =>
          player.uuid === action.player.uuid
            ? {...player, status: "DISCONNECTED"}
            : player
        )
      }))
    }
  }

  const processNextAction = useCallback(async () => {
    if (processingRef.current || actionQueue.length === 0) return

    processingRef.current = true
    const action = actionQueue[0]
    const handler = actionHandlers[action.type]

    if (handler) {
      await new Promise(resolve => setTimeout(resolve, 50))
      await handler.handle(action)
    } else {
      console.log("no handler for action found", action)
    }

    setActionQueue(queue => queue.slice(1))
    processingRef.current = false
  }, [actionQueue])

  const addActions = useCallback((actions: GameAction[]) => {
    setActionQueue(queue => [...queue, ...actions])
  }, [])

  useEffect(() => {
    processNextAction()
  }, [actionQueue, processNextAction])

  return {addActions}
}

export default useGameActions