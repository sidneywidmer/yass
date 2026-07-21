import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {useGameStateStore} from "@/store/game-state.ts";
import {Dialog, DialogContent, DialogDescription, DialogTitle} from "@/components/ui/dialog.tsx";
import {Forward} from "lucide-react";
import {Button} from "@/components/ui/button.tsx";
import {api} from "@/api/client.ts";
import {Trump} from "@/api/generated";
import {GameStates} from "@/types/game-states.ts";
import {TrumpIcon} from "@/components/game/trump-icon";
import {TrumpMultiplierBadge} from "@/components/game/trump-multiplier-badge";
import {useTranslation} from "react-i18next";

function TrumpButton({trump, onSelect}: {
  trump: Trump
  onSelect: (trump: Trump) => void
}) {
  return (
    <Button variant="outline" className="w-11 h-11 relative" size="icon" onClick={() => onSelect(trump)}>
      <TrumpIcon trump={trump} className="w-6 h-6"/>
      <TrumpMultiplierBadge trump={trump}/>
    </Button>
  )
}

export function ChooseTrump() {
  const handleAxiosError = useAxiosErrorHandler()
  const state = useGameStateStore(state => state.state)
  const gameUuid = useGameStateStore(state => state.gameUuid)
  const isMyPos = useGameStateStore((state) => state.activePosition === state.position)
  const {t} = useTranslation()

  /**
   * We handle 3 cases  here:
   * 1. Game is still in state SCHIEBE and the player chose SCHIEBE as "trump", meaning we will schiebe
   * 2. Game is in state SCHIEBE and the player chose a trump (e.g. HEARTS), meaning we will tell the api to NO SCHIEBE and directly after send the trump
   * 3. Game is in state TRUMP, meaning the partner chose SCHIEBE so we just select the trump
   */
  const selectTrump = async (trump: Trump | "SCHIEBE") => {
    if (trump === "SCHIEBE") {
      return api.schiebe({game: gameUuid!, gschobe: "YES"})
        .catch(handleAxiosError)
    }

    const action = state === GameStates.SCHIEBE
      ? api.schiebe({game: gameUuid!, gschobe: "NO"})
        .then(() => api.chooseTrump({game: gameUuid!, trump}))
      : api.chooseTrump({game: gameUuid!, trump})

    return action.catch(handleAxiosError)
  }

  if ((state !== GameStates.SCHIEBE && state !== GameStates.TRUMP) || !isMyPos) {
    return
  }

  return (
    <Dialog open={true} modal>
      <DialogContent disableClose={true} className="sm:max-w-md" onPointerDownOutside={e => e.preventDefault()} container={document.getElementById('root')}>
        <DialogTitle className="text-center">{t("chooseTrump.title")}</DialogTitle>
        <DialogDescription className="sr-only">{t("chooseTrump.description")}</DialogDescription>
        <div className="flex flex-col gap-4 items-center pt-4">
          <div className="flex gap-4">
            <TrumpButton trump="HEARTS" onSelect={selectTrump}/>
            <TrumpButton trump="DIAMONDS" onSelect={selectTrump}/>
            <TrumpButton trump="SPADES" onSelect={selectTrump}/>
            <TrumpButton trump="CLUBS" onSelect={selectTrump}/>
          </div>

          <div className="flex gap-4">
            <TrumpButton trump="UNEUFE" onSelect={selectTrump}/>
            <TrumpButton trump="OBEABE" onSelect={selectTrump}/>
          </div>

          {state == GameStates.SCHIEBE &&
              <div>
                  <Button variant="outline" className="w-11 h-11" onClick={() => selectTrump("SCHIEBE")}>
                      <Forward className="w-6 h-6"/>
                  </Button>
              </div>
          }
        </div>
      </DialogContent>
    </Dialog>
  )
}