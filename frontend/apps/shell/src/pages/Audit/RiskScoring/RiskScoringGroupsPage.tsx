import { Tabs, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { Group1Table } from "./Group1Table";
import { Group2Table } from "./Group2Table";

/** "Nhom chi tieu" - gom Nhom cap 1 (ZTC_DGRR_Group1) va Nhom cap 2 (ZTC_DGRR_Group2). */
export function RiskScoringGroupsPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringGroups")}</Typography.Title>
      <Tabs
        items={[
          { key: "group1", label: t("riskScoring.tabs.group1"), children: <Group1Table /> },
          { key: "group2", label: t("riskScoring.tabs.group2"), children: <Group2Table /> },
        ]}
      />
    </div>
  );
}
