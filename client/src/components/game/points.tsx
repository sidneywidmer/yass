import {useGameStateStore} from "@/store/game-state"
import {cn} from "@/lib/utils"
import {motion, AnimatePresence} from "framer-motion"
import {useRef, useEffect, useState} from "react"

interface TeamSums {
  total: number
  totalCardPoints: number
  totalWeisPoints: number
}

interface TeamTotals {
  NORTH_SOUTH: TeamSums
  EAST_WEST: TeamSums
}

function calculateTeamTotals(teams: any): TeamTotals {
  return {
    NORTH_SOUTH: {
      total: teams.NORTH.cardPoints + teams.SOUTH.cardPoints + teams.NORTH.weisPoints + teams.SOUTH.weisPoints,
      totalCardPoints: teams.NORTH.cardPoints + teams.SOUTH.cardPoints,
      totalWeisPoints: teams.NORTH.weisPoints + teams.SOUTH.weisPoints,
    },
    EAST_WEST: {
      total: teams.EAST.cardPoints + teams.WEST.cardPoints + teams.EAST.weisPoints + teams.WEST.weisPoints,
      totalCardPoints: teams.EAST.cardPoints + teams.WEST.cardPoints,
      totalWeisPoints: teams.EAST.weisPoints + teams.WEST.weisPoints,
    },
  }
}

interface PointsProps {
  team: 'NORTH_SOUTH' | 'EAST_WEST'
}

const Points = ({team}: PointsProps) => {
  const {points, position} = useGameStateStore()
  const previousTotalRef = useRef(0)
  const totals = calculateTeamTotals(points)
  const currentTotal = totals[team].total
  const relevantTeam = (position === 'NORTH' || position === 'SOUTH') ? 'NORTH_SOUTH' : 'EAST_WEST'
  const isRelevant = team === relevantTeam
  const difference = currentTotal - previousTotalRef.current
  const [showDiff, setShowDiff] = useState(false)

  useEffect(() => {
    if (difference > 0) {
      setShowDiff(true)
      const timeout = setTimeout(() => setShowDiff(false), 2000)
      return () => clearTimeout(timeout)
    }
  }, [difference])

  useEffect(() => {
    const timeout = setTimeout(() => {
      previousTotalRef.current = currentTotal
    }, 1000)
    return () => clearTimeout(timeout)
  }, [currentTotal])

  return (
    <div className="relative">
      <motion.span
        className={cn(
          "text-lg font-bold inline-block",
          isRelevant && "rounded px-2 py-0 border border-blue-200 bg-blue-100"
        )}
        initial={{opacity: 1}}
        animate={{
          opacity: 1,
          scale: difference !== 0 ? [1, 1.1, 1] : 1
        }}
        transition={{duration: 0.3}}
      >
        <motion.span
          initial={{y: 0}}
          animate={{y: 0}}
          key={currentTotal}
        >
          {currentTotal}
        </motion.span>
      </motion.span>

      <AnimatePresence>
        {showDiff && difference > 0 && (
          <motion.span
            className="absolute -right-5 -top-3 font-semibold text-green-600"
            initial={{opacity: 0, y: -10}}
            animate={{opacity: 1, y: 0}}
            exit={{opacity: 0, y: 10}}
            key={currentTotal}
          >
            +{difference}
          </motion.span>
        )}
      </AnimatePresence>
    </div>
  )
}

export default Points