import { Tabs, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { GroupHOTable } from "./GroupHOTable";

/**
 * Trang goc cua sub-module "Cham Diem" (song song Master Data CDRR) - xu ly tung sheet cua tai lieu
 * "2. Cham diem.xlsx". Bat dau voi 1 tab (Nhom rui ro HO), se them tab moi khi xu ly them sheet.
 */
export function RiskScoringExecPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExec")}</Typography.Title>
      <Tabs items={[{ key: "group-ho", label: t("riskScoringExec.tabs.groupHO"), children: <GroupHOTable /> }]} />
    </div>
  );
}
