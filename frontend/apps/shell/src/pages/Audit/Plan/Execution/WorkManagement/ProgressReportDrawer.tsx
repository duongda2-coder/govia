import { useCallback, useEffect, useState } from "react";
import { App, Button, Drawer, Table, Tag } from "antd";
import type { TableProps } from "antd";
import { CheckCircleOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import dayjs from "dayjs";
import { approveAuditProgressReports, listAuditProgressReports, type AuditProgressReportItem } from "../../../../../api/auditProgressReport";
import { useAuth } from "../../../../../auth/AuthContext";
import type { AuditEngagementItem } from "../../../../../api/auditEngagement";

export interface ProgressReportDrawerProps {
  open: boolean;
  engagementId: string | null;
  engagement: AuditEngagementItem | undefined;
  onClose: () => void;
}

/** "1. Báo cáo tiến độ" (man hinh "Quản lý công việc THKT") - bang doc, sinh tu dong khi upload
 * file TTSS (xem AuditProgressReportService.recordUpload), truong doan phe duyet hang loat. */
export function ProgressReportDrawer({ open, engagementId, engagement, onClose }: ProgressReportDrawerProps) {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { user } = useAuth();

  const [items, setItems] = useState<AuditProgressReportItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  const isTeamLead = !!user?.employeeCode && !!engagement && user.employeeCode === engagement.teamLeadEmployeeCode;

  const load = useCallback(async () => {
    if (!engagementId) return;
    setLoading(true);
    try {
      setItems(await listAuditProgressReports(engagementId));
    } catch {
      message.error(t("auditProgressReport.loadError"));
    } finally {
      setLoading(false);
    }
  }, [engagementId, message, t]);

  useEffect(() => {
    if (open) load();
  }, [open, load]);

  const handleApprove = () => {
    if (!engagementId || selectedIds.length === 0) return;
    modal.confirm({
      title: t("auditProgressReport.approveConfirmTitle", { count: selectedIds.length }),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      onOk: async () => {
        try {
          await approveAuditProgressReports(engagementId, selectedIds);
          message.success(t("auditProgressReport.approveSuccess"));
          setSelectedIds([]);
          await load();
        } catch {
          message.error(t("auditProgressReport.approveError"));
        }
      },
    });
  };

  const selected = items.filter((i) => selectedIds.includes(i.id));
  const approveDisabled = selected.length === 0 || !isTeamLead || selected.some((i) => i.approvalStatus === "APPROVED");

  const columns: TableProps<AuditProgressReportItem>["columns"] = [
    { title: t("auditProgressReport.columns.businessSegment"), dataIndex: "businessSegmentCode", width: 110, render: (v) => v ?? "-" },
    { title: t("auditProgressReport.columns.totalFindings"), dataIndex: "totalFindings", width: 100 },
    { title: t("auditProgressReport.columns.totalTtss"), dataIndex: "totalTtss", width: 100 },
    { title: t("auditProgressReport.columns.totalMaterialFindings"), dataIndex: "totalMaterialFindings", width: 110 },
    { title: t("auditProgressReport.columns.totalMaterialTtss"), dataIndex: "totalMaterialTtss", width: 110 },
    { title: t("auditProgressReport.columns.totalSamples"), dataIndex: "totalSamples", width: 100 },
    { title: t("auditProgressReport.columns.completedSamples"), dataIndex: "completedSamples", width: 110 },
    { title: t("auditProgressReport.columns.reportDate"), dataIndex: "reportDate", width: 120, render: (v: string) => dayjs(v).format("DD.MM.YYYY") },
    { title: t("auditProgressReport.columns.reportRound"), dataIndex: "reportRound", width: 100, render: (v: number) => `Lần ${v}` },
    { title: t("auditProgressReport.columns.reportedByUsername"), dataIndex: "reportedByUsername", width: 130, render: (v) => v ?? "-" },
    { title: t("auditProgressReport.columns.note"), dataIndex: "note", render: (v) => v ?? "-" },
    {
      title: t("auditProgressReport.columns.approvalStatus"),
      width: 130,
      render: (_: unknown, item: AuditProgressReportItem) => (
        <Tag color={item.approvalStatus === "APPROVED" ? "success" : item.approvalStatus === "PENDING" ? "processing" : "default"}>
          {item.approvalStatus ? t(`auditWorkManagement.approvalStatus.${item.approvalStatus}`) : "-"}
        </Tag>
      ),
    },
  ];

  return (
    <Drawer title={t("auditProgressReport.title")} open={open} onClose={onClose} width={1000} destroyOnClose>
      <Button
        type="primary"
        icon={<CheckCircleOutlined />}
        disabled={approveDisabled}
        onClick={handleApprove}
        style={{ marginBottom: 16 }}
      >
        {t("common.approve")}
      </Button>
      <Table<AuditProgressReportItem>
        rowKey="id"
        loading={loading}
        dataSource={items}
        columns={columns}
        pagination={false}
        rowSelection={{ selectedRowKeys: selectedIds, onChange: (keys) => setSelectedIds(keys as string[]) }}
        scroll={{ x: "max-content" }}
      />
    </Drawer>
  );
}
