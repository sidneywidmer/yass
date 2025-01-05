import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {useGameStateStore} from "@/store/game-state.ts";
import {Dialog, DialogContent, DialogDescription, DialogTitle} from "@/components/ui/dialog.tsx";
import {ArrowDown, ArrowUp, Club, Diamond, Forward, Heart, Spade} from "lucide-react";
import {Button} from "@/components/ui/button.tsx";
import {api} from "@/api/client.ts";
import {Trump} from "@/api/generated";

export function ChooseTrump() {
  const handleAxiosError = useAxiosErrorHandler()
  const {state, gameUuid} = useGameStateStore()
  const isMyPos = useGameStateStore((state) => state.activePosition === state.position)

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
        <DialogTitle className="text-center">Trumpf wählen</DialogTitle>
        <DialogDescription className="sr-only">Choose a trump</DialogDescription>
        <div className="flex flex-col gap-4 items-center pt-4">
          <div className="flex gap-4">
            <Button variant="outline" size="icon" onClick={() => selectTrump("HEARTS")}>
              <Heart className="w-4 h-4 text-red-500"/>
            </Button>
            <Button variant="outline" size="icon" onClick={() => selectTrump("SPADES")}>
              <Spade className="w-4 h-4"/>
            </Button>
            <Button variant="outline" size="icon" onClick={() => selectTrump("CLUBS")}>
              <Club className="w-4 h-4"/>
            </Button>
            <Button variant="outline" size="icon" onClick={() => selectTrump("DIAMONDS")}>
              <Diamond className="w-4 h-4 text-red-500"/>
            </Button>
          </div>

          <div className="flex gap-4">
            <Button variant="outline" size="icon" onClick={() => selectTrump("UNEUFE")}>
              <ArrowUp className="w-4 h-4"/>
            </Button>
            <Button variant="outline" size="icon" onClick={() => selectTrump("OBEABE")}>
              <ArrowDown className="w-4 h-4"/>
            </Button>
          </div>

          {state == "SCHIEBE" &&
              <div>
                  <Button variant="outline" size="icon" onClick={() => selectTrump("SCHIEBE")}>
                      <Forward className="w-4 h-4"/>
                  </Button>
              </div>
          }
        </div>
      </DialogContent>
    </Dialog>
  )
}