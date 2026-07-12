import {useGameStateStore} from "@/store/game-state.ts";
import {Dialog, DialogContent, DialogDescription, DialogFooter, DialogTitle} from "@/components/ui/dialog.tsx";
import {Button} from "@/components/ui/button.tsx";
import {WeisDisplay} from "@/components/game/weis-display.tsx";
import {api} from "@/api/client.ts";
import {useState} from "react";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {Weis} from "@/api/generated";
import {GameStates} from "@/types/game-states.ts";
import {useTranslation} from "react-i18next";

export function Weisen() {
  const isMyPos = useGameStateStore((state) => state.activePosition === state.position)
  const state = useGameStateStore(state => state.state)
  const weise = useGameStateStore(state => state.weise)
  const gameUuid = useGameStateStore(state => state.gameUuid)
  const shownWeise = useGameStateStore(state => state.shownWeise)
  const [open, setOpen] = useState(true)
  const handleAxiosError = useAxiosErrorHandler()
  const {t} = useTranslation()

  // It's possible that shown weise of other players are still displayed - wait with opening
  // this overlay so we don't have two open == bad UX
  if (state !== GameStates.WEISEN_FIRST || shownWeise || !isMyPos) {
    return
  }

  const onWeisSelect = (weis: Weis) => {
    api.weisen({game: gameUuid!, weis: weis})
      .then(() => setOpen(false))
      .catch(handleAxiosError)
  }

  const onSkip = () => {
    api.weisen({game: gameUuid!, weis: {type: "SKIP", cards: []}})
      .then(() => setOpen(false))
      .catch(handleAxiosError)
  }

  return (
    <Dialog open={open}>
      <DialogContent disableClose={true} className="w-auto min-w-[22rem] max-w-[90vw] sm:max-w-2xl" onPointerDownOutside={e => e.preventDefault()} container={document.getElementById('root')}>
        <DialogTitle className="text-center">{t("weisen.title")}</DialogTitle>
        <DialogDescription className="sr-only">{t("weisen.description")}</DialogDescription>
        <div className="flex flex-wrap justify-center gap-4 max-h-[60vh] overflow-y-auto p-2">
          {weise && weise.filter(weis => weis.type !== 'SKIP').map((weis, idx) => (
            <div
              key={idx}
              className="p-4 rounded-lg hover:bg-muted transition-colors cursor-pointer"
              onClick={() => onWeisSelect(weis)}
            >
              <WeisDisplay weis={weis}/>
            </div>
          ))}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onSkip}>{t("weisen.zeroWeis")}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
