import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmTd1,
  deleteAuditCmTd1,
  exportAuditCmTd1,
  importAuditCmTd1,
  listAuditCmTd1,
  updateAuditCmTd1,
  type AuditCmTd1Item,
  type AuditCmTd1Request,
} from "../../../../api/auditCmTd1";
import { listAuditEngagements, listEmployeeOptions, type AuditEngagementItem, type EmployeeOption } from "../../../../api/auditEngagement";
import { listAuditProcessStepSummaries, type AuditProcessStepSummaryItem } from "../../../../api/auditProcessStep";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  assignedEmployeeId?: string;
  processStepSummaryId?: string;
  branchCode: string;
  auditDate: dayjs.Dayjs;
  customerCode?: string;
  sampleFilterUser?: string;
  customerName: string;
  approvedAmount?: number;
  loanPurpose?: string;
  description?: string;
  onBalanceDebt?: number;
  guaranteeBalance?: number;
  riskClassifiedDebt?: number;
  vamcSoldDebt?: number;
  debtGroup?: string;
  auditScope?: string;
  auditorCode?: string;
  sampleReason?: string;
  note?: string;
  active: boolean;
}

const numberFormatter = new Intl.NumberFormat("vi-VN");

/** Man hinh "Danh sach khach hang chon mau tin dung" (sheet ZTC_CM_TD1) - trong nhom "Thuc hien kiem
 * toan" cua "Lap ke hoach". Cac cot IPCAS (ma/ten KH, can bo...) la text tu do, khong FK. */
