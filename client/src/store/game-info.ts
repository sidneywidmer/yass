import {create} from 'zustand'
import {api} from '@/api/client'
import type {RunningGame} from '@/api/generated'

interface GameInfoState {
  runningGames: RunningGame[]
  dailyChallengePlayed: boolean
  isLoading: boolean
  refresh: () => Promise<void>
}

export const useGameInfoStore = create<GameInfoState>((set) => ({
  runningGames: [],
  dailyChallengePlayed: false,
  isLoading: true,
  // The lobby stays usable if this fails, the sections just fall back to their default state.
  refresh: async () => {
    set({isLoading: true})
    try {
      const {data} = await api.gameInfo()
      set({
        runningGames: data?.runningGames ?? [],
        dailyChallengePlayed: data?.dailyChallengePlayed ?? false
      })
    } catch {
      set({runningGames: [], dailyChallengePlayed: false})
    } finally {
      set({isLoading: false})
    }
  }
}))
