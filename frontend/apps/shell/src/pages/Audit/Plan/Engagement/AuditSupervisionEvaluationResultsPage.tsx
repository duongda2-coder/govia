import { useCallback, useEffect, useState } from "react";
import { App, Button, Checkbox, Col, Row, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import dayjs from "dayjs";
import { listAllSupervisionEvaluations, type AuditSupervisionEvaluationItem } from "../../../../api/auditSupervisionTeam";
import type { AuditEngagementMonitoringItem } from "../../../../api/auditEngagementMonitoring";

export interface AuditSupervisionEvaluationResultsPageProps {
  engagement: Pick<AuditEngagementMonitoringItem, "id" | "code">;
  onBack: () => void;
}

/** "Ket qua danh gia cua to giam sat" - TAT CA dong danh gia cua CKT (ke ca thanh vien chua danh
 * gia lan nao, hien thi rong), chi xem - khac voi AuditSupervisionEvaluationPage (chi dong cua
 * chinh minh, co sua). */
export function AuditSupervisionEvaluationResultsPage({ engagement, onBack }: AuditSupervisionEvaluationResultsPageProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();

  const [items, setItems] = useState<AuditSupervisionEvaluationItem[]>([]);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listAllSupervisionEvaluations(engagement.id));
    } catch {
      message.error(t("auditSupervisionTeam.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [engagement.id, message, t]);

  useEffect(() => {
    load();
  }, [load]);

  const columns: TableProps<AuditSupervisionEvaluationItem>["columns"] = [
    { title: t("auditSupervisionTeam.columns.employeeCode"), dataIndex: "employeeCode", width: 110 },
    { title: t("auditSupervisionTeam.columns.username"), dataIndex: "username", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditSupervisionTeam.columns.employeeName"), dataIndex: "employeeName" },
    {
      title: t("auditSupervisionTeam.columns.evaluatedAt"),
      dataIndex: "evaluatedAt",
      width: 150,
      render: (v: string | null) => (v ? dayjs(v).format("DD.MM.YYYY HH:mm") : "-"),
    },
    { title: t("auditSupervisionTeam.columns.progress"), dataIndex: "progress", width: 90, render: (v: boolean) => <Checkbox checked={v} disabled /> },
    {
      title: t("auditSupervisionTeam.columns.contentAssured"),
      dataIndex: "contentAssured",
      width: 130,
      render: (v: boolean) => <Checkbox checked={v} disabled />,
    },
    {
      title: t("auditSupervisionTeam.columns.qualityAssured"),
      dataIndex: "qualityAssured",
      width: 130,
      render: (v: boolean) => <Checkbox checked={v} disabled />,
    },
    { title: t("auditSupervisionTeam.columns.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
  ];

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {t("auditSupervisionTeam.evaluationResultsTitle", { code: engagement.code })}
          </Typography.Title>
        </Col>
        <Col>
          <Button onClick={onBack}>{t("common.back")}</Button>
        </Col>
      </Row>
      <Table<AuditSupervisionEvaluationItem> rowKey="employeeId" loading={loading} dataSource={items} columns={columns} pagination={false} />
    </div>
  );
}
