import {CardDescription, CardHeader, CardTitle} from "@/components/ui/card"
import {Frown, Trophy} from "lucide-react"
import {cn} from "@/lib/utils.ts"
import {useTranslation} from "react-i18next"

interface AnalyzeHeaderProps {
  code: string
  winnerPoints: number | undefined
  loserPoints: number | undefined
  playerInGame: any
  winner: boolean
}

export function AnalyzeHeader({code, winnerPoints, loserPoints, playerInGame, winner}: AnalyzeHeaderProps) {
  const {t} = useTranslation()

  return (
    <>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-2xl">{t('analysis.title')}</CardTitle>
            <CardDescription>{t('analysis.code')} {code}</CardDescription>
          </div>
        </div>
        <div className="mb-6 text-center space-y-2">
          <div className="text-4xl font-bold">
            {winnerPoints} - {loserPoints}
          </div>
          {playerInGame &&
              <div className={cn(
                "inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm",
                winner ? "bg-green-500/10 text-green-500" : "bg-red-500/10 text-red-500"
              )}>
                {winner ? (
                  <>
                    <Trophy className="w-4 h-4 text-green-500"/>
                    {t('analysis.winner')}
                  </>
                ) : (
                  <>
                    <Frown className="w-4 h-4 text-red-500"/>
                    {t('analysis.loser')}
                  </>
                )}
              </div>
          }
        </div>
      </CardHeader>
    </>
  )
}