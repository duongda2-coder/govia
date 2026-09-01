import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, InputNumber, Modal, Result, Row, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmNtd12,
  deleteAuditCmNtd12,
  exportAuditCmNtd12,
  importAuditCmNtd12,
  listAuditCmNtd12,
  updateAuditCmNtd12,
  type AuditCmNtd12Item,
  type AuditCmNtd12Request,
} from "../../../../api/auditCmNtd12";
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
  active: boolean;
}

const numberFormatter = new Intl.NumberFormat("vi-VN");

/** Man hinh "Danh cac but toan chon mau TCKT" (sheet ZTC_CM_NTD12) - trong nhom "Thuc hien kiem
 * toan" cua "Lap ke hoach". Cac cot IPCAS (ma chi nhanh, trang thai giao dich...) la text tu do,
 * khong FK. */
export function CmNtd12Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_NTD12.VIEW");
  const canCreate = hasPermission("AUDIT.CM_NTD12.CREATE");
  const canEdit = hasPermission("AUDIT.CM_NTD12.EDIT");
  const canDelete = hasPermission("AUDIT.CM_NTD12.DELETE");
  const canExport = hasPermission("AUDIT.CM_NTD12.EXPORT");
  const canImport = hasPermission("AUDIT.CM_NTD12.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmNtd12Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmNtd12Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmNtd12Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmNtd12Item | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listAuditCmNtd12());
    } catch {
      message.error(t("auditCmNtd12.messages.loadError"));
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
      entryNumber: target.entryNumber,
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
    setSubmitting(true);
    try {
      const request: AuditCmNtd12Request = {
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
        await updateAuditCmNtd12(editing.id, request);
        message.success(t("auditCmNtd12.messages.updateSuccess"));
      } else {
        await createAuditCmNtd12(request);
        message.success(t("auditCmNtd12.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditCmNtd12.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmNtd12.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmNtd12(item.id)));
          message.success(t("auditCmNtd12.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditCmNtd12.messages.deleteError"));
        }
      },
    });
  };

  const money = (v: number | null) => (v == null ? "-" : numberFormatter.format(v));

  const columns: TableProps<AuditCmNtd12Item>["columns"] = [
    { title: t("auditCmNtd12.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmNtd12.columns.transactionDate"), dataIndex: "transactionDate", width: 130 },
    { title: t("auditCmNtd12.columns.postingUser"), dataIndex: "postingUser", width: 130 },
    { title: t("auditCmNtd12.columns.entryNumber"), dataIndex: "entryNumber", width: 140, align: "right", render: money },
    { title: t("auditCmNtd12.columns.debitAmount"), dataIndex: "debitAmount", width: 150, align: "right", render: money },
    { title: t("auditCmNtd12.columns.creditAmount"), dataIndex: "creditAmount", width: 150, align: "right", render: money },
    { title: t("auditCmNtd12.columns.transactionStatus"), dataIndex: "transactionStatus", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd12.columns.currency"), dataIndex: "currency", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd12.columns.accountNumber"), dataIndex: "accountNumber", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd12.columns.content"), dataIndex: "content", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd12.columns.sampleReason"), dataIndex: "sampleReason", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd12.columns.auditResult"), dataIndex: "auditResult", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd12.columns.recommendationType"), dataIndex: "recommendationType", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd12.columns.transactionStaff"), dataIndex: "transactionStaff", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd12.columns.controlUser"), dataIndex: "controlUser", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd12.columns.controlStaff"), dataIndex: "controlStaff", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd12.columns.controlStaffTitle"), dataIndex: "controlStaffTitle", render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmNtd12.title")}</Typography.Title>
      <CrudTable<AuditCmNtd12Item>
        tableId="audit.plan.execution.cmNtd12"
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
        onExportExcel={canExport ? () => exportAuditCmNtd12("excel") : undefined}
        onExportWord={canExport ? () => exportAuditCmNtd12("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditCmNtd12(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmNtd12.form.editTitle") : t("auditCmNtd12.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={760}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="branchCode" label={t("auditCmNtd12.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="transactionDate" label={t("auditCmNtd12.columns.transactionDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="postingUser" label={t("auditCmNtd12.columns.postingUser")} rules={[{ required: true }]}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="entryNumber" label={t("auditCmNtd12.columns.entryNumber")} rules={[{ required: true }]}>
                <InputNumber style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="debitAmount" label={t("auditCmNtd12.columns.debitAmount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="creditAmount" label={t("auditCmNtd12.columns.creditAmount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="transactionStatus" label={t("auditCmNtd12.columns.transactionStatus")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="currency" label={t("auditCmNtd12.columns.currency")}>
                <Input maxLength={3} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="accountNumber" label={t("auditCmNtd12.columns.accountNumber")}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="content" label={t("auditCmNtd12.columns.content")}>
            <Input maxLength={200} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="sampleReason" label={t("auditCmNtd12.columns.sampleReason")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="auditResult" label={t("auditCmNtd12.columns.auditResult")}>
                <Input maxLength={200} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="recommendationType" label={t("auditCmNtd12.columns.recommendationType")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="transactionStaff" label={t("auditCmNtd12.columns.transactionStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="controlUser" label={t("auditCmNtd12.columns.controlUser")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="controlStaff" label={t("auditCmNtd12.columns.controlStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="controlStaffTitle" label={t("auditCmNtd12.columns.controlStaffTitle")}>
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
