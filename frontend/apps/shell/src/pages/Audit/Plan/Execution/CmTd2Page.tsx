import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmTd2,
  deleteAuditCmTd2,
  exportAuditCmTd2,
  importAuditCmTd2,
  listAuditCmTd2,
  updateAuditCmTd2,
  type AuditCmTd2Item,
  type AuditCmTd2Request,
} from "../../../../api/auditCmTd2";
import { listAuditEngagements, listEmployeeOptions, type AuditEngagementItem, type EmployeeOption } from "../../../../api/auditEngagement";
import { listAuditProcessStepSummaries, type AuditProcessStepSummaryItem } from "../../../../api/auditProcessStep";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  assignedEmployeeId?: string;
  processStepSummaryId?: string;
  branchCode: string;
  transactionDate: dayjs.Dayjs;
  valueDate: dayjs.Dayjs;
  postingUser: string;
  entryNumber?: number;
  customerCode?: string;
  customerName: string;
  disbursementNumber?: string;
  businessCode?: string;
  transactionStatus?: string;
  currency?: string;
  debitAmount?: number;
  creditAmount?: number;
  accountNumber?: string;
  ipcasReviewResult?: string;
  documentCheckResult?: string;
  active: boolean;
}

const numberFormatter = new Intl.NumberFormat("vi-VN");

/** Man hinh "Ket qua kiem toan chon mau cac but toan giao dich huy, lui ngay" (sheet ZTC_CM_TD2) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach". Cac cot IPCAS (ma/ten KH, can bo...) la text
 * tu do, khong FK. postingDateDiff la cot he thong tinh (valueDate - transactionDate), chi hien thi. */
