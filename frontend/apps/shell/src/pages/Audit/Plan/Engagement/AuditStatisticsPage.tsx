import { useCallback, useEffect, useState } from "react";
import { App, Button, Card, Modal, Result, Select, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { DownloadOutlined, EyeOutlined, SearchOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  exportStatsByEmployee,
  exportStatsByEngagement,
  exportStatsByUnit,
  exportStatsByYear,
  fetchStatsByEmployee,
  fetchStatsByEngagement,
  fetchStatsByUnit,
  fetchStatsByYear,
  fetchTtssDetail,
  fetchUnitDetail,
  listStatisticsEmployeeOptions,
  listStatisticsEngagementOptions,
  listStatisticsYearOptions,
  type EmployeeCodeOption,
  type EmployeeStatRow,
  type EngagementOption,
  type EngagementSegmentStatRow,
  type TtssDetailRow,
  type UnitDetailStatRow,
  type UnitStatRow,
  type YearSegmentStatRow,
} from "../../../../api/auditStatistics";
import { useAuth } from "../../../../auth/AuthContext";

type Keyed<T> = T & { key: string };

const numberFormatter = new Intl.NumberFormat("vi-VN");
const money = (v: number | null) => (v == null ? "-" : numberFormatter.format(v));

/** Man hinh "Thong ke" (tcode ztc_thongke, sheet "Thống kê" cua "Tao CKT (3).xlsx") - bao cao CHI DOC
 * gom 4 nhom: theo Nam, theo CKT, theo thanh vien doan, theo don vi bi kiem toan (co drill-down "Chi
 * tiet theo chi nhanh"); nhom "theo Nam" con co drill-down "Xem chi tiet TTSS" theo dung dac ta. */
export function AuditStatisticsPage() {
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.STATISTICS.VIEW");
  const canExport = hasPermission("AUDIT.STATISTICS.EXPORT");

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditStatistics.pageTitle")}</Typography.Title>
      <Space direction="vertical" size={16} style={{ width: "100%" }}>
        <YearSection canExport={canExport} />
        <EngagementSection canExport={canExport} />
        <EmployeeSection canExport={canExport} />
        <UnitSection canExport={canExport} />
      </Space>
    </div>
  );
}

function withKeys<T>(items: T[], keyOf: (item: T) => string): Keyed<T>[] {
  return items.map((item) => ({ ...item, key: keyOf(item) }));
}

// ===================== 1: Thong ke theo nam =====================

