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
  const [isPaused, setIsPaused] = useState(false)
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
        // New hand means new trump will be chosen, clear the previous trump chooser and gschobe
        if (action.newCards) {
          useGameStateStore.setState({trumpChosenBy: undefined, gschobeBy: undefined})
        }
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
      handle: async (action) => useGameStateStore.setState({trump: action.trump, trumpChosenBy: action.position}),
    },
    UpdateGschobe: {
      handle: async (action) => useGameStateStore.setState({gschobeBy: action.position}),
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
    // Don't process if already processing, queue is empty, or paused
    if (processingRef.current || actionQueue.length === 0 || isPaused) return

    const action = actionQueue[0]

    // Special handling for ClearPlayedCards: check if weise overlay is open otherwise it's possible the player
    // doesn't see all the cards played by bots
    if (action.type === 'ClearPlayedCards') {
      const isWeiseOverlayOpen = useGameStateStore.getState().weiseOverlayOpen

      if (isWeiseOverlayOpen) {
        setIsPaused(true)
        return
      }
    }

    processingRef.current = true
    const handler = actionHandlers[action.type]

    if (handler) {
      await new Promise(resolve => setTimeout(resolve, 50))
      await handler.handle(action)
    } else {
      console.log("no handler for action found", action)
    }

    setActionQueue(queue => queue.slice(1))
    processingRef.current = false
  }, [actionQueue, isPaused])

  const addActions = useCallback((actions: GameAction[]) => {
    setActionQueue(queue => [...queue, ...actions])
  }, [])

  useEffect(() => {
    processNextAction()
  }, [actionQueue, processNextAction])

  // Resume queue processing 2 seconds after weise overlay closes
  useEffect(() => {
    if (!isPaused) return

    let timer: NodeJS.Timeout | null = null

    const unsubscribe = useGameStateStore.subscribe((state, prevState) => {
      // Detect when overlay closes (transitions from true to false) while we're paused
      if (prevState.weiseOverlayOpen && !state.weiseOverlayOpen) {
        // Start the 2-second countdown
        timer = setTimeout(() => {
          setIsPaused(false)
          // Trigger queue processing to resume (will process the ClearPlayedCards)
          processNextAction()
        }, 2000)
      }
    })

    return () => {
      unsubscribe()
      if (timer) clearTimeout(timer)
    }
  }, [isPaused, processNextAction])

  return {addActions}
}

export default useGameActions