import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, InputNumber, Modal, Result, Row, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmNtd2,
  deleteAuditCmNtd2,
  exportAuditCmNtd2,
  importAuditCmNtd2,
  listAuditCmNtd2,
  updateAuditCmNtd2,
  type AuditCmNtd2Item,
  type AuditCmNtd2Request,
} from "../../../../api/auditCmNtd2";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  branchCode: string;
  transactionDate: dayjs.Dayjs;
  valueDate?: dayjs.Dayjs;
  postingUser: string;
  entryNumber: number;
  currency?: string;
  amount?: number;
  accountNumber?: string;
  bookNumber?: string;
  transactionType?: string;
  transactionStatus?: string;
  auditResult?: string;
  recommendationType?: string;
  transactionStaff?: string;
  controlUser?: string;
  controlStaff?: string;
  controlStaffTitle?: string;
  active: boolean;
}

const numberFormatter = new Intl.NumberFormat("vi-VN");

/** Man hinh "Danh sach chon mau giao dich ve nghiep vu HDV" (sheet ZTC_CM_NTD2) - trong nhom "Thuc
 * hien kiem toan" cua "Lap ke hoach". Cac cot IPCAS/can bo la text tu do, khong FK. */
export function CmNtd2Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_NTD2.VIEW");
  const canCreate = hasPermission("AUDIT.CM_NTD2.CREATE");
  const canEdit = hasPermission("AUDIT.CM_NTD2.EDIT");
  const canDelete = hasPermission("AUDIT.CM_NTD2.DELETE");
  const canExport = hasPermission("AUDIT.CM_NTD2.EXPORT");
  const canImport = hasPermission("AUDIT.CM_NTD2.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmNtd2Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmNtd2Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmNtd2Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmNtd2Item | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listAuditCmNtd2());
    } catch {
      message.error(t("auditCmNtd2.messages.loadError"));
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
      valueDate: target.valueDate ? dayjs(target.valueDate) : undefined,
      postingUser: target.postingUser,
      entryNumber: target.entryNumber ?? undefined,
      currency: target.currency ?? undefined,
      amount: target.amount ?? undefined,
      accountNumber: target.accountNumber ?? undefined,
      bookNumber: target.bookNumber ?? undefined,
      transactionType: target.transactionType ?? undefined,
      transactionStatus: target.transactionStatus ?? undefined,
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
      const request: AuditCmNtd2Request = {
        branchCode: values.branchCode,
        transactionDate: values.transactionDate.format("YYYY-MM-DD"),
        valueDate: values.valueDate ? values.valueDate.format("YYYY-MM-DD") : null,
        postingUser: values.postingUser,
        entryNumber: values.entryNumber,
        currency: values.currency ?? null,
        amount: values.amount ?? null,
        accountNumber: values.accountNumber ?? null,
        bookNumber: values.bookNumber ?? null,
        transactionType: values.transactionType ?? null,
        transactionStatus: values.transactionStatus ?? null,
        auditResult: values.auditResult ?? null,
        recommendationType: values.recommendationType ?? null,
        transactionStaff: values.transactionStaff ?? null,
        controlUser: values.controlUser ?? null,
        controlStaff: values.controlStaff ?? null,
        controlStaffTitle: values.controlStaffTitle ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditCmNtd2(editing.id, request);
        message.success(t("auditCmNtd2.messages.updateSuccess"));
      } else {
        await createAuditCmNtd2(request);
        message.success(t("auditCmNtd2.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditCmNtd2.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmNtd2.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmNtd2(item.id)));
          message.success(t("auditCmNtd2.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditCmNtd2.messages.deleteError"));
        }
      },
    });
  };

  const money = (v: number | null) => (v == null ? "-" : numberFormatter.format(v));

  const columns: TableProps<AuditCmNtd2Item>["columns"] = [
    { title: t("auditCmNtd2.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmNtd2.columns.transactionDate"), dataIndex: "transactionDate", width: 130 },
    { title: t("auditCmNtd2.columns.valueDate"), dataIndex: "valueDate", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd2.columns.postingUser"), dataIndex: "postingUser", width: 120 },
    { title: t("auditCmNtd2.columns.entryNumber"), dataIndex: "entryNumber", width: 120, align: "right", render: money },
    { title: t("auditCmNtd2.columns.currency"), dataIndex: "currency", width: 90, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd2.columns.amount"), dataIndex: "amount", width: 150, align: "right", render: money },
    { title: t("auditCmNtd2.columns.accountNumber"), dataIndex: "accountNumber", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd2.columns.bookNumber"), dataIndex: "bookNumber", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd2.columns.transactionType"), dataIndex: "transactionType", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd2.columns.transactionStatus"), dataIndex: "transactionStatus", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd2.columns.auditResult"), dataIndex: "auditResult", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd2.columns.recommendationType"), dataIndex: "recommendationType", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd2.columns.transactionStaff"), dataIndex: "transactionStaff", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd2.columns.controlUser"), dataIndex: "controlUser", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd2.columns.controlStaff"), dataIndex: "controlStaff", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd2.columns.controlStaffTitle"), dataIndex: "controlStaffTitle", width: 170, render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmNtd2.title")}</Typography.Title>
      <CrudTable<AuditCmNtd2Item>
        tableId="audit.plan.execution.cmNtd2"
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
        onExportExcel={canExport ? () => exportAuditCmNtd2("excel") : undefined}
        onExportWord={canExport ? () => exportAuditCmNtd2("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditCmNtd2(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmNtd2.form.editTitle") : t("auditCmNtd2.form.createTitle")}
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
              <Form.Item name="branchCode" label={t("auditCmNtd2.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="transactionDate" label={t("auditCmNtd2.columns.transactionDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="valueDate" label={t("auditCmNtd2.columns.valueDate")}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="postingUser" label={t("auditCmNtd2.columns.postingUser")} rules={[{ required: true }]}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="entryNumber" label={t("auditCmNtd2.columns.entryNumber")} rules={[{ required: true }]}>
                <InputNumber style={{ width: "100%" }} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="currency" label={t("auditCmNtd2.columns.currency")}>
                <Input maxLength={3} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="amount" label={t("auditCmNtd2.columns.amount")}>
                <InputNumber style={{ width: "100%" }} min={0} formatter={(v) => (v ? numberFormatter.format(Number(v)) : "")} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="accountNumber" label={t("auditCmNtd2.columns.accountNumber")}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="bookNumber" label={t("auditCmNtd2.columns.bookNumber")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="transactionType" label={t("auditCmNtd2.columns.transactionType")}>
                <Input maxLength={30} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="transactionStatus" label={t("auditCmNtd2.columns.transactionStatus")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="auditResult" label={t("auditCmNtd2.columns.auditResult")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="recommendationType" label={t("auditCmNtd2.columns.recommendationType")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="transactionStaff" label={t("auditCmNtd2.columns.transactionStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="controlUser" label={t("auditCmNtd2.columns.controlUser")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="controlStaff" label={t("auditCmNtd2.columns.controlStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="controlStaffTitle" label={t("auditCmNtd2.columns.controlStaffTitle")}>
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
