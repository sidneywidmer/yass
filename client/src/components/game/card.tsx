import {Card as C} from "@/api/generated";
import {useSettingsStore} from "@/store/settings.ts";
import {CardDeck} from "@/types/card-deck.ts";
import {cn} from "@/lib/utils.ts";

interface CardProps {
  card: C,
  className?: string
}

const getCardImageSrc = (card: C, cardDeck: CardDeck): string => {
  if (card.suit === "WELCOME") {
    return `/cards/welcome/${card.suit!!}-${card.rank!!}.svg`;
  }

  if (cardDeck === CardDeck.SWISS) {
    return `/cards/swiss/${card.suit!!}-${card.rank!!}.png`;
  }

  return `/cards/french/${card.suit!!}-${card.rank!!}.svg`;
};

export function Card({card, className}: CardProps) {
  const {cardDeck} = useSettingsStore();
  const imageSrc = getCardImageSrc(card, cardDeck);

  return (
    <div className={cn(
      "w-full h-full rounded-lg bg-white shadow-md ring-1 ring-black/10 border border-gray-50",
      className
    )}>
      <img
        src={imageSrc}
        alt={`${card.suit!!} ${card.rank!!}`}
        className="w-full h-full"
        draggable={false}
      />
    </div>
  )
}
