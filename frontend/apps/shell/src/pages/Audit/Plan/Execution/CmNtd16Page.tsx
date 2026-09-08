import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmNtd16,
  deleteAuditCmNtd16,
  exportAuditCmNtd16,
  importAuditCmNtd16,
  listAuditCmNtd16,
  updateAuditCmNtd16,
  type AuditCmNtd16Item,
  type AuditCmNtd16Request,
} from "../../../../api/auditCmNtd16";
import { listAuditEngagements, listEmployeeOptions, type AuditEngagementItem, type EmployeeOption } from "../../../../api/auditEngagement";
import { listAuditProcessStepSummaries, type AuditProcessStepSummaryItem } from "../../../../api/auditProcessStep";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  assignedEmployeeId?: string;
  processStepSummaryId?: string;
  branchCode: string;
  transactionDate: dayjs.Dayjs;
  postingUser: string;
  entryNumber: number;
  debitAmount?: number;
  creditAmount?: number;
  transactionStatus?: string;
  currency?: string;
  accountNumber?: string;
  content?: string;
  sampleReason?: string;
  auditResult?: string;
  recommendationType?: string;
  transactionStaff?: string;
  controlUser?: string;
  controlStaff?: string;
  controlStaffTitle?: string;
  active: boolean;
}

const numberFormatter = new Intl.NumberFormat("vi-VN");

/** Man hinh "Danh sách các bút toán chọn mẫu nghiệp vụ HDV (03G)" (sheet ZTC_CM_NTD16) - trong nhom
 * "Thuc hien kiem toan" cua "Lap ke hoach". Cung cau truc voi CmNtd12Page (07F/CD)/CmNtd15Page
 * (05D/MF). Cac cot IPCAS (ma chi nhanh, trang thai giao dich...) la text tu do, khong FK. */
