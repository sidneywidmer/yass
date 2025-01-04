import {useEffect, useState} from 'react'
import {Dialog, DialogContent, DialogTitle} from '@/components/ui/dialog'
import {useGameStateStore} from '@/store/game-state'
import {Card} from '@/components/game/card'
import {useTranslation} from "react-i18next";

export function ShowWeise() {
  const [open, setOpen] = useState(false)
  const {otherWeise, clearWeise} = useGameStateStore()
  const {t} = useTranslation()

  useEffect(() => {
    if (Object.keys(otherWeise).length > 0) {
      setOpen(true)
    }
  }, [otherWeise])

  const handleClose = () => {
    setOpen(false)
    clearWeise()
  }

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-md">
        <DialogTitle className="text-center">{t("weisen.title")}</DialogTitle>
        <div className="space-y-6 max-h-[60vh] overflow-y-auto p-2">
          {Object.entries(otherWeise).map(([position, weise]) =>
            weise.map((weis, idx) => (
              <div key={`${position}-${idx}`} className="p-4 rounded-lg">
                <div className="flex items-center gap-4 mb-2">
                  <span className="font-medium">{position}</span>
                  <div className="h-4 w-px bg-border"/>
                  <span className="font-medium">{weis.type}</span>
                  <div className="h-4 w-px bg-border"/>
                  <span className="text-sm text-muted-foreground">{weis.points} {t("weisen.points")}</span>
                </div>
                <div className="grid grid-cols-3 sm:grid-cols-4 gap-2">
                  {weis.cards!!.map((card, i) => (
                    <Card key={i} card={card}/>
                  ))}
                </div>
              </div>
            ))
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
