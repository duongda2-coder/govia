import type { Locale } from "antd/es/locale";
import enUS from "antd/locale/en_US";
import viVN from "antd/locale/vi_VN";
import deDE from "antd/locale/de_DE";
import zhCN from "antd/locale/zh_CN";

/** Map ngon ngu he thong (i18next) sang locale pack cua antd (dinh dang ngay/gio, text noi bo cua Table/Pagination...). */
export const ANTD_LOCALES: Record<string, Locale> = {
  vi: viVN,
  en: enUS,
  de: deDE,
  zh: zhCN,
};
