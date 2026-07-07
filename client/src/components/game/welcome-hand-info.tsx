import {useGameStateStore} from "@/store/game-state.ts";
import {useTranslation} from "react-i18next";

export function WelcomeHandInfo() {
  const isWelcomeHand = useGameStateStore(state => state.isWelcomeHand());
  const cardsPlayed = useGameStateStore(state => state.cardsPlayed);
  const {t} = useTranslation();

  const showMessage = !cardsPlayed || cardsPlayed.length === 0;

  if (!isWelcomeHand || !showMessage) {
    return null;
  }

  return (
    <div className="fixed top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 text-gray-400 text-center pointer-events-none max-w-[300px]">
      {t('welcomeHand.message')}
    </div>
  );
}
