import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card"
import {Trophy} from "lucide-react"
import {useTranslation} from "react-i18next"
import {useEffect, useState} from "react"
import {cn} from "@/lib/utils"
import {api} from "@/api/client"
import type {DailyLeaderboardEntry} from "@/api/generated"

const skeletonRows = [0, 1, 2, 3, 4]

export function DailyLeaderboard() {
  const {t} = useTranslation()
  const [entries, setEntries] = useState<DailyLeaderboardEntry[]>()
  const [hasError, setHasError] = useState(false)

  // The board is fetched once when the lobby mounts. Coming back from a finished game remounts it,
  // so the own run shows up without any polling.
  useEffect(() => {
    let active = true

    api.dailyLeaderboard()
      .then(response => active && setEntries(response.data?.entries ?? []))
      .catch(() => active && setHasError(true))

    return () => {
      active = false
    }
  }, [])

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Trophy className="h-5 w-5 text-primary"/>
          <CardTitle className="text-lg">{t("lobby.daily.leaderboard.title")}</CardTitle>
        </div>
      </CardHeader>
      <CardContent>
        {hasError && <p className="text-sm text-muted-foreground">{t("lobby.daily.leaderboard.error")}</p>}

        {!hasError && !entries && (
          <div className="flex flex-col gap-2">
            {skeletonRows.map(row => <div key={row} className="h-8 animate-pulse rounded-md bg-muted"/>)}
          </div>
        )}

        {entries?.length === 0 && (
          <p className="text-sm text-muted-foreground">{t("lobby.daily.leaderboard.empty")}</p>
        )}

        {entries && entries.length > 0 && (
          <ol className="flex flex-col">
            {entries.map((entry, index) => (
              <li
                key={`${index}-${entry.player}`}
                className="flex items-center gap-3 rounded-md px-2 py-1.5 text-sm odd:bg-muted/40"
              >
                <span className={cn(
                  "w-5 shrink-0 text-right tabular-nums text-muted-foreground",
                  index < 3 && "font-semibold text-primary"
                )}>
                  {index + 1}
                </span>
                <span className="flex-1 truncate">{entry.player}</span>
                <span className="shrink-0 font-medium tabular-nums">{entry.points}</span>
              </li>
            ))}
          </ol>
        )}
      </CardContent>
    </Card>
  )
}
