import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Select, Space, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn, type CrudColumn } from "@govia/ui-kit";
import { riskBranchScoreApi, type RiskBranchScoreCombinedRowItem } from "../../../api/riskScoringExec";
import { group1Api, weightByBusinessApi, type Group1Item, type WeightByBusinessItem } from "../../../api/riskScoring";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

/**
 * "Ket qua cham diem tong hop dinh tinh dinh luong theo tung chi nhanh" (sheet CT_Diem_All) - man
 * hinh CHI XEM, khong co them/sua/xoa. Cot phang theo tung nghiep vu (khong nhom 2 tang nhu man
 * dinh luong/dinh tinh) - xem RiskBranchScoreCombinedService.
 */
export function RiskBranchScoreCombinedTable() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const { getSearchColumnProps } = useClientSearchColumn<RiskBranchScoreCombinedRowItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [rows, setRows] = useState<RiskBranchScoreCombinedRowItem[]>([]);
  const [group1List, setGroup1List] = useState<Group1Item[]>([]);
  const [weightList, setWeightList] = useState<WeightByBusinessItem[]>([]);
  const [loading, setLoading] = useState(false);

  const yearOptions = useMemo(() => years.map((y) => ({ value: Number(y.code), label: y.code })), [years]);

  useEffect(() => {
    if (!canView) return;
    listMasterDataItems("YEAR")
      .then(setYears)
      .catch(() => setYears([]));
    group1Api
      .list()
      .then(setGroup1List)
      .catch(() => setGroup1List([]));
    weightByBusinessApi
      .list()
      .then(setWeightList)
      .catch(() => setWeightList([]));
  }, [canView]);

  const load = useCallback(
    async (selectedYear: number) => {
      setLoading(true);
      try {
        setRows(await riskBranchScoreApi.listCombined(selectedYear));
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

  const columns: CrudColumn<RiskBranchScoreCombinedRowItem>[] = useMemo(() => {
    const fixed: CrudColumn<RiskBranchScoreCombinedRowItem>[] = [
      { title: t("riskScoringExec.assessmentOther.year"), dataIndex: "year", width: 90, sorter: (a, b) => a.year - b.year },
      { title: t("riskScoringExec.hsrr.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
      {
        title: t("riskScoringExec.hsrr.branchName"),
        width: 200,
        ...getSearchColumnProps("branchName", searchLabels),
        render: (v: string | null) => v ?? "-",
      },
      {
        title: t("riskScoringExec.ranking.riskScore"),
        dataIndex: "totalScore",
        width: 110,
        sorter: (a, b) => a.totalScore - b.totalScore,
        defaultSortOrder: "descend",
      },
      {
        title: t("riskScoringExec.ranking.rankLabel"),
        width: 110,
        ...getSearchColumnProps("rankLabel", searchLabels),
        render: (v: string | null) => v ?? "-",
      },
    ];

    const businessLineCodes = new Set<string>();
    for (const g of group1List) businessLineCodes.add((g.businessLineCode || g.code).trim());
    for (const w of weightList) businessLineCodes.add(w.businessCode.trim());

    const dynamic: CrudColumn<RiskBranchScoreCombinedRowItem>[] = [...businessLineCodes]
      .sort((a, b) => a.localeCompare(b))
      .map((code) => ({
        title: code,
        width: 110,
        render: (_: unknown, record: RiskBranchScoreCombinedRowItem) => record.scoresByBusinessLineCode[code] ?? "-",
      }));

    return [...fixed, ...dynamic];
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [group1List, weightList, t]);

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
      <CrudTable<RiskBranchScoreCombinedRowItem>
        tableId="riskScoringExec.branchScoreCombined"
        columns={columns}
        dataSource={rows}
        rowKey="branchCode"
        loading={loading}
      />
    </div>
  );
}
