import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmNtd10,
  deleteAuditCmNtd10,
  exportAuditCmNtd10,
  importAuditCmNtd10,
  listAuditCmNtd10,
  updateAuditCmNtd10,
  type AuditCmNtd10Item,
  type AuditCmNtd10Request,
} from "../../../../api/auditCmNtd10";
import { listAuditEngagements, listEmployeeOptions, type AuditEngagementItem, type EmployeeOption } from "../../../../api/auditEngagement";
import { listAuditProcessStepSummaries, type AuditProcessStepSummaryItem } from "../../../../api/auditProcessStep";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  assignedEmployeeId?: string;
  processStepSummaryId?: string;
  branchCode: string;
  issueDate: dayjs.Dayjs;
  customerCode?: string;
  customerName: string;
  accountNumber: string;
  cardTier?: string;
  issuingUser?: string;
  issuanceFee?: number;
  issuanceType?: string;
  issuanceOccurrence?: string;
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

const ISSUANCE_TYPE_OPTIONS = [
  { value: "Phát hành nhanh", label: "Phát hành nhanh" },
  { value: "Phát hành thường", label: "Phát hành thường" },
];

const ISSUANCE_OCCURRENCE_OPTIONS = [
  { value: "Lần đầu", label: "Lần đầu" },
  { value: "Phát hành lại", label: "Phát hành lại" },
];

/** Man hinh "Ket qua kiem toan ho so phat hanh the" (sheet ZTC_CM_NTD10) - trong nhom "Thuc hien
 * kiem toan" cua "Lap ke hoach". Cac cot IPCAS (ma/ten KH, can bo...) la text tu do, khong FK.
 * issuanceType/issuanceOccurrence la list cung 2 gia tri theo cot "Logic" cua sheet - gioi han
 * bang Select o UI, van luu String o backend (khong FK). */
