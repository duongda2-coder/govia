import { Tabs, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { RiskCriteriaQuantitativeValueTable } from "./RiskCriteriaQuantitativeValueTable";
import { RiskCriteriaQualitativeValueTable } from "./RiskCriteriaQualitativeValueTable";

/** "Ho so rui ro" (sheet ZTC_HSRR) - 1 muc rieng cua submenu "Cham Diem", gom 2 tab upload:
 * Dinh luong (wide-format) va Dinh tinh (long-format). */
export function RiskCriteriaHsrrPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecHsrr")}</Typography.Title>
      <Tabs
        items={[
          { key: "quantitative", label: t("riskScoringExec.hsrr.tabQuantitative"), children: <RiskCriteriaQuantitativeValueTable /> },
          { key: "qualitative", label: t("riskScoringExec.hsrr.tabQualitative"), children: <RiskCriteriaQualitativeValueTable /> },
        ]}
      />
    </div>
  );
}
