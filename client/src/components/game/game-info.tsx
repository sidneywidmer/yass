import {useGameStateStore} from "@/store/game-state.ts";
import {Card, CardContent} from "@/components/ui/card.tsx";
import {Shield, Users} from "lucide-react";

export function GameInfo() {
  const {code, seat} = useGameStateStore()

  return (
    <Card className="absolute top-4 left-1/2 -translate-x-1/2 w-72 sm:w-80 md:w-96">
      <CardContent className="py-4">
        <div className="flex items-center justify-center gap-4">
          <div className="flex items-center gap-2">
            <Users size={18} className="text-muted-foreground"/>
            <span className="text-sm">Game: {code}</span>
          </div>

          {seat?.trump && (
            <>
              <div className="h-4 w-px bg-border"/>
              <div className="flex items-center gap-2">
                <Shield size={18} className="text-muted-foreground"/>
                <span className="text-sm">
                 {seat.trump.toLowerCase()}
               </span>
              </div>
            </>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
