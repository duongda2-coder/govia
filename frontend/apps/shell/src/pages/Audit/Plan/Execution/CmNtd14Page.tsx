import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmNtd14,
  deleteAuditCmNtd14,
  exportAuditCmNtd14,
  importAuditCmNtd14,
  listAuditCmNtd14,
  updateAuditCmNtd14,
  type AuditCmNtd14Item,
  type AuditCmNtd14Request,
} from "../../../../api/auditCmNtd14";
import { listAuditEngagements, listEmployeeOptions, type AuditEngagementItem, type EmployeeOption } from "../../../../api/auditEngagement";
import { listAuditProcessStepSummaries, type AuditProcessStepSummaryItem } from "../../../../api/auditProcessStep";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  assignedEmployeeId?: string;
  processStepSummaryId?: string;
  branchCode: string;
  attendanceDate: dayjs.Dayjs;
  staffCode: string;
  staffName?: string;
  attendanceCode?: string;
  description?: string;
  matchedTransactionCount?: number;
  unmatchedTransactionCount?: number;
  adjustedTransactionCount?: number;
  userCode?: string;
  note?: string;
  sampleReason?: string;
  auditResult?: string;
  active: boolean;
}

const numberFormatter = new Intl.NumberFormat("vi-VN");

/** Man hinh "Danh sách chọn mẫu User Ipcas chấm công (08C)" (sheet ZTC_CM_NTD14) - trong nhom "Thuc
 * hien kiem toan" cua "Lap ke hoach". Cac cot IPCAS (ma chi nhanh, ma can bo, ly do chon mau...) la
 * text tu do, khong FK. */
