import {Bot, User} from "lucide-react"
import {cn, getRelativePosition} from "@/lib/utils"
import {Position} from "@/api/generated"
import {useGameStateStore} from "@/store/game-state"
import {Card} from "@/components/ui/card.tsx";
import {AnimatePresence, motion} from "framer-motion"

const positionStyles: Record<Position, { transform: string, opacity: number }> = {
  NORTH: {transform: 'translate(-50%, calc(-50% - 200px))', opacity: 1},
  SOUTH: {transform: 'translate(-50%, calc(-50% + 200px))', opacity: 1},
  EAST: {transform: 'translate(calc(-50% + 300px), -50%)', opacity: 1},
  WEST: {transform: 'translate(calc(-50% - 300px), -50%)', opacity: 1}
}

const positionStylesInitial: Record<Position, { transform: string }> = {
  NORTH: {transform: 'translate(-50%, calc(-50% - 230px))'},
  SOUTH: {transform: 'translate(-50%, calc(-50% + 230px))'},
  EAST: {transform: 'translate(calc(-50% + 330px), -50%)'},
  WEST: {transform: 'translate(calc(-50% - 330px), -50%)'}
}

export function OtherPlayers() {
  const {otherPlayers, position} = useGameStateStore()
  return (
    <>
      <AnimatePresence mode="popLayout" initial={true}>
        {otherPlayers?.filter((player) => player.position != position).map(player => (
          <motion.div
            key={player.uuid}
            className="absolute top-1/2 left-1/2"
            style={positionStylesInitial[getRelativePosition(position!!, player.position!!)]}
            initial={{opacity: 0}}
            animate={positionStyles[getRelativePosition(position!!, player.position!!)]}
          >
            <Card
              className={cn(
                "h-10 p-3 flex flex-row items-center justify-center gap-2",
                player.status === "DISCONNECTED" && "border-red-500",
                (player.status === "CONNECTED" || player.status === "BOT") && "border-green-500"
              )}
            >
              {player.bot ? (<Bot className="h-4 w-4"/>) : (<User className="h-4 w-4"/>)}
              <span className="text-sm text-center">{player.name}</span>
            </Card>
          </motion.div>
        ))}
      </AnimatePresence>
    </>
  )
}

export default OtherPlayers