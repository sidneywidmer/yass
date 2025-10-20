import CodeWithCopy from "@/components/code-with-copy.tsx";
import QRCode from "react-qr-code";
import {useGameStateStore} from "@/store/game-state.ts";

export function Share() {
  const otherPlayers = useGameStateStore(state => state.otherPlayers);
  const code = useGameStateStore(state => state.code);
  if (otherPlayers && otherPlayers.length < 4) {
    return
  }

  return (
    <div className="absolute inset-0 flex items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <CodeWithCopy code={code!!} copyFullUrl={true}/>
        <div className="bg-white p-4 rounded-lg">
          <QRCode value={`${window.location.origin}/game/${code}`} size={100}/>
        </div>
      </div>
    </div>
  )
}
