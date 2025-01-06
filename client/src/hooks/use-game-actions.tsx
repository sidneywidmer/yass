import {useCallback, useEffect, useRef, useState} from 'react'
import {useGameStateStore} from '@/store/game-state'

interface GameAction {
  type: string

  [key: string]: any
}

interface ActionHandler {
  handle: (action: GameAction) => void
  delay?: number
}

const useGameActions = () => {
  const [actionQueue, setActionQueue] = useState<GameAction[]>([])
  const processingRef = useRef(false)
  const {playCard, addCardsToHand, position, clearCards, addWeis} = useGameStateStore()

  const actionHandlers: Record<string, ActionHandler> = {
    CardPlayed: {
      handle: (action) => {
        if (position == action.card.position) {
          return
        }
        playCard(action.card)
      }
    },
    UpdateHand: {
      handle: (action) => {
        addCardsToHand(action.cards)
      }
    },
    ClearPlayedCards: {
      handle: (action) => {
        clearCards(action.position)
      },
      delay: 2000
    },
    UpdateState: {
      handle: (action) => useGameStateStore.setState({state: action.state}),
    },
    UpdateTrump: {
      handle: (action) => useGameStateStore.setState({trump: action.trump}),
    },
    UpdateActive: {
      handle: (action) => useGameStateStore.setState({activePosition: action.position}),
    },
    UpdatePossibleWeise: {
      handle: (action) => useGameStateStore.setState({weise: action.weise}),
    },
    ShowWeis: {
      handle: (action) => addWeis(action.position, action.weis)
    },
    PlayerJoined: {
      handle: (action) => useGameStateStore.setState((state) => {
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
      handle: (action) => useGameStateStore.setState((state) => ({
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
      if (handler.delay) {
        console.log("handle action in " + handler.delay, action)
        await new Promise(resolve => setTimeout(resolve, handler.delay))
      } else {
        console.log("handle action w/o delay", action)
        await new Promise(resolve => setTimeout(resolve, 50))
      }
      handler.handle(action)
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