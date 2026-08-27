import { Typography } from "antd";
import { useTranslation } from "react-i18next";
import { WeightByBusinessSegmentTable } from "./WeightByBusinessSegmentTable";

/** "Ty trong DT/DL" theo Mang nghiep vu (ZTC_DTDL_TT). Tab "Theo nghiep vu" (ZTC_DLDT_TT,
 * WeightByBusinessTable) da bi go bo vi trung lap voi tab nay - xem git history neu can khoi phuc. */
export function RiskScoringWeightPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringWeight")}</Typography.Title>
      <WeightByBusinessSegmentTable />
    </div>
  );
}
