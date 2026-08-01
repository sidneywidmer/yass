import {DailySection} from "@/components/lobby/daily-section.tsx"
import {DailyLeaderboard} from "@/components/lobby/daily-leaderboard.tsx"

export function DailyTab() {
  return (
    <div className="flex flex-col gap-4">
      <DailySection/>
      <DailyLeaderboard/>
    </div>
  )
}