export function CmTd2Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_TD2.VIEW");
  const canCreate = hasPermission("AUDIT.CM_TD2.CREATE");
  const canEdit = hasPermission("AUDIT.CM_TD2.EDIT");
  const canDelete = hasPermission("AUDIT.CM_TD2.DELETE");
  const canExport = hasPermission("AUDIT.CM_TD2.EXPORT");
  const canImport = hasPermission("AUDIT.CM_TD2.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmTd2Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmTd2Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmTd2Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmTd2Item | null>(null);
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
        setItems(await listAuditCmTd2(selectedEngagementId));
      } catch {
        message.error(t("auditCmTd2.messages.loadError"));
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
      valueDate: dayjs(target.valueDate),
      postingUser: target.postingUser,
      entryNumber: target.entryNumber ?? undefined,
      customerCode: target.customerCode ?? undefined,
      customerName: target.customerName,
      disbursementNumber: target.disbursementNumber ?? undefined,
      businessCode: target.businessCode ?? undefined,
      transactionStatus: target.transactionStatus ?? undefined,
      currency: target.currency ?? undefined,
      debitAmount: target.debitAmount ?? undefined,
      creditAmount: target.creditAmount ?? undefined,
      accountNumber: target.accountNumber ?? undefined,
      ipcasReviewResult: target.ipcasReviewResult ?? undefined,
      documentCheckResult: target.documentCheckResult ?? undefined,
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
      const request: AuditCmTd2Request = {
        engagementId,
        assignedEmployeeId: values.assignedEmployeeId ?? null,
        processStepSummaryId: values.processStepSummaryId ?? null,
        branchCode: values.branchCode,
        transactionDate: values.transactionDate.format("YYYY-MM-DD"),
        valueDate: values.valueDate.format("YYYY-MM-DD"),
        postingUser: values.postingUser,
        entryNumber: values.entryNumber ?? null,
        customerCode: values.customerCode ?? null,
        customerName: values.customerName,
        disbursementNumber: values.disbursementNumber ?? null,
        businessCode: values.businessCode ?? null,
        transactionStatus: values.transactionStatus ?? null,
        currency: values.currency ?? null,
        debitAmount: values.debitAmount ?? null,
        creditAmount: values.creditAmount ?? null,
        accountNumber: values.accountNumber ?? null,
        ipcasReviewResult: values.ipcasReviewResult ?? null,
        documentCheckResult: values.documentCheckResult ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditCmTd2(editing.id, request);
        message.success(t("auditCmTd2.messages.updateSuccess"));
      } else {
        await createAuditCmTd2(request);
        message.success(t("auditCmTd2.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load(engagementId);
    } catch {
      message.error(t("auditCmTd2.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmTd2.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmTd2(item.id)));
          message.success(t("auditCmTd2.messages.deleteSuccess"));
          setSelected([]);
          if (engagementId) await load(engagementId);
        } catch {
          message.error(t("auditCmTd2.messages.deleteError"));
        }
      },
    });
  };

  const money = (v: number | null) => (v == null ? "-" : numberFormatter.format(v));

  const columns: TableProps<AuditCmTd2Item>["columns"] = [
    { title: t("auditCmTd2.columns.assignedUsername"), dataIndex: "assignedUsername", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd2.columns.processStepSummaryCode"), dataIndex: "processStepSummaryCode", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd2.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmTd2.columns.transactionDate"), dataIndex: "transactionDate", width: 130 },
    { title: t("auditCmTd2.columns.valueDate"), dataIndex: "valueDate", width: 120 },
    { title: t("auditCmTd2.columns.postingUser"), dataIndex: "postingUser", width: 120 },
    { title: t("auditCmTd2.columns.entryNumber"), dataIndex: "entryNumber", width: 120, align: "right", render: money },
    { title: t("auditCmTd2.columns.customerCode"), dataIndex: "customerCode", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd2.columns.customerName"), ...getSearchColumnProps("customerName", searchLabels) },
    { title: t("auditCmTd2.columns.disbursementNumber"), dataIndex: "disbursementNumber", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd2.columns.businessCode"), dataIndex: "businessCode", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd2.columns.transactionStatus"), dataIndex: "transactionStatus", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd2.columns.currency"), dataIndex: "currency", width: 90, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd2.columns.debitAmount"), dataIndex: "debitAmount", width: 150, align: "right", render: money },
    { title: t("auditCmTd2.columns.creditAmount"), dataIndex: "creditAmount", width: 150, align: "right", render: money },
    { title: t("auditCmTd2.columns.accountNumber"), dataIndex: "accountNumber", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd2.columns.postingDateDiff"), dataIndex: "postingDateDiff", width: 140, align: "right", render: money },
    { title: t("auditCmTd2.columns.ipcasReviewResult"), dataIndex: "ipcasReviewResult", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmTd2.columns.documentCheckResult"), dataIndex: "documentCheckResult", render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmTd2.title")}</Typography.Title>
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
      <CrudTable<AuditCmTd2Item>
        tableId="audit.plan.execution.cmTd2"
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
        onExportExcel={canExport && engagementId ? () => exportAuditCmTd2("excel", engagementId) : undefined}
        onExportWord={canExport && engagementId ? () => exportAuditCmTd2("word", engagementId) : undefined}
        onImport={
          canImport && engagementId
            ? async (file) => {
                const result = await importAuditCmTd2(engagementId, file);
                await load(engagementId);
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmTd2.form.editTitle") : t("auditCmTd2.form.createTitle")}
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
              <Form.Item name="assignedEmployeeId" label={t("auditCmTd2.columns.assignedUsername")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={employees.filter((e) => e.username).map((e) => ({ value: e.id, label: `${e.fullName} (${e.username})` }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="processStepSummaryId" label={t("auditCmTd2.columns.processStepSummaryCode")}>
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
              <Form.Item name="branchCode" label={t("auditCmTd2.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="transactionDate" label={t("auditCmTd2.columns.transactionDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="valueDate" label={t("auditCmTd2.columns.valueDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="postingUser" label={t("auditCmTd2.columns.postingUser")} rules={[{ required: true }]}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="entryNumber" label={t("auditCmTd2.columns.entryNumber")}>
                <InputNumber style={{ width: "100%" }} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="customerCode" label={t("auditCmTd2.columns.customerCode")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="customerName" label={t("auditCmTd2.columns.customerName")} rules={[{ required: true }]}>
                <Input maxLength={200} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="disbursementNumber" label={t("auditCmTd2.columns.disbursementNumber")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="businessCode" label={t("auditCmTd2.columns.businessCode")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="transactionStatus" label={t("auditCmTd2.columns.transactionStatus")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="currency" label={t("auditCmTd2.columns.currency")}>
                <Input maxLength={3} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="debitAmount" label={t("auditCmTd2.columns.debitAmount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="creditAmount" label={t("auditCmTd2.columns.creditAmount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="accountNumber" label={t("auditCmTd2.columns.accountNumber")}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="ipcasReviewResult" label={t("auditCmTd2.columns.ipcasReviewResult")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="documentCheckResult" label={t("auditCmTd2.columns.documentCheckResult")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
