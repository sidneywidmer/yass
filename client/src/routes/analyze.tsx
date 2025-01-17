import {useEffect, useState} from 'react'
import {api} from '@/api/client'
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger,} from "@/components/ui/accordion"
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card"
import {ArrowLeft, Frown, Hand, Trophy} from "lucide-react"
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {useParams} from "react-router";
import {Card as PlayingCard, CARD_HEIGHT, CARD_WIDTH} from "@/components/game/card"
import {cn} from "@/lib/utils.ts";
import {AnalyzeHand, PlayedCardWithPlayer, Player, PlayerWithCards, TrickWithCards} from "@/api/generated";
import {Button} from "@/components/ui/button.tsx";
import {useNavigate} from "react-router-dom";
import {Badge} from "@/components/ui/badge.tsx";
import {useTranslation} from "react-i18next";
import {usePlayerStore} from "@/store/player.ts";

export default function Analyze() {
  const [analysis, setAnalysis] = useState<any>(null)
  const handleAxiosError = useAxiosErrorHandler()
  const {code} = useParams()
  const navigate = useNavigate()
  const {t} = useTranslation()
  const {uuid} = usePlayerStore()

  useEffect(() => {
    api.analyzeGame(code!!)
      .then(response => setAnalysis(response.data))
      .catch(error => {
        navigate("/")
        handleAxiosError(error)
      })
  }, [code])

  const getTeamColor = (position: string) => {
    return position === 'NORTH' || position === 'SOUTH' ? 'text-indigo-500' : 'text-orange-500'
  }

  const getTeamBorder = (position: string) => {
    return position === 'NORTH' || position === 'SOUTH' ? 'border-indigo-500/60' : 'border-orange-500/60'
  }

  const getPlayerPosition = (player: Player, hand: AnalyzeHand) => {
    return hand.players.find(p => p.uuid === player.uuid)?.position
  }

  const getRunningTotals = (tricks: TrickWithCards[], currentIndex: number, hand: AnalyzeHand) => {
    return tricks.slice(0, currentIndex + 1).reduce((totals, trick) => {
      if (!trick.winnerPlayer) return totals;

      const position = getPlayerPosition(trick.winnerPlayer, hand);

      if (position === 'NORTH' || position === 'SOUTH') {
        return {...totals, northSouth: totals.northSouth + trick.points};
      } else {
        return {...totals, eastWest: totals.eastWest + trick.points};
      }
    }, {northSouth: 0, eastWest: 0});
  };

  const isPlayerInWinningTeam = (position: 'NORTH' | 'SOUTH' | 'EAST' | 'WEST', winners: { team: 'NS' | 'EW' }) => {
    const teamPositions = {
      'NS': ['NORTH', 'SOUTH'],
      'EW': ['EAST', 'WEST']
    }
    return teamPositions[winners.team].includes(position)
  }

  if (!analysis) return <div>Loading...</div>

  const playerInGame = analysis.hands[0].players.find((player: PlayerWithCards) => player.uuid === uuid) ?? null
  const winner = isPlayerInWinningTeam(playerInGame?.position, analysis.winners)

  return (
    <div className="flex min-h-svh bg-muted">
      <div className="container mx-auto p-4 max-w-4xl">
        <Button variant="ghost" className="mb-4 -ml-2 w-fit h-8 gap-2" onClick={() => navigate("/")}>
          <ArrowLeft className="w-4 h-4"/>
          {t('analysis.back')}
        </Button>
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-2xl">{t('analysis.title')}</CardTitle>
                <CardDescription>{t('analysis.code')} {code}</CardDescription>
              </div>
            </div>
            <div className="mb-6 text-center space-y-2">
              <div className="text-4xl font-bold">
                {analysis.winners.points} - {analysis.losers.points}
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
          <CardContent>
            <Accordion type="single" collapsible className="w-full">
              {analysis.hands.map((hand: any, index: number) => (
                <AccordionItem key={index} value={`item-${index}`}>
                  <AccordionTrigger className="hover:no-underline">
                    <div className="flex items-center justify-between w-full">
                      <div className="flex items-center gap-4">
                        <Hand className="w-4 h-4"/>
                        <span>{t('analysis.hand')} {index + 1} - {t('analysis.trump')}: {t(`trumps.${hand.trump}`)}</span>
                        <span className="text-muted-foreground text-sm">
                      {t('analysis.startingPlayer')}: {hand.startingPlayer.name}
                    </span>
                      </div>
                      <div className="flex items-center gap-6 text-sm">
                        <div className={getTeamColor("NORTH")}>{t('analysis.teams.northSouth')}: <span
                          className="font-medium">{hand.points.NORTH.cardPoints + hand.points.NORTH.weisPoints + hand.points.SOUTH.cardPoints + hand.points.SOUTH.weisPoints}</span>
                        </div>
                        <div className={getTeamColor("EAST")}>{t('analysis.teams.eastWest')}: <span
                          className="font-medium">{hand.points.EAST.cardPoints + hand.points.EAST.weisPoints + hand.points.WEST.cardPoints + hand.points.WEST.weisPoints}</span>
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
                              <Badge variant="outline" className={getTeamColor(getPlayerPosition(player, hand)!!)}>
                                +{hand.points[player.position].weisPoints}
                              </Badge>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    <h4 className="text-sm font-medium mb-4">{t("analysis.tricks")}</h4>
                    {hand.tricks.reverse().map((trick: TrickWithCards, trickIndex: number) => (
                      <div key={trickIndex} className="mb-4 last:mb-0">
                        <div className="grid grid-cols-6 gap-1 p-4">
                          <div className="font-medium mb-2 text-center content-center">
                            <Badge variant="outline">#{trickIndex + 1}</Badge>
                          </div>
                          {trick.cards.map((play: PlayedCardWithPlayer) => (
                            <div key={play.player.uuid} className="space-y-2">
                              <div className="text-sm text-muted-foreground text-left">
                                {play.player.name}
                              </div>
                              <div className="relative" style={{
                                width: CARD_WIDTH * 0.8,
                                height: CARD_HEIGHT * 0.8,
                              }}>
                                {play.card ? (
                                  <PlayingCard card={play.card}
                                               className={getTeamBorder(getPlayerPosition(play.player, hand)!!)}/>
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
                                getTeamColor(getPlayerPosition(trick.winnerPlayer, hand)!!)
                              )}>
                               +{trick.points}
                              </span>
                              <span className="border-l pl-4">
                                {getRunningTotals(hand.tricks, trickIndex, hand).northSouth} - {getRunningTotals(hand.tricks, trickIndex, hand).eastWest}
                              </span>
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </AccordionContent>
                </AccordionItem>
              ))}
            </Accordion>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
