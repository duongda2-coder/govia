import { useCallback, useEffect, useState } from "react";
import { App, Button, Col, Row, Space, Tag, Typography } from "antd";
import { FileExcelOutlined, StarOutlined } from "@ant-design/icons";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, getApiErrorMessage } from "@govia/ui-kit";
import type { AuditEngagementItem } from "../../../api/auditEngagement";
import { DGCL_APPENDICES, exportDgclPl04b1, listDgclSubjects, type DgclSubjectRow } from "../../../api/auditDgcl";
import { useAuth } from "../../../auth/AuthContext";
import { DgclSheetPage } from "./DgclSheetPage";
import { formatScore } from "./dgclShared";

interface Props {
  engagement: AuditEngagementItem;
  onBack: () => void;
}

/** Man hinh "Đánh giá chất lượng" (sheet logic dong 30-43): tat ca thanh vien di kiem toan trong doan + dong cuoi
 * "Cuộc kiểm toán" (danh gia ca doan). Diem PL01A/B/F chi hien khi phu luc da xac nhan hoan thanh. */
export function DgclTeamPage({ engagement, onBack }: Props) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canExport = hasPermission("AUDIT.DGCL.EXPORT");

  const [rows, setRows] = useState<DgclSubjectRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [selected, setSelected] = useState<DgclSubjectRow | null>(null);
  const [evaluating, setEvaluating] = useState<DgclSubjectRow | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await listDgclSubjects(engagement.id));
    } catch (err) {
      message.error(getApiErrorMessage(err, t("auditDgcl.messages.loadError")));
    } finally {
      setLoading(false);
    }
  }, [engagement.id, message, t]);

  useEffect(() => {
    if (!evaluating) load();
  }, [evaluating, load]);

  const exportPl04b1 = async () => {
    setExporting(true);
    try {
      await exportDgclPl04b1(engagement.id, engagement.code);
    } catch {
      message.error(t("auditDgcl.messages.exportError"));
    } finally {
      setExporting(false);
    }
  };

  if (evaluating) {
    return <DgclSheetPage engagement={engagement} subject={evaluating} onBack={() => setEvaluating(null)} />;
  }

  const score = (v: number | null) => formatScore(v) ?? "-";
  const columns: TableProps<DgclSubjectRow>["columns"] = [
    { title: t("auditDgcl.columns.engagementCode"), dataIndex: "engagementCode", width: 140 },
    {
      title: t("auditDgcl.columns.employeeName"),
      dataIndex: "employeeName",
      render: (v: string, r) => (r.team ? <Typography.Text strong>{t("auditDgcl.teamRow")}</Typography.Text> : v),
    },
    { title: t("auditDgcl.columns.segmentCodes"), dataIndex: "segmentCodes", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditDgcl.columns.role"), dataIndex: "role", width: 80, render: (v: string | null) => v ?? "-" },
    { title: "PL01A", dataIndex: "pl01aScore", width: 80, align: "right", render: score },
    { title: "PL01B", dataIndex: "pl01bScore", width: 80, align: "right", render: score },
    { title: "PL01F", dataIndex: "pl01fScore", width: 80, align: "right", render: score },
    { title: t("auditDgcl.columns.classification"), dataIndex: "classification", width: 190, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditDgcl.columns.progress"),
      width: 150,
      render: (_: unknown, r) =>
        r.confirmedCount === 0 ? (
          "-"
        ) : (
          <Tag color={r.confirmedCount === DGCL_APPENDICES.length ? "green" : "blue"}>
            {t("auditDgcl.progress", { done: r.confirmedCount, total: DGCL_APPENDICES.length })}
          </Tag>
        ),
    },
    { title: t("auditDgcl.columns.evaluator"), dataIndex: "evaluatorNames", width: 170, render: (v: string | null) => v ?? "-" },
    { title: t("auditDgcl.columns.controller"), dataIndex: "controllerNames", width: 170, render: (v: string | null) => v ?? "-" },
  ];

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {t("auditDgcl.teamTitle", { code: engagement.code })}
          </Typography.Title>
        </Col>
        <Col>
          <Button onClick={onBack}>{t("common.back")}</Button>
        </Col>
      </Row>

      <Space style={{ marginBottom: 12 }}>
        <Button type="primary" icon={<StarOutlined />} disabled={!selected} onClick={() => selected && setEvaluating(selected)}>
          {t("common.evaluate")}
        </Button>
        {canExport && (
          <Button icon={<FileExcelOutlined />} loading={exporting} onClick={exportPl04b1}>
            PL04B1
          </Button>
        )}
      </Space>

      <CrudTable<DgclSubjectRow>
        tableId="audit.dgcl.subjects"
        columns={columns}
        dataSource={rows}
        rowKey="subjectKey"
        loading={loading}
        pagination={false}
        onSelectionChange={(_keys, r) => setSelected(r[0] ?? null)}
      />
    </div>
  );
}
