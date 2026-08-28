import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { RiskAssessmentOtherTable } from "./RiskAssessmentOtherTable";

/** "Cham diem rui ro HO, CNTT, Du an, Dich vu thue ngoai..." - 1 muc rieng cua submenu "Cham Diem". */
export function RiskAssessmentOtherPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecAssessmentOther")}</Typography.Title>
      <RiskAssessmentOtherTable />
    </div>
  );
}
