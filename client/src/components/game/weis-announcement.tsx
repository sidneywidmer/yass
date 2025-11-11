import { Card as CardComponent } from "@/components/game/card";
import { Card } from "@/components/ui/card";
import { Card as C } from "@/api/generated";
import { useCardDimensions } from "@/hooks/use-card-dimensions";

export function WeisAnnouncement() {
  const { CARD_HEIGHT } = useCardDimensions();

  // Hardcoded cards for now
  const weisCards: C[] = [
    { suit: "HEARTS", rank: "ACE", skin: "default" },
    { suit: "DIAMONDS", rank: "KING", skin: "default" },
    { suit: "CLUBS", rank: "QUEEN", skin: "default" },
    { suit: "SPADES", rank: "JACK", skin: "default" },
  ];

  const smallCardHeight = CARD_HEIGHT * 0.9;
  const smallCardWidth = smallCardHeight * (2/3);
  const overlapDistance = smallCardWidth * 0.35;
  const containerWidth = smallCardWidth + (weisCards.length - 1) * overlapDistance;

  return (
    <div className="fixed inset-0 flex items-center justify-center pointer-events-none z-40">
      <Card className="bg-white p-6 rounded-lg">
        {/* Text announcement */}
        <div className="text-center mb-4">
          <p className="text-xs font-medium">Player XYZ weises 140 points!</p>
        </div>

        {/* Cards container with overlapping effect */}
        <div className="relative overflow-hidden" style={{height: `${smallCardHeight}px`, width: `${containerWidth}px`}}>
          {weisCards.map((card, index) => (
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
              <CardComponent card={card} />
            </div>
          ))}
          {/* Gradient fade out at bottom */}
          <div className="absolute bottom-0 left-0 pointer-events-none bg-gradient-to-b from-transparent via-transparent to-white" style={{width: `${containerWidth}px`, height: `${smallCardHeight}px`, zIndex: 1000}} />
        </div>
      </Card>
    </div>
  );
}
