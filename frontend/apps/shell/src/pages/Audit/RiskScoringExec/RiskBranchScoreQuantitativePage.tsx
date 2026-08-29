import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { RiskBranchScoreQuantitativeTable } from "./RiskBranchScoreQuantitativeTable";

/** "Ket qua cham diem rui ro dinh luong" (sheet CT_Diem_DL) - 1 muc rieng cua submenu "Cham Diem". */
export function RiskBranchScoreQuantitativePage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecBranchScoreQuantitative")}</Typography.Title>
      <RiskBranchScoreQuantitativeTable />
    </div>
  );
}
