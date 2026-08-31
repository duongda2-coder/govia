import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { RiskBranchScoreExpertRankTable } from "./RiskBranchScoreExpertRankTable";

/** "Xep hang rui ro chi nhanh theo y kien chuyen gia" (sheet ZTC_DGRR_cg) - 1 muc rieng cua submenu "Cham Diem". */
export function RiskBranchScoreExpertRankPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecBranchScoreExpertRank")}</Typography.Title>
      <RiskBranchScoreExpertRankTable />
    </div>
  );
}
