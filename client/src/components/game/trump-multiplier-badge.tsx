import {Trump} from "@/api/generated"
import {cn} from "@/lib/utils"

// Mirrors multiplyByTrump on the server
const trumpMultipliers: Partial<Record<Trump, number>> = {
  SPADES: 2,
  CLUBS: 2,
  OBEABE: 3,
  UNEUFE: 3
}

// eslint-disable-next-line react-refresh/only-export-components
export enum TrumpMultiplierBadgeVariant {
  BADGE = "BADGE",
  SUBTLE = "SUBTLE"
}

const variantStyles: Record<TrumpMultiplierBadgeVariant, string> = {
  [TrumpMultiplierBadgeVariant.BADGE]: "-top-1.5 -right-1.5 rounded-full bg-primary text-primary-foreground font-semibold px-1 py-0.5",
  [TrumpMultiplierBadgeVariant.SUBTLE]: "bottom-1 right-0 font-medium text-muted-foreground"
}

// Renders into a corner of the nearest relative-positioned parent
export function TrumpMultiplierBadge({trump, variant = TrumpMultiplierBadgeVariant.BADGE}: {
  trump: Trump
  variant?: TrumpMultiplierBadgeVariant
}) {
  const multiplier = trumpMultipliers[trump]

  if (!multiplier) return null

  return (
    <span className={cn("absolute text-[10px] leading-none pointer-events-none", variantStyles[variant])}>
      ×{multiplier}
    </span>
  )
}
