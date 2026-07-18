import {useGameStateStore} from "@/store/game-state.ts";
import {CardInHand, State} from "@/api/generated";
import {GameStates} from "@/types/game-states.ts";
import {AnimatePresence, motion, useAnimation} from "motion/react"
import {useEffect, useMemo, useRef, useState} from "react";
import {Card} from "@/components/game/card.tsx";
import {useCardDimensions} from "@/hooks/use-card-dimensions.ts";
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {api} from "@/api/client.ts";
import {isTouchDevice, weisVerdictFor} from "@/lib/utils.ts";
import {WeisPointsBubble} from "@/components/game/weis-points-bubble.tsx";

// While one of these overlays is open the player's own cards render above it
const OVERLAY_STATES: State[] = [GameStates.WEISEN_FIRST, GameStates.TRUMP, GameStates.SCHIEBE]

// No hand to preselect from in these states
const NO_PRESELECT_STATES: State[] = [GameStates.WAITING_FOR_PLAYERS, GameStates.FINISHED]

// Entering PLAY_CARD from one of these means no card was played right before, so no UpdateHand
// is queued behind the state change and the last processed card states are already accurate.
// Entering from anywhere else (turn pass, trick won) a card WAS just played and its UpdateHand
// is still in the action queue - auto-playing before it arrives acts on stale states and the
// late UpdateHand then resurrects the played card in the hand.
const SAFE_PLAY_ENTRY_STATES: State[] = [
  GameStates.TRUMP, GameStates.TRUMP_BOT,
  GameStates.SCHIEBE, GameStates.SCHIEBE_BOT,
  GameStates.WEISEN_FIRST, GameStates.WEISEN_FIRST_BOT,
  GameStates.WEISEN_SECOND, GameStates.WEISEN_SECOND_BOT
]

type AnimationType = 'initial' | 'hover' | 'adjacentLeft' | 'adjacentRight' | 'preselected'

