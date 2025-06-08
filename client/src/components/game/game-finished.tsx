import {useEffect, useState} from 'react'
import {useGameStateStore} from '@/store/game-state'
import {usePlayerStore} from '@/store/player'
import {Dialog, DialogContent, DialogTitle} from '@/components/ui/dialog'
import {DialogDescription} from "@radix-ui/react-dialog";
import {Button} from "@/components/ui/button.tsx";
import {useTranslation} from "react-i18next";
import {useNavigate} from "react-router-dom";

export function GameFinished() {
  const state = useGameStateStore(state => state.state)
  const finished = useGameStateStore(state => state.finished)
  const code = useGameStateStore(state => state.code)

  const uuid = usePlayerStore(state => state.uuid)
  const [open, setOpen] = useState(false)
  const {t} = useTranslation()
  const navigate = useNavigate()

  useEffect(() => {
    if (state === 'FINISHED' && (finished)) {
      setOpen(true)
    }
  }, [state, finished])

  if (!finished) return null
  const isWinner = finished.winners?.some(p => p.uuid === uuid)

  return (
    <Dialog open={open} modal>
      <DialogContent className="sm:max-w-md focus:outline-none focus-visible:outline-none" disableClose={true}
                     tabIndex={-1} confetti={isWinner}>
        <DialogTitle className={"text-center"}>
          {isWinner ? (
            <>
              <span className="font-bold text-green-500">{t("finished.titleWinner")}</span>
            </>
          ) : (
            <>
              <span className="font-bold text-red-500">{t("finished.titleLooser")}</span>
            </>
          )}
        </DialogTitle>
        <DialogDescription></DialogDescription>
        <div className="flex flex-col items-center gap-6">
          <div className="text-center">
            <p
              className="text-lg">{isWinner ? finished.winnerPoints : finished.loserPoints} {t("finished.points")}</p>
            <p className="text-sm text-muted-foreground">
              vs {isWinner ? finished!!.loserPoints : finished.winnerPoints} {t("finished.points")}
            </p>
          </div>
          <div className="grid w-full grid-cols-2 gap-4 mx-auto">
            <div className="text-right">
              <h3 className="mb-2 text-sm font-medium">{t("finished.winners")}</h3>
              {finished.winners!!.map(w => (
                <p key={w.uuid} className="text-sm text-muted-foreground">{w.name}</p>
              ))}
            </div>
            <div>
              <h3 className="mb-2 text-sm font-medium">{t("finished.losers")}</h3>
              {finished.losers!!.map(l => (
                <p key={l.uuid} className="text-sm text-muted-foreground">{l.name}</p>
              ))}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4 mt-6">
            <Button type="submit"
                    onClick={() => navigate(`/game/${code}/analyze`)}
            >
              {t("finished.analyze")}</Button>
            <Button variant={"outline"} type="submit"
                    onClick={() => navigate('/')}
            >{t("finished.back")}</Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}