import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { RiskCriteriaOtherTable } from "./RiskCriteriaOtherTable";

/** "Chi tieu DGRR khac" (HO/CNTT/DA/DVTN) - 1 muc rieng cua submenu "Cham Diem". */
export function RiskCriteriaOtherPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecCriteriaOther")}</Typography.Title>
      <RiskCriteriaOtherTable />
    </div>
  );
}