function YearSection({ canExport }: { canExport: boolean }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { getSearchColumnProps } = useClientSearchColumn<Keyed<YearSegmentStatRow>>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [years, setYears] = useState<number[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [rows, setRows] = useState<Keyed<YearSegmentStatRow>[]>([]);
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<{ year: number; segmentId: string; segmentLabel: string } | null>(null);

  const load = useCallback(
    async (y: number | undefined) => {
      setLoading(true);
      try {
        setRows(withKeys(await fetchStatsByYear(y), (r) => `${r.year}|${r.businessSegmentCode}`));
      } catch {
        message.error(t("auditStatistics.messages.loadError"));
      } finally {
        setLoading(false);
      }
    },
    [message, t],
  );

  useEffect(() => {
    listStatisticsYearOptions().then(setYears).catch(() => setYears([]));
    load(undefined);
  }, [load]);

  const columns: TableProps<Keyed<YearSegmentStatRow>>["columns"] = [
    { title: t("auditStatistics.columns.year"), dataIndex: "year", width: 90, sorter: (a, b) => (a.year ?? 0) - (b.year ?? 0) },
    { title: t("auditStatistics.columns.businessSegment"), width: 160, ...getSearchColumnProps("businessSegmentCode", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditStatistics.columns.ttssCount"), dataIndex: "ttssCount", width: 130, align: "right", sorter: (a, b) => a.ttssCount - b.ttssCount, render: money },
    { title: t("auditStatistics.columns.materialTtssCount"), dataIndex: "materialTtssCount", width: 160, align: "right", sorter: (a, b) => a.materialTtssCount - b.materialTtssCount, render: money },
    { title: t("auditStatistics.columns.recommendationCount"), dataIndex: "recommendationCount", width: 130, align: "right", sorter: (a, b) => a.recommendationCount - b.recommendationCount, render: money },
    {
      title: t("common.actions"),
      width: 160,
      render: (_: unknown, row) => (
        <Button
          size="small"
          icon={<EyeOutlined />}
          disabled={row.year == null || !row.businessSegmentCode}
          onClick={() => setDetail({ year: row.year as number, segmentId: row.businessSegmentCode as string, segmentLabel: row.businessSegmentName ?? row.businessSegmentCode ?? "" })}
        >
          {t("auditStatistics.form.viewTtssDetail")}
        </Button>
      ),
    },
  ];

  return (
    <Card
      size="small"
      title={t("auditStatistics.section.byYear")}
      extra={
        <Space>
          <Select
            allowClear
            style={{ width: 160 }}
            placeholder={t("auditStatistics.form.filterByYear")}
            value={year}
            onChange={(v) => {
              setYear(v);
              load(v);
            }}
            options={years.map((y) => ({ value: y, label: y }))}
          />
          {canExport && (
            <Button icon={<DownloadOutlined />} onClick={() => exportStatsByYear(year)}>
              {t("common.export")}
            </Button>
          )}
        </Space>
      }
    >
      <CrudTable<Keyed<YearSegmentStatRow>> tableId="audit.statistics.byYear" columns={columns} dataSource={rows} rowKey="key" loading={loading} />

      <TtssDetailModal detail={detail} onClose={() => setDetail(null)} />
    </Card>
  );
}

function TtssDetailModal({ detail, onClose }: { detail: { year: number; segmentId: string; segmentLabel: string } | null; onClose: () => void }) {
  const { t } = useTranslation();
  const [rows, setRows] = useState<TtssDetailRow[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!detail) return;
    setLoading(true);
    fetchTtssDetail(detail.year, detail.segmentId)
      .then(setRows)
      .catch(() => setRows([]))
      .finally(() => setLoading(false));
  }, [detail]);

  const columns: TableProps<TtssDetailRow>["columns"] = [
    { title: t("auditStatistics.columns.engagementCode"), dataIndex: "engagementCode", width: 130 },
    { title: t("auditStatistics.columns.findingCode"), dataIndex: "findingCode", width: 120 },
    { title: t("auditStatistics.columns.findingName"), dataIndex: "findingName" },
    { title: t("auditStatistics.columns.material"), dataIndex: "material", width: 100, render: (v: boolean) => (v ? t("common.yes") : t("common.no")) },
    { title: t("auditStatistics.columns.customerName"), dataIndex: "customerName", width: 180 },
    { title: t("auditStatistics.columns.amount"), dataIndex: "amount", width: 140, align: "right", render: money },
    { title: t("auditStatistics.columns.exceptionDate"), dataIndex: "exceptionDate", width: 120 },
  ];

  return (
    <Modal
      open={!!detail}
      onCancel={onClose}
      footer={null}
      width={1000}
      title={detail ? t("auditStatistics.form.ttssDetailTitle", { year: detail.year, segment: detail.segmentLabel }) : ""}
      destroyOnClose
    >
      <Table<TtssDetailRow> columns={columns} dataSource={rows} rowKey={(r) => `${r.engagementCode}-${r.findingCode}-${r.customerName}`} loading={loading} size="small" pagination={{ pageSize: 10 }} />
    </Modal>
  );
}

// ===================== 2: Thong ke theo CKT =====================

function EngagementSection({ canExport }: { canExport: boolean }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { getSearchColumnProps } = useClientSearchColumn<Keyed<EngagementSegmentStatRow>>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [engagements, setEngagements] = useState<EngagementOption[]>([]);
  const [engagementCode, setEngagementCode] = useState<string | undefined>(undefined);
  const [rows, setRows] = useState<Keyed<EngagementSegmentStatRow>[]>([]);
  const [loading, setLoading] = useState(false);

  const load = useCallback(
    async (code: string | undefined) => {
      setLoading(true);
      try {
        setRows(withKeys(await fetchStatsByEngagement(code), (r) => `${r.engagementCode}|${r.businessSegmentCode}`));
      } catch {
        message.error(t("auditStatistics.messages.loadError"));
      } finally {
        setLoading(false);
      }
    },
    [message, t],
  );

  useEffect(() => {
    listStatisticsEngagementOptions().then(setEngagements).catch(() => setEngagements([]));
    load(undefined);
  }, [load]);

  const columns: TableProps<Keyed<EngagementSegmentStatRow>>["columns"] = [
    { title: t("auditStatistics.columns.engagementCode"), width: 150, ...getSearchColumnProps("engagementCode", searchLabels) },
    { title: t("auditStatistics.columns.businessSegment"), width: 160, ...getSearchColumnProps("businessSegmentCode", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditStatistics.columns.ttssCount"), dataIndex: "ttssCount", width: 130, align: "right", sorter: (a, b) => a.ttssCount - b.ttssCount, render: money },
    { title: t("auditStatistics.columns.materialTtssCount"), dataIndex: "materialTtssCount", width: 160, align: "right", sorter: (a, b) => a.materialTtssCount - b.materialTtssCount, render: money },
    { title: t("auditStatistics.columns.recommendationCount"), dataIndex: "recommendationCount", width: 130, align: "right", sorter: (a, b) => a.recommendationCount - b.recommendationCount, render: money },
  ];

  return (
    <Card
      size="small"
      title={t("auditStatistics.section.byEngagement")}
      extra={
        <Space>
          <Select
            allowClear
            showSearch
            style={{ width: 200 }}
            placeholder={t("auditStatistics.form.filterByEngagement")}
            value={engagementCode}
            onChange={(v) => {
              setEngagementCode(v);
              load(v);
            }}
            options={engagements.map((e) => ({ value: e.code, label: e.code }))}
          />
          {canExport && (
            <Button icon={<DownloadOutlined />} onClick={() => exportStatsByEngagement(engagementCode)}>
              {t("common.export")}
            </Button>
          )}
        </Space>
      }
    >
      <CrudTable<Keyed<EngagementSegmentStatRow>> tableId="audit.statistics.byEngagement" columns={columns} dataSource={rows} rowKey="key" loading={loading} />
    </Card>
  );
}

// ===================== 3: Thong ke theo thanh vien doan =====================

function EmployeeSection({ canExport }: { canExport: boolean }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { getSearchColumnProps } = useClientSearchColumn<Keyed<EmployeeStatRow>>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [employees, setEmployees] = useState<EmployeeCodeOption[]>([]);
  const [employeeCode, setEmployeeCode] = useState<string | undefined>(undefined);
  const [rows, setRows] = useState<Keyed<EmployeeStatRow>[]>([]);
  const [loading, setLoading] = useState(false);

  const load = useCallback(
    async (code: string | undefined) => {
      setLoading(true);
      try {
        setRows(withKeys(await fetchStatsByEmployee(code), (r) => `${r.employeeUsername}|${r.year}`));
      } catch {
        message.error(t("auditStatistics.messages.loadError"));
      } finally {
        setLoading(false);
      }
    },
    [message, t],
  );

  useEffect(() => {
    listStatisticsEmployeeOptions().then(setEmployees).catch(() => setEmployees([]));
    load(undefined);
  }, [load]);

  const columns: TableProps<Keyed<EmployeeStatRow>>["columns"] = [
    { title: t("auditStatistics.columns.employeeCode"), width: 130, ...getSearchColumnProps("employeeCode", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditStatistics.columns.employeeName"), ...getSearchColumnProps("employeeName", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditStatistics.columns.year"), dataIndex: "year", width: 90, sorter: (a, b) => (a.year ?? 0) - (b.year ?? 0) },
    { title: t("auditStatistics.columns.ttssCount"), dataIndex: "ttssCount", width: 130, align: "right", sorter: (a, b) => a.ttssCount - b.ttssCount, render: money },
    { title: t("auditStatistics.columns.materialTtssCount"), dataIndex: "materialTtssCount", width: 160, align: "right", sorter: (a, b) => a.materialTtssCount - b.materialTtssCount, render: money },
    { title: t("auditStatistics.columns.recommendationCount"), dataIndex: "recommendationCount", width: 130, align: "right", sorter: (a, b) => a.recommendationCount - b.recommendationCount, render: money },
  ];

  return (
    <Card
      size="small"
      title={t("auditStatistics.section.byEmployee")}
      extra={
        <Space>
          <Select
            allowClear
            showSearch
            style={{ width: 220 }}
            placeholder={t("auditStatistics.form.filterByEmployee")}
            value={employeeCode}
            onChange={(v) => {
              setEmployeeCode(v);
              load(v);
            }}
            optionFilterProp="label"
            options={employees.map((e) => ({ value: e.employeeCode, label: `${e.fullName} (${e.employeeCode})` }))}
          />
          {canExport && (
            <Button icon={<DownloadOutlined />} onClick={() => exportStatsByEmployee(employeeCode)}>
              {t("common.export")}
            </Button>
          )}
        </Space>
      }
    >
      <CrudTable<Keyed<EmployeeStatRow>> tableId="audit.statistics.byEmployee" columns={columns} dataSource={rows} rowKey="key" loading={loading} />
    </Card>
  );
}

// ===================== 4: Thong ke theo don vi bi kiem toan =====================

function UnitSection({ canExport }: { canExport: boolean }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { getSearchColumnProps } = useClientSearchColumn<Keyed<UnitStatRow>>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [rows, setRows] = useState<Keyed<UnitStatRow>[]>([]);
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<{ unitId: string; label: string } | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRows(withKeys(await fetchStatsByUnit(), (r) => r.auditObjectUnitId));
    } catch {
      message.error(t("auditStatistics.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    load();
  }, [load]);

  const columns: TableProps<Keyed<UnitStatRow>>["columns"] = [
    { title: t("auditStatistics.columns.unitCode"), width: 130, ...getSearchColumnProps("unitCode", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditStatistics.columns.unitName"), ...getSearchColumnProps("unitName", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditStatistics.columns.engagementCount"), dataIndex: "engagementCount", width: 150, align: "right", sorter: (a, b) => a.engagementCount - b.engagementCount, render: money },
    { title: t("auditStatistics.columns.ttssCount"), dataIndex: "ttssCount", width: 130, align: "right", sorter: (a, b) => a.ttssCount - b.ttssCount, render: money },
    { title: t("auditStatistics.columns.materialTtssCount"), dataIndex: "materialTtssCount", width: 160, align: "right", sorter: (a, b) => a.materialTtssCount - b.materialTtssCount, render: money },
    { title: t("auditStatistics.columns.recommendationCount"), dataIndex: "recommendationCount", width: 130, align: "right", sorter: (a, b) => a.recommendationCount - b.recommendationCount, render: money },
    {
      title: t("common.actions"),
      width: 160,
      render: (_: unknown, row) => (
        <Button size="small" icon={<SearchOutlined />} onClick={() => setDetail({ unitId: row.auditObjectUnitId, label: row.unitName ?? row.unitCode ?? "" })}>
          {t("auditStatistics.form.viewUnitDetail")}
        </Button>
      ),
    },
  ];

  return (
    <Card
      size="small"
      title={t("auditStatistics.section.byUnit")}
      extra={
        canExport && (
          <Button icon={<DownloadOutlined />} onClick={() => exportStatsByUnit()}>
            {t("common.export")}
          </Button>
        )
      }
    >
      <CrudTable<Keyed<UnitStatRow>> tableId="audit.statistics.byUnit" columns={columns} dataSource={rows} rowKey="key" loading={loading} />

      <UnitDetailModal detail={detail} onClose={() => setDetail(null)} />
    </Card>
  );
}

function UnitDetailModal({ detail, onClose }: { detail: { unitId: string; label: string } | null; onClose: () => void }) {
  const { t } = useTranslation();
  const [rows, setRows] = useState<UnitDetailStatRow[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!detail) return;
    setLoading(true);
    fetchUnitDetail(detail.unitId)
      .then(setRows)
      .catch(() => setRows([]))
      .finally(() => setLoading(false));
  }, [detail]);

  const columns: TableProps<UnitDetailStatRow>["columns"] = [
    { title: t("auditStatistics.columns.decisionNumber"), dataIndex: "decisionNumber", width: 130 },
    { title: t("auditStatistics.columns.year"), dataIndex: "year", width: 90 },
    { title: t("auditStatistics.columns.ttssCount"), dataIndex: "ttssCount", width: 130, align: "right", render: money },
    { title: t("auditStatistics.columns.materialTtssCount"), dataIndex: "materialTtssCount", width: 160, align: "right", render: money },
    { title: t("auditStatistics.columns.recommendationCount"), dataIndex: "recommendationCount", width: 130, align: "right", render: money },
    { title: t("auditStatistics.columns.completedRecommendationCount"), dataIndex: "completedRecommendationCount", width: 150, align: "right", render: money },
    { title: t("auditStatistics.columns.riskRank"), dataIndex: "riskRank", width: 130, render: (v: string | null) => v ?? "-" },
  ];

  return (
    <Modal open={!!detail} onCancel={onClose} footer={null} width={1000} title={detail ? t("auditStatistics.form.unitDetailTitle", { unit: detail.label }) : ""} destroyOnClose>
      <Table<UnitDetailStatRow> columns={columns} dataSource={rows} rowKey={(r) => `${r.decisionNumber}-${r.year}`} loading={loading} size="small" pagination={{ pageSize: 10 }} />
    </Modal>
  );
}
