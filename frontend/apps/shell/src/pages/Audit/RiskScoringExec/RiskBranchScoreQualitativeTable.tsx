import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Select, Space, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn, type CrudColumn } from "@govia/ui-kit";
import { riskBranchScoreApi, type RiskBranchScoreQualitativeRowItem } from "../../../api/riskScoringExec";
import { group1Api, group2Api, type Group1Item, type Group2Item } from "../../../api/riskScoring";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

/**
 * "Ket qua cham diem rui ro dinh tinh" theo chi nhanh/nam (sheet CT_Diem_DT) - man hinh CHI XEM,
 * khong co them/sua/xoa. Cot dong theo nhom cap 1 (nghiep vu, header tang 1) -> nhom cap 2 (header
 * tang 2), gia tri moi o la tong diem dong gop cua cac chi tieu thuoc nhom cap 2 do - xem
 * RiskBranchScoreQualitativeService.
 */
export function RiskBranchScoreQualitativeTable() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const { getSearchColumnProps } = useClientSearchColumn<RiskBranchScoreQualitativeRowItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [rows, setRows] = useState<RiskBranchScoreQualitativeRowItem[]>([]);
  const [group1List, setGroup1List] = useState<Group1Item[]>([]);
  const [group2List, setGroup2List] = useState<Group2Item[]>([]);
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
    group2Api
      .list()
      .then(setGroup2List)
      .catch(() => setGroup2List([]));
  }, [canView]);

  const load = useCallback(
    async (selectedYear: number) => {
      setLoading(true);
      try {
        setRows(await riskBranchScoreApi.listQualitative(selectedYear));
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

  const columns: CrudColumn<RiskBranchScoreQualitativeRowItem>[] = useMemo(() => {
    const fixed: CrudColumn<RiskBranchScoreQualitativeRowItem>[] = [
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
    const group2ByGroup1 = new Map<string, Group2Item[]>();
    for (const g2 of group2List) {
      if (!group2ByGroup1.has(g2.group1Id)) group2ByGroup1.set(g2.group1Id, []);
      group2ByGroup1.get(g2.group1Id)?.push(g2);
    }

    const dynamic: CrudColumn<RiskBranchScoreQualitativeRowItem>[] = [...group2ByGroup1.entries()]
      .map(([group1Id, items]) => {
        const group1 = group1ById.get(group1Id);
        return {
          title: group1 ? `${group1.code} - ${group1.name}` : group1Id,
          children: items.map((g2) => ({
            title: g2.code,
            width: 100,
            render: (_: unknown, record: RiskBranchScoreQualitativeRowItem) => record.scoresByGroup2Code[g2.code] ?? "-",
          })),
        };
      })
      .sort((a, b) => String(a.title).localeCompare(String(b.title)));

    return [...fixed, ...dynamic];
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [group1List, group2List, t]);

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
      <CrudTable<RiskBranchScoreQualitativeRowItem>
        tableId="riskScoringExec.branchScoreQualitative"
        columns={columns}
        dataSource={rows}
        rowKey="branchCode"
        loading={loading}
      />
    </div>
  );
}
