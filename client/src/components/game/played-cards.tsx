import {AnimatePresence, motion} from "framer-motion"
import {useGameStateStore} from "@/store/game-state"
import {Card, CARD_HEIGHT, CARD_WIDTH} from "@/components/game/card.tsx";
import {Position} from "@/api/generated";
import {useMemo} from "react";
import {getRelativePosition} from "@/lib/utils.ts";

export function PlayedCards() {
  const {clearDirection, cardsPlayed, position} = useGameStateStore()

  const cardRotations = useMemo(() => ({
    NORTH: 180 + (Math.random() * 10 - 5),
    EAST: -90 + (Math.random() * 10 - 5),
    SOUTH: (Math.random() * 10 - 5),
    WEST: 90 + (Math.random() * 10 - 5),
  }), [])

  const getInitialPosition = (position: Position, exclRotate: boolean = false) => {
    const target = getPosition(position)
    const transforms = {
      NORTH: {x: target.x, y: target.y - 200, rotate: woop(180), opacity: 0},
      EAST: {x: target.x + 200, y: target.y, rotate: woop(-90), opacity: 0},
      SOUTH: {x: target.x, y: target.y + 200, rotate: 0, opacity: 0},
      WEST: {x: target.x - 200, y: target.y, rotate: woop(90), opacity: 0},
    }
    if (exclRotate) {
      const result = transforms[position]
      const {rotate, ...rest} = result
      return rest
    }
    return transforms[position]
  }

  const getPosition = (position: Position) => {
    const transforms = {
      NORTH: {x: -CARD_WIDTH / 2, y: -CARD_HEIGHT, rotate: cardRotations.NORTH},
      EAST: {x: 40, y: -CARD_HEIGHT / 2, rotate: cardRotations.EAST},
      SOUTH: {x: -CARD_WIDTH / 2, y: 0, rotate: cardRotations.SOUTH},
      WEST: {x: -140, y: -CARD_WIDTH / 2, rotate: cardRotations.WEST},
    }
    return transforms[position]
  }

  // Give a rare chance to do a full 360
  const woop = (rotation: number): number => {
    if (Math.random() < 0.1) {
      return rotation + (rotation >= 0 ? 360 : -360)
    }
    return rotation
  }

  const handleExit = () => {
    return getInitialPosition(getRelativePosition(position!!, clearDirection!!), true)
  }

  return (
    <div className="fixed inset-0 pointer-events-none">
      <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2" key={"table"}>
        <AnimatePresence initial={true}>
          {cardsPlayed!!.map((card) => (
            <motion.div
              layoutId={`cardlayout-${card.position}-${card.suit}-${card.rank}`}
              key={`cardtable-${card.position}-${card.suit}-${card.rank}`}
              className="absolute origin-center"
              initial={getInitialPosition(getRelativePosition(position!!, card.position!!))}
              animate={
                clearDirection && getRelativePosition(position!!, card.position!!) === clearDirection
                  ? {
                    ...getPosition(getRelativePosition(position!!, card.position!!)),
                    opacity: 1,
                    rotate: [
                      cardRotations[getRelativePosition(position!!, card.position!!)],
                      cardRotations[getRelativePosition(position!!, card.position!!)] - 15,
                      cardRotations[getRelativePosition(position!!, card.position!!)] + 15,
                      cardRotations[getRelativePosition(position!!, card.position!!)] - 10,
                      cardRotations[getRelativePosition(position!!, card.position!!)] + 10,
                      cardRotations[getRelativePosition(position!!, card.position!!)]
                    ],
                    zIndex: 100,
                    transition: {rotate: {duration: 0.2, ease: "easeInOut"}}
                  }
                  : {
                    ...getPosition(getRelativePosition(position!!, card.position!!)),
                    opacity: 1,
                    transition: {type: "tween", duration: 0.3}
                  }
              }
              // animate={{
              //   ...getPosition(getRelativePosition(position!!, card.position!!)),
              //   opacity: 1
              // }}
              // transition={{type: "tween", duration: 0.3}}
              exit={clearDirection ? {...handleExit(), opacity: 0} : {}}
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