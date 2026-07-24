import {create} from 'zustand'
import {
  CardInHand,
  CardOnTable,
  JoinGameResponse,
  PlayerAtTable,
  Position,
  State,
  TotalPoints,
  Trump,
  WeisWithPoints
} from "@/api/generated"
import {useSettingsStore} from "@/store/settings.ts"
import {GameDelay, playSpeedTimings} from "@/types/play-speed.ts"

type FlatGameState = Omit<JoinGameResponse, 'seat'> & {
  isConnected: boolean
  uuid: string
  cards: CardInHand[]
  position?: Position
  clearDirection?: Position
  points: { [p: string]: TotalPoints }
  state?: State
  activePosition?: Position
  trump?: Trump
  trumpChosenBy?: Position
  gschobeBy?: Position
  weise: WeisWithPoints[]
  declaredWeisPoints: { [position: string]: number }
  weisWinners?: Position[]
  shownWeise?: { [position: string]: WeisWithPoints[] }
}

interface GameStateActions {
  setGameState: (state: JoinGameResponse) => void
  reset: () => void
  removeCardFromHand: (card: CardInHand) => void
  addCardsToHand: (cards: CardInHand[]) => void
  playCard: (card: CardOnTable) => void
  resetActivePosition: () => void
  clearCards: (position: Position) => Promise<void>
  declareWeis: (position: Position, points: number) => void
  showWeise: (weiseByPosition: { [position: string]: WeisWithPoints[] }) => void
  dismissShownWeise: () => void
  clearDeclaredWeise: () => void
  getPlayer: (position: Position) => PlayerAtTable | undefined
}

const initialState: FlatGameState = {
  uuid: "",
  gameUuid: "",
  code: "",
  cards: [],
  points: {
    NORTH: {cardPoints: 0, weisPoints: 0},
    SOUTH: {cardPoints: 0, weisPoints: 0},
    EAST: {cardPoints: 0, weisPoints: 0},
    WEST: {cardPoints: 0, weisPoints: 0}
  },
  weise: [],
  declaredWeisPoints: {},
  weisWinners: undefined,
  shownWeise: undefined,
  cardsPlayed: [],
  otherPlayers: [],
  isConnected: false
}

export const useGameStateStore = create<FlatGameState & GameStateActions>((set) => ({
  ...initialState,
  setGameState: (response) => set({
    ...response,
    uuid: response.seat?.uuid ?? "",
    cards: response.seat?.cards ?? [],
    position: response.seat?.position,
    points: response.seat?.points,
    state: response.seat?.state,
    activePosition: response.seat?.activePosition,
    trump: response.seat?.trump,
    trumpChosenBy: response.seat?.trumpChosenBy ?? undefined,
    gschobeBy: response.seat?.gschobeBy ?? undefined,
    weise: response.seat?.weise ?? []
  }),
  reset: () => set(initialState),
  removeCardFromHand: (card) => set((state) => ({
    cards: state.cards.filter(c => !(card.suit === c.suit && card.rank === c.rank))
  })),
  playCard: (card) => set((state) => ({
    cardsPlayed: [...state.cardsPlayed!, card]
  })),
  addCardsToHand: (cards) => set({cards: cards}),
  clearCards: async (position) => {
    set({clearDirection: position})
    // The winning card's shake animation (played-cards.tsx) only takes 200ms, but we hold
    // here so players have time to register who won the trick before the cards slide away
    await new Promise(resolve => setTimeout(resolve, playSpeedTimings[useSettingsStore.getState().playSpeed][GameDelay.WINNER_HOLD]))
    set({cardsPlayed: []})
    set({clearDirection: undefined})
    // Give some time after cleaning the table until the next card comes flying out
    await new Promise(resolve => setTimeout(resolve, 300))
  },
  declareWeis: (position, points) => set((state) => ({
    declaredWeisPoints: {...state.declaredWeisPoints, [position]: points}
  })),
  showWeise: (weiseByPosition) => set((state) => {
    // Stöck reveals mid-play share the dialog but never decide the weis comparison
    const stoeckOnly = Object.values(weiseByPosition).flat().every(weis => weis.type === 'STOECK')
    return {
      shownWeise: weiseByPosition,
      weisWinners: stoeckOnly ? state.weisWinners : Object.keys(weiseByPosition) as Position[]
    }
  }),
  dismissShownWeise: () => set({shownWeise: undefined}),
  clearDeclaredWeise: () => set({declaredWeisPoints: {}, weisWinners: undefined}),
  resetActivePosition: () => set({activePosition: undefined}),
  getPlayer: (position: Position) => {
    const otherPlayers: Array<PlayerAtTable> = useGameStateStore.getState().otherPlayers!
    return otherPlayers.find(p => p.position === position)
  }
}))