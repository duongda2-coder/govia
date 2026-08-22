import { Select } from "antd";
import { useTranslation } from "react-i18next";
import { SUPPORTED_LANGUAGES } from "../i18n";

/** Nut chon ngon ngu dung chung - dat o header sau khi dang nhap va o goc man hinh login. */
export function LanguageSwitcher() {
  const { i18n } = useTranslation();
  // resolvedLanguage la ma da chuan hoa ve dung 1 trong SUPPORTED_LANGUAGES (vd "en-US" -> "en");
  // i18n.language co the con giu nguyen ma trinh duyet phat hien, khong khop option nao.
  const currentLanguage = i18n.resolvedLanguage ?? i18n.language;

  return (
    <Select
      value={currentLanguage}
      onChange={(lng) => i18n.changeLanguage(lng)}
      size="small"
      style={{ width: 150 }}
      options={SUPPORTED_LANGUAGES.map((l) => ({
        value: l.code,
        label: (
          <span style={{ display: "inline-flex", alignItems: "center", gap: 8 }}>
            <img src={l.flag} alt="" width={20} height={15} style={{ borderRadius: 2, objectFit: "cover" }} />
            {l.label}
          </span>
        ),
      }))}
    />
  );
}
