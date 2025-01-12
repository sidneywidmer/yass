import {api} from "@/api/client.ts";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {useGameStateStore} from "@/store/game-state.ts";
import {GameInfo} from "@/components/game/game-info.tsx";
import {PlayerHand} from "@/components/game/player-hand.tsx";
import {PlayedCards} from "@/components/game/played-cards.tsx";
import {useEffect} from "react";
import {WebSocketHandler} from "@/components/game/websocket-handler.tsx";
import {PingHandler} from "@/components/game/ping-handler.tsx";
import {ChooseTrump} from "@/components/game/choose-trump.tsx";
import {Weisen} from "@/components/game/weisen.tsx";
import {ShowWeise} from "@/components/game/show-weise.tsx";
import OtherPlayers from "@/components/game/other-players.tsx";
import {GameFinished} from "@/components/game/game-finished.tsx";

interface GameInstanceProps {
  tryCode: string
}

export function GameInstance({tryCode}: GameInstanceProps) {
  const handleAxiosError = useAxiosErrorHandler()
  const {setGameState} = useGameStateStore()

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

  return (
    <div className="fixed inset-0 overflow-hidden">
      <GameInfo/>
      <PlayedCards/>
      <OtherPlayers/>
      <PlayerHand/>
      <ChooseTrump/>
      <Weisen/>
      <ShowWeise/>
      <GameFinished/>
      <PingHandler/>
      <WebSocketHandler/>
    </div>
  )
}