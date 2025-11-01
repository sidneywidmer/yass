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
import {useErrorStore} from "@/store/error.ts";
import {useTranslation} from "react-i18next";
import {useNavigate} from "react-router-dom";
import {Share} from "@/components/game/share.tsx";

interface GameInstanceProps {
  tryCode: string
}

export function GameInstance({tryCode}: GameInstanceProps) {
  const setGameState = useGameStateStore(state => state.setGameState);
  const handleAxiosError = useAxiosErrorHandler()
  const {t} = useTranslation()
  const {addError} = useErrorStore()
  const navigate = useNavigate()

  useEffect(() => {
    api.joinGame({code: tryCode})
      .then((response) => {
        setGameState(response.data!!)
      })
      .catch(error => {
        if (error.response?.status === 404) {
          navigate('/lobby')
          return addError({
            title: t('errors.gameNotFound.title'),
            description: t('errors.gameNotFound.description')
          })
        }
        if (error.response.data.payload.domainError == "GameAlreadyFull") {
          navigate('/lobby')
          return addError({
            title: t('errors.gameFull.title'),
            description: t('errors.gameFull.description')
          })
        }
        return handleAxiosError(error)
      })
  }, [tryCode])

  return (
    <div className="flex min-h-svh bg-muted">
      <div className="fixed inset-0 overflow-hidden">
        <GameInfo/>
        <PlayedCards/>
        <OtherPlayers/>
        <ChooseTrump/>
        <Weisen/>
        <ShowWeise/>
        <GameFinished/>
        <PingHandler/>
        <WebSocketHandler/>
        <Share/>
      </div>
      <PlayerHand/>
    </div>
  )
}