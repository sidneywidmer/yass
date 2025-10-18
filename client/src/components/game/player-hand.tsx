import {useGameStateStore} from "@/store/game-state.ts";
import {CardInHand} from "@/api/generated";
import {AnimatePresence, motion, useAnimation} from "framer-motion"
import {useEffect, useMemo, useRef} from "react";
import {Card} from "@/components/game/card.tsx";
import {useCardDimensions} from "@/hooks/use-card-dimensions.ts";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {api} from "@/api/client.ts";
import {isTouchDevice} from "@/lib/utils.ts";

type AnimationType = 'initial' | 'hover' | 'adjacentLeft' | 'adjacentRight'

type CardPosition = {
  offset: number // this gives us our nice little arch
  angle: number
  index: number
}

type CardAnimationConfig = {
  card: CardInHand
  animationType: AnimationType
  position: CardPosition
  isTouch: boolean
}

// Animation constants factory
const getAnimationConfig = (CARD_HEIGHT: number) => ({
  CARD_ROTATION: 3, // how rotated each card is
  HOVER_SCALE: 1.25, // also for touch
  ADJACENT_SCALE: 1.1, // touch only, cards left and right of current are a little smaller
  DURATIONS: { // how fast the easy in/out happens
    HOVER_TOUCH: 0.05,
    HOVER_DESKTOP: 0.08,
    ADJACENT_TOUCH: 0.06,
    ADJACENT_DESKTOP: 0.08,
    INITIAL: 0.08
  },
  EASING: [0.4, 0, 0.2, 1] as const, // feels "snappy", better than bouncy default of framer
  ADJACENT_ROTATION: 15, // touch only
  BRIGHTNESS: { // depending on state we want to "mute" unplayable cards
    PLAYABLE: "brightness(1)",
    DISABLED: "brightness(0.75)"
  },
  HOVER_BASE_Y: -70 - ((CARD_HEIGHT - 100) / 30) * 20, // hover state based on CARD_HEIGHT, lol try and error
  HOVER_ADDITIONAL_Y: -10 - ((CARD_HEIGHT - 100) / 30) * 10 // extra lift for main hover
} as const)

// Animation overrides for specific card states
const getAnimationOverrides = (ANIMATION_CONFIG: ReturnType<typeof getAnimationConfig>) => ({
  hover: (isTouch: boolean) => ({
    y: ANIMATION_CONFIG.HOVER_BASE_Y + ANIMATION_CONFIG.HOVER_ADDITIONAL_Y,
    scale: ANIMATION_CONFIG.HOVER_SCALE,
    rotate: 0,
    transition: {
      duration: isTouch ? ANIMATION_CONFIG.DURATIONS.HOVER_TOUCH : ANIMATION_CONFIG.DURATIONS.HOVER_DESKTOP,
      ease: ANIMATION_CONFIG.EASING
    }
  }),
  adjacentLeft: (isTouch: boolean) => ({
    y: ANIMATION_CONFIG.HOVER_BASE_Y,
    scale: ANIMATION_CONFIG.ADJACENT_SCALE,
    rotate: -ANIMATION_CONFIG.ADJACENT_ROTATION,
    transition: {
      duration: isTouch ? ANIMATION_CONFIG.DURATIONS.ADJACENT_TOUCH : ANIMATION_CONFIG.DURATIONS.ADJACENT_DESKTOP,
      ease: ANIMATION_CONFIG.EASING
    }
  }),
  adjacentRight: (isTouch: boolean) => ({
    y: ANIMATION_CONFIG.HOVER_BASE_Y,
    scale: ANIMATION_CONFIG.ADJACENT_SCALE,
    rotate: ANIMATION_CONFIG.ADJACENT_ROTATION,
    transition: {
      duration: isTouch ? ANIMATION_CONFIG.DURATIONS.ADJACENT_TOUCH : ANIMATION_CONFIG.DURATIONS.ADJACENT_DESKTOP,
      ease: ANIMATION_CONFIG.EASING
    }
  })
}) as const

