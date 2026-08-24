import { Tabs, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { WeightByBusinessTable } from "./WeightByBusinessTable";
import { WeightByBusinessSegmentTable } from "./WeightByBusinessSegmentTable";

/** "Ty trong DT/DL" - gom theo Nghiep vu (ZTC_DLDT_TT) va theo Mang nghiep vu (ZTC_DTDL_TT). */
export function RiskScoringWeightPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringWeight")}</Typography.Title>
      <Tabs
        items={[
          { key: "byBusiness", label: t("riskScoring.tabs.weightByBusiness"), children: <WeightByBusinessTable /> },
          { key: "bySegment", label: t("riskScoring.tabs.weightBySegment"), children: <WeightByBusinessSegmentTable /> },
        ]}
      />
    </div>
  );
}
