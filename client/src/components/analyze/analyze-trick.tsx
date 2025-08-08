import {Trophy} from "lucide-react"
import {Badge} from "@/components/ui/badge.tsx"
import {Card as PlayingCard} from "@/components/game/card"
import {useCardDimensions} from "@/hooks/use-card-dimensions.ts"
import {cn} from "@/lib/utils.ts"
import {PlayedCardWithPlayer, Position, TrickWithCards} from "@/api/generated"
import {TEAM_BORDER_MAP, TEAM_COLOR_MAP} from "@/components/analyze/analyze-instance.tsx";

interface AnalyzeTrickProps {
  trick: TrickWithCards
  trickIndex: number
  handTricks: Array<TrickWithCards>
  playerPositionMap: Map<string, Position>
}

export function AnalyzeTrick({trick, trickIndex, handTricks, playerPositionMap}: AnalyzeTrickProps) {
  const {CARD_WIDTH, CARD_HEIGHT} = useCardDimensions()

  const calculateAllRunningTotals = (tricks: TrickWithCards[]) => {
    const runningTotalsMap = new Map();
    let northSouthTotal = 0;
    let eastWestTotal = 0;

    tricks.forEach((trick, index) => {
      if (trick.winnerPlayer) {
        const position = playerPositionMap.get(trick.winnerPlayer.uuid!);
        if (position === 'NORTH' || position === 'SOUTH') {
          northSouthTotal += trick.points;
        } else {
          eastWestTotal += trick.points;
        }
      }

      runningTotalsMap.set(index, {
        northSouth: northSouthTotal,
        eastWest: eastWestTotal
      });
    });

    return runningTotalsMap;
  };

  const runningTotalsMap = calculateAllRunningTotals(handTricks);

  return (
    <div className="mb-4 last:mb-0">
      <div className="grid grid-cols-1 sm:grid-cols-6 gap-1 sm:gap-6 p-4 place-items-center sm:place-items-start">
        <div className="font-medium mb-2 text-center content-center">
          <Badge variant="outline">#{trickIndex + 1}</Badge>
        </div>
        {trick.cards.map((play: PlayedCardWithPlayer) => (
          <div key={play.player.uuid} className="space-y-2">
            <div className="text-sm text-muted-foreground text-left truncate">
              {play.player.name}
            </div>
            <div className="relative" style={{
              width: CARD_WIDTH * 0.8,
              height: CARD_HEIGHT * 0.8,
            }}>
              {play.card ? (
                <PlayingCard card={play.card}
                             className={TEAM_BORDER_MAP[playerPositionMap.get(play.player.uuid!)!]}/>
              ) : (
                <div className="w-full h-full rounded-lg border-2 border-dashed border-muted"/>
              )}
              {play.player.uuid === trick.winnerPlayer?.uuid && (
                <div
                  className="absolute -top-2 -right-2 bg-green-500 text-primary-foreground rounded-full p-1">
                  <Trophy className="w-4 h-4"/>
                </div>
              )}
            </div>
          </div>
        ))}
        {trick.winnerPlayer && (
          <div className={cn("text-sm text-muted-foreground flex items-center gap-2")}>
            <span className={cn(
              "font-medium",
              TEAM_COLOR_MAP[playerPositionMap.get(trick.winnerPlayer.uuid!)!]
            )}>
             +{trick.points}
            </span>
            <span className="border-l pl-4">
              {runningTotalsMap.get(trickIndex).northSouth} - {runningTotalsMap.get(trickIndex).eastWest}
            </span>
          </div>
        )}
      </div>
    </div>
  )
}