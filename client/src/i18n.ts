import i18n from 'i18next';
import {initReactI18next} from 'react-i18next';
import enTranslations from '@/i18n/en.json';
import deTranslations from '@/i18n/de.json';

const storedLang = localStorage.getItem('language-storage');
const initialLang = storedLang ? JSON.parse(storedLang).state.language : 'de';

i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: {translation: enTranslations},
      de: {translation: deTranslations}
    },
    lng: initialLang,
    fallbackLng: 'de',
    interpolation: {
      escapeValue: false
    }
  });

export default i18n;