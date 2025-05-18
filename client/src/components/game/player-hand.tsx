import {useGameStateStore} from "@/store/game-state.ts";
import {CardInHand} from "@/api/generated";
import {AnimatePresence, motion} from "framer-motion"
import {useState} from "react";
import {Card, CARD_HEIGHT, CARD_WIDTH} from "@/components/game/card.tsx";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {api} from "@/api/client.ts";

export function PlayerHand() {
  const gameUuid = useGameStateStore(state => state.gameUuid)
  const cards = useGameStateStore(state => state.cards)
  const position = useGameStateStore(state => state.position)
  const removeCardFromHand = useGameStateStore(state => state.removeCardFromHand)
  const playCard = useGameStateStore(state => state.playCard)
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null)
  const handleAxiosError = useAxiosErrorHandler()
  const isMyPos = useGameStateStore((state) => state.activePosition === state.position)
  const isPlayCardState = useGameStateStore((state) => state.state === "PLAY_CARD")

  const cardPlayable = (card: CardInHand) => {
    return card.state == "PLAYABLE" && isMyPos && isPlayCardState
  }

  const cardClicked = (card: CardInHand) => {
    if (!cardPlayable(card)) return

    api.playCard({game: gameUuid!!, card: {suit: card.suit, rank: card.rank, skin: "french"}})
      .catch(handleAxiosError)

    setHoveredIndex(null)
    playCard({suit: card.suit, rank: card.rank, position: position})
    removeCardFromHand(card)
  }

  const getHoverAnimation = (card: CardInHand, y: number, currentAngle: number) => {
    const isPlayable = cardPlayable(card)
    return {
      y: y - Math.cos(currentAngle * Math.PI / 180) * (isPlayable ? 60 : 20),
      rotate: currentAngle * 0.8,
      scale: isPlayable ? 1.1 : 1,
      filter: isPlayable ? "brightness(1)" : "brightness(0.95)",
      transition: {duration: 0.12}
    }
  }

  const getInitialStyle = (card: CardInHand, y: number, currentAngle: number) => {
    const isPlayable = cardPlayable(card)
    return {
      y: y - (isPlayable ? 40 : 0),
      rotate: currentAngle,
      scale: isPlayable ? 1.02 : 1,
      filter: isPlayable ? "brightness(1)" : "brightness(0.95)"
    }
  }

  const calculateOffset = (index: number, totalItems: number): number => {
    const middle = (totalItems - 1) / 2
    const distance = Math.abs(index - middle)
    return (totalItems / 2 - distance) * 8 * -1
  }

  if (!cards) return null

  const totalCards = cards?.filter((card) => card.state != "ALREADY_PLAYED").length!!
  const cardRotation = 3
  const startRotation = -(cardRotation / 2) - (cardRotation * ((totalCards / 2) - 1))

  return (
    <div className="fixed -bottom-[60px] w-full flex justify-center">
      <div className="flex -space-x-10" key={totalCards}>
        <AnimatePresence mode="popLayout" initial={true}>
          {cards.filter(card => card.state != "ALREADY_PLAYED").map((card, i) => {
            const y = calculateOffset(i, totalCards)
            const currentAngle = startRotation + cardRotation * i

            return (
              <motion.div
                layoutId={`cardlayout-${position}-${card.suit}-${card.rank}`}
                key={`cardhand-${card.suit}-${card.rank}`}
                onClick={() => cardClicked(card)}
                onHoverStart={() => setHoveredIndex(i)}
                onHoverEnd={() => setHoveredIndex(null)}
                className={`transition-shadow ${cardPlayable(card) ? 'hover:shadow-xl' : ''}`}
                style={{
                  cursor: cardPlayable(card) ? 'pointer' : 'not-allowed',
                  width: CARD_WIDTH,
                  height: CARD_HEIGHT,
                  zIndex: hoveredIndex === i ? 100 : i,
                }}
                initial={getInitialStyle(card, y, currentAngle)}
                animate={getInitialStyle(card, y, currentAngle)}
                whileHover={getHoverAnimation(card, y, currentAngle)}
              >
                <Card card={{suit: card.suit!!, rank: card.rank!!}}/>
              </motion.div>
            )
          })}
        </AnimatePresence>
      </div>
    </div>
  )
}