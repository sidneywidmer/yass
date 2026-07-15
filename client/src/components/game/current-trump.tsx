import {motion} from "motion/react"
import {useGameStateStore} from "@/store/game-state"
import {TrumpIcon} from "@/components/game/trump-icon"
import {TrumpMultiplierBadge, TrumpMultiplierBadgeVariant} from "@/components/game/trump-multiplier-badge"

export function CurrentTrump() {
  const trump = useGameStateStore(state => state.trump)

  if (!trump) return null

  return (
    <motion.div
      key={trump}
      initial={{scale: 0.5}}
      animate={{scale: 1}}
      transition={{
        type: "spring",
        stiffness: 260,
        damping: 20
      }}
      className="w-10 h-10 flex items-center justify-center bg-white relative"
    >
      <TrumpIcon trump={trump} className={"w-6 h-6"} />
      <TrumpMultiplierBadge trump={trump} variant={TrumpMultiplierBadgeVariant.SUBTLE}/>
    </motion.div>
  )
}