export function PlayerHand() {
  const {CARD_WIDTH, CARD_HEIGHT} = useCardDimensions();
  const ANIMATION_CONFIG = getAnimationConfig(CARD_HEIGHT);
  const ANIMATION_OVERRIDES = getAnimationOverrides(ANIMATION_CONFIG);
  const gameUuid = useGameStateStore(state => state.gameUuid)
  const cards = useGameStateStore(state => state.cards)
  const position = useGameStateStore(state => state.position)
  const removeCardFromHand = useGameStateStore(state => state.removeCardFromHand)
  const resetActivePosition = useGameStateStore(state => state.resetActivePosition)
  const playCard = useGameStateStore(state => state.playCard)
  const isMyPos = useGameStateStore((state) => state.activePosition === state.position)
  const isPlayCardState = useGameStateStore((state) => state.state === "PLAY_CARD")
  const state = useGameStateStore(state => state.state)

  const handleAxiosError = useAxiosErrorHandler()
  const isTouch = useMemo(() => isTouchDevice(), [])
  const filteredCards = useMemo(() => cards?.filter(card => card.state !== "ALREADY_PLAYED") || [], [cards])
  const totalCards = filteredCards.length

  const hoveredIndexRef = useRef<number | null>(null)

  if (!cards) return null

  const startRotation = useMemo(() => {
    return -(ANIMATION_CONFIG.CARD_ROTATION / 2) - (ANIMATION_CONFIG.CARD_ROTATION * ((totalCards / 2) - 1))
  }, [totalCards])

  // Create a fixed number of animation controls (max possible cards in hand which is 9). Each card, when being created -
  // gets one of those assigned. Like this we hava a reference to the cards animation handler and can manually invoke
  // it wherever we want.
  const cardControls = [
    useAnimation(), useAnimation(), useAnimation(),
    useAnimation(), useAnimation(), useAnimation(),
    useAnimation(), useAnimation(), useAnimation()
  ]

  const getCardControlsByIndex = (index: number) => {
    return cardControls[index]
  }

  const triggerCardHover = (index: number | null, allCards: CardInHand[]) => {
    if (index == null) {
      hoveredIndexRef.current = null
    }

    allCards.forEach((card: CardInHand, i: number) => {
      const offset = calculateOffset(i, allCards.length)
      const currentAngle = startRotation + ANIMATION_CONFIG.CARD_ROTATION * i

      const animationType = getCardAnimationType(i, index, allCards.length)
      const position: CardPosition = {offset, angle: currentAngle, index: i}

      const animation = createCardAnimation({card, animationType, position, isTouch})

      getCardControlsByIndex(i).start(animation)
    })
  }

  const cardPlayable = (card: CardInHand) => {
    return card.state == "PLAYABLE" && isMyPos && isPlayCardState
  }

  const cardsAboveOverlay = () => {
    return isMyPos && ["WEISEN_FIRST", "TRUMP", "SCHIEBE"].includes(state!!)
  }

  const createCardAnimation = ({card, animationType, position, isTouch}: CardAnimationConfig) => {
    const baseAnimation = {
      y: position.offset,
      rotate: position.angle,
      scale: 1,
      filter: (cardsAboveOverlay() || cardPlayable(card)) ? ANIMATION_CONFIG.BRIGHTNESS.PLAYABLE : ANIMATION_CONFIG.BRIGHTNESS.DISABLED,
      zIndex: position.index,
      transition: {duration: ANIMATION_CONFIG.DURATIONS.INITIAL, ease: ANIMATION_CONFIG.EASING}
    }

    const override = ANIMATION_OVERRIDES[animationType as keyof typeof ANIMATION_OVERRIDES]?.(isTouch)
    return override ? {...baseAnimation, ...override} : baseAnimation
  }

  const getCardAnimationType = (cardIndex: number, selectedIndex: number | null, totalCards: number): AnimationType => {
    if (selectedIndex === null) return 'initial'
    if (cardIndex === selectedIndex) return 'hover'
    if (isTouch && cardIndex === selectedIndex - 1 && selectedIndex > 0) return 'adjacentLeft'
    if (isTouch && cardIndex === selectedIndex + 1 && selectedIndex < totalCards - 1) return 'adjacentRight'
    return 'initial'
  }

  // Update all card animations when playable state changes
  useEffect(() => {
    if (!isMyPos) {
      return
    }

    filteredCards.forEach((card, i) => {
      const filter = (cardsAboveOverlay() || cardPlayable(card)) ? ANIMATION_CONFIG.BRIGHTNESS.PLAYABLE : ANIMATION_CONFIG.BRIGHTNESS.DISABLED
      getCardControlsByIndex(i).start({filter: filter})
    })
  }, [cards, isMyPos, isPlayCardState])

  const playCardAction = (card: CardInHand) => {
    resetActivePosition()
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
      if (cardPlayable(card)) {
        playCardAction(card)
      }

      // Reset all animations
      hoveredIndexRef.current = null
      triggerCardHover(null, filteredCards)
      return
    }

    // First tap - select the card
    hoveredIndexRef.current = index
    triggerCardHover(index, filteredCards)
  }


  /**
   * Calculate y offset of the card in the hand, this gives us the card "arch".
   */
  const calculateOffset = (index: number, totalItems: number): number => {
    const middle = (totalItems - 1) / 2
    const distance = Math.abs(index - middle)

    return Math.round((distance * 2 / middle * 10) - (CARD_HEIGHT / 3))
  }

  const handleDrag = (action: any) => {
    const element = document.elementFromPoint(action.clientX, action.clientY)
    const cardElement = element?.closest('[data-card-index]')
    if (cardElement) {
      const newIndex = parseInt(cardElement.getAttribute('data-card-index') || '0')
      if (newIndex !== hoveredIndexRef.current) {
        hoveredIndexRef.current = newIndex
        triggerCardHover(newIndex, filteredCards)
      }
    }
  }

  return (
    <div id={"playerHand"}
         className={`fixed -bottom-[60px] w-full flex justify-center ${cardsAboveOverlay() ? "z-50" : ""}`}>
      <motion.div
        className="flex -space-x-10"
        key={totalCards}
        drag={isTouch}
        dragConstraints={{left: 0, right: 0, top: 0, bottom: 0}}
        dragElastic={0}
        onDrag={(mouse) => handleDrag(mouse)}
        onMouseMove={(mouse) => !isTouch && handleDrag(mouse)}
        onMouseLeave={() => !isTouch && triggerCardHover(null, filteredCards)}
      >
        <AnimatePresence mode="popLayout" initial={true}>
          {filteredCards.map((card, i) => {
            const offset = calculateOffset(i, totalCards)
            const currentAngle = startRotation + ANIMATION_CONFIG.CARD_ROTATION * i
            const initialAnimation = createCardAnimation({
              card,
              animationType: 'initial',
              position: {offset, angle: currentAngle, index: i},
              isTouch
            })

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
                layout
                transition={{
                  layout: {duration: 0.3, ease: [0.4, 0, 0.2, 1]}
                }}
                initial={initialAnimation}
                animate={getCardControlsByIndex(i)}
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