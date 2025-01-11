import {motion} from "framer-motion"
import {useState, useEffect} from "react"
import {useGameStateStore} from "@/store/game-state"
import {Heart, Spade, Diamond, Club, ArrowUp, ArrowDown, LucideIcon, Smile} from "lucide-react"
import {Trump} from "@/api/generated";

type TrumpIconConfig = {
  icon: LucideIcon
  className: string
}

const trumpIcons: Record<Trump, TrumpIconConfig> = {
  FREESTYLE: {
    icon: Smile,
    className: ""
  },
  HEARTS: {
    icon: Heart,
    className: "text-red-500 fill-red-500"
  },
  SPADES: {
    icon: Spade,
    className: "fill-foreground"
  },
  DIAMONDS: {
    icon: Diamond,
    className: "text-red-500 fill-red-500"
  },
  CLUBS: {
    icon: Club,
    className: "fill-foreground"
  },
  OBEABE: {
    icon: ArrowDown,
    className: "fill-foreground"
  },
  UNEUFE: {
    icon: ArrowUp,
    className: "fill-foreground"
  }
}

export function CurrentTrump() {
  const {trump} = useGameStateStore()
  const [key, setKey] = useState(0)

  useEffect(() => {
    setKey(prev => prev + 1)
  }, [trump])

  if (!trump || !trumpIcons[trump]) return null

  const {icon: IconComponent, className} = trumpIcons[trump]

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
      <IconComponent className={`w-6 h-6 ${className}`}/>
    </motion.div>
  )
}