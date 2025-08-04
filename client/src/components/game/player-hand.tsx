import {useGameStateStore} from "@/store/game-state.ts";
import {CardInHand} from "@/api/generated";
import {AnimatePresence, motion} from "framer-motion"
import React, {useState, useMemo} from "react";
import {Card, CARD_HEIGHT, CARD_WIDTH} from "@/components/game/card.tsx";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {api} from "@/api/client.ts";
import {isTouchDevice} from "@/lib/utils.ts";

export function PlayerHand() {
  const gameUuid = useGameStateStore(state => state.gameUuid)
  const cards = useGameStateStore(state => state.cards)
  const position = useGameStateStore(state => state.position)
  const removeCardFromHand = useGameStateStore(state => state.removeCardFromHand)
  const playCard = useGameStateStore(state => state.playCard)
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null)
  const [isDragging, setIsDragging] = useState(false)
  const [touchStartTime, setTouchStartTime] = useState<number | null>(null)
  const [hasMoved, setHasMoved] = useState(false)
  // const handleAxiosError = useAxiosErrorHandler()
  const isMyPos = useGameStateStore((state) => state.activePosition === state.position)
  const isPlayCardState = useGameStateStore((state) => state.state === "PLAY_CARD")
  const isTouch = useMemo(() => isTouchDevice(), [])

  const cardPlayable = (card: CardInHand) => {
    return card.state == "PLAYABLE" && isMyPos && isPlayCardState
  }

  const playCardAction = (card: CardInHand) => {
    api.playCard({game: gameUuid!!, card: {suit: card.suit, rank: card.rank, skin: "french"}})
       // .catch(handleAxiosError)

    setHoveredIndex(null)
    playCard({suit: card.suit, rank: card.rank, position: position})
    removeCardFromHand(card)
  }

  const cardClicked = (card: CardInHand) => {
    if (isTouch || !cardPlayable(card)) return
    playCardAction(card)
  }

  const handleTouchStart = () => {
    setTouchStartTime(Date.now())
    setHasMoved(false)
    setIsDragging(true)
  }

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!isDragging) return
    setHasMoved(true)
    e.preventDefault()
    
    const touch = e.touches[0]
    const element = document.elementFromPoint(touch.clientX, touch.clientY)
    const cardElement = element?.closest('[data-card-index]')
    
    if (cardElement) {
      const newIndex = parseInt(cardElement.getAttribute('data-card-index') || '0')
      if (newIndex !== hoveredIndex) {
        setHoveredIndex(newIndex)
      }
    }
  }

  const handleTouchEnd = (card: CardInHand, index: number) => {
    setIsDragging(false)
    if (!cardPlayable(card)) return
    
    const touchDuration = touchStartTime ? Date.now() - touchStartTime : 0
    const isQuickTap = touchDuration < 300 && !hasMoved

    if (isQuickTap) {
      if (hoveredIndex === index) {
        playCardAction(card)
      } else {
        setHoveredIndex(index)
      }
    }
  }

  const getHoverAnimation = (card: CardInHand, y: number, currentAngle: number) => {
    const isPlayable = cardPlayable(card)
    return {
      y: y - Math.cos(currentAngle * Math.PI / 180) * (isPlayable ? CARD_HEIGHT / 2.5 : CARD_HEIGHT / 4),
      scale: isPlayable ? 1.05 : 1,
      filter: isPlayable ? "brightness(1)" : "brightness(0.95)",
      transition: {duration: isTouch ? 0.05 : 0.1}
    }
  }

  const getInitialStyle = (card: CardInHand, y: number, currentAngle: number) => {
    const isPlayable = cardPlayable(card)
    return {
      y: y - (isPlayable ? CARD_HEIGHT / 6 : 0),
      rotate: currentAngle,
      scale: isPlayable ? 1.02 : 1,
      filter: isPlayable ? "brightness(1)" : "brightness(0.95)"
    }
  }

  const calculateOffset = (index: number, totalItems: number): number => {
    const middle = (totalItems - 1) / 2
    const distance = Math.abs(index - middle)
    return (totalItems / 2 - distance) * 7 * -1
  }

  const touchEventHandlers = {
    onTouchStart: handleTouchStart,
    onTouchMove: handleTouchMove,
  }

  const setHoveredIndexToI = (i: number) => () => setHoveredIndex(i)
  const clearHoveredIndex = () => setHoveredIndex(null)

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

            const eventHandlers = isTouch ? {
              ...touchEventHandlers,
              onTouchEnd: () => handleTouchEnd(card, i),
            } : {
              onClick: () => cardClicked(card),
              onHoverStart: setHoveredIndexToI(i),
              onHoverEnd: clearHoveredIndex,
            }

            return (
              <motion.div
                layoutId={`cardlayout-${position}-${card.suit}-${card.rank}`}
                key={`cardhand-${card.suit}-${card.rank}`}
                data-card-index={i}
                {...eventHandlers}
                className={`transition-shadow ${cardPlayable(card) ? 'hover:shadow-xl' : ''}`}
                style={{
                  cursor: cardPlayable(card) ? 'pointer' : 'not-allowed',
                  width: CARD_WIDTH,
                  height: CARD_HEIGHT,
                  zIndex: hoveredIndex === i ? 100 : i,
                  touchAction: isTouch ? 'none' : 'auto',
                }}
                initial={getInitialStyle(card, y, currentAngle)}
                animate={
                  hoveredIndex === i
                    ? getHoverAnimation(card, y, currentAngle)
                    : getInitialStyle(card, y, currentAngle)
                }
                whileHover={!isTouch ? getHoverAnimation(card, y, currentAngle) : undefined}
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