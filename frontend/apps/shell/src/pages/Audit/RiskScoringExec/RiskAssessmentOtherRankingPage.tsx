import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { RiskAssessmentOtherRankingTable } from "./RiskAssessmentOtherRankingTable";

/** "Bang xep hang cham diem rui ro khac" - 1 muc rieng cua submenu "Cham Diem". */
export function RiskAssessmentOtherRankingPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecAssessmentOtherRanking")}</Typography.Title>
      <RiskAssessmentOtherRankingTable />
    </div>
  );
}
