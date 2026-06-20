import {useCallback, useEffect, useRef, useState} from 'react'
import {useGameStateStore} from '@/store/game-state'
import {CardInHand, CardOnTable, GameFinished, PlayerAtTable, Position, State, TotalPoints, Trump, WeisWithPoints} from '@/api/generated'

type CardPlayedAction = { type: 'CardPlayed'; card: CardOnTable }
type UpdateHandAction = { type: 'UpdateHand'; cards: CardInHand[]; newCards?: boolean }
type ClearPlayedCardsAction = { type: 'ClearPlayedCards'; position: Position }
type UpdateStateAction = { type: 'UpdateState'; state: State }
type UpdateTrumpAction = { type: 'UpdateTrump'; trump: Trump; position: Position }
type UpdateGschobeAction = { type: 'UpdateGschobe'; position: Position }
type UpdateActiveAction = { type: 'UpdateActive'; position: Position }
type UpdatePossibleWeiseAction = { type: 'UpdatePossibleWeise'; weise: WeisWithPoints[] }
type GameFinishedAction = { type: 'GameFinished' } & GameFinished
type UpdatePointsAction = { type: 'UpdatePoints'; points: Record<string, TotalPoints> }
type ShowWeisAction = { type: 'ShowWeis'; position: Position; weis: WeisWithPoints }
type PlayerJoinedAction = { type: 'PlayerJoined'; player: PlayerAtTable }
type PlayerDisconnectedAction = { type: 'PlayerDisconnected'; player: PlayerAtTable }

export type GameAction =
  | CardPlayedAction
  | UpdateHandAction
  | ClearPlayedCardsAction
  | UpdateStateAction
  | UpdateTrumpAction
  | UpdateGschobeAction
  | UpdateActiveAction
  | UpdatePossibleWeiseAction
  | GameFinishedAction
  | UpdatePointsAction
  | ShowWeisAction
  | PlayerJoinedAction
  | PlayerDisconnectedAction

type ActionHandlerFn<T extends GameAction> = (action: T) => Promise<void>
type ActionHandlerMap = { [K in GameAction['type']]: ActionHandlerFn<Extract<GameAction, { type: K }>> }

const useGameActions = () => {
  const [actionQueue, setActionQueue] = useState<GameAction[]>([])
  const [isPaused, setIsPaused] = useState(false)
  const processingRef = useRef(false)
  const playCard = useGameStateStore(state => state.playCard)
  const addCardsToHand = useGameStateStore(state => state.addCardsToHand)
  const position = useGameStateStore(state => state.position)
  const clearCards = useGameStateStore(state => state.clearCards)
  const addWeis = useGameStateStore(state => state.addWeis)

  const actionHandlers = {
    CardPlayed: async (action: CardPlayedAction) => {
      if (position == action.card.position) {
        return
      }
      playCard(action.card)
    },
    UpdateHand: async (action: UpdateHandAction) => {
      addCardsToHand(action.cards)
      // New hand means new trump will be chosen, clear the previous trump chooser and gschobe
      if (action.newCards) {
        useGameStateStore.setState({trumpChosenBy: undefined, gschobeBy: undefined})
      }
    },
    ClearPlayedCards: async (action: ClearPlayedCardsAction) => {
      // Allow enough time the players to register what card was played
      await new Promise(resolve => setTimeout(resolve, 1000))
      await clearCards(action.position)
    },
    UpdateState: async (action: UpdateStateAction) => useGameStateStore.setState({state: action.state}),
    UpdateTrump: async (action: UpdateTrumpAction) => useGameStateStore.setState({trump: action.trump, trumpChosenBy: action.position}),
    UpdateGschobe: async (action: UpdateGschobeAction) => useGameStateStore.setState({gschobeBy: action.position}),
    UpdateActive: async (action: UpdateActiveAction) => useGameStateStore.setState({activePosition: action.position}),
    UpdatePossibleWeise: async (action: UpdatePossibleWeiseAction) => useGameStateStore.setState({weise: action.weise}),
    GameFinished: async (action: GameFinishedAction) => useGameStateStore.setState({
      finished: {
        winners: action.winners,
        losers: action.losers,
        winnerPoints: action.winnerPoints,
        loserPoints: action.loserPoints
      }
    }),
    UpdatePoints: async (action: UpdatePointsAction) => {
      useGameStateStore.setState({points: action.points})
    },
    ShowWeis: async (action: ShowWeisAction) => addWeis(action.position, action.weis),
    PlayerJoined: async (action: PlayerJoinedAction) => useGameStateStore.setState((state) => {
      const existingPlayerIndex = state.otherPlayers!.findIndex(p => p.uuid === action.player.uuid)

      if (existingPlayerIndex !== -1) {
        return {
          otherPlayers: state.otherPlayers!.map((p, i) =>
            i === existingPlayerIndex ? {...p, status: 'CONNECTED' as const} : p
          )
        }
      }

      return {
        otherPlayers: [...state.otherPlayers!, action.player]
      }
    }),
    PlayerDisconnected: async (action: PlayerDisconnectedAction) => useGameStateStore.setState((state) => ({
      otherPlayers: state.otherPlayers!.map(player =>
        player.uuid === action.player.uuid
          ? {...player, status: 'DISCONNECTED' as const}
          : player
      )
    }))
  } satisfies ActionHandlerMap

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
    const handler = (actionHandlers as Record<string, ActionHandlerFn<GameAction>>)[action.type]

    if (handler) {
      await new Promise(resolve => setTimeout(resolve, 50))
      await handler(action)
    } else {
      console.log("no handler for action found", action)
    }

    setActionQueue(queue => queue.slice(1))
    processingRef.current = false
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [actionQueue, isPaused])

  const addActions = useCallback((actions: GameAction[]) => {
    setActionQueue(queue => [...queue, ...actions])
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    processNextAction()
  }, [actionQueue, processNextAction])

  // Resume queue processing 2 seconds after weise overlay closes
  useEffect(() => {
    if (!isPaused) return

    let timer: ReturnType<typeof setTimeout> | null = null

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
