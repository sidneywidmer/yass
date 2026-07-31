import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card"
import {Button} from "@/components/ui/button"
import {AlertCircle, Bot, Loader2, Plus, User} from "lucide-react"
import {useTranslation} from "react-i18next"
import {Label} from "@/components/ui/label"
import {RadioGroup, RadioGroupItem} from "@/components/ui/radio-group"
import {Input} from "@/components/ui/input"
import {api} from "@/api/client"
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler"
import {useAsyncAction} from "@/hooks/use-async-action"
import {CreateCustomGameRequest} from "@/api/generated";
import {useState} from "react";
import {isAxiosError} from "axios";
import {useNavigate} from "react-router-dom";
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert";

type TeamComposition = 'player-player' | 'player-bot' | 'bot-bot'

const getTeamConfig = (leftTeam: TeamComposition, rightTeam: TeamComposition): Pick<CreateCustomGameRequest, 'botNorth' | 'botEast' | 'botSouth' | 'botWest'> => {
  const configMap: Record<string, Pick<CreateCustomGameRequest, 'botNorth' | 'botEast' | 'botSouth' | 'botWest'>> = {
    'player-player-player-player': {botNorth: false, botEast: false, botSouth: false, botWest: false},
    'player-player-player-bot': {botNorth: true, botEast: false, botSouth: false, botWest: false},
    'player-player-bot-bot': {botNorth: true, botEast: false, botSouth: true, botWest: false},
    'player-bot-player-player': {botNorth: false, botEast: false, botSouth: false, botWest: true},
    'player-bot-player-bot': {botNorth: true, botEast: false, botSouth: false, botWest: true},
    'player-bot-bot-bot': {botNorth: true, botEast: true, botSouth: false, botWest: true},
    'bot-bot-player-player': {botNorth: false, botEast: false, botSouth: true, botWest: true},
    'bot-bot-player-bot': {botNorth: true, botEast: false, botSouth: true, botWest: true},
    'bot-bot-bot-bot': {botNorth: true, botEast: true, botSouth: true, botWest: true},
  }
  return configMap[`${leftTeam}-${rightTeam}`] || configMap['player-player-bot-bot']
}

const TeamButton = ({team, selected, onClick}: { team: TeamComposition, selected: boolean, onClick: () => void }) => {
  return (
    <Button
      onClick={onClick}
      variant={selected ? "default" : "outline"}
      className="w-full h-auto py-4 flex flex-col items-center gap-2"
    >
      <div className="flex gap-1">
        {(team === 'player-player' || team === 'player-bot') && <User className="h-5 w-5"/>}
        {team === 'player-bot' && <Bot className="h-5 w-5"/>}
        {team === 'bot-bot' && <>
            <Bot className="h-5 w-5"/>
            <Bot className="h-5 w-5"/>
        </>}
        {team === 'player-player' && <User className="h-5 w-5"/>}
      </div>
    </Button>
  )
}

