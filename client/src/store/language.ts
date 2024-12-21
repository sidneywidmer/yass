import {create} from 'zustand';
import i18n from "@/i18n.ts";
import {createJSONStorage, persist} from 'zustand/middleware';
import {SupportedLanguage} from "@/types/language.ts";


interface LanguageState {
  language: SupportedLanguage;
  setLanguage: (lang: SupportedLanguage) => void;
}

export const useLangStore = create<LanguageState>()(
  persist(
    (set) => ({
      language: SupportedLanguage.DE,
      setLanguage: (lang) => {
        i18n.changeLanguage(lang);
        set({language: lang});
      },
    }),
    {
      name: 'language-storage',
      storage: createJSONStorage(() => localStorage),
    }
  )
);
// export const useLangStore = create<LanguageState>((set) => ({
//   language: 'de',
//   setLanguage: (lang) => {
//     i18n.changeLanguage(lang);
//     set({language: lang});
//   }
// }));