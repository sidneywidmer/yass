import {Card} from "@/components/ui/card";
import {Button} from "@/components/ui/button";
import {Position, WeisWithPoints} from "@/api/generated";
import {useGameStateStore} from "@/store/game-state";
import {WeisDisplay} from "@/components/game/weis-display";
import {useEffect} from "react";
import {useTranslation} from "react-i18next";
import {AnimatePresence, motion} from "motion/react";

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
    <AnimatePresence>
      {shownWeise && (
        <div className="fixed inset-0 flex items-center justify-center z-40">
          <motion.div
            initial={{opacity: 0, scale: 0.9}}
            animate={{opacity: 1, scale: 1}}
            exit={{opacity: 0, scale: 0.9}}
            transition={{duration: 0.3, ease: [0.4, 0, 0.2, 1]}}
          >
            <Card className="bg-white p-6 rounded-lg flex flex-col gap-6 max-w-[90vw] max-h-[85svh] overflow-y-auto">
              <div className="flex flex-wrap justify-center gap-x-12 gap-y-6">
                {Object.entries(shownWeise).map(([position, weise]) => (
                  <PositionWeise key={position} position={position as Position} weise={weise}/>
                ))}
              </div>
              <Button variant="outline" className="self-center" onClick={dismissShownWeise}>
                {t("weisen.close")}
              </Button>
            </Card>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
