import { useState } from "react";
import { Segmented, Tabs, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { RiskCriteriaQuantitativeValueTable } from "./RiskCriteriaQuantitativeValueTable";
import { RiskCriteriaQuantitativeWideTable } from "./RiskCriteriaQuantitativeWideTable";
import { RiskCriteriaQualitativeValueTable } from "./RiskCriteriaQualitativeValueTable";

type QuantitativeViewMode = "wide" | "list";

/** Tab "Ho so rui ro dinh luong" co 2 che do xem: "wide" (bang tong hop, 1 dong/1 chi nhanh, tung
 * chi tieu 1 cot - dung dinh dang voi sheet DL_Nhaptructiep / mau DL_HSRR_Upload, mac dinh) va
 * "list" (danh sach chi tiet, 1 dong/1 chi tieu - RiskCriteriaQuantitativeValueTable, van giu de
 * Them/Sua/Xoa/Import/Export tung dong nhu truoc). */
function QuantitativeSection() {
  const { t } = useTranslation();
  const [mode, setMode] = useState<QuantitativeViewMode>("wide");

  return (
    <div>
      <Segmented
        style={{ marginBottom: 16 }}
        value={mode}
        onChange={(v) => setMode(v as QuantitativeViewMode)}
        options={[
          { label: t("riskScoringExec.hsrr.viewWide"), value: "wide" },
          { label: t("riskScoringExec.hsrr.viewList"), value: "list" },
        ]}
      />
      {mode === "wide" ? <RiskCriteriaQuantitativeWideTable /> : <RiskCriteriaQuantitativeValueTable />}
    </div>
  );
}

/** "Ho so rui ro" (sheet ZTC_HSRR) - 1 muc rieng cua submenu "Cham Diem", gom 2 tab upload:
 * Dinh luong (wide-format) va Dinh tinh (long-format). */
export function RiskCriteriaHsrrPage() {
  const { t } = useTranslation();

  return (
    <div>
      <Typography.Title level={4}>{t("menu.riskScoringExecHsrr")}</Typography.Title>
      <Tabs
        items={[
          { key: "quantitative", label: t("riskScoringExec.hsrr.tabQuantitative"), children: <QuantitativeSection /> },
          { key: "qualitative", label: t("riskScoringExec.hsrr.tabQualitative"), children: <RiskCriteriaQualitativeValueTable /> },
        ]}
      />
    </div>
  );
}
