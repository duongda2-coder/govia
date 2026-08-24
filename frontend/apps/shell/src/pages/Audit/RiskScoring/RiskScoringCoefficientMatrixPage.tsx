import { Tabs, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { FrequencyCoefficientTable } from "./FrequencyCoefficientTable";
import { MatrixTable } from "./MatrixTable";
import { RankTable } from "./RankTable";

/** "He so & Ma tran diem" - gom He so tan suat (ZTC_HSSP_DT), Ma tran rui ro (ztc_mtrr_dt), Thang diem (ztc_rank). */
export function RiskScoringCoefficientMatrixPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringCoefficientMatrix")}</Typography.Title>
      <Tabs
        items={[
          { key: "frequencyCoefficient", label: t("riskScoring.tabs.frequencyCoefficient"), children: <FrequencyCoefficientTable /> },
          { key: "matrix", label: t("riskScoring.tabs.matrix"), children: <MatrixTable /> },
          { key: "rank", label: t("riskScoring.tabs.rank"), children: <RankTable /> },
        ]}
      />
    </div>
  );
}
