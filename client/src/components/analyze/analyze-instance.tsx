import {Card, CardContent} from "@/components/ui/card"
import {Accordion} from "@/components/ui/accordion"
import {AnalyzeGameStateResponse, AnalyzeHand as AnalyzeHandType, PlayerWithCards, Position} from "@/api/generated"
import {usePlayerStore} from "@/store/player.ts"
import {AnalyzeHeader} from "@/components/analyze/analyze-header"
import {AnalyzeHand} from "@/components/analyze/analyze-hand"
import {Button} from "@/components/ui/button.tsx";
import {ArrowLeft} from "lucide-react";
import {useNavigate} from "react-router-dom";
import {useTranslation} from "react-i18next";

interface AnalyzeInstanceProps {
  code: string
  analysis: AnalyzeGameStateResponse
}

export const TEAM_COLOR_MAP = {
  'NORTH': 'text-indigo-500',
  'SOUTH': 'text-indigo-500',
  'EAST': 'text-orange-500',
  'WEST': 'text-orange-500'
};
export const TEAM_BORDER_MAP = {
  'NORTH': 'border-indigo-500/60',
  'SOUTH': 'border-indigo-500/60',
  'EAST': 'border-orange-500/60',
  'WEST': 'border-orange-500/60'
};

export function AnalyzeInstance({code, analysis}: AnalyzeInstanceProps) {
  const currentPlayerUuid = usePlayerStore(state => state.uuid)
  const navigate = useNavigate()
  const {t} = useTranslation()

  const playerPositionMap = new Map<string, Position>(
    analysis.hands[0].players.map(player => [player.uuid, player.position])
  )

  const isPlayerInWinningTeam = (position: Position | undefined, winners: "NS" | "EW" | undefined) => {
    if (!position || !winners) return false

    const teamPositions = {
      'NS': ['NORTH', 'SOUTH'],
      'EW': ['EAST', 'WEST']
    }
    return teamPositions[winners].includes(position)
  }

  const playerInGame: PlayerWithCards | null = analysis.hands[0].players.find((player: PlayerWithCards) => player.uuid === currentPlayerUuid) ?? null
  const winner = isPlayerInWinningTeam(playerInGame?.position, analysis.winners.team)

  return (
    <div className="flex min-h-svh bg-muted">
      <div className="container mx-auto p-4 max-w-4xl">
        <Button variant="ghost" className="mb-4 -ml-2 w-fit h-8 gap-2" onClick={() => navigate("/")}>
          <ArrowLeft className="w-4 h-4"/>
          {t('analysis.back')}
        </Button>
        <Card>
          <AnalyzeHeader
            code={code}
            winnerPoints={analysis.winners.points}
            loserPoints={analysis.losers.points}
            playerInGame={playerInGame}
            winner={winner}
          />
          <CardContent>
            <Accordion type="single" collapsible className="w-full">
              {analysis.hands.map((hand: AnalyzeHandType, index: number) => (
                <AnalyzeHand
                  key={index}
                  hand={hand}
                  index={index}
                  playerPositionMap={playerPositionMap}
                />
              ))}
            </Accordion>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}