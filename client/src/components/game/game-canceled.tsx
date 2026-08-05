import {useGameStateStore} from '@/store/game-state'
import {Dialog, DialogContent, DialogTitle} from '@/components/ui/dialog'
import {DialogDescription} from "@radix-ui/react-dialog";
import {Button} from "@/components/ui/button.tsx";
import {useTranslation} from "react-i18next";
import {useNavigate} from "react-router-dom";

/**
 * Whoever created the game ended it, so there is nothing left to come back to for anyone
 * still sitting at the table.
 */
export function GameCanceled() {
  const canceledBy = useGameStateStore(state => state.canceledBy)
  const code = useGameStateStore(state => state.code)
  const {t} = useTranslation()
  const navigate = useNavigate()

  if (!canceledBy) return null

  return (
    <Dialog open modal>
      <DialogContent className="sm:max-w-md focus:outline-none focus-visible:outline-none" disableClose={true}
                     tabIndex={-1}>
        <DialogTitle className="text-center">{t("canceled.title")}</DialogTitle>
        <DialogDescription></DialogDescription>
        <div className="flex flex-col items-center gap-6">
          <p className="text-center text-sm text-muted-foreground">
            {t("canceled.description", {name: canceledBy.name})}
          </p>
          <div className="grid w-full grid-cols-2 gap-4">
            <Button type="submit"
                    onClick={() => navigate(`/game/${code}/analyze`)}
            >
              {t("canceled.analyze")}</Button>
            <Button variant={"outline"} type="submit"
                    onClick={() => navigate('/lobby')}
            >{t("canceled.back")}</Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