export function CreateSection() {
  const {t} = useTranslation()
  const handleAxiosError = useAxiosErrorHandler()
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()
  const [nsTeam, setNsTeam] = useState<TeamComposition>('player-bot')
  const [ewTeam, setEwTeam] = useState<TeamComposition>('bot-bot')

  const {
    execute: executeCreateGame,
    isLoading,
    hasError,
    reset
  } = useAsyncAction(async (settings: CreateCustomGameRequest) => {
    return api.createGame(settings)
  })
  const [settings, setSettings] = useState<CreateCustomGameRequest>({
    ...getTeamConfig('player-bot', 'bot-bot'),
    winningConditionType: 'POINTS',
    winningConditionValue: 2500
  })

  const handleCreate = async () => {
    setError(null)
    reset()
    try {
      const response = await executeCreateGame(settings)
      navigate(`/game/${response.data?.code}`)
    } catch (error: unknown) {
      if (isAxiosError(error) && error.response?.status === 422) {
        // Handle constraint violations
        const payload = error.response.data?.payload
        if (Array.isArray(payload)) {
          const violation = (payload as Array<{ path: string }>).find(v => v.path === 'winningConditionValue')
          if (violation) {
            return setError(t('errors.gameSettings.invalidValue'))
          }
        }

        // Handle domain-specific errors
        switch ((error.response.data?.payload as { error?: string })?.error) {
          case "GameSettingsMaxBots":
            return setError(t('errors.gameSettings.maxBots'))
          case "GameSettingsInvalidValue":
            return setError(t('errors.gameSettings.invalidValue'))
        }
      }
      if (isAxiosError(error)) handleAxiosError(error)
    }
  }

  const handleTeamSelection = (team: 'NS' | 'EW', teamComp: TeamComposition) => {
    if (team === 'NS') {
      setNsTeam(teamComp)
      setSettings(s => ({
        ...s,
        ...getTeamConfig(teamComp, ewTeam)
      }))
    } else {
      setEwTeam(teamComp)
      setSettings(s => ({
        ...s,
        ...getTeamConfig(nsTeam, teamComp)
      }))
    }
  }

  const handleConditionTypeChange = (type: 'POINTS' | 'HANDS') => {
    const defaultValue = type === 'POINTS' ? 2500 : 5
    setSettings(s => ({
      ...s,
      winningConditionType: type,
      winningConditionValue: defaultValue
    }))
  }

  const getConfigDescriptionKey = (): string => {
    const keyMap: Record<string, string> = {
      'player-player-player-player': 'playerPlayerVsPlayerPlayer',
      'player-player-player-bot': 'playerPlayerVsPlayerBot',
      'player-player-bot-bot': 'playerPlayerVsBotBot',
      'player-bot-player-player': 'playerBotVsPlayerPlayer',
      'player-bot-player-bot': 'playerBotVsPlayerBot',
      'player-bot-bot-bot': 'playerBotVsBotBot',
      'bot-bot-player-player': 'botBotVsPlayerPlayer',
      'bot-bot-player-bot': 'botBotVsPlayerBot',
      'bot-bot-bot-bot': 'botBotVsBotBot',
    }
    return keyMap[`${nsTeam}-${ewTeam}`] || 'playerBotVsBotBot'
  }

  return (
    <Card className="h-full">
      <CardHeader>
        <div className="flex items-center gap-2">
          <Plus className="h-5 w-5 text-primary"/>
          <CardTitle className="text-lg">{t("create.title")}</CardTitle>
        </div>
        <CardDescription>{t("create.description")}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
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
          <div>
            <Label>{t("create.players")}</Label>
            <p className="text-xs italic text-muted-foreground mt-1">
              {t(`create.configDescription.${getConfigDescriptionKey()}`)}
            </p>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <TeamButton
                team="player-player"
                selected={nsTeam === 'player-player'}
                onClick={() => handleTeamSelection('NS', 'player-player')}
              />
              <TeamButton
                team="player-bot"
                selected={nsTeam === 'player-bot'}
                onClick={() => handleTeamSelection('NS', 'player-bot')}
              />
              <TeamButton
                team="bot-bot"
                selected={nsTeam === 'bot-bot'}
                onClick={() => handleTeamSelection('NS', 'bot-bot')}
              />
            </div>

            <div className="flex flex-col gap-2">
              <TeamButton
                team="player-player"
                selected={ewTeam === 'player-player'}
                onClick={() => handleTeamSelection('EW', 'player-player')}
              />
              <TeamButton
                team="player-bot"
                selected={ewTeam === 'player-bot'}
                onClick={() => handleTeamSelection('EW', 'player-bot')}
              />
              <TeamButton
                team="bot-bot"
                selected={ewTeam === 'bot-bot'}
                onClick={() => handleTeamSelection('EW', 'bot-bot')}
              />
            </div>
          </div>
        </div>

        <div className="grid gap-2">
          <Label>{t("create.winningCondition")}</Label>
          <RadioGroup
            value={settings.winningConditionType}
            onValueChange={(value) => handleConditionTypeChange(value as 'POINTS' | 'HANDS')}
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
            min="1"
            max="9999"
            value={settings.winningConditionValue}
            onChange={(e) => setSettings(s => ({
              ...s,
              winningConditionValue: parseInt(e.target.value) || 0
            }))}
          />
        </div>

        <Button
          onClick={handleCreate}
          disabled={isLoading}
          variant={hasError ? "destructive" : "default"}
        >
          {isLoading && <Loader2 className="h-4 w-4 animate-spin"/>}
          {t("create.submit")}
        </Button>
      </CardContent>
    </Card>
  )
}
