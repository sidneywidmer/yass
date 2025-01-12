import {create} from 'zustand'

interface PlayerState {
  uuid?: string
  name?: string
  isAuthenticated: boolean
  isAnon: boolean
  setOryPlayer: (uuid: string, name: string) => void
  setAnonPlayer: (uuid: string, name: string) => void
  logout: () => void
}

export const usePlayerStore = create<PlayerState>((set) => ({
  isAuthenticated: false,
  isAnon: false,
  setAnonPlayer: (uuid, name) => set({
    uuid,
    name,
    isAuthenticated: true,
    isAnon: true,
  }),
  setOryPlayer: (uuid, name) => set({
    uuid,
    name,
    isAuthenticated: true,
    isAnon: false
  }),
  logout: () => set({
    uuid: undefined,
    name: undefined,
    isAuthenticated: false,
  })
}))