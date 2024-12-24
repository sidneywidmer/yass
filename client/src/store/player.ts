import {create} from 'zustand'

interface PlayerState {
  oryUuid?: string
  anonToken?: string
  name?: string
  isAuthenticated: boolean
  isAnon: () => boolean
  setOryPlayer: (uuid: string, name: string) => void
  setAnonPlayer: (token: string, name: string) => void
  logout: () => void
}

export const usePlayerStore = create<PlayerState>((set, get) => ({
  isAuthenticated: false,
  isAnon: () => !!get().anonToken,
  setAnonPlayer: (token, name) => set({
    anonToken: token,
    name,
    isAuthenticated: true,
  }),
  setOryPlayer: (oryUuid, name) => set({
    oryUuid,
    name,
    isAuthenticated: true,
  }),
  logout: () => set({
    oryUuid: undefined,
    anonToken: undefined,
    name: undefined,
    isAuthenticated: false,
  })
}))