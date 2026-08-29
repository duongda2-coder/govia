import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Select, Space, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn, type CrudColumn } from "@govia/ui-kit";
import { riskCriteriaQualitativeValueApi, type RiskCriteriaQualitativeValueItem } from "../../../api/riskScoringExec";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

/**
 * "Ho so rui ro dinh tinh" (sheet ZTC_HSRR - nut "Upload HSRR dinh tinh", mau DT_HSRR_Upload) -
 * upload file long-format (1 dong = 1 chi tieu/chi nhanh/nam), xem RiskCriteriaQualitativeValueService.
 */
export function RiskCriteriaQualitativeValueTable() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canImport = hasPermission("AUDIT.RISK_SCORING_EXEC.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<RiskCriteriaQualitativeValueItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [items, setItems] = useState<RiskCriteriaQualitativeValueItem[]>([]);
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
        setItems(await riskCriteriaQualitativeValueApi.list(selectedYear));
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

  const columns: CrudColumn<RiskCriteriaQualitativeValueItem>[] = [
    { title: t("riskScoringExec.assessmentOther.year"), dataIndex: "year", width: 90, sorter: (a, b) => a.year - b.year },
    { title: t("riskScoringExec.hsrr.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    {
      title: t("riskScoringExec.hsrr.branchName"),
      ...getSearchColumnProps("branchName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.columns.criteriaCode"),
      width: 110,
      ...getSearchColumnProps("criteriaCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.columns.criteriaName"),
      ...getSearchColumnProps("criteriaName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.hsrr.violation"),
      width: 110,
      ...getSearchColumnProps("violation", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("riskScoringExec.hsrr.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
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
      <CrudTable<RiskCriteriaQualitativeValueItem>
        tableId="riskScoringExec.hsrrQualitative"
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onImport={
          canImport
            ? async (file) => {
                const result = await riskCriteriaQualitativeValueApi.importExcel(file);
                if (year != null) await load(year);
                return result;
              }
            : undefined
        }
      />
    </div>
  );
}
