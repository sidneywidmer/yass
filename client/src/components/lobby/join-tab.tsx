import {RunningGames} from "@/components/lobby/running-games.tsx"
import {JoinSection} from "@/components/lobby/join-section.tsx"

export function JoinTab() {
  return (
    <div className="flex flex-col gap-4">
      <RunningGames/>
      <JoinSection/>
    </div>
  )
}
