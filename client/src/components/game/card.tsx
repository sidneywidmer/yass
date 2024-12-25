import {Card as C} from "@/api/generated";

// TODO: Refactor, copy pasted
export const CARD_ASPECT_RATIO = 261.11 / 355.19
export const CARD_HEIGHT = 130
export const CARD_WIDTH = CARD_HEIGHT * CARD_ASPECT_RATIO

interface CardProps {
  card: C
}

export function Card({card}: CardProps) {
  return (
    <div className="w-full h-full rounded-lg bg-white shadow-md ring-1 ring-black/10 border border-gray-50">
      <img
        src={`/cards/french/${card.suit!!}-${card.rank!!}.svg`}
        alt={`${card.suit!!} ${card.rank!!}`}
        className="w-full h-full"
      />
    </div>
  )
}
