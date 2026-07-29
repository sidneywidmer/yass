import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card"
import {Button} from "@/components/ui/button"
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert"
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from "@/components/ui/accordion"
import {AlertCircle, CalendarDays, Loader2} from "lucide-react"
import {useTranslation} from "react-i18next"
import {useState} from "react"
import {isAxiosError} from "axios"
import {useNavigate} from "react-router-dom"
import {api} from "@/api/client"
import {useAsyncAction} from "@/hooks/use-async-action"
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler"
import {useMidnightCountdown} from "@/hooks/use-midnight-countdown"
import {usePlayerStore} from "@/store/player.ts"

const rules = ["sameCards", "oncePerDay", "opponents"]

export function DailySection() {
  const {t} = useTranslation()
  const navigate = useNavigate()
  const handleAxiosError = useAxiosErrorHandler()
  const isAnon = usePlayerStore(state => state.isAnon)
  const remaining = useMidnightCountdown()
  const [error, setError] = useState<string | null>(null)

  const {execute: executeDaily, isLoading, hasError, reset} = useAsyncAction(async () => {
    return api.dailyChallenge()
  })

  const handlePlay = async () => {
    setError(null)
    reset()
    try {
      const response = await executeDaily()
      navigate(`/game/${response.data?.code}`)
    } catch (error: unknown) {
      if (!isAxiosError(error)) return
      const domainError = (error.response?.data as { payload?: { domainError?: string } })?.payload?.domainError
      if (domainError === "SignedUpPlayersOnly") {
        return setError(t("lobby.daily.signedUpOnly"))
      }
      handleAxiosError(error)
    }
  }

  return (
    <Card className="h-full">
      <CardHeader>
        <div className="flex items-center gap-2">
          <CalendarDays className="h-5 w-5 text-primary"/>
          <CardTitle className="text-lg">{t("lobby.daily.title")}</CardTitle>
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {error && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4"/>
            <AlertTitle>{t("errors.title")}</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <div className="flex flex-col items-center gap-1 rounded-lg border bg-muted/40 py-4">
          <span className="font-mono text-3xl font-semibold tabular-nums">{remaining}</span>
          <span className="text-xs text-muted-foreground">{t("lobby.daily.countdown")}</span>
        </div>

        {isAnon ? (
          <div className="flex flex-col gap-2 rounded-lg border border-dashed p-4 text-center">
            <p className="text-sm text-muted-foreground">{t("lobby.daily.signedUpOnly")}</p>
            <Button variant="outline" onClick={() => navigate("/signup")}>
              {t("auth.upgrade.title")}
            </Button>
          </div>
        ) : (
          <Button
            className="w-full"
            onClick={handlePlay}
            disabled={isLoading}
            variant={hasError ? "destructive" : "default"}
          >
            {isLoading && <Loader2 className="h-4 w-4 animate-spin"/>}
            {t("lobby.daily.play")}
          </Button>
        )}

        <Accordion type="single" collapsible>
          <AccordionItem value="explanation" className="border-b-0">
            <AccordionTrigger className="py-2 text-muted-foreground">
              {t("lobby.daily.explanation")}
            </AccordionTrigger>
            <AccordionContent className="pb-0">
              <ul className="list-disc space-y-1 pl-4 text-sm text-muted-foreground">
                {rules.map(key => <li key={key}>{t(`lobby.daily.rules.${key}`)}</li>)}
              </ul>
            </AccordionContent>
          </AccordionItem>
        </Accordion>
      </CardContent>
    </Card>
  )
}
