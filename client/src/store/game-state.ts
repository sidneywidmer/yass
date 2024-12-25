import {create} from 'zustand'
import {CardInHand, CardOnTable, JoinGameResponse} from "@/api/generated";

interface GameState extends JoinGameResponse {
  isConnected: boolean
  setGameState: (state: JoinGameResponse) => void
  reset: () => void,
  removeCardFromHand: (card: CardInHand) => void,
  playCard: (card: CardOnTable) => void,
  setAllCardsUnplayable: () => void
}

const initialState = {
  gameUuid: "",
  code: "",
  seat: {},
  cardsPlayed: [],
  otherPlayers: [],
  isConnected: false
}

export const useGameStateStore = create<GameState>((set) => ({
  ...initialState,
  setGameState: (state) => set({...state}),
  reset: () => set(initialState),
  removeCardFromHand: (card) => set((state) => ({
    seat: {
      ...state.seat,
      cards: state.seat!!.cards!!.filter(c => !(card.suit === c.suit && card.rank === c.rank)
      )
    }
  })),
  setAllCardsUnplayable: () => set((state) => ({
    seat: {
      ...state.seat,
      cards: state.seat!!.cards!!.filter((card) => card.state == "PLAYABLE").map(card => ({
        ...card,
        state: "UNPLAYABLE"
      }))
    }
  })),
  playCard: (card: CardOnTable) => set((state) => ({
    cardsPlayed: [...state.cardsPlayed!!, card]
  })),
}))