import { Tabs, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { CriteriaQualitativeTable } from "./CriteriaQualitativeTable";
import { CriteriaQuantitativeTable } from "./CriteriaQuantitativeTable";

/** "Chi tieu cham diem" - gom Dinh tinh (ZTC_CTDGRR_DT) va Dinh luong (ZTC_CTDGRR_DL). */
export function RiskScoringCriteriaPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringCriteria")}</Typography.Title>
      <Tabs
        items={[
          { key: "qualitative", label: t("riskScoring.tabs.qualitative"), children: <CriteriaQualitativeTable /> },
          { key: "quantitative", label: t("riskScoring.tabs.quantitative"), children: <CriteriaQuantitativeTable /> },
        ]}
      />
    </div>
  );
}
