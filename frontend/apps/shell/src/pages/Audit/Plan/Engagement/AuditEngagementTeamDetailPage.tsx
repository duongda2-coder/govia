import { useCallback, useEffect, useState } from "react";
import { App, Button, Col, Form, Input, InputNumber, Modal, Row, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable } from "@govia/ui-kit";
import {
  getAuditEngagementTeamDetail,
  updateAuditEngagementTeamMemberScoring,
  type AuditEngagementTeamMemberDetailItem,
  type ProgressStat,
} from "../../../../api/auditEngagementMonitoring";
import { listAuditTtssRecords, type AuditTtssRecordItem } from "../../../../api/auditTtss";
import type { AuditEngagementMonitoringItem } from "../../../../api/auditEngagementMonitoring";
import { useAuth } from "../../../../auth/AuthContext";
import { ProgressReportDrawer } from "../Execution/WorkManagement/ProgressReportDrawer";
import { OtherReportFilesDrawer } from "../Execution/WorkManagement/OtherReportFilesDrawer";
import { AuditEngagementAssignmentPage } from "./AuditEngagementAssignmentPage";

export interface AuditEngagementTeamDetailPageProps {
  engagement: AuditEngagementMonitoringItem;
  onBack: () => void;
}

interface ScoringFormValues {
  score: number | null;
  ranking: string | null;
  note: string | null;
}

function formatProgress(stat: ProgressStat): string {
  const pct = stat.total > 0 ? ` (${((stat.completed / stat.total) * 100).toFixed(2)}%)` : "";
  return `${stat.completed}/${stat.total}${pct}`;
}

/** Nut "Chi tiet doan KT" tren man hinh "Quan ly dot kiem toan" - 4 chuc nang con (Bao cao tien
 * do/Xem file/Xem chi tiet TTSS/Chi tiet phan cong) deu tai su dung component/API da co san cua
 * man hinh "Quan ly cong viec" (xem AuditEngagementMonitoringService, phan Javadoc). */
