import {Button} from "@/components/ui/button";
import {Dialog, DialogContent, DialogDescription, DialogTitle} from "@/components/ui/dialog";
import {Position, WeisWithPoints} from "@/api/generated";
import {useGameStateStore} from "@/store/game-state";
import {WeisDisplay} from "@/components/game/weis-display";
import {useEffect} from "react";
import {useTranslation} from "react-i18next";

const BUBBLE_LINGER_MS = 800

function PositionWeise({position, weise}: { position: Position, weise: WeisWithPoints[] }) {
  const getPlayer = useGameStateStore(state => state.getPlayer)
  const {t} = useTranslation()

  const totalPoints = weise.reduce((sum, weis) => sum + (weis.points ?? 0), 0)

  return (
    <div className="flex flex-col items-center gap-3">
      <p className="text-sm font-medium text-center">
        {t("weisen.announcement", {name: getPlayer(position)?.name ?? position, points: totalPoints})}
      </p>
      <div className="flex flex-wrap justify-center gap-4">
        {weise.map((weis, idx) => (
          <WeisDisplay key={idx} weis={weis}/>
        ))}
      </div>
    </div>
  )
}

export function WeisAnnouncement() {
  // The shown weise live in the store (not local state) because the action queue pauses
  // ClearPlayedCards while the overlay is open and resumes when it closes
  const shownWeise = useGameStateStore(state => state.shownWeise)
  const weisWinners = useGameStateStore(state => state.weisWinners)
  const dismissShownWeise = useGameStateStore(state => state.dismissShownWeise)
  const clearDeclaredWeise = useGameStateStore(state => state.clearDeclaredWeise)
  const {t} = useTranslation()

  useEffect(() => {
    if (shownWeise || !weisWinners) return
    const timer = setTimeout(clearDeclaredWeise, BUBBLE_LINGER_MS)
    return () => clearTimeout(timer)
  }, [shownWeise, weisWinners, clearDeclaredWeise])

  return (
    <Dialog open={!!shownWeise}>
      <DialogContent disableClose={true} className="w-auto max-w-[90vw] max-h-[85svh] overflow-y-auto flex flex-col gap-6" onPointerDownOutside={e => e.preventDefault()} container={document.getElementById('root')}>
        <DialogTitle className="sr-only">{t("weisen.announcementTitle")}</DialogTitle>
        <DialogDescription className="sr-only">{t("weisen.announcementDescription")}</DialogDescription>
        <div className="flex flex-wrap justify-center gap-x-12 gap-y-6">
          {shownWeise && Object.entries(shownWeise).map(([position, weise]) => (
            <PositionWeise key={position} position={position as Position} weise={weise}/>
          ))}
        </div>
        <Button variant="outline" className="self-center" onClick={dismissShownWeise}>
          {t("weisen.close")}
        </Button>
      </DialogContent>
    </Dialog>
  );
}
