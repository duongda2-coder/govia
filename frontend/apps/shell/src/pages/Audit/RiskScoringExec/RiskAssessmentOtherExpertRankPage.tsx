import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { RiskAssessmentOtherExpertRankTable } from "./RiskAssessmentOtherExpertRankTable";

/** "Xep hang rui ro theo y kien chuyen gia cua DTKT khac" - 1 muc rieng cua submenu "Cham Diem". */
export function RiskAssessmentOtherExpertRankPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecAssessmentOtherExpertRank")}</Typography.Title>
      <RiskAssessmentOtherExpertRankTable />
    </div>
  );
}
