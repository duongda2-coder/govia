import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmNtd3,
  deleteAuditCmNtd3,
  exportAuditCmNtd3,
  importAuditCmNtd3,
  listAuditCmNtd3,
  updateAuditCmNtd3,
  type AuditCmNtd3Item,
  type AuditCmNtd3Request,
} from "../../../../api/auditCmNtd3";
import { listAuditEngagements, listEmployeeOptions, type AuditEngagementItem, type EmployeeOption } from "../../../../api/auditEngagement";
import { listAuditProcessStepSummaries, type AuditProcessStepSummaryItem } from "../../../../api/auditProcessStep";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  assignedEmployeeId?: string;
  processStepSummaryId?: string;
  branchCode: string;
  transactionDate: dayjs.Dayjs;
  customerCode?: string;
  customerName: string;
  customerAddress?: string;
  accountNumber: string;
  currency?: string;
  originalCurrencyBalance?: number;
  convertedBalance?: number;
  auditResult?: string;
  recommendationType?: string;
  transactionStaff?: string;
  controlUser?: string;
  controlStaff?: string;
  controlStaffTitle?: string;
  active: boolean;
}

const numberFormatter = new Intl.NumberFormat("vi-VN");

/** Man hinh "Danh sach chon mau khach hang to chuc tien gui HDV" (sheet ZTC_CM_NTD3) - trong nhom
 * "Thuc hien kiem toan" cua "Lap ke hoach". Cac cot IPCAS (ma/ten/dia chi KH, can bo...) la text tu
 * do, khong FK. */
