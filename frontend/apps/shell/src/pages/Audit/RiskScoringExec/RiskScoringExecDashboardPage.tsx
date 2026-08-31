import { useEffect, useMemo, useState } from "react";
import { App, Card, Col, Row, Select, Space, Spin, Statistic, Typography } from "antd";
import { Bar, Line, Pie } from "@ant-design/plots";
import { useTranslation } from "react-i18next";
import { riskBranchScoreApi, type RiskBranchScoreCombinedRowItem } from "../../../api/riskScoringExec";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

const CHART_HEIGHT = 320;
const TOP_BRANCH_COUNT = 10;

/** Dashboard tong quan cho module Cham diem rui ro - doc du lieu tu cung API
 * "branch-score/combined" da dung cho man "Ket qua cham diem tong hop" (khong
 * co API rieng). rankLabel la chuoi dong tu RiskScoreRank nen nhom theo gia
 * tri thuc te xuat hien trong data, khong hardcode danh sach xep loai.
 *
 * Bo loc Nam la multi-select: chon bao nhieu nam thi KPI/chart tong hop dung
 * bay nhieu nam do (ke ca bieu do xu huong - khong con co dinh lay tat ca cac
 * nam nhu truoc). */
export function RiskScoringExecDashboardPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [selectedYears, setSelectedYears] = useState<number[]>([]);
  const [rows, setRows] = useState<RiskBranchScoreCombinedRowItem[]>([]);
  const [loading, setLoading] = useState(false);

  const yearOptions = useMemo(() => years.map((y) => ({ value: Number(y.code), label: y.code })), [years]);
  const isMultiYear = selectedYears.length > 1;

  useEffect(() => {
    if (!canView) return;
    listMasterDataItems("YEAR")
      .then((items) => {
        setYears(items);
        if (items.length > 0) {
          const maxYear = Math.max(...items.map((y) => Number(y.code)));
          setSelectedYears([maxYear]);
        }
      })
      .catch(() => setYears([]));
  }, [canView]);

  useEffect(() => {
    if (!canView || selectedYears.length === 0) {
      setRows([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    Promise.all(selectedYears.map((y) => riskBranchScoreApi.listCombined(y)))
      .then((allRows) => {
        if (!cancelled) setRows(allRows.flat());
      })
      .catch(() => {
        if (!cancelled) {
          setRows([]);
          message.error(t("riskScoringExec.messages.loadError"));
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [canView, selectedYears, message, t]);

  const kpi = useMemo(() => {
    if (rows.length === 0) return null;
    const avgScore = rows.reduce((sum, r) => sum + r.totalScore, 0) / rows.length;
    const topBranch = rows.reduce((max, r) => (r.totalScore > max.totalScore ? r : max), rows[0]);
    const distinctBranchCount = new Set(rows.map((r) => r.branchCode)).size;
    return { distinctBranchCount, avgScore, topBranch };
  }, [rows]);

  const rankDistribution = useMemo(() => {
    const counts = new Map<string, number>();
    for (const r of rows) {
      const label = r.rankLabel ?? "-";
      counts.set(label, (counts.get(label) ?? 0) + 1);
    }
    return [...counts.entries()].map(([rankLabel, count]) => ({ rankLabel, count }));
  }, [rows]);

  const topBranches = useMemo(
    () =>
      [...rows]
        .sort((a, b) => b.totalScore - a.totalScore)
        .slice(0, TOP_BRANCH_COUNT)
        .map((r) => {
          const name = r.branchName ?? r.branchCode;
          return { branchLabel: isMultiYear ? `${name} (${r.year})` : name, totalScore: r.totalScore };
        })
        .reverse(),
    [rows, isMultiYear],
  );

  const trend = useMemo(() => {
    const byYear = new Map<number, { sum: number; count: number }>();
    for (const r of rows) {
      const acc = byYear.get(r.year) ?? { sum: 0, count: 0 };
      acc.sum += r.totalScore;
      acc.count += 1;
      byYear.set(r.year, acc);
    }
    return [...byYear.entries()]
      .map(([year, { sum, count }]) => ({ year, avgScore: sum / count }))
      .sort((a, b) => a.year - b.year);
  }, [rows]);

  if (!canView) {
    return null;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("riskScoringExec.dashboard.title")}</Typography.Title>
      <Space style={{ marginBottom: 16 }}>
        <Typography.Text>{t("riskScoringExec.assessmentOther.year")}</Typography.Text>
        <Select
          mode="multiple"
          allowClear
          style={{ minWidth: 240 }}
          options={yearOptions}
          value={selectedYears}
          onChange={setSelectedYears}
          placeholder={t("riskScoringExec.ranking.selectYear")}
        />
      </Space>

      <Spin spinning={loading}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Card>
              <Statistic
                title={t("riskScoringExec.dashboard.kpiTotalBranches")}
                value={kpi?.distinctBranchCount ?? 0}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card>
              <Statistic
                title={t("riskScoringExec.dashboard.kpiAvgScore")}
                value={kpi?.avgScore ?? 0}
                precision={2}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card>
              <Statistic
                title={t("riskScoringExec.dashboard.kpiTopBranch")}
                value={
                  kpi
                    ? `${kpi.topBranch.branchName ?? kpi.topBranch.branchCode}${
                        isMultiYear ? ` (${kpi.topBranch.year})` : ""
                      } (${kpi.topBranch.totalScore})`
                    : "-"
                }
              />
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
          <Col xs={24} lg={10}>
            <Card title={t("riskScoringExec.dashboard.chartRankDistribution")}>
              <Pie
                height={CHART_HEIGHT}
                data={rankDistribution}
                angleField="count"
                colorField="rankLabel"
                label={{ text: "count", style: { fontWeight: "bold" } }}
                legend={{ color: { position: "bottom", layout: { justifyContent: "center" } } }}
              />
            </Card>
          </Col>
          <Col xs={24} lg={14}>
            <Card title={t("riskScoringExec.dashboard.chartTopBranches")}>
              <Bar
                height={CHART_HEIGHT}
                data={topBranches}
                xField="totalScore"
                yField="branchLabel"
                colorField="branchLabel"
                legend={false}
              />
            </Card>
          </Col>
        </Row>

        <Row style={{ marginTop: 16 }}>
          <Col span={24}>
            <Card title={t("riskScoringExec.dashboard.chartTrendTitle")}>
              <Line
                height={CHART_HEIGHT}
                data={trend}
                xField="year"
                yField="avgScore"
                point={{ shape: "circle" }}
                label={{ text: "avgScore", style: { fontSize: 10 } }}
              />
            </Card>
          </Col>
        </Row>
      </Spin>
    </div>
  );
}
