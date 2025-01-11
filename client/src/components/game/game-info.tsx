import {Card, CardContent} from "@/components/ui/card.tsx";
import {CurrentTrump} from "@/components/game/current-trump.tsx";
import Points from "@/components/game/points.tsx";

export function GameInfo() {
  return (
    <Card className="absolute top-4 left-1/2 -translate-x-1/2">
      <CardContent className="py-1">
        <div className="flex items-center justify-center gap-4">
          <Points team={"NORTH_SOUTH"}/>
          <CurrentTrump/>
          <Points team={"EAST_WEST"}/>
        </div>
      </CardContent>
    </Card>
  )
}