import {AccordionContent, AccordionItem, AccordionTrigger} from "@/components/ui/accordion"
import {Hand} from "lucide-react"
import {useTranslation} from "react-i18next"
import {useSettingsStore} from "@/store/settings.ts"
import {AnalyzeHand as AnalyzeHandType, PlayerWithCards, Position, TrickWithCards} from "@/api/generated"
import {Badge} from "@/components/ui/badge.tsx"
import {AnalyzeTrick} from "@/components/analyze/analyze-trick"
import {TEAM_COLOR_MAP} from "@/components/analyze/analyze-instance.tsx";

interface AnalyzeHandProps {
  hand: AnalyzeHandType
  index: number
  playerPositionMap: Map<string, Position>
}

export function AnalyzeHand({hand, index, playerPositionMap}: AnalyzeHandProps) {
  const {t} = useTranslation()
  const cardDeck = useSettingsStore(settings => settings.cardDeck)

  return (
    <AccordionItem key={index} value={`item-${index}`}>
      <AccordionTrigger className="hover:no-underline">
        <div className="flex items-center justify-between w-full">
          <div className="flex items-center gap-4">
            <Hand className="hidden sm:block w-4 h-4"/>
            <span>{index + 1} - {t(`trumps.${cardDeck}.${hand.trump}`)}</span>
            <span className="text-muted-foreground text-sm truncate">
              {t('analysis.startingPlayer')}: {hand.startingPlayer.name}
            </span>
          </div>
          <div className="flex items-center gap-6 text-sm">
            <div className={TEAM_COLOR_MAP["NORTH"]}>{t('analysis.teams.northSouth')}: <span
              className="font-medium">{hand.points.NORTH.cardPoints! + hand.points.NORTH.weisPoints! + hand.points.SOUTH.cardPoints! + hand.points.SOUTH.weisPoints!}</span>
            </div>
            <div className={TEAM_COLOR_MAP["EAST"]}>{t('analysis.teams.eastWest')}: <span
              className="font-medium">{hand.points.EAST.cardPoints! + hand.points.EAST.weisPoints! + hand.points.WEST.cardPoints! + hand.points.WEST.weisPoints!}</span>
            </div>
          </div>
        </div>
      </AccordionTrigger>
      <AccordionContent>
        <div className="mb-6">
          <h4 className="text-sm font-medium mb-4">Weise</h4>
          <div className="grid grid-cols-2 gap-4">
            {hand.players.map((player: PlayerWithCards, index: number) => (
              <div key={index} className="space-y-2">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-muted-foreground">{player.name}</span>
                  <Badge variant="outline" className={TEAM_COLOR_MAP[playerPositionMap.get(player.uuid)!]}>
                    +{hand.points[player.position].weisPoints}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </div>

        <h4 className="text-sm font-medium mb-4">{t("analysis.tricks")}</h4>
        {hand.tricks.toReversed().map((trick: TrickWithCards, trickIndex: number) => (
          <AnalyzeTrick
            key={trickIndex}
            trick={trick}
            trickIndex={trickIndex}
            handTricks={hand.tricks.toReversed()}
            playerPositionMap={playerPositionMap}
          />
        ))}
      </AccordionContent>
    </AccordionItem>
  )
}