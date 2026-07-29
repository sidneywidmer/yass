import {useEffect, useState} from "react"

const format = () => {
  const now = new Date()
  const midnight = new Date(now)
  midnight.setHours(24, 0, 0, 0)

  const seconds = Math.max(0, Math.floor((midnight.getTime() - now.getTime()) / 1000))
  const parts = [Math.floor(seconds / 3600), Math.floor(seconds / 60) % 60, seconds % 60]
  return parts.map(part => String(part).padStart(2, "0")).join(":")
}

// Time left until the next local midnight as HH:MM:SS. Players are assumed to be in Swiss time,
// which is what the server resets on.
export function useMidnightCountdown() {
  const [remaining, setRemaining] = useState(format)

  useEffect(() => {
    const interval = setInterval(() => setRemaining(format()), 1000)
    return () => clearInterval(interval)
  }, [])

  return remaining
}