const cardKey = (card: CardInHand) => `${card.suit}-${card.rank}-${card.skin}`

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
  preselected?: boolean
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
    INITIAL: 0.08,
    DEAL: 0.1,
    PRESELECT: 0.15
  },
  EASING: [0.4, 0, 0.2, 1] as const, // feels "snappy", better than bouncy default of framer
  DEAL_STAGGER: 0.02, // per-card delay, gives the left-to-right deal-in sweep
  DEAL_Y_SHIFT: CARD_HEIGHT, // cards rise in from fully below the viewport when dealt
  ADJACENT_ROTATION: 15, // touch only
  BRIGHTNESS: { // depending on state we want to "mute" unplayable cards
    PLAYABLE: "brightness(1)",
    DISABLED: "brightness(0.75)"
  },
  HOVER_BASE_Y: -70 - ((CARD_HEIGHT - 100) / 30) * 20, // hover state based on CARD_HEIGHT, lol try and error
  HOVER_ADDITIONAL_Y: -10 - ((CARD_HEIGHT - 100) / 30) * 10, // extra lift for main hover
  PRESELECT_Y: -(CARD_HEIGHT * 1.1) // preselected cards slide way out so they clearly read as "armed"
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
  }),
  preselected: (_isTouch: boolean) => ({
    y: ANIMATION_CONFIG.PRESELECT_Y,
    scale: 1.1,
    rotate: 0,
    transition: {
      duration: ANIMATION_CONFIG.DURATIONS.PRESELECT,
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
  const isPlayCardState = useGameStateStore((state) => state.state === GameStates.PLAY_CARD)
  const state = useGameStateStore(state => state.state)
  const ownWeisPoints = useGameStateStore(state => state.position ? state.declaredWeisPoints[state.position] : undefined)
  const weisWinners = useGameStateStore(state => state.weisWinners)


  const weisDisplayBottom = CARD_HEIGHT * 1.1

  const handleAxiosError = useAxiosErrorHandler()
  const isTouch = useMemo(() => isTouchDevice(), [])
  const filteredCards = useMemo(() => cards?.filter(card => card.state !== "ALREADY_PLAYED") || [], [cards])
  const totalCards = filteredCards.length

  const hoveredIndexRef = useRef<number | null>(null)
  // Card preselected while waiting for the own turn, tracked by identity since indices shift when cards leave
  const preselectedCardRef = useRef<string | null>(null)
  // Cards are only ever removed mid-hand, so a growing count means a fresh deal (or page load)
  const [prevCardCount, setPrevCardCount] = useState(0)
  const [isDealingIn, setIsDealingIn] = useState(false)
  if (totalCards !== prevCardCount) {
    setPrevCardCount(totalCards)
    setIsDealingIn(totalCards > prevCardCount)
  }

  const startRotation = useMemo(() => {
    return -(ANIMATION_CONFIG.CARD_ROTATION / 2) - (ANIMATION_CONFIG.CARD_ROTATION * ((totalCards / 2) - 1))
  }, [totalCards]) // eslint-disable-line react-hooks/exhaustive-deps

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

      const animation = createCardAnimation({card, animationType, position, isTouch, preselected: cardPreselected(card)})

      getCardControlsByIndex(i).start(animation)
    })
  }

  const cardPlayable = (card: CardInHand) => {
    return card.state == "PLAYABLE" && isMyPos && isPlayCardState
  }

  const cardsAboveOverlay = () => {
    return isMyPos && OVERLAY_STATES.includes(state!)
  }

  const cardPreselected = (card: CardInHand) => {
    return preselectedCardRef.current === cardKey(card)
  }

  const createCardAnimation = ({card, animationType, position, isTouch, preselected}: CardAnimationConfig) => {
    // A preselected card stays slid out no matter what hover/reset animations run around it
    const effectiveType = preselected ? 'preselected' : animationType

    const baseAnimation = {
      y: position.offset,
      rotate: position.angle,
      scale: 1,
      filter: (cardsAboveOverlay() || cardPlayable(card) || preselected) ? ANIMATION_CONFIG.BRIGHTNESS.PLAYABLE : ANIMATION_CONFIG.BRIGHTNESS.DISABLED,
      zIndex: position.index,
      transition: {duration: ANIMATION_CONFIG.DURATIONS.INITIAL, ease: ANIMATION_CONFIG.EASING}
    }

    const override = ANIMATION_OVERRIDES[effectiveType as keyof typeof ANIMATION_OVERRIDES]?.(isTouch)
    return override ? {...baseAnimation, ...override} : baseAnimation
  }

  const getCardAnimationType = (cardIndex: number, selectedIndex: number | null, totalCards: number): AnimationType => {
    if (selectedIndex === null) return 'initial'
    if (cardIndex === selectedIndex) return 'hover'
    if (isTouch && cardIndex === selectedIndex - 1 && selectedIndex > 0) return 'adjacentLeft'
    if (isTouch && cardIndex === selectedIndex + 1 && selectedIndex < totalCards - 1) return 'adjacentRight'
    return 'initial'
  }

  const playCardAction = (card: CardInHand) => {
    resetActivePosition()
    api.playCard({game: gameUuid!, card: {suit: card.suit, rank: card.rank, skin: card.skin}})
      .catch(handleAxiosError)

    hoveredIndexRef.current = null
    // Clear the preselection only after the reset so the card holds its slid-out
    // pose until it leaves the hand, instead of dipping back in for a frame
    triggerCardHover(null, filteredCards)
    playCard({suit: card.suit, rank: card.rank, skin: card.skin, position: position})
    removeCardFromHand(card)
    preselectedCardRef.current = null
  }

  // Preselecting is possible whenever a direct play isn't: waiting for others, own trump/weisen
  // overlays or while the trick-clear animation still runs on the own turn
  const cardPreselectable = () => {
    return !!state && !NO_PRESELECT_STATES.includes(state) && !(isMyPos && isPlayCardState)
  }

  const preselectCard = (card: CardInHand) => {
    preselectedCardRef.current = cardKey(card)
    triggerCardHover(hoveredIndexRef.current, filteredCards)
  }

  const unpreselectCard = () => {
    preselectedCardRef.current = null
    hoveredIndexRef.current = null
    triggerCardHover(null, filteredCards)
  }

  const cardClicked = (card: CardInHand, index: number) => {
    // No idea why the click event fires more reliably than the typ but here are
    if (isTouch) {
      return cardTapped(card, index)
    }

    if (cardPreselected(card)) {
      unpreselectCard()
      return
    }

    if (cardPlayable(card)) {
      playCardAction(card)
      return
    }

    if (cardPreselectable()) {
      preselectCard(card)
    }
  }

  const cardTapped = (card: CardInHand, index: number) => {
    // Tapping a preselected card slides it back into the hand
    if (cardPreselected(card)) {
      unpreselectCard()
      return
    }

    // Second tap - play the card, or preselect it while a direct play isn't possible
    if (hoveredIndexRef.current === index) {
      if (cardPlayable(card)) {
        playCardAction(card)
      } else if (cardPreselectable()) {
        preselectedCardRef.current = cardKey(card)
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
    const arch = middle === 0 ? 0 : distance * 2 / middle * 10

    return Math.round(arch - (CARD_HEIGHT / 3))
  }

  const handleDrag = (action: { clientX: number; clientY: number }) => {
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

  // Stagger the cards in from the left whenever a fresh hand arrives
  useEffect(() => {
    if (!isDealingIn) return

    preselectedCardRef.current = null
    filteredCards.forEach((card, i) => {
      const offset = calculateOffset(i, totalCards)
      const currentAngle = startRotation + ANIMATION_CONFIG.CARD_ROTATION * i
      const restingAnimation = createCardAnimation({
        card,
        animationType: 'initial',
        position: {offset, angle: currentAngle, index: i},
        isTouch
      })

      getCardControlsByIndex(i).start({
        ...restingAnimation,
        transition: {
          duration: ANIMATION_CONFIG.DURATIONS.DEAL,
          ease: ANIMATION_CONFIG.EASING,
          delay: i * ANIMATION_CONFIG.DEAL_STAGGER
        }
      })
    })
  }, [isDealingIn]) // eslint-disable-line react-hooks/exhaustive-deps

  // Update all card animations when playable state changes
  useEffect(() => {
    if (!isMyPos) {
      return
    }

    filteredCards.forEach((card, i) => {
      const filter = (cardsAboveOverlay() || cardPlayable(card) || cardPreselected(card)) ? ANIMATION_CONFIG.BRIGHTNESS.PLAYABLE : ANIMATION_CONFIG.BRIGHTNESS.DISABLED
      getCardControlsByIndex(i).start({filter: filter})
    })
  }, [cards, isMyPos, isPlayCardState]) // eslint-disable-line react-hooks/exhaustive-deps

  // Auto-play the preselected card once it becomes playable, otherwise slide it back.
  // The server publishes UpdateActive/UpdateState before the UpdateHand of the same batch and
  // the action queue processes them with delays in between - so only act on card states that
  // arrived after the turn flipped to us (or on a safe entry where none are coming).
  const prevCardsRef = useRef(cards)
  const prevStateRef = useRef(state)

  useEffect(() => {
    const handChanged = prevCardsRef.current !== cards
    const enteredPlayState = isPlayCardState && !!prevStateRef.current && SAFE_PLAY_ENTRY_STATES.includes(prevStateRef.current)
    prevCardsRef.current = cards
    prevStateRef.current = state

    if (!preselectedCardRef.current) return

    const card = filteredCards.find(c => cardKey(c) === preselectedCardRef.current)
    if (!card) {
      preselectedCardRef.current = null
      return
    }

    if (!isMyPos || !isPlayCardState) return

    // Stale hand - the fresh UpdateHand is still in the queue and will re-trigger this
    if (!handChanged && !enteredPlayState) return

    if (cardPlayable(card)) {
      playCardAction(card)
    } else {
      // Turned out to be an invalid play - return it to the hand
      unpreselectCard()
    }
  }, [cards, isMyPos, isPlayCardState, state]) // eslint-disable-line react-hooks/exhaustive-deps

  if (!cards) return null

  return (
    <>
      <AnimatePresence>
        {ownWeisPoints !== undefined && position && (
          <div id={"weisPointsDisplay"}
               className={`fixed w-full flex justify-center ${cardsAboveOverlay() ? "z-50" : ""}`}
               style={{bottom: `${weisDisplayBottom}px`}}>
            <WeisPointsBubble
              points={ownWeisPoints}
              tailDirection="SOUTH"
              verdict={weisVerdictFor(position, weisWinners)}
            />
          </div>
        )}
      </AnimatePresence>
      <div id={"playerHand"}
           className={`fixed -bottom-[60px] w-full flex justify-center ${cardsAboveOverlay() ? "z-50" : ""}`}>
        <motion.div
          className="flex -space-x-10"
          key={totalCards}
          drag={isTouch}
          dragConstraints={{left: 0, right: 0, top: 0, bottom: 0}}
          dragElastic={0}
          onDrag={(_, info) => handleDrag({clientX: info.point.x, clientY: info.point.y})}
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
                layoutId={`cardlayout-${position}-${card.suit}-${card.rank}-${card.skin}`}
                key={`cardhand-${card.suit}-${card.rank}-${card.skin}`}
                data-card-index={i}
                className={`transition-shadow ${cardPlayable(card) ? 'hover:shadow-xl' : ''}`}
                style={{
                  cursor: 'pointer',
                  width: CARD_WIDTH,
                  height: CARD_HEIGHT,
                }}
                layout
                transition={{layout: {duration: 0.3, ease: [0.4, 0, 0.2, 1]}}}
                initial={isDealingIn ? {...initialAnimation, y: initialAnimation.y + ANIMATION_CONFIG.DEAL_Y_SHIFT} : initialAnimation}
                animate={getCardControlsByIndex(i)}
                onClick={() => cardClicked(card, i)}
              >
                <Card card={{suit: card.suit!, rank: card.rank!, skin: card.skin!}}/>
              </motion.div>
            )
          })}
        </AnimatePresence>
        </motion.div>
      </div>
    </>
  )
}
