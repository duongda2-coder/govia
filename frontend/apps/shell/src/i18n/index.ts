import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import en from "./locales/en.json";
import vi from "./locales/vi.json";
import de from "./locales/de.json";
import zh from "./locales/zh.json";
// Import truc tiep tung file SVG can dung (khong import ca bo CSS cua flag-icons) -
// bo CSS day du keo theo SVG cua ~250 quoc gia khong dung toi, lam phinh bundle.
import flagVn from "flag-icons/flags/4x3/vn.svg";
import flagGb from "flag-icons/flags/4x3/gb.svg";
import flagDe from "flag-icons/flags/4x3/de.svg";
import flagCn from "flag-icons/flags/4x3/cn.svg";

/**
 * Danh sach ngon ngu he thong ho tro. Them ngon ngu moi: tao file locales/<code>.json
 * (sao chep tu en.json roi dich), them vao "resources" ben duoi, va them 1 dong vao day
 * (import them SVG co tuong ung tu flag-icons/flags/4x3/<code>.svg).
 */
export const SUPPORTED_LANGUAGES = [
  { code: "vi", label: "Tiếng Việt", flag: flagVn },
  { code: "en", label: "English", flag: flagGb },
  { code: "de", label: "Deutsch", flag: flagDe },
  { code: "zh", label: "中文", flag: flagCn },
] as const;

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      vi: { translation: vi },
      en: { translation: en },
      de: { translation: de },
      zh: { translation: zh },
    },
    fallbackLng: "vi",
    supportedLngs: SUPPORTED_LANGUAGES.map((l) => l.code),
    load: "languageOnly",
    interpolation: { escapeValue: false },
    detection: {
      order: ["localStorage", "navigator"],
      caches: ["localStorage"],
      lookupLocalStorage: "govia.language",
    },
  });

export default i18n;
