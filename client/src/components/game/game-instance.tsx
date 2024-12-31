import {api} from "@/api/client.ts";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {useGameStateStore} from "@/store/game-state.ts";
import {GameInfo} from "@/components/game/game-info.tsx";
import {PlayerHand} from "@/components/game/player-hand.tsx";
import {PlayedCards} from "@/components/game/played-cards.tsx";
import {useEffect} from "react";

interface GameInstanceProps {
  tryCode: string
}

export function GameInstance({tryCode}: GameInstanceProps) {
  const handleAxiosError = useAxiosErrorHandler()
  const {setGameState, seat} = useGameStateStore()

  useEffect(() => {
    api.joinGame({code: tryCode})
      .then((response) => {
        setGameState(response.data!!)
      })
      .catch(error => {
        // Redirect back to home
        handleAxiosError(error)
      })
  }, [tryCode])

  if (!seat?.uuid) {
    return (
      <p>Loading</p>
    )
  }

  // render played cards, other players, my hand, points
  return (
    <div className="fixed inset-0 overflow-hidden">
      <GameInfo/>
      <PlayedCards/>
      {/*<OpponentHand position="top"/>*/}
      {/*<OpponentHand position="left"/>*/}
      {/*<OpponentHand position="right"/>*/}
      <PlayerHand/>
      {/*<WebSocketHandler/>*/}
    </div>
  )
}