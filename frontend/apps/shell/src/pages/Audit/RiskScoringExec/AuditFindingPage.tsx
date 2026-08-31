import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { AuditFindingTable } from "./AuditFindingTable";

/** "Phat hien kiem toan" - 1 muc rieng cua submenu "Cham Diem" (xem AuditFindingTable). */
export function AuditFindingPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecFindings")}</Typography.Title>
      <AuditFindingTable />
    </div>
  );
}
