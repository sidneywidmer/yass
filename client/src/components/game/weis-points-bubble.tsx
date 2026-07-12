import {cn, WeisVerdict} from "@/lib/utils"
import {Position} from "@/api/generated"
import {motion} from "motion/react"

// Depending on if in the end that team won/lost the weis the bubble gets colored
const verdictStyles: Record<WeisVerdict, { bubble: string, tail: Record<Position, string> }> = {
  none: {
    bubble: "bg-gray-200 text-gray-800",
    tail: {NORTH: "border-b-gray-200", SOUTH: "border-t-gray-200", EAST: "border-l-gray-200", WEST: "border-r-gray-200"}
  },
  won: {
    bubble: "bg-green-200 text-green-900",
    tail: {NORTH: "border-b-green-200", SOUTH: "border-t-green-200", EAST: "border-l-green-200", WEST: "border-r-green-200"}
  },
  lost: {
    bubble: "bg-red-200 text-red-900",
    tail: {NORTH: "border-b-red-200", SOUTH: "border-t-red-200", EAST: "border-l-red-200", WEST: "border-r-red-200"}
  }
}

const tailPlacement: Record<Position, string> = {
  NORTH: "bottom-full -mb-1 left-1/2 -translate-x-1/2 border-l-8 border-l-transparent border-r-8 border-r-transparent border-b-8",
  SOUTH: "top-full -mt-1 left-1/2 -translate-x-1/2 border-l-8 border-l-transparent border-r-8 border-r-transparent border-t-8",
  EAST: "left-full -ml-1 top-1/2 -translate-y-1/2 border-t-8 border-t-transparent border-b-8 border-b-transparent border-l-8",
  WEST: "right-full -mr-1 top-1/2 -translate-y-1/2 border-t-8 border-t-transparent border-b-8 border-b-transparent border-r-8"
}

export const WeisPointsBubble = ({points, tailDirection, verdict = 'none'}: {
  points: number
  tailDirection: Position
  verdict?: WeisVerdict
}) => {
  const styles = verdictStyles[verdict]
  return (
    <motion.div
      className="relative"
      initial={{opacity: 0, scale: 0.5}}
      animate={{opacity: 1, scale: 1}}
      exit={{opacity: 0, scale: 0.5}}
      transition={{duration: 0.3, ease: [0.4, 0, 0.2, 1]}}
    >
      <div className={cn(
        "px-3 py-1 rounded-full text-sm font-medium whitespace-nowrap relative transition-colors duration-500",
        styles.bubble
      )}>
        {points}
        <div className={cn(
          "absolute w-0 h-0 transition-colors duration-500",
          tailPlacement[tailDirection],
          styles.tail[tailDirection]
        )}/>
      </div>
    </motion.div>
  )
}
