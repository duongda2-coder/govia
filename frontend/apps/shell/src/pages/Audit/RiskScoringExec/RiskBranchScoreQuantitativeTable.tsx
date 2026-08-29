import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Select, Space, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn, type CrudColumn } from "@govia/ui-kit";
import { riskBranchScoreApi, type RiskBranchScoreQuantitativeRowItem } from "../../../api/riskScoringExec";
import { criteriaQuantitativeApi, group1Api, type CriteriaQuantitativeItem, type Group1Item } from "../../../api/riskScoring";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

/**
 * "Ket qua cham diem rui ro dinh luong" theo chi nhanh/nam (sheet CT_Diem_DL) - man hinh CHI XEM,
 * khong co them/sua/xoa. Cot dong theo nhom chi tieu cap 1 (header tang 1) -> ma chi tieu (header
 * tang 2), gia tri moi o la diem dong gop cua chi tieu do (khong phai gia tri HSRR tho) - xem
 * RiskBranchScoreQuantitativeService.
 */
export function RiskBranchScoreQuantitativeTable() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canExport = hasPermission("AUDIT.RISK_SCORING_EXEC.EXPORT");
  const { getSearchColumnProps } = useClientSearchColumn<RiskBranchScoreQuantitativeRowItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [rows, setRows] = useState<RiskBranchScoreQuantitativeRowItem[]>([]);
  const [criteriaList, setCriteriaList] = useState<CriteriaQuantitativeItem[]>([]);
  const [group1List, setGroup1List] = useState<Group1Item[]>([]);
  const [loading, setLoading] = useState(false);

  const yearOptions = useMemo(() => years.map((y) => ({ value: Number(y.code), label: y.code })), [years]);

  useEffect(() => {
    if (!canView) return;
    listMasterDataItems("YEAR")
      .then(setYears)
      .catch(() => setYears([]));
    criteriaQuantitativeApi
      .list()
      .then(setCriteriaList)
      .catch(() => setCriteriaList([]));
    group1Api
      .list()
      .then(setGroup1List)
      .catch(() => setGroup1List([]));
  }, [canView]);

  const load = useCallback(
    async (selectedYear: number) => {
      setLoading(true);
      try {
        setRows(await riskBranchScoreApi.listQuantitative(selectedYear));
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

  const handleExport = (kind: "excel" | "word") => {
    if (year == null) {
      message.warning(t("riskScoringExec.hsrr.selectYearFirst"));
      return Promise.resolve();
    }
    return riskBranchScoreApi.exportQuantitativeFile(year, kind);
  };

  const columns: CrudColumn<RiskBranchScoreQuantitativeRowItem>[] = useMemo(() => {
    const fixed: CrudColumn<RiskBranchScoreQuantitativeRowItem>[] = [
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

    const group1ById = new Map(group1List.map((g) => [g.id, g]));
    const criteriaByGroup1 = new Map<string, CriteriaQuantitativeItem[]>();
    for (const c of criteriaList) {
      if (!criteriaByGroup1.has(c.group1Id)) criteriaByGroup1.set(c.group1Id, []);
      criteriaByGroup1.get(c.group1Id)?.push(c);
    }

    const dynamic: CrudColumn<RiskBranchScoreQuantitativeRowItem>[] = [...criteriaByGroup1.entries()]
      .map(([group1Id, items]) => {
        const group = group1ById.get(group1Id);
        return {
          title: group ? `${group.code} - ${group.name}` : group1Id,
          children: items.map((c) => ({
            title: c.code,
            width: 90,
            render: (_: unknown, record: RiskBranchScoreQuantitativeRowItem) => record.scoresByCriteriaCode[c.code] ?? "-",
          })),
        };
      })
      .sort((a, b) => String(a.title).localeCompare(String(b.title)));

    return [...fixed, ...dynamic];
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [criteriaList, group1List, t]);

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
      <CrudTable<RiskBranchScoreQuantitativeRowItem>
        tableId="riskScoringExec.branchScoreQuantitative"
        columns={columns}
        dataSource={rows}
        rowKey="branchCode"
        loading={loading}
        onExportExcel={canExport ? () => handleExport("excel") : undefined}
        onExportWord={canExport ? () => handleExport("word") : undefined}
      />
    </div>
  );
}
