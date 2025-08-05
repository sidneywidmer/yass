import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {useGameStateStore} from "@/store/game-state.ts";
import {Dialog, DialogContent, DialogDescription, DialogTitle} from "@/components/ui/dialog.tsx";
import {Forward} from "lucide-react";
import {Button} from "@/components/ui/button.tsx";
import {api} from "@/api/client.ts";
import {Trump} from "@/api/generated";
import {TrumpIcon} from "@/components/game/trump-icon";
import {useTranslation} from "react-i18next";

export function ChooseTrump() {
  const handleAxiosError = useAxiosErrorHandler()
  const state = useGameStateStore(state => state.state)
  const gameUuid = useGameStateStore(state => state.gameUuid)
  const isMyPos = useGameStateStore((state) => state.activePosition === state.position)
  const {t} = useTranslation()

  /**
   * We handle 3 cases  here:
   * 1. Game is still in state SCHIEBE and the player chose SCHIEBE as "trump", meaning we well schiebe
   * 2. Game is in state SCHIEBE and the playe chose a trump (e.g. HEARTS), meaning we will tell the api to NO SCHIEBE and directly after send the trump
   * 3. Game is in state TRUMP, meaning the partner chose SCHIEBE so we just select the trump
   */
  const selectTrump = async (trump: Exclude<Trump, "FREESTYLE"> | "SCHIEBE") => {
    if (trump === "SCHIEBE") {
      return api.schiebe({game: gameUuid!!, gschobe: "YES"})
        .catch(handleAxiosError)
    }

    const action = state === "SCHIEBE"
      ? api.schiebe({game: gameUuid!!, gschobe: "NO"})
        .then(() => api.chooseTrump({game: gameUuid!!, trump}))
      : api.chooseTrump({game: gameUuid!!, trump})

    return action.catch(handleAxiosError)
  }

  if ((state !== "SCHIEBE" && state !== "TRUMP") || !isMyPos) {
    return
  }

  return (
    <Dialog open={true} modal>
      <DialogContent disableClose={true} className="sm:max-w-md" onPointerDownOutside={e => e.preventDefault()}>
        <DialogTitle className="text-center">{t("chooseTrump.title")}</DialogTitle>
        <DialogDescription className="sr-only">{t("chooseTrump.description")}</DialogDescription>
        <div className="flex flex-col gap-4 items-center pt-4">
          <div className="flex gap-4">
            <Button variant="outline" className="w-11 h-11" size="icon" onClick={() => selectTrump("HEARTS")}>
              <TrumpIcon trump="HEARTS" className="w-6 h-6"/>
            </Button>
            <Button variant="outline" className="w-11 h-11" size="icon" onClick={() => selectTrump("SPADES")}>
              <TrumpIcon trump="SPADES" className="w-6 h-6"/>
            </Button>
            <Button variant="outline" className="w-11 h-11" size="icon" onClick={() => selectTrump("CLUBS")}>
              <TrumpIcon trump="CLUBS" className="w-6 h-6"/>
            </Button>
            <Button variant="outline" className="w-11 h-11" size="icon" onClick={() => selectTrump("DIAMONDS")}>
              <TrumpIcon trump="DIAMONDS" className="w-6 h-6"/>
            </Button>
          </div>

          <div className="flex gap-4">
            <Button variant="outline" className="w-11 h-11" size="icon" onClick={() => selectTrump("UNEUFE")}>
              <TrumpIcon trump="UNEUFE" className="w-6 h-6"/>
            </Button>
            <Button variant="outline" className="w-11 h-11" size="icon" onClick={() => selectTrump("OBEABE")}>
              <TrumpIcon trump="OBEABE" className="w-6 h-6"/>
            </Button>
          </div>

          {state == "SCHIEBE" &&
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