export function CmNtd3Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_NTD3.VIEW");
  const canCreate = hasPermission("AUDIT.CM_NTD3.CREATE");
  const canEdit = hasPermission("AUDIT.CM_NTD3.EDIT");
  const canDelete = hasPermission("AUDIT.CM_NTD3.DELETE");
  const canExport = hasPermission("AUDIT.CM_NTD3.EXPORT");
  const canImport = hasPermission("AUDIT.CM_NTD3.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmNtd3Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmNtd3Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmNtd3Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmNtd3Item | null>(null);
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
        setItems(await listAuditCmNtd3(selectedEngagementId));
      } catch {
        message.error(t("auditCmNtd3.messages.loadError"));
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
      customerCode: target.customerCode ?? undefined,
      customerName: target.customerName,
      customerAddress: target.customerAddress ?? undefined,
      accountNumber: target.accountNumber,
      currency: target.currency ?? undefined,
      originalCurrencyBalance: target.originalCurrencyBalance ?? undefined,
      convertedBalance: target.convertedBalance ?? undefined,
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
      const request: AuditCmNtd3Request = {
        engagementId,
        assignedEmployeeId: values.assignedEmployeeId ?? null,
        processStepSummaryId: values.processStepSummaryId ?? null,
        branchCode: values.branchCode,
        transactionDate: values.transactionDate.format("YYYY-MM-DD"),
        customerCode: values.customerCode ?? null,
        customerName: values.customerName,
        customerAddress: values.customerAddress ?? null,
        accountNumber: values.accountNumber,
        currency: values.currency ?? null,
        originalCurrencyBalance: values.originalCurrencyBalance ?? null,
        convertedBalance: values.convertedBalance ?? null,
        auditResult: values.auditResult ?? null,
        recommendationType: values.recommendationType ?? null,
        transactionStaff: values.transactionStaff ?? null,
        controlUser: values.controlUser ?? null,
        controlStaff: values.controlStaff ?? null,
        controlStaffTitle: values.controlStaffTitle ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditCmNtd3(editing.id, request);
        message.success(t("auditCmNtd3.messages.updateSuccess"));
      } else {
        await createAuditCmNtd3(request);
        message.success(t("auditCmNtd3.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load(engagementId);
    } catch {
      message.error(t("auditCmNtd3.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmNtd3.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmNtd3(item.id)));
          message.success(t("auditCmNtd3.messages.deleteSuccess"));
          setSelected([]);
          if (engagementId) await load(engagementId);
        } catch {
          message.error(t("auditCmNtd3.messages.deleteError"));
        }
      },
    });
  };

  const money = (v: number | null) => (v == null ? "-" : numberFormatter.format(v));

  const columns: TableProps<AuditCmNtd3Item>["columns"] = [
    { title: t("auditCmNtd3.columns.assignedUsername"), dataIndex: "assignedUsername", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd3.columns.processStepSummaryCode"), dataIndex: "processStepSummaryCode", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd3.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmNtd3.columns.transactionDate"), dataIndex: "transactionDate", width: 130 },
    { title: t("auditCmNtd3.columns.customerCode"), dataIndex: "customerCode", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd3.columns.customerName"), ...getSearchColumnProps("customerName", searchLabels) },
    { title: t("auditCmNtd3.columns.customerAddress"), dataIndex: "customerAddress", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd3.columns.accountNumber"), dataIndex: "accountNumber", width: 130 },
    { title: t("auditCmNtd3.columns.currency"), dataIndex: "currency", width: 90, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd3.columns.originalCurrencyBalance"), dataIndex: "originalCurrencyBalance", width: 150, align: "right", render: money },
    { title: t("auditCmNtd3.columns.convertedBalance"), dataIndex: "convertedBalance", width: 150, align: "right", render: money },
    { title: t("auditCmNtd3.columns.auditResult"), dataIndex: "auditResult", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd3.columns.recommendationType"), dataIndex: "recommendationType", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd3.columns.transactionStaff"), dataIndex: "transactionStaff", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd3.columns.controlUser"), dataIndex: "controlUser", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd3.columns.controlStaff"), dataIndex: "controlStaff", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd3.columns.controlStaffTitle"), dataIndex: "controlStaffTitle", width: 170, render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmNtd3.title")}</Typography.Title>
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
      <CrudTable<AuditCmNtd3Item>
        tableId="audit.plan.execution.cmNtd3"
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
        onExportExcel={canExport && engagementId ? () => exportAuditCmNtd3("excel", engagementId) : undefined}
        onExportWord={canExport && engagementId ? () => exportAuditCmNtd3("word", engagementId) : undefined}
        onImport={
          canImport && engagementId
            ? async (file) => {
                const result = await importAuditCmNtd3(engagementId, file);
                await load(engagementId);
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmNtd3.form.editTitle") : t("auditCmNtd3.form.createTitle")}
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
              <Form.Item name="assignedEmployeeId" label={t("auditCmNtd3.columns.assignedUsername")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={employees.filter((e) => e.username).map((e) => ({ value: e.id, label: `${e.fullName} (${e.username})` }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="processStepSummaryId" label={t("auditCmNtd3.columns.processStepSummaryCode")}>
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
              <Form.Item name="branchCode" label={t("auditCmNtd3.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="transactionDate" label={t("auditCmNtd3.columns.transactionDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="customerCode" label={t("auditCmNtd3.columns.customerCode")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="customerName" label={t("auditCmNtd3.columns.customerName")} rules={[{ required: true }]}>
                <Input maxLength={200} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="customerAddress" label={t("auditCmNtd3.columns.customerAddress")}>
                <Input maxLength={200} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="accountNumber" label={t("auditCmNtd3.columns.accountNumber")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="currency" label={t("auditCmNtd3.columns.currency")}>
                <Input maxLength={3} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="originalCurrencyBalance" label={t("auditCmNtd3.columns.originalCurrencyBalance")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="convertedBalance" label={t("auditCmNtd3.columns.convertedBalance")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="auditResult" label={t("auditCmNtd3.columns.auditResult")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="recommendationType" label={t("auditCmNtd3.columns.recommendationType")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="transactionStaff" label={t("auditCmNtd3.columns.transactionStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="controlUser" label={t("auditCmNtd3.columns.controlUser")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="controlStaff" label={t("auditCmNtd3.columns.controlStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="controlStaffTitle" label={t("auditCmNtd3.columns.controlStaffTitle")}>
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
