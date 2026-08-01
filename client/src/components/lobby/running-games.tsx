import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card"
import {Button} from "@/components/ui/button"
import {History, Play} from "lucide-react"
import {useTranslation} from "react-i18next"
import {useNavigate} from "react-router-dom"
import {useGameInfoStore} from "@/store/game-info.ts"

const units: [Intl.RelativeTimeFormatUnit, number][] = [["day", 86400], ["hour", 3600], ["minute", 60]]

const startedAgo = (startedAt: string, language: string) => {
  const seconds = (Date.now() - new Date(startedAt).getTime()) / 1000
  const [unit, size] = units.find(([, size]) => seconds >= size) ?? units[units.length - 1]
  return new Intl.RelativeTimeFormat(language, {numeric: "always"})
    .format(-Math.max(1, Math.floor(seconds / size)), unit)
}

export function RunningGames() {
  const {t, i18n} = useTranslation()
  const navigate = useNavigate()
  const runningGames = useGameInfoStore(state => state.runningGames)

  if (runningGames.length === 0) return null

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <History className="h-5 w-5 text-primary"/>
          <CardTitle className="text-lg">{t("lobby.running.title")}</CardTitle>
        </div>
        <CardDescription>{t("lobby.running.description")}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-2">
        {runningGames.map(game => (
          <Button
            key={game.uuid}
            variant="outline"
            className="h-auto w-full justify-between py-3"
            onClick={() => navigate(`/game/${game.code}`)}
          >
            <span className="flex flex-col items-start gap-0.5">
              <span className="font-medium">{t(`lobby.running.kind.${game.kind}`)}</span>
              <span className="text-xs font-normal text-muted-foreground">
                {startedAgo(game.startedAt, i18n.language)}
              </span>
            </span>
            <span className="flex items-center gap-3">
              <span className="font-mono tracking-[0.2em]">{game.code}</span>
              <Play className="h-4 w-4 text-primary"/>
            </span>
          </Button>
        ))}
      </CardContent>
    </Card>
  )
}
