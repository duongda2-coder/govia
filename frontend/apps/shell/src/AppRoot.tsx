import { App as AntApp, ConfigProvider } from "antd";
import { useTranslation } from "react-i18next";
import { ANTD_LOCALES } from "./i18n/antdLocale";
import { AuthProvider } from "./auth/AuthContext";
import App from "./App";

/**
 * Tach rieng khoi main.tsx vi ConfigProvider can re-render theo ngon ngu dang chon
 * (i18n.language) - phai la component, khong the dat truc tiep trong ham render tinh.
 */
export function AppRoot() {
  const { i18n } = useTranslation();
  const antdLocale = ANTD_LOCALES[i18n.resolvedLanguage ?? i18n.language] ?? ANTD_LOCALES.vi;

  return (
    <ConfigProvider
      locale={antdLocale}
      theme={{
        token: {
          colorPrimary: "#2563eb",
          colorInfo: "#2563eb",
          colorBgLayout: "#eff6ff",
          borderRadius: 8,
        },
      }}
    >
      {/* antd's App component cung cap context cho message/notification/modal - */}
      {/* moi man hinh (va govia-ui-kit) phai dung App.useApp() thay vi import message tinh */}
      <AntApp>
        <AuthProvider>
          <App />
        </AuthProvider>
      </AntApp>
    </ConfigProvider>
  );
}
