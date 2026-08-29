import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { RiskBranchScoreCombinedTable } from "./RiskBranchScoreCombinedTable";

/** "Ket qua cham diem tong hop dinh tinh dinh luong" (sheet CT_Diem_All) - 1 muc rieng cua submenu "Cham Diem". */
export function RiskBranchScoreCombinedPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecBranchScoreCombined")}</Typography.Title>
      <RiskBranchScoreCombinedTable />
    </div>
  );
}
