import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { UserAssignmentTable } from "./UserAssignmentTable";

/** "Phan quyen chi tieu" - Phan quyen User theo chi tieu dinh luong (sheet ZTC_HSRR_DL_User). */
export function RiskScoringUserAssignmentPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringUserAssignment")}</Typography.Title>
      <UserAssignmentTable />
    </div>
  );
}
