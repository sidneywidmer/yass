import {useSettingsStore} from "@/store/settings"
import {Trump} from "@/api/generated";

type TrumpConfig = {
  fileName: string
}

const trumpConfigs: Record<Trump, TrumpConfig> = {
  FREESTYLE: {fileName: "FREESTYLE.svg",},
  HEARTS: {fileName: "HEARTS.svg",},
  SPADES: {fileName: "SPADES.svg",},
  DIAMONDS: {fileName: "DIAMONDS.svg",},
  CLUBS: {fileName: "CLUBS.svg",},
  OBEABE: {fileName: "OBEABE.svg",},
  UNEUFE: {fileName: "UNEUFE.svg",}
}

interface TrumpIconProps {
  trump: Trump
  className?: string
}

export function TrumpIcon({trump, className = "w-8 h-8"}: TrumpIconProps) {
  const cardDeck = useSettingsStore(settings => settings.cardDeck)

  if (!trumpConfigs[trump]) return null

  const {fileName} = trumpConfigs[trump]
  const imageSrc = `/trumps/${cardDeck}/${fileName}`

  return (
    <img
      src={imageSrc}
      alt={trump}
      className={className}
      draggable={false}
    />
  )
}