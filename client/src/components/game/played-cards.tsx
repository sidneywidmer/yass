import {AnimatePresence, motion} from "framer-motion"
import {useGameStateStore} from "@/store/game-state"
import {Card, CARD_HEIGHT, CARD_WIDTH} from "@/components/game/card.tsx";
import {Position} from "@/api/generated";

export function PlayedCards() {
  const {cardsPlayed} = useGameStateStore()

  const getInitialPosition = (position: Position) => {
    const target = getPosition(position)
    const transforms = {
      NORTH: {x: target.x, y: target.y - 200, rotate: woop(180), opacity: 0},
      EAST: {x: target.x + 200, y: target.y, rotate: woop(-90), opacity: 0},
      SOUTH: {x: target.x, y: target.y + 200, rotate: woop(0), opacity: 0},
      WEST: {x: target.x - 200, y: target.y, rotate: woop(90), opacity: 0},
    }
    return transforms[position]
  }

  const getPosition = (position: Position) => {
    const transforms = {
      NORTH: {x: -CARD_WIDTH / 2, y: -CARD_HEIGHT, rotate: 183},
      EAST: {x: 40, y: -CARD_HEIGHT / 2, rotate: -86},
      SOUTH: {x: -CARD_WIDTH / 2, y: 0, rotate: 5},
      WEST: {x: -140, y: -CARD_WIDTH / 2, rotate: 94},
    }
    return transforms[position]
  }

  // Give a rare chance to do a full 360
  const woop = (rotation: number): number => {
    if (Math.random() < 0.25) {
      return rotation + (rotation >= 0 ? 360 : -360)
    }
    return rotation
  }

  return (
    <div className="fixed inset-0 pointer-events-none">
      <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2">
        <AnimatePresence>
          {cardsPlayed!!.map((card) => (
            <motion.div
              layoutId={`card-${card.suit}-${card.rank}`}
              key={`${card.position}-${card.suit}-${card.rank}`}
              className="absolute origin-center"
              initial={getInitialPosition(card.position!!)}
              animate={{
                ...getPosition(card.position!!),
                opacity: 1
              }}
              transition={{type: "tween", duration: 0.3}}
              style={{width: CARD_WIDTH, height: CARD_HEIGHT}}
            >
              <Card card={card}/>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </div>
  )
}