export function CmNtd16Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_NTD16.VIEW");
  const canCreate = hasPermission("AUDIT.CM_NTD16.CREATE");
  const canEdit = hasPermission("AUDIT.CM_NTD16.EDIT");
  const canDelete = hasPermission("AUDIT.CM_NTD16.DELETE");
  const canExport = hasPermission("AUDIT.CM_NTD16.EXPORT");
  const canImport = hasPermission("AUDIT.CM_NTD16.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmNtd16Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmNtd16Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmNtd16Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmNtd16Item | null>(null);
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
        setItems(await listAuditCmNtd16(selectedEngagementId));
      } catch {
        message.error(t("auditCmNtd16.messages.loadError"));
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
      transactionDate: dayjs(target.transactionDate),
      postingUser: target.postingUser,
      entryNumber: target.entryNumber ?? undefined,
      debitAmount: target.debitAmount ?? undefined,
      creditAmount: target.creditAmount ?? undefined,
      transactionStatus: target.transactionStatus ?? undefined,
      currency: target.currency ?? undefined,
      accountNumber: target.accountNumber ?? undefined,
      content: target.content ?? undefined,
      sampleReason: target.sampleReason ?? undefined,
      auditResult: target.auditResult ?? undefined,
      recommendationType: target.recommendationType ?? undefined,
      transactionStaff: target.transactionStaff ?? undefined,
      controlUser: target.controlUser ?? undefined,
      controlStaff: target.controlStaff ?? undefined,
      controlStaffTitle: target.controlStaffTitle ?? undefined,
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
      const request: AuditCmNtd16Request = {
        engagementId,
        assignedEmployeeId: values.assignedEmployeeId ?? null,
        processStepSummaryId: values.processStepSummaryId ?? null,
        branchCode: values.branchCode,
        transactionDate: values.transactionDate.format("YYYY-MM-DD"),
        postingUser: values.postingUser,
        entryNumber: values.entryNumber,
        debitAmount: values.debitAmount ?? null,
        creditAmount: values.creditAmount ?? null,
        transactionStatus: values.transactionStatus ?? null,
        currency: values.currency ?? null,
        accountNumber: values.accountNumber ?? null,
        content: values.content ?? null,
        sampleReason: values.sampleReason ?? null,
        auditResult: values.auditResult ?? null,
        recommendationType: values.recommendationType ?? null,
        transactionStaff: values.transactionStaff ?? null,
        controlUser: values.controlUser ?? null,
        controlStaff: values.controlStaff ?? null,
        controlStaffTitle: values.controlStaffTitle ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditCmNtd16(editing.id, request);
        message.success(t("auditCmNtd16.messages.updateSuccess"));
      } else {
        await createAuditCmNtd16(request);
        message.success(t("auditCmNtd16.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load(engagementId);
    } catch {
      message.error(t("auditCmNtd16.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmNtd16.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmNtd16(item.id)));
          message.success(t("auditCmNtd16.messages.deleteSuccess"));
          setSelected([]);
          if (engagementId) await load(engagementId);
        } catch {
          message.error(t("auditCmNtd16.messages.deleteError"));
        }
      },
    });
  };

  const money = (v: number | null) => (v == null ? "-" : numberFormatter.format(v));

  const columns: TableProps<AuditCmNtd16Item>["columns"] = [
    { title: t("auditCmNtd16.columns.assignedUsername"), dataIndex: "assignedUsername", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd16.columns.processStepSummaryCode"), dataIndex: "processStepSummaryCode", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd16.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmNtd16.columns.transactionDate"), dataIndex: "transactionDate", width: 130 },
    { title: t("auditCmNtd16.columns.postingUser"), dataIndex: "postingUser", width: 130 },
    { title: t("auditCmNtd16.columns.entryNumber"), dataIndex: "entryNumber", width: 140, align: "right", render: money },
    { title: t("auditCmNtd16.columns.debitAmount"), dataIndex: "debitAmount", width: 150, align: "right", render: money },
    { title: t("auditCmNtd16.columns.creditAmount"), dataIndex: "creditAmount", width: 150, align: "right", render: money },
    { title: t("auditCmNtd16.columns.transactionStatus"), dataIndex: "transactionStatus", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd16.columns.currency"), dataIndex: "currency", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd16.columns.accountNumber"), dataIndex: "accountNumber", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd16.columns.content"), dataIndex: "content", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd16.columns.sampleReason"), dataIndex: "sampleReason", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd16.columns.auditResult"), dataIndex: "auditResult", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd16.columns.recommendationType"), dataIndex: "recommendationType", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd16.columns.transactionStaff"), dataIndex: "transactionStaff", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd16.columns.controlUser"), dataIndex: "controlUser", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd16.columns.controlStaff"), dataIndex: "controlStaff", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd16.columns.controlStaffTitle"), dataIndex: "controlStaffTitle", render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmNtd16.title")}</Typography.Title>
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
      <CrudTable<AuditCmNtd16Item>
        tableId="audit.plan.execution.cmNtd16"
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
        onExportExcel={canExport && engagementId ? () => exportAuditCmNtd16("excel", engagementId) : undefined}
        onExportWord={canExport && engagementId ? () => exportAuditCmNtd16("word", engagementId) : undefined}
        onImport={
          canImport && engagementId
            ? async (file) => {
                const result = await importAuditCmNtd16(engagementId, file);
                await load(engagementId);
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmNtd16.form.editTitle") : t("auditCmNtd16.form.createTitle")}
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
              <Form.Item name="assignedEmployeeId" label={t("auditCmNtd16.columns.assignedUsername")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={employees.filter((e) => e.username).map((e) => ({ value: e.id, label: `${e.fullName} (${e.username})` }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="processStepSummaryId" label={t("auditCmNtd16.columns.processStepSummaryCode")}>
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
              <Form.Item name="branchCode" label={t("auditCmNtd16.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="transactionDate" label={t("auditCmNtd16.columns.transactionDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="postingUser" label={t("auditCmNtd16.columns.postingUser")} rules={[{ required: true }]}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="entryNumber" label={t("auditCmNtd16.columns.entryNumber")} rules={[{ required: true }]}>
                <InputNumber style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="debitAmount" label={t("auditCmNtd16.columns.debitAmount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="creditAmount" label={t("auditCmNtd16.columns.creditAmount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="transactionStatus" label={t("auditCmNtd16.columns.transactionStatus")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="currency" label={t("auditCmNtd16.columns.currency")}>
                <Input maxLength={3} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="accountNumber" label={t("auditCmNtd16.columns.accountNumber")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="content" label={t("auditCmNtd16.columns.content")}>
            <Input maxLength={500} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="sampleReason" label={t("auditCmNtd16.columns.sampleReason")}>
                <Input maxLength={1000} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="auditResult" label={t("auditCmNtd16.columns.auditResult")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="recommendationType" label={t("auditCmNtd16.columns.recommendationType")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="transactionStaff" label={t("auditCmNtd16.columns.transactionStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="controlUser" label={t("auditCmNtd16.columns.controlUser")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="controlStaff" label={t("auditCmNtd16.columns.controlStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="controlStaffTitle" label={t("auditCmNtd16.columns.controlStaffTitle")}>
            <Input maxLength={120} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
