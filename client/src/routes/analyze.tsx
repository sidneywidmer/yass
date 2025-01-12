import {useEffect, useState} from 'react'
import {api} from '@/api/client'
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger,} from "@/components/ui/accordion"
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card"
import {Hand, Trophy} from "lucide-react"
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {useParams} from "react-router";
import {Card as PlayingCard, CARD_HEIGHT, CARD_WIDTH} from "@/components/game/card"
import {cn} from "@/lib/utils.ts";
import {AnalyzeHand, Player} from "@/api/generated";

export default function Analyze() {
  const [analysis, setAnalysis] = useState<any>(null)
  const handleAxiosError = useAxiosErrorHandler()
  const {code} = useParams()

  useEffect(() => {
    console.log(code)
    api.analyzeGame(code!!)
      .then(response => setAnalysis(response.data))
      .catch(handleAxiosError)
  }, [code])


  const getTeamColor = (position: string) => {
    return position === 'NORTH' || position === 'SOUTH' ? 'text-blue-500' : 'text-orange-500'
  }

  const getTeamBorder = (position: string) => {
    return position === 'NORTH' || position === 'SOUTH' ? 'border-blue-500/60' : 'border-orange-500/60'
  }

  const getPlayerPosition = (player: Player, hand: AnalyzeHand) => {
    return hand.players.find(p => p.uuid === player.uuid)?.position
  }

  const getTrickWinnerPoints = (trick: any, hand: any) => {
    if (!trick.winnerPlayer) return 0;

    const position = getPlayerPosition(trick.winnerPlayer, hand);

    const points = trick.points;
    if (position === 'NORTH' || position === 'SOUTH') {
      return points.NORTH.cardPoints + points.SOUTH.cardPoints
    } else {
      return points.EAST.cardPoints + points.WEST.cardPoints
    }
  };

  const getRunningTotals = (tricks: any[], currentIndex: number, hand) => {
    return tricks.slice(0, currentIndex + 1).reduce((totals, trick) => {
      if (!trick.winnerPlayer) return totals;

      const position = getPlayerPosition(trick.winnerPlayer, hand);
      const trickPoints = getTrickWinnerPoints(trick, hand);

      if (position === 'NORTH' || position === 'SOUTH') {
        return {...totals, northSouth: totals.northSouth + trickPoints};
      } else {
        return {...totals, eastWest: totals.eastWest + trickPoints};
      }
    }, {northSouth: 0, eastWest: 0});
  };

  if (!analysis) return <div>Loading...</div>

  return (
    <div className="container mx-auto p-4 max-w-4xl">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-2xl">Game Analysis</CardTitle>
              <CardDescription>Code: {code}</CardDescription>
            </div>
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
                      <span>Hand {index + 1} - Trump: {hand.trump.toLowerCase()}</span>
                      <span className="text-muted-foreground text-sm">
                      Starting Player: {hand.startingPlayer.name}
                    </span>
                    </div>
                    <div className="flex items-center gap-6 text-sm">
                      <div className={getTeamColor("NORTH")}>North/South: <span
                        className="font-medium">{hand.points.NORTH.cardPoints + hand.points.NORTH.weisPoints + hand.points.SOUTH.cardPoints + hand.points.SOUTH.weisPoints}</span>
                      </div>
                      <div className={getTeamColor("EAST")}>East/West: <span
                        className="font-medium">{hand.points.EAST.cardPoints + hand.points.EAST.weisPoints + hand.points.WEST.cardPoints + hand.points.WEST.weisPoints}</span>
                      </div>
                    </div>
                  </div>
                </AccordionTrigger>
                <AccordionContent>
                  <Card className="p-4">
                    {hand.tricks.map((trick: any, trickIndex: number) => (
                      <div key={trickIndex} className="mb-4 last:mb-0">
                        <div className="grid grid-cols-6 gap-1 p-4">
                          <div className="font-medium mb-2">Trick {trickIndex + 1}</div>
                          {trick.cards.map((play: any) => (
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
                               +{getTrickWinnerPoints(trick, hand)}
                              </span>
                              <span className="border-l pl-4">
                                {getRunningTotals(hand.tricks, trickIndex, hand).northSouth} - {getRunningTotals(hand.tricks, trickIndex, hand).eastWest}
                              </span>
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </Card>
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </CardContent>
      </Card>
    </div>
  )
}