export function CmTd1Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_TD1.VIEW");
  const canCreate = hasPermission("AUDIT.CM_TD1.CREATE");
  const canEdit = hasPermission("AUDIT.CM_TD1.EDIT");
  const canDelete = hasPermission("AUDIT.CM_TD1.DELETE");
  const canExport = hasPermission("AUDIT.CM_TD1.EXPORT");
  const canImport = hasPermission("AUDIT.CM_TD1.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmTd1Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmTd1Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmTd1Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmTd1Item | null>(null);
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
        setItems(await listAuditCmTd1(selectedEngagementId));
      } catch {
        message.error(t("auditCmTd1.messages.loadError"));
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
      auditDate: dayjs(target.auditDate),
      customerCode: target.customerCode ?? undefined,
      sampleFilterUser: target.sampleFilterUser ?? undefined,
      customerName: target.customerName,
      approvedAmount: target.approvedAmount ?? undefined,
      loanPurpose: target.loanPurpose ?? undefined,
      description: target.description ?? undefined,
      onBalanceDebt: target.onBalanceDebt ?? undefined,
      guaranteeBalance: target.guaranteeBalance ?? undefined,
      riskClassifiedDebt: target.riskClassifiedDebt ?? undefined,
      vamcSoldDebt: target.vamcSoldDebt ?? undefined,
      debtGroup: target.debtGroup ?? undefined,
      auditScope: target.auditScope ?? undefined,
      auditorCode: target.auditorCode ?? undefined,
      sampleReason: target.sampleReason ?? undefined,
      note: target.note ?? undefined,
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
      const request: AuditCmTd1Request = {
        engagementId,
        assignedEmployeeId: values.assignedEmployeeId ?? null,
        processStepSummaryId: values.processStepSummaryId ?? null,
        branchCode: values.branchCode,
        auditDate: values.auditDate.format("YYYY-MM-DD"),
        customerCode: values.customerCode ?? null,
        sampleFilterUser: values.sampleFilterUser ?? null,
        customerName: values.customerName,
        approvedAmount: values.approvedAmount ?? null,
        loanPurpose: values.loanPurpose ?? null,
        description: values.description ?? null,
        onBalanceDebt: values.onBalanceDebt ?? null,
        guaranteeBalance: values.guaranteeBalance ?? null,
        riskClassifiedDebt: values.riskClassifiedDebt ?? null,
        vamcSoldDebt: values.vamcSoldDebt ?? null,
        debtGroup: values.debtGroup ?? null,
        auditScope: values.auditScope ?? null,
        auditorCode: values.auditorCode ?? null,
        sampleReason: values.sampleReason ?? null,
        note: values.note ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditCmTd1(editing.id, request);
        message.success(t("auditCmTd1.messages.updateSuccess"));
      } else {
        await createAuditCmTd1(request);
        message.success(t("auditCmTd1.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load(engagementId);
    } catch {
      message.error(t("auditCmTd1.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmTd1.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmTd1(item.id)));
          message.success(t("auditCmTd1.messages.deleteSuccess"));
          setSelected([]);
          if (engagementId) await load(engagementId);
        } catch {
          message.error(t("auditCmTd1.messages.deleteError"));
        }
      },
    });
  };

  const money = (v: number | null) => (v == null ? "-" : numberFormatter.format(v));

  const columns: TableProps<AuditCmTd1Item>["columns"] = [
    { title: t("auditCmTd1.columns.assignedUsername"), dataIndex: "assignedUsername", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd1.columns.processStepSummaryCode"), dataIndex: "processStepSummaryCode", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd1.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmTd1.columns.auditDate"), dataIndex: "auditDate", width: 120 },
    { title: t("auditCmTd1.columns.customerCode"), dataIndex: "customerCode", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd1.columns.sampleFilterUser"), dataIndex: "sampleFilterUser", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd1.columns.customerName"), ...getSearchColumnProps("customerName", searchLabels) },
    { title: t("auditCmTd1.columns.approvedAmount"), dataIndex: "approvedAmount", width: 150, align: "right", render: money },
    { title: t("auditCmTd1.columns.loanPurpose"), dataIndex: "loanPurpose", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd1.columns.description"), dataIndex: "description", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd1.columns.onBalanceDebt"), dataIndex: "onBalanceDebt", width: 140, align: "right", render: money },
    { title: t("auditCmTd1.columns.guaranteeBalance"), dataIndex: "guaranteeBalance", width: 140, align: "right", render: money },
    { title: t("auditCmTd1.columns.riskClassifiedDebt"), dataIndex: "riskClassifiedDebt", width: 140, align: "right", render: money },
    { title: t("auditCmTd1.columns.vamcSoldDebt"), dataIndex: "vamcSoldDebt", width: 140, align: "right", render: money },
    { title: t("auditCmTd1.columns.totalCreditBalance"), dataIndex: "totalCreditBalance", width: 150, align: "right", render: money },
    { title: t("auditCmTd1.columns.debtGroup"), dataIndex: "debtGroup", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd1.columns.auditScope"), dataIndex: "auditScope", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd1.columns.auditorCode"), dataIndex: "auditorCode", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd1.columns.sampleReason"), dataIndex: "sampleReason", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd1.columns.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmTd1.title")}</Typography.Title>
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
      <CrudTable<AuditCmTd1Item>
        tableId="audit.plan.execution.cmTd1"
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
        onExportExcel={canExport && engagementId ? () => exportAuditCmTd1("excel", engagementId) : undefined}
        onExportWord={canExport && engagementId ? () => exportAuditCmTd1("word", engagementId) : undefined}
        onImport={
          canImport && engagementId
            ? async (file) => {
                const result = await importAuditCmTd1(engagementId, file);
                await load(engagementId);
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmTd1.form.editTitle") : t("auditCmTd1.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={720}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="assignedEmployeeId" label={t("auditCmTd1.columns.assignedUsername")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={employees.filter((e) => e.username).map((e) => ({ value: e.id, label: `${e.fullName} (${e.username})` }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="processStepSummaryId" label={t("auditCmTd1.columns.processStepSummaryCode")}>
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
              <Form.Item name="branchCode" label={t("auditCmTd1.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="auditDate" label={t("auditCmTd1.columns.auditDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="customerCode" label={t("auditCmTd1.columns.customerCode")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="sampleFilterUser" label={t("auditCmTd1.columns.sampleFilterUser")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={16}>
              <Form.Item name="customerName" label={t("auditCmTd1.columns.customerName")} rules={[{ required: true }]}>
                <Input maxLength={200} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="approvedAmount" label={t("auditCmTd1.columns.approvedAmount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="loanPurpose" label={t("auditCmTd1.columns.loanPurpose")}>
                <Input maxLength={60} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="description" label={t("auditCmTd1.columns.description")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={6}>
              <Form.Item name="onBalanceDebt" label={t("auditCmTd1.columns.onBalanceDebt")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="guaranteeBalance" label={t("auditCmTd1.columns.guaranteeBalance")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="riskClassifiedDebt" label={t("auditCmTd1.columns.riskClassifiedDebt")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="vamcSoldDebt" label={t("auditCmTd1.columns.vamcSoldDebt")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="debtGroup" label={t("auditCmTd1.columns.debtGroup")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="auditorCode" label={t("auditCmTd1.columns.auditorCode")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="auditScope" label={t("auditCmTd1.columns.auditScope")}>
            <Input maxLength={120} />
          </Form.Item>
          <Form.Item name="sampleReason" label={t("auditCmTd1.columns.sampleReason")}>
            <Input.TextArea rows={2} maxLength={1000} />
          </Form.Item>
          <Form.Item name="note" label={t("auditCmTd1.columns.note")}>
            <Input.TextArea rows={2} maxLength={120} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
