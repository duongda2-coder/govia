import { useCallback, useEffect, useState } from "react";
import { App, Button, Result, Space, Tooltip, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import { listMySupervisionEngagements } from "../../../../api/auditSupervisionTeam";
import type { AuditEngagementMonitoringItem } from "../../../../api/auditEngagementMonitoring";
import type { AuditEngagementItem } from "../../../../api/auditEngagement";
import { useAuth } from "../../../../auth/AuthContext";
import { AuditSupervisionEvaluationResultsPage } from "./AuditSupervisionEvaluationResultsPage";
import { OtherReportFilesDrawer } from "../Execution/WorkManagement/OtherReportFilesDrawer";

const SCREEN_KEY = "audit.plan.engagement.supervisionTeam";

/** "Man hinh quan ly to giam sat" (sheet "To giam sat" cua Tao CKT (4).xlsx) - copy layout man
 * hinh "Quản lý đợt kiểm toán" nhung chi liet ke CKT ma nguoi dang dang nhap la thanh vien to
 * giam sat (backend da loc san qua AuditSupervisionTeamService.listMyEngagements). */
export function AuditSupervisionTeamManagementPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.SUPERVISION_TEAM.VIEW");
  const { getSearchColumnProps } = useClientSearchColumn<AuditEngagementMonitoringItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditEngagementMonitoringItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditEngagementMonitoringItem[]>([]);
  const [resultsFor, setResultsFor] = useState<AuditEngagementMonitoringItem | null>(null);
  const [filesFor, setFilesFor] = useState<AuditEngagementItem | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listMySupervisionEngagements());
    } catch {
      message.error(t("auditSupervisionTeam.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  const columns: TableProps<AuditEngagementMonitoringItem>["columns"] = [
    { title: t("auditEngagementMonitoring.columns.code"), width: 150, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditEngagementMonitoring.columns.auditObjectUnitName"), dataIndex: "auditObjectUnitName", render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.name"), ...getSearchColumnProps("name", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.fieldworkStartDate"), dataIndex: "fieldworkStartDate", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.fieldworkEndDate"), dataIndex: "fieldworkEndDate", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.decisionNumber"), dataIndex: "decisionNumber", width: 130 },
    { title: t("auditEngagementMonitoring.columns.teamLeadEmployee"), dataIndex: "teamLeadEmployeeName", render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.status"), dataIndex: "status", width: 140, render: (v: AuditEngagementItem["status"]) => t(`auditEngagement.status.${v}`) },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  if (resultsFor) {
    return <AuditSupervisionEvaluationResultsPage engagement={resultsFor} onBack={() => setResultsFor(null)} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditSupervisionTeam.managementPageTitle")}</Typography.Title>

      <Space style={{ marginBottom: 12 }}>
        <Button disabled={selected.length !== 1} onClick={() => selected[0] && setResultsFor(selected[0])}>
          {t("auditSupervisionTeam.evaluationResultsButton")}
        </Button>
        <Button disabled={selected.length !== 1} onClick={() => selected[0] && setFilesFor(selected[0])}>
          {t("auditSupervisionTeam.reportFilesButton")}
        </Button>
        <Tooltip title={t("auditSupervisionTeam.checkSampleUploadComingSoon")}>
          <Button disabled>{t("auditSupervisionTeam.checkSampleUploadButton")}</Button>
        </Tooltip>
      </Space>

      <CrudTable<AuditEngagementMonitoringItem>
        tableId={SCREEN_KEY}
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
      />

      <OtherReportFilesDrawer open={!!filesFor} engagementId={filesFor?.id ?? null} onClose={() => setFilesFor(null)} readOnly />
    </div>
  );
}
