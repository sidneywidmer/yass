import {motion} from "framer-motion"
import {useEffect, useState} from "react"
import {useGameStateStore} from "@/store/game-state"
import {TrumpIcon} from "@/components/game/trump-icon"

export function CurrentTrump() {
  const trump = useGameStateStore(state => state.trump)
  const [key, setKey] = useState(0)

  useEffect(() => {
    setKey(prev => prev + 1)
  }, [trump])

  if (!trump) return null

  return (
    <motion.div
      key={key}
      initial={{scale: 0.5}}
      animate={{scale: 1}}
      transition={{
        type: "spring",
        stiffness: 260,
        damping: 20
      }}
      className="w-10 h-10 flex items-center justify-center bg-white"
    >
      <TrumpIcon trump={trump} className={"w-6 h-6"} />
    </motion.div>
  )
}