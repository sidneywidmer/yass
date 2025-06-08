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
const StatusIndicator = ({status, isActive}: { status: string, isActive?: boolean }) => {
  return (
    <div className="relative">
      <motion.div
        className={cn(
          "absolute w-2 h-2 rounded-full",
          status === "DISCONNECTED" && "bg-red-500/50",
          status === "BOT" && "bg-blue-500/50",
          status === "CONNECTED" && "bg-green-500/50"
        )}
        animate={isActive ? {
          scale: [0.8, 20, 0.8],
          opacity: [0.7, 0, 0]
        } : {opacity: 0}}
        transition={isActive ? {
          duration: 3,
          repeat: Infinity,
          ease: "easeOut",
          times: [0, 0.6, 1] // 3 Keyframes to avoid flickering when "downsizing" again
        } : undefined}
      />
      <div className={cn(
        "relative w-2 h-2 rounded-full",
        status === "DISCONNECTED" && "bg-red-500",
        status === "BOT" && "bg-blue-500",
        status === "CONNECTED" && "bg-green-500"
      )}/>
    </div>
  )
}

export function OtherPlayers() {
  const otherPlayers = useGameStateStore(state => state.otherPlayers)
  const position = useGameStateStore(state => state.position)
  const activePosition = useGameStateStore(state => state.activePosition)
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
            <Card className="h-10 p-3 flex flex-row items-center justify-center gap-2 overflow-hidden">
              {player.bot ? (<Bot className="h-4 w-4"/>) : (<User className="h-4 w-4"/>)}
              <span className="text-sm text-center">{player.name}</span>
              <StatusIndicator
                status={player.status!!}
                isActive={player.position == activePosition && !player.bot}
              />
            </Card>
          </motion.div>
        ))}
      </AnimatePresence>
    </>
  )
}

export default OtherPlayers