import {useGameStateStore} from "@/store/game-state.ts";
import {CardInHand} from "@/api/generated";
import {motion} from "framer-motion"
import {useState} from "react";
import {Card, CARD_HEIGHT, CARD_WIDTH} from "@/components/game/card.tsx";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {api} from "@/api/client.ts";

export function PlayerHand() {
  const {gameUuid, seat, removeCardFromHand, setAllCardsUnplayable, playCard} = useGameStateStore()
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null)
  const handleAxiosError = useAxiosErrorHandler()

  const cardClicked = (card: CardInHand) => {
    if (card.state != 'PLAYABLE') {
      return
    }

    api.playCard({game: gameUuid!!, card: {suit: card.suit, rank: card.rank, skin: "french"}})
      .catch(handleAxiosError)

    playCard({suit: card.suit, rank: card.rank, position: "SOUTH"}) // TODO: SOUTH dynamic from store
    removeCardFromHand(card)
    setAllCardsUnplayable()
  }

  const onHover = (card: CardInHand, x: number, y: number, currentAngle: number) => {
    let multi = 60
    if (card.state != 'PLAYABLE') {
      multi = 20
    }

    return {
      y: y - Math.cos(currentAngle * Math.PI / 180) * multi,
      x: x + Math.sin(currentAngle * Math.PI / 180) * multi,
      rotate: currentAngle * 0.8,
      transition: {
        duration: 0.1
      }
    }
  }

  function calculateOffset(index: number, card: CardInHand, totalItems: number): number {
    const middle = (totalItems - 1) / 2
    const distance = Math.abs(index - middle)
    const result = totalItems / 2 - distance

    return result * 8 * -1
  }

  if (!seat?.cards) return null

  const totalCards = seat.cards?.filter((card) => card.state != "ALREADY_PLAYED").length!!
  const spacing = CARD_WIDTH / 1.5;
  const cardRotation = 3;
  const startRotation = -(cardRotation / 2) - (cardRotation * ((totalCards / 2) - 1));
  const totalWidth = spacing * (totalCards - 1) + CARD_WIDTH

  return (
    <div className="absolute -bottom-[80px] w-full">
      <div className="relative mx-auto" style={{height: CARD_HEIGHT, width: totalWidth}} key={seat.cards.length}>
        {seat.cards.filter((card) => card.state != "ALREADY_PLAYED").map((card, i) => {
          const x = spacing * i;
          const y = calculateOffset(i, card, totalCards)
          const currentAngle = startRotation + cardRotation * i;

          return (
            <motion.div
              layoutId={`card-${card.suit}-${card.rank}`}
              onClick={() => cardClicked(card)}
              key={i}
              className="absolute touch-none"
              onHoverStart={() => setHoveredIndex(i)}
              onHoverEnd={() => setHoveredIndex(null)}
              style={{
                cursor: card.state === 'PLAYABLE' ? 'pointer' : 'not-allowed',
                width: CARD_WIDTH,
                height: CARD_HEIGHT,
                zIndex: hoveredIndex === i ? 100 : i,
              }}
              initial={{x, y, rotate: currentAngle, transformOrigin: 'center bottom'}}
              animate={{zIndex: hoveredIndex === i ? 100 : i,}}
              whileHover={onHover(card, x, y, currentAngle)}
            >
              <Card card={{suit: card.suit!!, rank: card.rank!!}}/>
            </motion.div>
          )
        })}
      </div>
    </div>
  )
}
