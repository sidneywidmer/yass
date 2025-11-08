import {useSettingsStore} from "@/store/settings.ts";
import {CardDeck} from "@/types/card-deck.ts";
import {getResponsiveValue} from "@/lib/utils.ts";

const SWISS_ASPECT_RATIO = 320 / 208;
const FRENCH_ASPECT_RATIO = 355.19 / 261.11;

export const CARD_WIDTH = getResponsiveValue(74, 110);

export const useCardDimensions = () => {
  const cardDeck = useSettingsStore(settings => settings.cardDeck);

  const aspectRatio = cardDeck === CardDeck.SWISS ? SWISS_ASPECT_RATIO : FRENCH_ASPECT_RATIO;
  const CARD_HEIGHT = Math.ceil(CARD_WIDTH * aspectRatio);

  return {
    CARD_WIDTH,
    CARD_HEIGHT,
    cardDeck
  };
};