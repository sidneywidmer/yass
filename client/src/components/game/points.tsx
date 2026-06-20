import {useGameStateStore} from "@/store/game-state"
import {cn} from "@/lib/utils"
import {motion, AnimatePresence} from "motion/react"
import {useEffect, useState} from "react"
import {TotalPoints} from "@/api/generated"

interface TeamSums {
  total: number
  totalCardPoints: number
  totalWeisPoints: number
}

interface TeamTotals {
  NORTH_SOUTH: TeamSums
  EAST_WEST: TeamSums
}

function calculateTeamTotals(teams: Record<string, TotalPoints>): TeamTotals {
  return {
    NORTH_SOUTH: {
      total: (teams.NORTH.cardPoints ?? 0) + (teams.SOUTH.cardPoints ?? 0) + (teams.NORTH.weisPoints ?? 0) + (teams.SOUTH.weisPoints ?? 0),
      totalCardPoints: (teams.NORTH.cardPoints ?? 0) + (teams.SOUTH.cardPoints ?? 0),
      totalWeisPoints: (teams.NORTH.weisPoints ?? 0) + (teams.SOUTH.weisPoints ?? 0),
    },
    EAST_WEST: {
      total: (teams.EAST.cardPoints ?? 0) + (teams.WEST.cardPoints ?? 0) + (teams.EAST.weisPoints ?? 0) + (teams.WEST.weisPoints ?? 0),
      totalCardPoints: (teams.EAST.cardPoints ?? 0) + (teams.WEST.cardPoints ?? 0),
      totalWeisPoints: (teams.EAST.weisPoints ?? 0) + (teams.WEST.weisPoints ?? 0),
    },
  }
}

interface PointsProps {
  team: 'NORTH_SOUTH' | 'EAST_WEST'
}

const Points = ({team}: PointsProps) => {
  const points = useGameStateStore(state => state.points)
  const position = useGameStateStore(state => state.position)
  const [previousTotal, setPreviousTotal] = useState(0)
  const [showDiff, setShowDiff] = useState(false)
  const totals = calculateTeamTotals(points)
  const currentTotal = totals[team].total
  const relevantTeam = (position === 'NORTH' || position === 'SOUTH') ? 'NORTH_SOUTH' : 'EAST_WEST'
  const isRelevant = team === relevantTeam
  const difference = currentTotal - previousTotal

  useEffect(() => {
    const diff = currentTotal - previousTotal
    if (diff > 0) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setShowDiff(true)
      const timeout = setTimeout(() => setShowDiff(false), 2000)
      return () => clearTimeout(timeout)
    }
  }, [currentTotal]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const timeout = setTimeout(() => {
      setPreviousTotal(currentTotal)
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