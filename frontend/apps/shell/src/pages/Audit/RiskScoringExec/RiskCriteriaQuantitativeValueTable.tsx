import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Select, Space, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, type CrudColumn } from "@govia/ui-kit";
import { riskCriteriaQuantitativeValueApi, type RiskCriteriaQuantitativeValueItem } from "../../../api/riskScoringExec";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

/**
 * "Ho so rui ro dinh luong" (sheet ZTC_HSRR - nut "Upload HSRR dinh luong", mau DL_HSRR_Upload) -
 * upload file wide-format (1 dong = 1 chi nhanh/nam, tung cot la 1 ma chi tieu dinh luong). He
 * thong chi ghi nhung o thuoc chi tieu ma user dang upload duoc phan quyen (xem
 * RiskCriteriaQuantitativeValueService), nen so gia tri thuc te ghi duoc co the it hon so o co du
 * lieu trong file.
 */
export function RiskCriteriaQuantitativeValueTable() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canImport = hasPermission("AUDIT.RISK_SCORING_EXEC.IMPORT");

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [items, setItems] = useState<RiskCriteriaQuantitativeValueItem[]>([]);
  const [loading, setLoading] = useState(false);

  const yearOptions = useMemo(() => years.map((y) => ({ value: Number(y.code), label: y.code })), [years]);

  useEffect(() => {
    if (!canView) return;
    listMasterDataItems("YEAR")
      .then(setYears)
      .catch(() => setYears([]));
  }, [canView]);

  const load = useCallback(
    async (selectedYear: number) => {
      setLoading(true);
      try {
        setItems(await riskCriteriaQuantitativeValueApi.list(selectedYear));
      } catch {
        message.error(t("riskScoringExec.messages.loadError"));
      } finally {
        setLoading(false);
      }
    },
    [message, t],
  );

  useEffect(() => {
    if (canView && year != null) load(year);
  }, [canView, year, load]);

  const columns: CrudColumn<RiskCriteriaQuantitativeValueItem>[] = [
    { title: t("riskScoringExec.assessmentOther.year"), dataIndex: "year", width: 90 },
    { title: t("riskScoringExec.hsrr.branchCode"), dataIndex: "branchCode", width: 110 },
    { title: t("riskScoringExec.hsrr.branchName"), dataIndex: "branchName", render: (v: string | null) => v ?? "-" },
    { title: t("riskScoringExec.columns.criteriaCode"), dataIndex: "criteriaCode", width: 110, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoringExec.columns.criteriaName"), dataIndex: "criteriaName", render: (v: string | null) => v ?? "-" },
    { title: t("riskScoringExec.hsrr.value"), dataIndex: "value", width: 130, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoringExec.hsrr.entryDate"), dataIndex: "entryDate", width: 130, render: (v: string | null) => v ?? "-" },
  ];

  if (!canView) {
    return null;
  }

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Typography.Text>{t("riskScoringExec.assessmentOther.year")}</Typography.Text>
        <Select
          style={{ width: 160 }}
          options={yearOptions}
          value={year}
          onChange={setYear}
          showSearch
          placeholder={t("riskScoringExec.ranking.selectYear")}
        />
      </Space>
      <CrudTable<RiskCriteriaQuantitativeValueItem>
        tableId="riskScoringExec.hsrrQuantitative"
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onImport={
          canImport
            ? async (file) => {
                const result = await riskCriteriaQuantitativeValueApi.importExcel(file);
                if (year != null) await load(year);
                return result;
              }
            : undefined
        }
      />
    </div>
  );
}
