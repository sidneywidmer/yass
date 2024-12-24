import {create} from "zustand";
import {persist} from "zustand/middleware";

interface AnonTokenState {
  token: string | null
  setToken: (token: string) => void
  clearToken: () => void
}

export const useAnonTokenStore = create<AnonTokenState>()(
  persist(
    (set) => ({
      token: null,
      setToken: (token) => set({ token }),
      clearToken: () => set({ token: null })
    }),
    {
      name: 'anon-token-storage'
    }
  )
)
