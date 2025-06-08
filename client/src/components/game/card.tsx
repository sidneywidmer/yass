import {Card as C} from "@/api/generated";
import {cn} from "@/lib/utils.ts";

// TODO: Refactor, copy pasted
export const CARD_ASPECT_RATIO = 261.11 / 355.19
export const CARD_HEIGHT = 130
export const CARD_WIDTH = Math.ceil(CARD_HEIGHT * CARD_ASPECT_RATIO)

interface CardProps {
  card: C
  className?: string
  imgClassName?: string
}

export function Card({card, className, imgClassName}: CardProps) {
  return (
    <div className={cn(
      "w-full h-full rounded-lg bg-white shadow-md ring-1 ring-black/10 border border-gray-50",
      className
    )}>
      <img
        src={`/cards/french/${card.suit!!}-${card.rank!!}.svg`}
        alt={`${card.suit!!} ${card.rank!!}`}
        className={cn("w-full h-full", imgClassName)}
      />
    </div>
  )
}
