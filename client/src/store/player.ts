import {create} from 'zustand'

interface PlayerState {
  uuid?: string
  name?: string
  isAuthenticated: boolean
  setPlayer: (uuid: string, name: string) => void
  wipe: () => void
}

export const usePlayerStore = create<PlayerState>((set) => ({
  isAuthenticated: false,
  setPlayer: (uuid, name) => set({
    uuid,
    name,
    isAuthenticated: true,
  }),
  wipe: () => set({
    uuid: undefined,
    name: undefined,
    isAuthenticated: false,
  })
}))