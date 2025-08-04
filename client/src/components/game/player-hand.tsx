import {useGameStateStore} from "@/store/game-state.ts";
import {CardInHand} from "@/api/generated";
import {AnimatePresence, motion, useAnimation} from "framer-motion"
import {useMemo, useRef} from "react";
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
  const hoveredIndexRef = useRef<number | null>(null)

  // Create a fixed number of animation controls (max possible cards in hand which is 9)
  const cardControls = [
    useAnimation(), useAnimation(), useAnimation(),
    useAnimation(), useAnimation(), useAnimation(),
    useAnimation(), useAnimation(), useAnimation()
  ]

  const filteredCards = useMemo(() => cards?.filter(card => card.state !== "ALREADY_PLAYED") || [], [cards])

  const getCardControlsByIndex = (index: number) => {
    return cardControls[index]
  }

  // Trigger hover animation on specific card
  const triggerCardHover = (selectedIndex: number | null, allCards: CardInHand[]) => {
    const totalCards = allCards.length
    const cardRotation = 3
    const startRotation = -(cardRotation / 2) - (cardRotation * ((totalCards / 2) - 1))

    allCards.forEach((card, i) => {
      const controls = getCardControlsByIndex(i)
      const y = calculateOffset(i, totalCards)
      const currentAngle = startRotation + cardRotation * i

      if (i === selectedIndex && isTouch) {
        // Start hover animation
        controls.start(getHoverAnimation(card, y, currentAngle))
      } else {
        // Return to initial state
        controls.start(getInitialStyle(card, y, currentAngle, i))
      }
    })
  }
  const handleAxiosError = useAxiosErrorHandler()
  const isMyPos = useGameStateStore((state) => state.activePosition === state.position)
  const isPlayCardState = useGameStateStore((state) => state.state === "PLAY_CARD")
  const isTouch = useMemo(() => isTouchDevice(), [])

  const cardPlayable = (card: CardInHand) => {
    return card.state == "PLAYABLE" && isMyPos && isPlayCardState
  }

  const playCardAction = (card: CardInHand) => {
    api.playCard({game: gameUuid!!, card: {suit: card.suit, rank: card.rank, skin: "french"}})
      .catch(handleAxiosError)

    hoveredIndexRef.current = null
    triggerCardHover(null, filteredCards)
    playCard({suit: card.suit, rank: card.rank, position: position})
    removeCardFromHand(card)
  }

  const cardClicked = (card: CardInHand, index: number) => {
    // No idea why the click event fires more reliably than the typ but here are
    if (isTouch) {
      return cardTapped(card, index)
    }

    if (!cardPlayable(card)) return
    playCardAction(card)
  }

  const cardTapped = (card: CardInHand, index: number) => {
    // Second tap - play the card
    if (hoveredIndexRef.current === index) {
      if (cardPlayable(card)) playCardAction(card)
      // Reset all animations
      hoveredIndexRef.current = null
      triggerCardHover(null, filteredCards)
      return
    }

    // First tap - select the card
    hoveredIndexRef.current = index
    triggerCardHover(index, filteredCards)
  }

  const getHoverAnimation = (card: CardInHand, y: number, currentAngle: number) => {
    const isPlayable = cardPlayable(card)
    return {
      y: y - Math.cos(currentAngle * Math.PI / 180) * (isPlayable ? CARD_HEIGHT / 2.5 : CARD_HEIGHT / 4),
      scale: isPlayable ? 1.05 : 1,
      filter: isPlayable ? "brightness(1)" : "brightness(0.95)",
      zIndex: 100,
      transition: {duration: isTouch ? 0.05 : 0.1}
    }
  }

  const getInitialStyle = (card: CardInHand, y: number, currentAngle: number, cardIndex: number = 0) => {
    const isPlayable = cardPlayable(card)
    return {
      y: y - (isPlayable ? CARD_HEIGHT / 6 : 0),
      rotate: currentAngle,
      scale: isPlayable ? 1.02 : 1,
      filter: isPlayable ? "brightness(1)" : "brightness(0.95)",
      zIndex: cardIndex
    }
  }

  const calculateOffset = (index: number, totalItems: number): number => {
    const middle = (totalItems - 1) / 2
    const distance = Math.abs(index - middle)
    return (totalItems / 2 - distance) * 7 * -1
  }

  // Debounced drag handler to improve performance
  const handleDrag = (info: any) => {
    const element = document.elementFromPoint(info.point.x, info.point.y)
    const cardElement = element?.closest('[data-card-index]')
    if (cardElement) {
      const newIndex = parseInt(cardElement.getAttribute('data-card-index') || '0')
      if (newIndex !== hoveredIndexRef.current) {
        console.log("set index to " + newIndex)
        hoveredIndexRef.current = newIndex
        triggerCardHover(newIndex, filteredCards)
      }
    }
  }

  if (!cards) return null

  const totalCards = filteredCards.length
  const cardRotation = 3
  const startRotation = -(cardRotation / 2) - (cardRotation * ((totalCards / 2) - 1))

  return (
    <div className="fixed -bottom-[60px] w-full flex justify-center">
      <motion.div
        className="flex -space-x-10"
        key={totalCards}
        drag={isTouch}
        dragConstraints={{left: 0, right: 0, top: 0, bottom: 0}}
        dragElastic={0}
        onDrag={(_, info) => handleDrag(info)}
      >
        <AnimatePresence mode="popLayout" initial={true}>
          {filteredCards.map((card, i) => {
            const y = calculateOffset(i, totalCards)
            const currentAngle = startRotation + cardRotation * i

            return (
              <motion.div
                layoutId={`cardlayout-${position}-${card.suit}-${card.rank}`}
                key={`cardhand-${card.suit}-${card.rank}`}
                data-card-index={i}
                className={`transition-shadow ${cardPlayable(card) ? 'hover:shadow-xl' : ''}`}
                style={{
                  cursor: 'pointer',
                  width: CARD_WIDTH,
                  height: CARD_HEIGHT,
                }}
                initial={getInitialStyle(card, y, currentAngle, i)}
                animate={getCardControlsByIndex(i)}
                whileHover={getHoverAnimation(card, y, currentAngle)}
                onClick={() => cardClicked(card, i)}
              >
                <Card card={{suit: card.suit!!, rank: card.rank!!}}/>
              </motion.div>
            )
          })}
        </AnimatePresence>
      </motion.div>
    </div>
  )
}