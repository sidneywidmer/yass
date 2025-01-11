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
  weise: WeisWithPoints[]
  otherWeise: { [position: string]: WeisWithPoints[] }
}

interface GameStateActions {
  setGameState: (state: JoinGameResponse) => void
  reset: () => void
  removeCardFromHand: (card: CardInHand) => void
  addCardsToHand: (cards: CardInHand[]) => void
  playCard: (card: CardOnTable) => void
  clearCards: (position: Position) => Promise<void>
  addWeis: (position: string, weis: WeisWithPoints) => void
  clearWeise: () => void
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
  otherWeise: {},
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
    weise: response.seat?.weise ?? []
  }),
  reset: () => set(initialState),
  removeCardFromHand: (card) => set((state) => ({
    cards: state.cards.filter(c => !(card.suit === c.suit && card.rank === c.rank))
  })),
  playCard: (card) => set((state) => ({
    cardsPlayed: [...state.cardsPlayed!!, card]
  })),
  addCardsToHand: (cards) => set({cards: cards}),
  clearCards: async (position) => {
    set({clearDirection: position})
    // Wait for next render cycle, I have no idea if this is a good idea but for now it works #providurium :)
    await new Promise(resolve => setTimeout(resolve, 0))
    set({cardsPlayed: []})
  },
  addWeis: (position, weis) => set((state) => ({
    otherWeise: {
      ...state.otherWeise,
      [position]: [...(state.otherWeise[position] || []), weis]
    }
  })),
  clearWeise: () => set({otherWeise: {}}),
  getPlayer: (position: Position) => {
    const otherPlayers: Array<PlayerAtTable> = useGameStateStore.getState().otherPlayers!!
    return otherPlayers.find(p => p.position === position)
  }
}))