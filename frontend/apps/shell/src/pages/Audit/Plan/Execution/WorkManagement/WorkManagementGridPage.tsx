import { useCallback, useEffect, useState } from "react";
import { App, Button, Form, Input, Modal, Result, Select, Space, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { FileTextOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { CrudTable } from "@govia/ui-kit";
import {
  approveAuditWorkAssignments,
  listAuditWorkManagement,
  updateAuditWorkAssignmentStatus,
  type AssignmentStatus,
  type AuditWorkManagementItem,
  type AuditWorkPhase,
} from "../../../../../api/auditWorkManagement";
import { listAuditEngagements, type AuditEngagementItem } from "../../../../../api/auditEngagement";
import { useAuth } from "../../../../../auth/AuthContext";
import { OtherReportFilesDrawer } from "./OtherReportFilesDrawer";
import { ProgressReportDrawer } from "./ProgressReportDrawer";

interface FormValues {
  status: AssignmentStatus;
  note?: string;
}

const STATUS_COLORS: Record<AssignmentStatus, string> = {
  NOT_STARTED: "default",
  IN_PROGRESS: "processing",
  DONE: "success",
};

export interface WorkManagementGridPageProps {
  phase: AuditWorkPhase;
  tableId: string;
  title: string;
  /** Chi THKT co nut "Báo cáo tiến độ" (Khối B) - xem dac ta. */
  showProgressReport?: boolean;
}

/** Grid dung chung cho "Quản lý công việc CBKT" va "Quản lý công việc THKT" (sheet cung ten trong
 * Tạo CKT (1).xlsx) - chi khac nhau o phase truyen vao, cot va hanh vi con lai giong het nhau. */
export function WorkManagementGridPage({ phase, tableId, title, showProgressReport }: WorkManagementGridPageProps) {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { user, hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.WORK_MANAGEMENT.VIEW");
  const canEdit = hasPermission("AUDIT.WORK_MANAGEMENT.EDIT");
  const canApprove = hasPermission("AUDIT.WORK_MANAGEMENT.APPROVE");

  const [engagements, setEngagements] = useState<AuditEngagementItem[]>([]);
  const [engagementId, setEngagementId] = useState<string | undefined>(undefined);
  const [items, setItems] = useState<AuditWorkManagementItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditWorkManagementItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [reportFilesOpen, setReportFilesOpen] = useState(false);
  const [progressReportOpen, setProgressReportOpen] = useState(false);
  const [form] = Form.useForm<FormValues>();

  useEffect(() => {
    if (!canView) return;
    listAuditEngagements().then(setEngagements).catch(() => setEngagements([]));
  }, [canView]);

  const load = useCallback(
    async (selectedEngagementId: string) => {
      setLoading(true);
      try {
        setItems(await listAuditWorkManagement(selectedEngagementId, phase));
      } catch {
        message.error(t("auditWorkManagement.messages.loadError"));
      } finally {
        setLoading(false);
      }
    },
    [phase, message, t],
  );

  useEffect(() => {
    if (canView && engagementId) load(engagementId);
    if (!engagementId) setItems([]);
  }, [canView, engagementId, load]);

  const currentEngagement = engagements.find((e) => e.id === engagementId);
  const isTeamLead = !!user?.employeeCode && !!currentEngagement && user.employeeCode === currentEngagement.teamLeadEmployeeCode;

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    form.setFieldsValue({ status: target.status, note: target.note ?? undefined });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const target = selected[0];
    if (!target || !engagementId) return;
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await updateAuditWorkAssignmentStatus(engagementId, target.assignmentId, { status: values.status, note: values.note ?? null });
      message.success(t("auditWorkManagement.messages.updateSuccess"));
      setModalOpen(false);
      setSelected([]);
      await load(engagementId);
    } catch {
      message.error(t("auditWorkManagement.messages.updateError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleApprove = () => {
    if (!engagementId || selected.length === 0) return;
    modal.confirm({
      title: t("auditWorkManagement.approveConfirmTitle", { count: selected.length }),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      onOk: async () => {
        try {
          await approveAuditWorkAssignments(
            engagementId,
            selected.map((i) => i.assignmentId),
          );
          message.success(t("auditWorkManagement.messages.approveSuccess"));
          setSelected([]);
          await load(engagementId);
        } catch {
          message.error(t("auditWorkManagement.messages.approveError"));
        }
      },
    });
  };

  const statusLabel = (status: AssignmentStatus) => t(`auditWorkManagement.status.${status}`);
  const approvalLabel = (item: AuditWorkManagementItem) =>
    item.approvalStatus ? t(`auditWorkManagement.approvalStatus.${item.approvalStatus}`) : "-";

  const columns: TableProps<AuditWorkManagementItem>["columns"] = [
    { title: t("auditWorkManagement.columns.engagementCode"), dataIndex: "engagementCode", width: 130 },
    { title: t("auditWorkManagement.columns.engagementName"), dataIndex: "engagementName", width: 160, render: (v) => v ?? "-" },
    { title: t("auditWorkManagement.columns.businessSegmentCode"), dataIndex: "businessSegmentCode", width: 130, render: (v) => v ?? "-" },
    { title: t("auditWorkManagement.columns.workItemCode"), dataIndex: "workItemCode", width: 120 },
    { title: t("auditWorkManagement.columns.workItemName"), dataIndex: "workItemName" },
    { title: t("auditWorkManagement.columns.employee"), dataIndex: "employeeUsername", width: 140, render: (v, r) => v ?? r.employeeName ?? "-" },
    {
      title: t("auditWorkManagement.columns.status"),
      dataIndex: "status",
      width: 140,
      render: (v: AssignmentStatus) => <Tag color={STATUS_COLORS[v]}>{statusLabel(v)}</Tag>,
    },
    {
      title: t("auditWorkManagement.columns.approvalStatus"),
      width: 140,
      render: (_: unknown, item: AuditWorkManagementItem) => approvalLabel(item),
    },
    { title: t("auditWorkManagement.columns.note"), dataIndex: "note", render: (v) => v ?? "-" },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  const approveDisabled = selected.length === 0 || !isTeamLead || selected.some((i) => i.status !== "DONE" || i.approvalStatus === "APPROVED");

  return (
    <div>
      <Typography.Title level={4}>{title}</Typography.Title>
      <Space style={{ marginBottom: 16 }}>
        <Typography.Text>{t("auditPlanExecution.engagementFilter")}</Typography.Text>
        <Select
          style={{ width: 220 }}
          showSearch
          optionFilterProp="label"
          placeholder={t("auditPlanExecution.selectEngagement")}
          options={engagements.map((e) => ({ value: e.id, label: e.code }))}
          value={engagementId}
          onChange={setEngagementId}
          allowClear
        />
        {showProgressReport && engagementId && (
          <Button icon={<FileTextOutlined />} onClick={() => setProgressReportOpen(true)}>
            {t("auditProgressReport.title")}
          </Button>
        )}
      </Space>
      <CrudTable<AuditWorkManagementItem>
        tableId={tableId}
        columns={columns}
        dataSource={items}
        rowKey="assignmentId"
        loading={loading}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onOtherReports={engagementId ? () => setReportFilesOpen(true) : undefined}
        onApprove={canApprove && engagementId ? handleApprove : undefined}
        approveDisabled={approveDisabled}
      />

      <Modal
        title={t("auditWorkManagement.form.editTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="status" label={t("auditWorkManagement.columns.status")} rules={[{ required: true }]}>
            <Select
              options={(["NOT_STARTED", "IN_PROGRESS", "DONE"] as AssignmentStatus[]).map((s) => ({ value: s, label: statusLabel(s) }))}
            />
          </Form.Item>
          <Form.Item name="note" label={t("auditWorkManagement.columns.note")}>
            <Input.TextArea rows={3} maxLength={2000} />
          </Form.Item>
        </Form>
      </Modal>

      <OtherReportFilesDrawer open={reportFilesOpen} engagementId={engagementId ?? null} onClose={() => setReportFilesOpen(false)} />
      {showProgressReport && (
        <ProgressReportDrawer
          open={progressReportOpen}
          engagementId={engagementId ?? null}
          engagement={currentEngagement}
          onClose={() => setProgressReportOpen(false)}
        />
      )}
    </div>
  );
}
