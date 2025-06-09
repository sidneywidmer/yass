import {Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger} from "@/components/ui/dialog"
import {Button} from "@/components/ui/button"
import {Plus, AlertCircle, Loader2} from "lucide-react"
import {useTranslation} from "react-i18next"
import {Label} from "@/components/ui/label"
import {Switch} from "@/components/ui/switch"
import {RadioGroup, RadioGroupItem} from "@/components/ui/radio-group"
import {Input} from "@/components/ui/input"
import {api} from "@/api/client"
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler"
import {useAsyncAction} from "@/hooks/use-async-action"
import {CreateCustomGameRequest} from "@/api/generated";
import {DialogDescription} from "@radix-ui/react-dialog";
import {useState} from "react";
import {useNavigate} from "react-router-dom";
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert";

type Position = 'North' | 'East' | 'South' | 'West'
type BotKey = `bot${Position}`

export function CreateGameOverlay() {
  const {t} = useTranslation()
  const handleAxiosError = useAxiosErrorHandler()
  const [open, setOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()
  
  const {execute: executeCreateGame, isLoading, hasError, reset} = useAsyncAction(async (settings: CreateCustomGameRequest) => {
    return api.createGame(settings)
  })
  const [settings, setSettings] = useState<CreateCustomGameRequest>({
    botNorth: true,
    botEast: true,
    botSouth: false,
    botWest: true,
    winningConditionType: 'POINTS',
    winningConditionValue: 2500
  })

  const handleCreate = async () => {
    setError(null)
    reset()
    try {
      const response = await executeCreateGame(settings)
      setOpen(false)
      navigate(`/game/${response.data?.code}`)
    } catch (error: any) {
      if (error.response?.status === 422) {
        switch (error.response?.data?.payload?.error) {
          case "GameSettingsMaxBots":
            return setError(t('errors.gameSettings.maxBots'))
          case "GameSettingsInvalidValue":
            return setError(t('errors.gameSettings.invalidValue'))
        }
      }
      handleAxiosError(error)
    }
  }

  const positions: Position[] = ['North', 'East', 'South', 'West']

  const handleBotToggle = (position: Position) => (checked: boolean) => {
    const key = `bot${position}` as BotKey
    setSettings(s => ({...s, [key]: checked}))
  }

  const validateAndClampValue = (value?: number, conditionType?: 'POINTS' | 'HANDS') => {
    const type = conditionType || settings.winningConditionType
    const min = type === 'POINTS' ? 100 : 1
    const max = type === 'POINTS' ? 9000 : 99
    const clampedValue = Math.max(min, Math.min(max, value || min))
    setSettings(s => ({
      ...s,
      winningConditionValue: clampedValue
    }))
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button className="w-full" variant="outline">
          <Plus className="mr-2 h-4 w-4"/>
          {t("main.game.create")}
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>{t("create.title")}</DialogTitle>
          <DialogDescription>{t("create.description")}</DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4"/>
              <AlertTitle>{t("errors.title")}</AlertTitle>
              <AlertDescription>
                {error}
              </AlertDescription>
            </Alert>
          )}
          <div className="grid gap-2">
            <Label>{t("create.bots")}</Label>
            <div className="grid grid-cols-2 gap-4">
              {positions.map((pos) => (
                <div key={pos} className="flex items-center justify-between">
                  <Label htmlFor={`bot${pos}`}>{t(`create.bot${pos}`)}</Label>
                  <Switch
                    id={`bot${pos}`}
                    checked={settings[`bot${pos}` as BotKey]}
                    onCheckedChange={handleBotToggle(pos)}
                  />
                </div>
              ))}
            </div>
          </div>

          <div className="grid gap-2">
            <Label>{t("create.winningCondition")}</Label>
            <RadioGroup
              value={settings.winningConditionType}
              onValueChange={(value) => {
                const newType = value as 'POINTS' | 'HANDS'
                setSettings(s => ({...s, winningConditionType: newType}))
                validateAndClampValue(settings.winningConditionValue, newType)
              }}
              className="flex gap-4"
            >
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="POINTS" id="points"/>
                <Label htmlFor="points">{t("create.points")}</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="HANDS" id="hands"/>
                <Label htmlFor="hands">{t("create.hands")}</Label>
              </div>
            </RadioGroup>
          </div>

          <div className="grid gap-2">
            <Label>{t("create.value")}</Label>
            <Input
              type="number"
              value={settings.winningConditionValue}
              onChange={(e) => setSettings(s => ({
                ...s,
                winningConditionValue: parseInt(e.target.value) || 0
              }))}
              onBlur={(e) => validateAndClampValue(parseInt(e.target.value))}
            />
          </div>
        </div>
        <Button 
          onClick={handleCreate} 
          disabled={isLoading}
          variant={hasError ? "destructive" : "default"}
        >
          {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          {t("create.submit")}
        </Button>
      </DialogContent>
    </Dialog>
  )
}