export function CmNtd10Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_NTD10.VIEW");
  const canCreate = hasPermission("AUDIT.CM_NTD10.CREATE");
  const canEdit = hasPermission("AUDIT.CM_NTD10.EDIT");
  const canDelete = hasPermission("AUDIT.CM_NTD10.DELETE");
  const canExport = hasPermission("AUDIT.CM_NTD10.EXPORT");
  const canImport = hasPermission("AUDIT.CM_NTD10.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmNtd10Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmNtd10Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmNtd10Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmNtd10Item | null>(null);
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
        setItems(await listAuditCmNtd10(selectedEngagementId));
      } catch {
        message.error(t("auditCmNtd10.messages.loadError"));
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
      issueDate: dayjs(target.issueDate),
      customerCode: target.customerCode ?? undefined,
      customerName: target.customerName,
      accountNumber: target.accountNumber,
      cardTier: target.cardTier ?? undefined,
      issuingUser: target.issuingUser ?? undefined,
      issuanceFee: target.issuanceFee ?? undefined,
      issuanceType: target.issuanceType ?? undefined,
      issuanceOccurrence: target.issuanceOccurrence ?? undefined,
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
      const request: AuditCmNtd10Request = {
        engagementId,
        assignedEmployeeId: values.assignedEmployeeId ?? null,
        processStepSummaryId: values.processStepSummaryId ?? null,
        branchCode: values.branchCode,
        issueDate: values.issueDate.format("YYYY-MM-DD"),
        customerCode: values.customerCode ?? null,
        customerName: values.customerName,
        accountNumber: values.accountNumber,
        cardTier: values.cardTier ?? null,
        issuingUser: values.issuingUser ?? null,
        issuanceFee: values.issuanceFee ?? null,
        issuanceType: values.issuanceType ?? null,
        issuanceOccurrence: values.issuanceOccurrence ?? null,
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
        await updateAuditCmNtd10(editing.id, request);
        message.success(t("auditCmNtd10.messages.updateSuccess"));
      } else {
        await createAuditCmNtd10(request);
        message.success(t("auditCmNtd10.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load(engagementId);
    } catch {
      message.error(t("auditCmNtd10.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmNtd10.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmNtd10(item.id)));
          message.success(t("auditCmNtd10.messages.deleteSuccess"));
          setSelected([]);
          if (engagementId) await load(engagementId);
        } catch {
          message.error(t("auditCmNtd10.messages.deleteError"));
        }
      },
    });
  };

  const money = (v: number | null) => (v == null ? "-" : numberFormatter.format(v));

  const columns: TableProps<AuditCmNtd10Item>["columns"] = [
    { title: t("auditCmNtd10.columns.assignedUsername"), dataIndex: "assignedUsername", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.processStepSummaryCode"), dataIndex: "processStepSummaryCode", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmNtd10.columns.issueDate"), dataIndex: "issueDate", width: 120 },
    { title: t("auditCmNtd10.columns.customerCode"), dataIndex: "customerCode", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.customerName"), ...getSearchColumnProps("customerName", searchLabels) },
    { title: t("auditCmNtd10.columns.accountNumber"), dataIndex: "accountNumber", width: 150 },
    { title: t("auditCmNtd10.columns.cardTier"), dataIndex: "cardTier", width: 110, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.issuingUser"), dataIndex: "issuingUser", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.issuanceFee"), dataIndex: "issuanceFee", width: 140, align: "right", render: money },
    { title: t("auditCmNtd10.columns.issuanceType"), dataIndex: "issuanceType", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.issuanceOccurrence"), dataIndex: "issuanceOccurrence", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.sampleReason"), dataIndex: "sampleReason", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.auditResult"), dataIndex: "auditResult", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.recommendationType"), dataIndex: "recommendationType", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.transactionStaff"), dataIndex: "transactionStaff", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.controlUser"), dataIndex: "controlUser", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.controlStaff"), dataIndex: "controlStaff", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd10.columns.controlStaffTitle"), dataIndex: "controlStaffTitle", render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmNtd10.title")}</Typography.Title>
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
      <CrudTable<AuditCmNtd10Item>
        tableId="audit.plan.execution.cmNtd10"
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
        onExportExcel={canExport && engagementId ? () => exportAuditCmNtd10("excel", engagementId) : undefined}
        onExportWord={canExport && engagementId ? () => exportAuditCmNtd10("word", engagementId) : undefined}
        onImport={
          canImport && engagementId
            ? async (file) => {
                const result = await importAuditCmNtd10(engagementId, file);
                await load(engagementId);
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmNtd10.form.editTitle") : t("auditCmNtd10.form.createTitle")}
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
              <Form.Item name="assignedEmployeeId" label={t("auditCmNtd10.columns.assignedUsername")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={employees.filter((e) => e.username).map((e) => ({ value: e.id, label: `${e.fullName} (${e.username})` }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="processStepSummaryId" label={t("auditCmNtd10.columns.processStepSummaryCode")}>
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
              <Form.Item name="branchCode" label={t("auditCmNtd10.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="issueDate" label={t("auditCmNtd10.columns.issueDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="customerCode" label={t("auditCmNtd10.columns.customerCode")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="customerName" label={t("auditCmNtd10.columns.customerName")} rules={[{ required: true }]}>
                <Input maxLength={200} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="accountNumber" label={t("auditCmNtd10.columns.accountNumber")} rules={[{ required: true }]}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="cardTier" label={t("auditCmNtd10.columns.cardTier")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="issuingUser" label={t("auditCmNtd10.columns.issuingUser")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="issuanceFee" label={t("auditCmNtd10.columns.issuanceFee")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="issuanceType" label={t("auditCmNtd10.columns.issuanceType")}>
                <Select allowClear options={ISSUANCE_TYPE_OPTIONS} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="issuanceOccurrence" label={t("auditCmNtd10.columns.issuanceOccurrence")}>
                <Select allowClear options={ISSUANCE_OCCURRENCE_OPTIONS} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="sampleReason" label={t("auditCmNtd10.columns.sampleReason")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="auditResult" label={t("auditCmNtd10.columns.auditResult")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="recommendationType" label={t("auditCmNtd10.columns.recommendationType")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="transactionStaff" label={t("auditCmNtd10.columns.transactionStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="controlUser" label={t("auditCmNtd10.columns.controlUser")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="controlStaff" label={t("auditCmNtd10.columns.controlStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="controlStaffTitle" label={t("auditCmNtd10.columns.controlStaffTitle")}>
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
