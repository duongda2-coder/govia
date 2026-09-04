import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmNtd11,
  deleteAuditCmNtd11,
  exportAuditCmNtd11,
  importAuditCmNtd11,
  listAuditCmNtd11,
  updateAuditCmNtd11,
  type AuditCmNtd11Item,
  type AuditCmNtd11Request,
} from "../../../../api/auditCmNtd11";
import { listAuditEngagements, listEmployeeOptions, type AuditEngagementItem, type EmployeeOption } from "../../../../api/auditEngagement";
import { listAuditProcessStepSummaries, type AuditProcessStepSummaryItem } from "../../../../api/auditProcessStep";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  assignedEmployeeId?: string;
  processStepSummaryId?: string;
  branchCode: string;
  referenceNumber: string;
  customerCode: string;
  customerName: string;
  transactionDate: dayjs.Dayjs;
  currency?: string;
  amount?: number;
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

/** Man hinh "Giao dich chuyen tien voi Nguoi nuoc ngoai va Kinh doanh ngoai te" (sheet ZTC_CM_NTD11) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach". Cac cot IPCAS (ma/ten KH, can bo...) la text
 * tu do, khong FK. */
export function CmNtd11Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_NTD11.VIEW");
  const canCreate = hasPermission("AUDIT.CM_NTD11.CREATE");
  const canEdit = hasPermission("AUDIT.CM_NTD11.EDIT");
  const canDelete = hasPermission("AUDIT.CM_NTD11.DELETE");
  const canExport = hasPermission("AUDIT.CM_NTD11.EXPORT");
  const canImport = hasPermission("AUDIT.CM_NTD11.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmNtd11Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmNtd11Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmNtd11Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmNtd11Item | null>(null);
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
        setItems(await listAuditCmNtd11(selectedEngagementId));
      } catch {
        message.error(t("auditCmNtd11.messages.loadError"));
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
      referenceNumber: target.referenceNumber,
      customerCode: target.customerCode,
      customerName: target.customerName,
      transactionDate: dayjs(target.transactionDate),
      currency: target.currency ?? undefined,
      amount: target.amount ?? undefined,
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
      const request: AuditCmNtd11Request = {
        engagementId,
        assignedEmployeeId: values.assignedEmployeeId ?? null,
        processStepSummaryId: values.processStepSummaryId ?? null,
        branchCode: values.branchCode,
        referenceNumber: values.referenceNumber,
        customerCode: values.customerCode,
        customerName: values.customerName,
        transactionDate: values.transactionDate.format("YYYY-MM-DD"),
        currency: values.currency ?? null,
        amount: values.amount ?? null,
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
        await updateAuditCmNtd11(editing.id, request);
        message.success(t("auditCmNtd11.messages.updateSuccess"));
      } else {
        await createAuditCmNtd11(request);
        message.success(t("auditCmNtd11.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load(engagementId);
    } catch {
      message.error(t("auditCmNtd11.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmNtd11.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmNtd11(item.id)));
          message.success(t("auditCmNtd11.messages.deleteSuccess"));
          setSelected([]);
          if (engagementId) await load(engagementId);
        } catch {
          message.error(t("auditCmNtd11.messages.deleteError"));
        }
      },
    });
  };

  const money = (v: number | null) => (v == null ? "-" : numberFormatter.format(v));

  const columns: TableProps<AuditCmNtd11Item>["columns"] = [
    { title: t("auditCmNtd11.columns.assignedUsername"), dataIndex: "assignedUsername", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd11.columns.processStepSummaryCode"), dataIndex: "processStepSummaryCode", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd11.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmNtd11.columns.referenceNumber"), dataIndex: "referenceNumber", width: 150 },
    { title: t("auditCmNtd11.columns.customerCode"), ...getSearchColumnProps("customerCode", searchLabels) },
    { title: t("auditCmNtd11.columns.customerName"), ...getSearchColumnProps("customerName", searchLabels) },
    { title: t("auditCmNtd11.columns.transactionDate"), dataIndex: "transactionDate", width: 130 },
    { title: t("auditCmNtd11.columns.currency"), dataIndex: "currency", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd11.columns.amount"), dataIndex: "amount", width: 160, align: "right", render: money },
    { title: t("auditCmNtd11.columns.sampleReason"), dataIndex: "sampleReason", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd11.columns.auditResult"), dataIndex: "auditResult", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd11.columns.recommendationType"), dataIndex: "recommendationType", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd11.columns.transactionStaff"), dataIndex: "transactionStaff", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd11.columns.controlUser"), dataIndex: "controlUser", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd11.columns.controlStaff"), dataIndex: "controlStaff", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd11.columns.controlStaffTitle"), dataIndex: "controlStaffTitle", render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmNtd11.title")}</Typography.Title>
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
      <CrudTable<AuditCmNtd11Item>
        tableId="audit.plan.execution.cmNtd11"
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
        onExportExcel={canExport && engagementId ? () => exportAuditCmNtd11("excel", engagementId) : undefined}
        onExportWord={canExport && engagementId ? () => exportAuditCmNtd11("word", engagementId) : undefined}
        onImport={
          canImport && engagementId
            ? async (file) => {
                const result = await importAuditCmNtd11(engagementId, file);
                await load(engagementId);
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmNtd11.form.editTitle") : t("auditCmNtd11.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={760}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="assignedEmployeeId" label={t("auditCmNtd11.columns.assignedUsername")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={employees.filter((e) => e.username).map((e) => ({ value: e.id, label: `${e.fullName} (${e.username})` }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="processStepSummaryId" label={t("auditCmNtd11.columns.processStepSummaryCode")}>
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
              <Form.Item name="branchCode" label={t("auditCmNtd11.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="referenceNumber" label={t("auditCmNtd11.columns.referenceNumber")} rules={[{ required: true }]}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="transactionDate" label={t("auditCmNtd11.columns.transactionDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="customerCode" label={t("auditCmNtd11.columns.customerCode")} rules={[{ required: true }]}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="customerName" label={t("auditCmNtd11.columns.customerName")} rules={[{ required: true }]}>
                <Input maxLength={200} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="currency" label={t("auditCmNtd11.columns.currency")}>
                <Input maxLength={3} />
              </Form.Item>
            </Col>
            <Col span={16}>
              <Form.Item name="amount" label={t("auditCmNtd11.columns.amount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="sampleReason" label={t("auditCmNtd11.columns.sampleReason")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="auditResult" label={t("auditCmNtd11.columns.auditResult")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="recommendationType" label={t("auditCmNtd11.columns.recommendationType")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="transactionStaff" label={t("auditCmNtd11.columns.transactionStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="controlUser" label={t("auditCmNtd11.columns.controlUser")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="controlStaff" label={t("auditCmNtd11.columns.controlStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="controlStaffTitle" label={t("auditCmNtd11.columns.controlStaffTitle")}>
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
