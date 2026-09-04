import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmNtd4,
  deleteAuditCmNtd4,
  exportAuditCmNtd4,
  importAuditCmNtd4,
  listAuditCmNtd4,
  updateAuditCmNtd4,
  type AuditCmNtd4Item,
  type AuditCmNtd4Request,
} from "../../../../api/auditCmNtd4";
import { listAuditEngagements, listEmployeeOptions, type AuditEngagementItem, type EmployeeOption } from "../../../../api/auditEngagement";
import { listAuditProcessStepSummaries, type AuditProcessStepSummaryItem } from "../../../../api/auditProcessStep";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  assignedEmployeeId?: string;
  processStepSummaryId?: string;
  branchCode: string;
  referenceNumber: number;
  openDate: dayjs.Dayjs;
  corebankCustomerCode?: string;
  amount?: number;
  beneficiary?: string;
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

/** Man hinh "Danh sach chon mau LC va nho thu TTQT" (sheet ZTC_CM_NTD4) - trong nhom "Thuc hien kiem
 * toan" cua "Lap ke hoach". Cac cot IPCAS (ma chi nhanh/ma KH) la text tu do, khong FK. */
export function CmNtd4Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_NTD4.VIEW");
  const canCreate = hasPermission("AUDIT.CM_NTD4.CREATE");
  const canEdit = hasPermission("AUDIT.CM_NTD4.EDIT");
  const canDelete = hasPermission("AUDIT.CM_NTD4.DELETE");
  const canExport = hasPermission("AUDIT.CM_NTD4.EXPORT");
  const canImport = hasPermission("AUDIT.CM_NTD4.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmNtd4Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmNtd4Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmNtd4Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmNtd4Item | null>(null);
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
        setItems(await listAuditCmNtd4(selectedEngagementId));
      } catch {
        message.error(t("auditCmNtd4.messages.loadError"));
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
      openDate: dayjs(target.openDate),
      corebankCustomerCode: target.corebankCustomerCode ?? undefined,
      amount: target.amount ?? undefined,
      beneficiary: target.beneficiary ?? undefined,
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
      const request: AuditCmNtd4Request = {
        engagementId,
        assignedEmployeeId: values.assignedEmployeeId ?? null,
        processStepSummaryId: values.processStepSummaryId ?? null,
        branchCode: values.branchCode,
        referenceNumber: values.referenceNumber,
        openDate: values.openDate.format("YYYY-MM-DD"),
        corebankCustomerCode: values.corebankCustomerCode ?? null,
        amount: values.amount ?? null,
        beneficiary: values.beneficiary ?? null,
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
        await updateAuditCmNtd4(editing.id, request);
        message.success(t("auditCmNtd4.messages.updateSuccess"));
      } else {
        await createAuditCmNtd4(request);
        message.success(t("auditCmNtd4.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load(engagementId);
    } catch {
      message.error(t("auditCmNtd4.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmNtd4.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmNtd4(item.id)));
          message.success(t("auditCmNtd4.messages.deleteSuccess"));
          setSelected([]);
          if (engagementId) await load(engagementId);
        } catch {
          message.error(t("auditCmNtd4.messages.deleteError"));
        }
      },
    });
  };

  const money = (v: number | null) => (v == null ? "-" : numberFormatter.format(v));

  const columns: TableProps<AuditCmNtd4Item>["columns"] = [
    { title: t("auditCmNtd4.columns.assignedUsername"), dataIndex: "assignedUsername", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd4.columns.processStepSummaryCode"), dataIndex: "processStepSummaryCode", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd4.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmNtd4.columns.referenceNumber"), dataIndex: "referenceNumber", width: 140, align: "right", render: money },
    { title: t("auditCmNtd4.columns.openDate"), dataIndex: "openDate", width: 120 },
    { title: t("auditCmNtd4.columns.corebankCustomerCode"), dataIndex: "corebankCustomerCode", width: 160, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd4.columns.amount"), dataIndex: "amount", width: 150, align: "right", render: money },
    { title: t("auditCmNtd4.columns.beneficiary"), dataIndex: "beneficiary", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd4.columns.sampleReason"), dataIndex: "sampleReason", width: 160, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd4.columns.auditResult"), dataIndex: "auditResult", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd4.columns.recommendationType"), dataIndex: "recommendationType", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd4.columns.transactionStaff"), dataIndex: "transactionStaff", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd4.columns.controlUser"), dataIndex: "controlUser", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd4.columns.controlStaff"), dataIndex: "controlStaff", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd4.columns.controlStaffTitle"), dataIndex: "controlStaffTitle", render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmNtd4.title")}</Typography.Title>
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
      <CrudTable<AuditCmNtd4Item>
        tableId="audit.plan.execution.cmNtd4"
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
        onExportExcel={canExport && engagementId ? () => exportAuditCmNtd4("excel", engagementId) : undefined}
        onExportWord={canExport && engagementId ? () => exportAuditCmNtd4("word", engagementId) : undefined}
        onImport={
          canImport && engagementId
            ? async (file) => {
                const result = await importAuditCmNtd4(engagementId, file);
                await load(engagementId);
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmNtd4.form.editTitle") : t("auditCmNtd4.form.createTitle")}
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
              <Form.Item name="assignedEmployeeId" label={t("auditCmNtd4.columns.assignedUsername")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={employees.filter((e) => e.username).map((e) => ({ value: e.id, label: `${e.fullName} (${e.username})` }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="processStepSummaryId" label={t("auditCmNtd4.columns.processStepSummaryCode")}>
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
              <Form.Item name="branchCode" label={t("auditCmNtd4.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="referenceNumber" label={t("auditCmNtd4.columns.referenceNumber")} rules={[{ required: true }]}>
                <InputNumber style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="openDate" label={t("auditCmNtd4.columns.openDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="corebankCustomerCode" label={t("auditCmNtd4.columns.corebankCustomerCode")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="amount" label={t("auditCmNtd4.columns.amount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="beneficiary" label={t("auditCmNtd4.columns.beneficiary")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="sampleReason" label={t("auditCmNtd4.columns.sampleReason")}>
            <Input.TextArea rows={2} maxLength={1000} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="recommendationType" label={t("auditCmNtd4.columns.recommendationType")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="auditResult" label={t("auditCmNtd4.columns.auditResult")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="transactionStaff" label={t("auditCmNtd4.columns.transactionStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="controlUser" label={t("auditCmNtd4.columns.controlUser")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="controlStaff" label={t("auditCmNtd4.columns.controlStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="controlStaffTitle" label={t("auditCmNtd4.columns.controlStaffTitle")}>
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
