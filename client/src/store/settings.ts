import {create} from 'zustand';
import i18n from "@/i18n.ts";
import {createJSONStorage, persist} from 'zustand/middleware';
import {SupportedLanguage} from "@/types/language.ts";
import {CardDeck} from "@/types/card-deck.ts";
import {PlaySpeed} from "@/types/play-speed.ts";

interface SettingsState {
  language: SupportedLanguage;
  cardDeck: CardDeck;
  playSpeed: PlaySpeed;
  setLanguage: (lang: SupportedLanguage) => void;
  setCardDeck: (deck: CardDeck) => void;
  setPlaySpeed: (speed: PlaySpeed) => void;
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      language: SupportedLanguage.DE as SupportedLanguage,
      cardDeck: CardDeck.SWISS as CardDeck,
      playSpeed: PlaySpeed.MEDIUM as PlaySpeed,
      setLanguage: (lang) => {
        i18n.changeLanguage(lang);
        set({language: lang});
      },
      setCardDeck: (deck) => {
        set({cardDeck: deck});
      },
      setPlaySpeed: (speed) => {
        set({playSpeed: speed});
      },
    }),
    {
      name: 'settings-storage',
      storage: createJSONStorage(() => localStorage),
      onRehydrateStorage: (_: SettingsState) => {
        return (state?: SettingsState | undefined, _?: unknown) => {
          if (state?.language) {
            i18n.changeLanguage(state?.language);
          }
        }
      },
    }
  )
);