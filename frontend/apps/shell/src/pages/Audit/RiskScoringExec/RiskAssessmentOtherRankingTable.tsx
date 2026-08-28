import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Select, Space, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable } from "@govia/ui-kit";
import { riskAssessmentOtherRankingApi, type RiskAssessmentOtherRankingItem } from "../../../api/riskScoringExec";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

/**
 * "Bang xep hang cham diem rui ro HO, CNTT, Du an, Dich vu thue ngoai..." (sheet ZTC_BXHRR_KHAC) -
 * man hinh CHI XEM, khong co them/sua/xoa truc tiep. NSD chon 1 Nam, he thong tinh diem rui ro +
 * xep loai dong tu du lieu da cham o "Cham diem rui ro khac" (RiskAssessmentOtherTable), sap xep
 * theo Diem tu cao xuong thap.
 */
export function RiskAssessmentOtherRankingTable() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [items, setItems] = useState<RiskAssessmentOtherRankingItem[]>([]);
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
        setItems(await riskAssessmentOtherRankingApi.list(selectedYear));
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

  const columns: TableProps<RiskAssessmentOtherRankingItem>["columns"] = [
    { title: t("riskScoringExec.assessmentOther.year"), dataIndex: "year", width: 90 },
    { title: t("riskScoring.columns.auditObjectCategory"), dataIndex: "auditObjectCategoryCode", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoringExec.assessmentOther.auditObjectCode"), dataIndex: "auditObjectCode", width: 140 },
    { title: t("riskScoringExec.assessmentOther.auditObjectName"), dataIndex: "auditObjectName", render: (v: string | null) => v ?? "-" },
    {
      title: t("riskScoringExec.ranking.riskScore"),
      dataIndex: "riskScore",
      width: 130,
      sorter: (a, b) => a.riskScore - b.riskScore,
      defaultSortOrder: "descend",
    },
    { title: t("riskScoringExec.ranking.rankLabel"), dataIndex: "rankLabel", width: 140, render: (v: string | null) => v ?? "-" },
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
      <CrudTable<RiskAssessmentOtherRankingItem>
        tableId="riskScoringExec.assessmentOtherRanking"
        columns={columns}
        dataSource={items}
        rowKey="headerId"
        loading={loading}
      />
    </div>
  );
}
