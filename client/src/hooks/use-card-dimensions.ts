import {useSettingsStore} from "@/store/settings.ts";
import {CardDeck} from "@/types/card-deck.ts";
import {getResponsiveValue} from "@/lib/utils.ts";
import {useGameStateStore} from "@/store/game-state.ts";

const SWISS_ASPECT_RATIO = 320 / 208;
const FRENCH_ASPECT_RATIO = 355.19 / 261.11;

export const CARD_WIDTH = getResponsiveValue(74, 105);

export const useCardDimensions = () => {
  const cardDeck = useSettingsStore(settings => settings.cardDeck);
  const cards = useGameStateStore(state => state.cards);

  // Check if this is a welcome hand (first hand with special welcome cards)
  // or if we have no cards to check against yet
  const isWelcomeHand = !cards || cards.length === 0 ||
    cards.some(card => card.suit === 'WELCOME' && card.rank === 'WELCOME');

  // Always use FRENCH_ASPECT_RATIO for welcome hand or no cards, otherwise use user's preference
  const aspectRatio = isWelcomeHand ? FRENCH_ASPECT_RATIO :
    (cardDeck === CardDeck.SWISS ? SWISS_ASPECT_RATIO : FRENCH_ASPECT_RATIO);

  const CARD_HEIGHT = Math.ceil(CARD_WIDTH * aspectRatio);

  return {
    CARD_WIDTH,
    CARD_HEIGHT,
    cardDeck,
    isWelcomeHand
  };
};