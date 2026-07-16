import {useGameStateStore} from "@/store/game-state"
import {cn} from "@/lib/utils"
import {motion, AnimatePresence} from "motion/react"
import {useEffect, useState} from "react"
import {TotalPoints} from "@/api/generated"

type Team = 'NORTH_SOUTH' | 'EAST_WEST'

const TEAM_POSITIONS: Record<Team, [string, string]> = {
  NORTH_SOUTH: ['NORTH', 'SOUTH'],
  EAST_WEST: ['EAST', 'WEST'],
}

const DIFF_VISIBLE_MS = 2000

function teamTotal(points: Record<string, TotalPoints>, team: Team): number {
  return TEAM_POSITIONS[team].reduce((sum, position) => {
    const playerPoints = points[position]
    return sum + (playerPoints?.cardPoints ?? 0) + (playerPoints?.weisPoints ?? 0)
  }, 0)
}

interface PointsProps {
  team: Team
}

const Points = ({team}: PointsProps) => {
  const points = useGameStateStore(state => state.points)
  const position = useGameStateStore(state => state.position)
  const isRelevant = team === ((position === 'NORTH' || position === 'SOUTH') ? 'NORTH_SOUTH' : 'EAST_WEST')

  const total = teamTotal(points, team)
  const [previousTotal, setPreviousTotal] = useState(total)
  const [difference, setDifference] = useState(0)
  if (total !== previousTotal) {
    setPreviousTotal(total)
    setDifference(total - previousTotal)
  }

  useEffect(() => {
    if (difference === 0) return
    const timeout = setTimeout(() => setDifference(0), DIFF_VISIBLE_MS)
    return () => clearTimeout(timeout)
  }, [difference])

  return (
    <div className="relative">
      <motion.span
        className={cn(
          "text-lg font-bold inline-block",
          isRelevant && "rounded px-2 py-0 border border-blue-200 bg-blue-100"
        )}
        animate={{scale: difference > 0 ? [1, 1.1, 1] : 1}}
        transition={{duration: 0.3}}
      >
        {total}
      </motion.span>

      <AnimatePresence>
        {difference > 0 && (
          <motion.span
            className="absolute -right-5 -top-3 z-10 font-semibold text-green-600"
            initial={{opacity: 0, y: -10}}
            animate={{opacity: 1, y: 0}}
            exit={{opacity: 0, y: 10}}
            key={total}
          >
            +{difference}
          </motion.span>
        )}
      </AnimatePresence>
    </div>
  )
}

export default Points
