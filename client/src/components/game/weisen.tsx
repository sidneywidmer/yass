import {useGameStateStore} from "@/store/game-state.ts";
import {Dialog, DialogContent, DialogDescription, DialogFooter, DialogTitle} from "@/components/ui/dialog.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Card} from "@/components/game/card.tsx";
import {api} from "@/api/client.ts";
import {useState} from "react";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {Weis} from "@/api/generated";
import {useTranslation} from "react-i18next";

export function Weisen() {
  const isMyPos = useGameStateStore((state) => state.activePosition === state.position)
  const state = useGameStateStore(state => state.state)
  const weise = useGameStateStore(state => state.weise)
  const gameUuid = useGameStateStore(state => state.gameUuid)
  const otherWeise = useGameStateStore(state => state.otherWeise)
  const [open, setOpen] = useState(true)
  const handleAxiosError = useAxiosErrorHandler()
  const {t} = useTranslation()

  // It's possible that we still display weis of other players - wait with opening
  // this overlay so we don't have two open == bad UX
  if (state !== "WEISEN_FIRST" || Object.entries(otherWeise).length > 0 || !isMyPos) {
    return
  }

  const onWeisSelect = (weis: Weis) => {
    api.weisen({game: gameUuid!!, weis: weis})
      .then(() => setOpen(false))
      .catch(handleAxiosError)
  }

  const onSkip = () => {
    api.weisen({game: gameUuid!!, weis: {type: "SKIP", cards: []}})
      .then(() => setOpen(false))
      .catch(handleAxiosError)
  }

  return (
    <Dialog open={open} modal>
      <DialogContent disableClose={true} className="sm:max-w-md" onPointerDownOutside={e => e.preventDefault()}>
        <DialogTitle className="text-center">{t("weisen.title")}</DialogTitle>
        <DialogDescription className="sr-only">{t("weisen.description")}</DialogDescription>
        <div className="space-y-6 max-h-[60vh] overflow-y-auto p-2">
          {weise && weise.filter(weis => weis.type !== 'SKIP').map((weis, idx) => (
            <div
              key={idx}
              className="p-4 rounded-lg hover:bg-muted transition-colors cursor-pointer"
              onClick={() => onWeisSelect(weis)}
            >
              <div className="flex items-center gap-4 mb-2">
                <span className="font-medium">{t(`weise.${weis.type}`)}</span>
                <div className="h-4 w-px bg-border"/>
                <span className="text-sm text-muted-foreground">{weis.points} {t("weisen.points")}</span>
              </div>
              <div className="grid grid-cols-3 sm:grid-cols-4 gap-2">
                {weis.cards!!.map((card, i) => (
                  <Card key={i} card={card}/>
                ))}
              </div>
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
