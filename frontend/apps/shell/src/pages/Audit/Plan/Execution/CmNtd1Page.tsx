import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, InputNumber, Modal, Result, Row, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmNtd1,
  deleteAuditCmNtd1,
  exportAuditCmNtd1,
  importAuditCmNtd1,
  listAuditCmNtd1,
  updateAuditCmNtd1,
  type AuditCmNtd1Item,
  type AuditCmNtd1Request,
} from "../../../../api/auditCmNtd1";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
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
  workType?: string;
  active: boolean;
}

const numberFormatter = new Intl.NumberFormat("vi-VN");

/** Man hinh "Danh sach cac but toan chon mau TCKT" (sheet ZTC_CM_NTD1) - trong nhom "Thuc hien kiem
 * toan" cua "Lap ke hoach". Cac cot IPCAS/can bo la text tu do, khong FK. */
export function CmNtd1Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_NTD1.VIEW");
  const canCreate = hasPermission("AUDIT.CM_NTD1.CREATE");
  const canEdit = hasPermission("AUDIT.CM_NTD1.EDIT");
  const canDelete = hasPermission("AUDIT.CM_NTD1.DELETE");
  const canExport = hasPermission("AUDIT.CM_NTD1.EXPORT");
  const canImport = hasPermission("AUDIT.CM_NTD1.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmNtd1Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmNtd1Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmNtd1Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmNtd1Item | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listAuditCmNtd1());
    } catch {
      message.error(t("auditCmNtd1.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

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
      workType: target.workType ?? undefined,
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
    setSubmitting(true);
    try {
      const request: AuditCmNtd1Request = {
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
        workType: values.workType ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditCmNtd1(editing.id, request);
        message.success(t("auditCmNtd1.messages.updateSuccess"));
      } else {
        await createAuditCmNtd1(request);
        message.success(t("auditCmNtd1.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditCmNtd1.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmNtd1.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmNtd1(item.id)));
          message.success(t("auditCmNtd1.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditCmNtd1.messages.deleteError"));
        }
      },
    });
  };

  const money = (v: number | null) => (v == null ? "-" : numberFormatter.format(v));

  const columns: TableProps<AuditCmNtd1Item>["columns"] = [
    { title: t("auditCmNtd1.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmNtd1.columns.transactionDate"), dataIndex: "transactionDate", width: 130 },
    { title: t("auditCmNtd1.columns.postingUser"), dataIndex: "postingUser", width: 120 },
    { title: t("auditCmNtd1.columns.entryNumber"), dataIndex: "entryNumber", width: 120, align: "right", render: money },
    { title: t("auditCmNtd1.columns.debitAmount"), dataIndex: "debitAmount", width: 150, align: "right", render: money },
    { title: t("auditCmNtd1.columns.creditAmount"), dataIndex: "creditAmount", width: 150, align: "right", render: money },
    { title: t("auditCmNtd1.columns.transactionStatus"), dataIndex: "transactionStatus", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd1.columns.currency"), dataIndex: "currency", width: 90, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd1.columns.accountNumber"), dataIndex: "accountNumber", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd1.columns.content"), dataIndex: "content", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd1.columns.sampleReason"), dataIndex: "sampleReason", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd1.columns.auditResult"), dataIndex: "auditResult", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd1.columns.recommendationType"), dataIndex: "recommendationType", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd1.columns.transactionStaff"), dataIndex: "transactionStaff", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd1.columns.controlUser"), dataIndex: "controlUser", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd1.columns.controlStaff"), dataIndex: "controlStaff", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd1.columns.controlStaffTitle"), dataIndex: "controlStaffTitle", width: 170, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd1.columns.workType"), dataIndex: "workType", width: 130, render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmNtd1.title")}</Typography.Title>
      <CrudTable<AuditCmNtd1Item>
        tableId="audit.plan.execution.cmNtd1"
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length === 0}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport ? () => exportAuditCmNtd1("excel") : undefined}
        onExportWord={canExport ? () => exportAuditCmNtd1("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditCmNtd1(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmNtd1.form.editTitle") : t("auditCmNtd1.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={800}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="branchCode" label={t("auditCmNtd1.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="transactionDate" label={t("auditCmNtd1.columns.transactionDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="postingUser" label={t("auditCmNtd1.columns.postingUser")} rules={[{ required: true }]}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="entryNumber" label={t("auditCmNtd1.columns.entryNumber")} rules={[{ required: true }]}>
                <InputNumber style={{ width: "100%" }} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="debitAmount" label={t("auditCmNtd1.columns.debitAmount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="creditAmount" label={t("auditCmNtd1.columns.creditAmount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="transactionStatus" label={t("auditCmNtd1.columns.transactionStatus")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="currency" label={t("auditCmNtd1.columns.currency")}>
                <Input maxLength={3} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="accountNumber" label={t("auditCmNtd1.columns.accountNumber")}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="content" label={t("auditCmNtd1.columns.content")}>
                <Input maxLength={200} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="sampleReason" label={t("auditCmNtd1.columns.sampleReason")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="auditResult" label={t("auditCmNtd1.columns.auditResult")}>
                <Input maxLength={200} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="recommendationType" label={t("auditCmNtd1.columns.recommendationType")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="transactionStaff" label={t("auditCmNtd1.columns.transactionStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="controlUser" label={t("auditCmNtd1.columns.controlUser")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="controlStaff" label={t("auditCmNtd1.columns.controlStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="controlStaffTitle" label={t("auditCmNtd1.columns.controlStaffTitle")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="workType" label={t("auditCmNtd1.columns.workType")}>
                <Input maxLength={50} />
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
