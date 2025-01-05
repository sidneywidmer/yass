import {create} from 'zustand'

interface PlayerState {
  oryUuid?: string
  name?: string
  isAuthenticated: boolean
  isAnon: boolean
  setOryPlayer: (uuid: string, name: string) => void
  setAnonPlayer: (name: string) => void
  logout: () => void
}

export const usePlayerStore = create<PlayerState>((set) => ({
  isAuthenticated: false,
  isAnon: false,
  setAnonPlayer: (name) => set({
    name,
    isAuthenticated: true,
    isAnon: true,
  }),
  setOryPlayer: (oryUuid, name) => set({
    oryUuid,
    name,
    isAuthenticated: true,
    isAnon: false
  }),
  logout: () => set({
    oryUuid: undefined,
    name: undefined,
    isAuthenticated: false,
  })
}))