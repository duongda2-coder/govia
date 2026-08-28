import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { GroupHOTable } from "./GroupHOTable";

/** "Nhom rui ro HO" - 1 muc rieng cua submenu "Cham Diem" (song song Master Data CDRR). Them muc
 * moi vao day (thay vi tab) khi xu ly them sheet cua tai lieu "2. Cham diem.xlsx". */
export function GroupHOPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecGroupHO")}</Typography.Title>
      <GroupHOTable />
    </div>
  );
}
