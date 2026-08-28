import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { RiskTypeHOTable } from "./RiskTypeHOTable";

/** "Loai rui ro HO" - 1 muc rieng cua submenu "Cham Diem", song song "Nhom rui ro HO". */
export function RiskTypeHOPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecRiskTypeHO")}</Typography.Title>
      <RiskTypeHOTable />
    </div>
  );
}