export function AuditEngagementTeamDetailPage({ engagement, onBack }: AuditEngagementTeamDetailPageProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canScore = hasPermission("AUDIT.PLAN_ENGAGEMENT_TEAM.EDIT");

  const [members, setMembers] = useState<AuditEngagementTeamMemberDetailItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditEngagementTeamMemberDetailItem | null>(null);
  const [scoringForm] = Form.useForm<ScoringFormValues>();
  const [scoringOpen, setScoringOpen] = useState(false);

  const [progressReportOpen, setProgressReportOpen] = useState(false);
  const [otherFilesOpen, setOtherFilesOpen] = useState(false);
  const [ttssOpen, setTtssOpen] = useState(false);
  const [ttssItems, setTtssItems] = useState<AuditTtssRecordItem[]>([]);
  const [ttssLoading, setTtssLoading] = useState(false);
  const [showAssignment, setShowAssignment] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setMembers(await getAuditEngagementTeamDetail(engagement.id));
    } catch {
      message.error(t("auditEngagementMonitoring.messages.loadDetailError"));
    } finally {
      setLoading(false);
    }
  }, [engagement.id, message, t]);

  useEffect(() => {
    load();
  }, [load]);

  const openScoring = () => {
    if (!selected) return;
    scoringForm.setFieldsValue({ score: selected.score, ranking: selected.ranking, note: selected.note });
    setScoringOpen(true);
  };

  const submitScoring = async () => {
    if (!selected) return;
    try {
      const values = await scoringForm.validateFields();
      await updateAuditEngagementTeamMemberScoring(engagement.id, selected.memberId, {
        score: values.score ?? null,
        ranking: values.ranking ?? null,
        note: values.note ?? null,
      });
      message.success(t("auditEngagementMonitoring.messages.updateScoringSuccess"));
      setScoringOpen(false);
      await load();
    } catch (err) {
      if (err instanceof Error) message.error(t("auditEngagementMonitoring.messages.updateScoringError"));
    }
  };

  const openTtss = async () => {
    setTtssOpen(true);
    setTtssLoading(true);
    try {
      setTtssItems(await listAuditTtssRecords(engagement.id));
    } catch {
      message.error(t("auditEngagementMonitoring.messages.loadError"));
    } finally {
      setTtssLoading(false);
    }
  };

  if (showAssignment) {
    return <AuditEngagementAssignmentPage engagement={engagement} onBack={() => setShowAssignment(false)} readOnly />;
  }

  const columns: TableProps<AuditEngagementTeamMemberDetailItem>["columns"] = [
    { title: t("auditEngagementMonitoring.detail.columns.stt"), dataIndex: "stt", width: 60 },
    { title: t("auditEngagementMonitoring.detail.columns.employeeName"), dataIndex: "employeeName", render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.detail.columns.roleTitle"), dataIndex: "roleTitle", width: 130 },
    { title: t("auditEngagementMonitoring.detail.columns.businessSegmentNames"), dataIndex: "businessSegmentNames", render: (v: string) => v || "-" },
    { title: t("auditEngagementMonitoring.detail.columns.totalFindings"), dataIndex: "totalFindings", width: 110 },
    { title: t("auditEngagementMonitoring.detail.columns.ttssTypeCount"), dataIndex: "ttssTypeCount", width: 110 },
    { title: t("auditEngagementMonitoring.detail.columns.totalMaterialFindings"), dataIndex: "totalMaterialFindings", width: 110 },
    { title: t("auditEngagementMonitoring.detail.columns.materialTtssTypeCount"), dataIndex: "materialTtssTypeCount", width: 110 },
    { title: t("auditEngagementMonitoring.detail.columns.recommendationCount"), dataIndex: "recommendationCount", width: 110 },
    {
      title: t("auditEngagementMonitoring.detail.columns.cbktProgress"),
      dataIndex: "cbktProgress",
      width: 140,
      render: (v: ProgressStat) => formatProgress(v),
    },
    {
      title: t("auditEngagementMonitoring.detail.columns.thktSampleProgress"),
      dataIndex: "thktSampleProgress",
      width: 160,
      render: (v: ProgressStat) => formatProgress(v),
    },
    {
      title: t("auditEngagementMonitoring.detail.columns.thktNoSampleProgress"),
      dataIndex: "thktNoSampleProgress",
      width: 160,
      render: (v: ProgressStat) => formatProgress(v),
    },
    { title: t("auditEngagementMonitoring.detail.columns.score"), dataIndex: "score", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.detail.columns.ranking"), dataIndex: "ranking", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.detail.columns.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
  ];

  const ttssColumns: TableProps<AuditTtssRecordItem>["columns"] = [
    { title: t("auditTtss.columns.findingCode"), dataIndex: "findingCode", width: 120 },
    { title: t("auditTtss.columns.findingName"), dataIndex: "findingName" },
    { title: t("auditTtss.columns.material"), dataIndex: "material", width: 100, render: (v: boolean) => (v ? t("common.yes") : t("common.no")) },
    { title: t("auditEngagementMonitoring.detail.ttssPerformerName"), dataIndex: "ttssPerformerName", width: 180 },
    { title: t("auditTtss.columns.businessSegment"), dataIndex: "businessSegmentCode", width: 100 },
  ];

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {t("auditEngagementMonitoring.detail.title", { code: engagement.code })}
          </Typography.Title>
        </Col>
        <Col>
          <Button onClick={onBack}>{t("common.back")}</Button>
        </Col>
      </Row>

      <Space style={{ marginBottom: 12 }} wrap>
        <Button onClick={() => setProgressReportOpen(true)}>{t("auditEngagementMonitoring.detail.progressReportButton")}</Button>
        <Button onClick={() => setOtherFilesOpen(true)}>{t("auditEngagementMonitoring.detail.otherFilesButton")}</Button>
        <Button onClick={openTtss}>{t("auditEngagementMonitoring.detail.ttssButton")}</Button>
        <Button onClick={() => setShowAssignment(true)}>{t("auditEngagementMonitoring.detail.assignmentButton")}</Button>
        {canScore && (
          <Button type="primary" disabled={!selected} onClick={openScoring}>
            {t("auditEngagementMonitoring.detail.scoringButton")}
          </Button>
        )}
      </Space>

      <CrudTable<AuditEngagementTeamMemberDetailItem>
        tableId="audit.planEngagement.teamDetail"
        columns={columns}
        dataSource={members}
        rowKey="memberId"
        loading={loading}
        onSelectionChange={(_keys, rows) => setSelected(rows[0] ?? null)}
      />

      <Modal
        title={t("auditEngagementMonitoring.detail.scoringTitle", { name: selected?.employeeName ?? "" })}
        open={scoringOpen}
        onCancel={() => setScoringOpen(false)}
        onOk={submitScoring}
        destroyOnClose
      >
        <Form<ScoringFormValues> form={scoringForm} layout="vertical">
          <Form.Item name="score" label={t("auditEngagementMonitoring.detail.columns.score")}>
            <InputNumber style={{ width: "100%" }} min={0} max={100} precision={2} />
          </Form.Item>
          <Form.Item name="ranking" label={t("auditEngagementMonitoring.detail.columns.ranking")}>
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item name="note" label={t("auditEngagementMonitoring.detail.columns.note")}>
            <Input.TextArea rows={3} maxLength={1000} />
          </Form.Item>
        </Form>
      </Modal>

      <ProgressReportDrawer open={progressReportOpen} engagementId={engagement.id} engagement={engagement} onClose={() => setProgressReportOpen(false)} />
      <OtherReportFilesDrawer open={otherFilesOpen} engagementId={engagement.id} onClose={() => setOtherFilesOpen(false)} />

      <Modal
        title={t("auditEngagementMonitoring.detail.ttssModalTitle", { code: engagement.code })}
        open={ttssOpen}
        onCancel={() => setTtssOpen(false)}
        footer={null}
        width={900}
      >
        <Table<AuditTtssRecordItem> columns={ttssColumns} dataSource={ttssItems} rowKey="id" loading={ttssLoading} size="small" scroll={{ x: "max-content" }} />
      </Modal>
    </div>
  );
}