export function CmNtd14Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_NTD14.VIEW");
  const canCreate = hasPermission("AUDIT.CM_NTD14.CREATE");
  const canEdit = hasPermission("AUDIT.CM_NTD14.EDIT");
  const canDelete = hasPermission("AUDIT.CM_NTD14.DELETE");
  const canExport = hasPermission("AUDIT.CM_NTD14.EXPORT");
  const canImport = hasPermission("AUDIT.CM_NTD14.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmNtd14Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmNtd14Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmNtd14Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmNtd14Item | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const [engagements, setEngagements] = useState<AuditEngagementItem[]>([]);
  const [engagementId, setEngagementId] = useState<string | undefined>(undefined);
  const [employees, setEmployees] = useState<EmployeeOption[]>([]);
  const [processStepSummaries, setProcessStepSummaries] = useState<AuditProcessStepSummaryItem[]>([]);

  useEffect(() => {
    if (!canView) return;
    listAuditEngagements().then(setEngagements).catch(() => setEngagements([]));
    listEmployeeOptions().then(setEmployees).catch(() => setEmployees([]));
    listAuditProcessStepSummaries().then(setProcessStepSummaries).catch(() => setProcessStepSummaries([]));
  }, [canView]);

  const load = useCallback(
    async (selectedEngagementId: string) => {
      setLoading(true);
      try {
        setItems(await listAuditCmNtd14(selectedEngagementId));
      } catch {
        message.error(t("auditCmNtd14.messages.loadError"));
      } finally {
        setLoading(false);
      }
    },
    [message, t],
  );

  useEffect(() => {
    if (canView && engagementId) load(engagementId);
    if (!engagementId) setItems([]);
  }, [canView, engagementId, load]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ active: true });
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      assignedEmployeeId: target.assignedEmployeeId ?? undefined,
      processStepSummaryId: target.processStepSummaryId ?? undefined,
      branchCode: target.branchCode,
      attendanceDate: dayjs(target.attendanceDate),
      staffCode: target.staffCode,
      staffName: target.staffName ?? undefined,
      attendanceCode: target.attendanceCode ?? undefined,
      description: target.description ?? undefined,
      matchedTransactionCount: target.matchedTransactionCount ?? undefined,
      unmatchedTransactionCount: target.unmatchedTransactionCount ?? undefined,
      adjustedTransactionCount: target.adjustedTransactionCount ?? undefined,
      userCode: target.userCode ?? undefined,
      note: target.note ?? undefined,
      sampleReason: target.sampleReason ?? undefined,
      auditResult: target.auditResult ?? undefined,
      active: target.active,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    if (!engagementId) return;
    setSubmitting(true);
    try {
      const request: AuditCmNtd14Request = {
        engagementId,
        assignedEmployeeId: values.assignedEmployeeId ?? null,
        processStepSummaryId: values.processStepSummaryId ?? null,
        branchCode: values.branchCode,
        attendanceDate: values.attendanceDate.format("YYYY-MM-DD"),
        staffCode: values.staffCode,
        staffName: values.staffName ?? null,
        attendanceCode: values.attendanceCode ?? null,
        description: values.description ?? null,
        matchedTransactionCount: values.matchedTransactionCount ?? null,
        unmatchedTransactionCount: values.unmatchedTransactionCount ?? null,
        adjustedTransactionCount: values.adjustedTransactionCount ?? null,
        userCode: values.userCode ?? null,
        note: values.note ?? null,
        sampleReason: values.sampleReason ?? null,
        auditResult: values.auditResult ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditCmNtd14(editing.id, request);
        message.success(t("auditCmNtd14.messages.updateSuccess"));
      } else {
        await createAuditCmNtd14(request);
        message.success(t("auditCmNtd14.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load(engagementId);
    } catch {
      message.error(t("auditCmNtd14.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmNtd14.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmNtd14(item.id)));
          message.success(t("auditCmNtd14.messages.deleteSuccess"));
          setSelected([]);
          if (engagementId) await load(engagementId);
        } catch {
          message.error(t("auditCmNtd14.messages.deleteError"));
        }
      },
    });
  };

  const count = (v: number | null) => (v == null ? "-" : numberFormatter.format(v));

  const columns: TableProps<AuditCmNtd14Item>["columns"] = [
    { title: t("auditCmNtd14.columns.assignedUsername"), dataIndex: "assignedUsername", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd14.columns.processStepSummaryCode"), dataIndex: "processStepSummaryCode", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd14.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmNtd14.columns.attendanceDate"), dataIndex: "attendanceDate", width: 120 },
    { title: t("auditCmNtd14.columns.staffCode"), ...getSearchColumnProps("staffCode", searchLabels) },
    { title: t("auditCmNtd14.columns.staffName"), ...getSearchColumnProps("staffName", searchLabels) },
    { title: t("auditCmNtd14.columns.attendanceCode"), dataIndex: "attendanceCode", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd14.columns.description"), dataIndex: "description", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd14.columns.matchedTransactionCount"), dataIndex: "matchedTransactionCount", width: 130, align: "right", render: count },
    { title: t("auditCmNtd14.columns.unmatchedTransactionCount"), dataIndex: "unmatchedTransactionCount", width: 130, align: "right", render: count },
    { title: t("auditCmNtd14.columns.adjustedTransactionCount"), dataIndex: "adjustedTransactionCount", width: 130, align: "right", render: count },
    { title: t("auditCmNtd14.columns.userCode"), dataIndex: "userCode", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd14.columns.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd14.columns.sampleReason"), dataIndex: "sampleReason", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd14.columns.auditResult"), dataIndex: "auditResult", render: (v: string | null) => v ?? "-" },
    {
      title: t("common.active"),
      dataIndex: "active",
      width: 110,
      sorter: (a, b) => Number(a.active) - Number(b.active),
      render: (v: boolean) => (v ? t("common.active") : t("common.inactive")),
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditCmNtd14.title")}</Typography.Title>
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
      </Space>
      <CrudTable<AuditCmNtd14Item>
        tableId="audit.plan.execution.cmNtd14"
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onAdd={canCreate && engagementId ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length === 0}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport && engagementId ? () => exportAuditCmNtd14("excel", engagementId) : undefined}
        onExportWord={canExport && engagementId ? () => exportAuditCmNtd14("word", engagementId) : undefined}
        onImport={
          canImport && engagementId
            ? async (file) => {
                const result = await importAuditCmNtd14(engagementId, file);
                await load(engagementId);
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmNtd14.form.editTitle") : t("auditCmNtd14.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={800}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="assignedEmployeeId" label={t("auditCmNtd14.columns.assignedUsername")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={employees.filter((e) => e.username).map((e) => ({ value: e.id, label: `${e.fullName} (${e.username})` }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="processStepSummaryId" label={t("auditCmNtd14.columns.processStepSummaryCode")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={processStepSummaries.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="branchCode" label={t("auditCmNtd14.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="attendanceDate" label={t("auditCmNtd14.columns.attendanceDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="staffCode" label={t("auditCmNtd14.columns.staffCode")} rules={[{ required: true }]}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="staffName" label={t("auditCmNtd14.columns.staffName")}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="attendanceCode" label={t("auditCmNtd14.columns.attendanceCode")}>
                <Input maxLength={1} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="userCode" label={t("auditCmNtd14.columns.userCode")}>
                <Input maxLength={15} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="description" label={t("auditCmNtd14.columns.description")}>
            <Input maxLength={120} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="matchedTransactionCount" label={t("auditCmNtd14.columns.matchedTransactionCount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="unmatchedTransactionCount" label={t("auditCmNtd14.columns.unmatchedTransactionCount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="adjustedTransactionCount" label={t("auditCmNtd14.columns.adjustedTransactionCount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="sampleReason" label={t("auditCmNtd14.columns.sampleReason")}>
                <Input maxLength={1000} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="auditResult" label={t("auditCmNtd14.columns.auditResult")}>
                <Input maxLength={200} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="note" label={t("auditCmNtd14.columns.note")}>
            <Input.TextArea rows={4} maxLength={1000} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
