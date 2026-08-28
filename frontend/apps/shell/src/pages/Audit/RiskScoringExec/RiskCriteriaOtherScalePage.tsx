import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { RiskCriteriaOtherScaleTable } from "./RiskCriteriaOtherScaleTable";

/** "Thang diem chi tieu DGRR khac" - 1 muc rieng cua submenu "Cham Diem". */
export function RiskCriteriaOtherScalePage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecCriteriaOtherScale")}</Typography.Title>
      <RiskCriteriaOtherScaleTable />
    </div>
  );
}
