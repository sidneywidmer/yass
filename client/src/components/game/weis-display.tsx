import {Card as CardComponent} from "@/components/game/card";
import {Card as C, WeisWithPoints} from "@/api/generated";
import {useCardDimensions} from "@/hooks/use-card-dimensions";
import {useTranslation} from "react-i18next";

// Cut off cards to safe some space
const FADE_MASK = "linear-gradient(to bottom, black 60%, transparent 100%)"

function WeisCards({cards}: { cards: C[] }) {
  const {CARD_HEIGHT} = useCardDimensions();

  const smallCardHeight = CARD_HEIGHT * 0.9;
  const smallCardWidth = smallCardHeight * (2 / 3);
  const overlapDistance = smallCardWidth * 0.35;
  const containerWidth = smallCardWidth + (cards.length - 1) * overlapDistance;
  const visibleHeight = smallCardHeight * 0.5;

  return (
    <div className="relative overflow-hidden"
         style={{
           height: `${visibleHeight}px`,
           width: `${containerWidth}px`,
           maskImage: FADE_MASK,
           WebkitMaskImage: FADE_MASK
         }}>
      {cards.map((card, index) => (
        <div
          key={index}
          className="absolute top-0"
          style={{
            width: `${smallCardWidth}px`,
            height: `${smallCardHeight}px`,
            left: `${index * overlapDistance}px`,
            zIndex: index,
          }}
        >
          <CardComponent card={card} className="border border-gray-300"/>
        </div>
      ))}
    </div>
  );
}

export function WeisDisplay({weis}: { weis: WeisWithPoints }) {
  const {t} = useTranslation()

  return (
    <div className="flex flex-col items-center gap-3">
      <WeisCards cards={weis.cards ?? []}/>
      <span className="text-xs text-muted-foreground">
        {t(`weise.${weis.type}`)} &middot; {weis.points} {t("weisen.points")}
      </span>
    </div>
  )
}
