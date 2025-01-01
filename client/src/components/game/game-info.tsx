import {useGameStateStore} from "@/store/game-state.ts";
import {Card, CardContent} from "@/components/ui/card.tsx";
import {Shield, Users} from "lucide-react";

export function GameInfo() {
  const {code, trump, state, activePosition, position} = useGameStateStore()

  return (
    <Card className="absolute top-4 left-1/2 -translate-x-1/2 w-full max-w-2xl">
      <CardContent className="py-4">
        <div className="flex items-center justify-center gap-4">
          <div className="flex items-center gap-2">
            <Users size={18} className="text-muted-foreground"/>
            <span className="text-sm">Game: {code}</span>
          </div>
          <div className="h-4 w-px bg-border"/>
          <div className="flex items-center gap-2">
            <span className="text-sm">abs. Pos.: {position}</span>
          </div>
          <div className="h-4 w-px bg-border"/>
          <div className="flex items-center gap-2">
            <span className="text-sm">{state}, {activePosition}</span>
          </div>

          {trump && (
            <>
              <div className="h-4 w-px bg-border"/>
              <div className="flex items-center gap-2">
                <Shield size={18} className="text-muted-foreground"/>
                <span className="text-sm">
                 {trump}
               </span>
              </div>
            </>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
