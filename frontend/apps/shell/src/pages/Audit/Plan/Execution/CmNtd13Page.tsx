import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, Modal, Result, Row, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmNtd13,
  deleteAuditCmNtd13,
  exportAuditCmNtd13,
  importAuditCmNtd13,
  listAuditCmNtd13,
  updateAuditCmNtd13,
  type AuditCmNtd13Item,
  type AuditCmNtd13Request,
} from "../../../../api/auditCmNtd13";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  branchCode: string;
  occurrenceDate: dayjs.Dayjs;
  merchantId?: string;
  merchantAccountNumber: string;
  businessRegistrationName: string;
  status?: string;
  sampleReason?: string;
  auditResult?: string;
  recommendationType?: string;
  transactionStaff?: string;
  controlUser?: string;
  controlStaff?: string;
  controlStaffTitle?: string;
  active: boolean;
}

/** Man hinh "Kết quả kiểm toán đơn vị chấp nhận thẻ (07D)" (sheet ZTC_CM_NTD13) - trong nhom "Thuc
 * hien kiem toan" cua "Lap ke hoach". Cac cot IPCAS (ma chi nhanh, ly do chon mau, ket qua kiem
 * toan...) la text tu do, khong FK. */
export function CmNtd13Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_NTD13.VIEW");
  const canCreate = hasPermission("AUDIT.CM_NTD13.CREATE");
  const canEdit = hasPermission("AUDIT.CM_NTD13.EDIT");
  const canDelete = hasPermission("AUDIT.CM_NTD13.DELETE");
  const canExport = hasPermission("AUDIT.CM_NTD13.EXPORT");
  const canImport = hasPermission("AUDIT.CM_NTD13.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmNtd13Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmNtd13Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmNtd13Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmNtd13Item | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listAuditCmNtd13());
    } catch {
      message.error(t("auditCmNtd13.messages.loadError"));
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
      occurrenceDate: dayjs(target.occurrenceDate),
      merchantId: target.merchantId ?? undefined,
      merchantAccountNumber: target.merchantAccountNumber,
      businessRegistrationName: target.businessRegistrationName,
      status: target.status ?? undefined,
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
      const request: AuditCmNtd13Request = {
        branchCode: values.branchCode,
        occurrenceDate: values.occurrenceDate.format("YYYY-MM-DD"),
        merchantId: values.merchantId ?? null,
        merchantAccountNumber: values.merchantAccountNumber,
        businessRegistrationName: values.businessRegistrationName,
        status: values.status ?? null,
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
        await updateAuditCmNtd13(editing.id, request);
        message.success(t("auditCmNtd13.messages.updateSuccess"));
      } else {
        await createAuditCmNtd13(request);
        message.success(t("auditCmNtd13.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditCmNtd13.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmNtd13.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmNtd13(item.id)));
          message.success(t("auditCmNtd13.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditCmNtd13.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditCmNtd13Item>["columns"] = [
    { title: t("auditCmNtd13.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmNtd13.columns.occurrenceDate"), dataIndex: "occurrenceDate", width: 120 },
    { title: t("auditCmNtd13.columns.merchantId"), dataIndex: "merchantId", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd13.columns.merchantAccountNumber"), ...getSearchColumnProps("merchantAccountNumber", searchLabels) },
    { title: t("auditCmNtd13.columns.businessRegistrationName"), ...getSearchColumnProps("businessRegistrationName", searchLabels) },
    { title: t("auditCmNtd13.columns.status"), dataIndex: "status", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd13.columns.sampleReason"), dataIndex: "sampleReason", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd13.columns.auditResult"), dataIndex: "auditResult", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd13.columns.recommendationType"), dataIndex: "recommendationType", render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd13.columns.transactionStaff"), dataIndex: "transactionStaff", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd13.columns.controlUser"), dataIndex: "controlUser", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd13.columns.controlStaff"), dataIndex: "controlStaff", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd13.columns.controlStaffTitle"), dataIndex: "controlStaffTitle", width: 160, render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmNtd13.title")}</Typography.Title>
      <CrudTable<AuditCmNtd13Item>
        tableId="audit.plan.execution.cmNtd13"
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
        onExportExcel={canExport ? () => exportAuditCmNtd13("excel") : undefined}
        onExportWord={canExport ? () => exportAuditCmNtd13("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditCmNtd13(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmNtd13.form.editTitle") : t("auditCmNtd13.form.createTitle")}
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
              <Form.Item name="branchCode" label={t("auditCmNtd13.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="occurrenceDate" label={t("auditCmNtd13.columns.occurrenceDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="merchantId" label={t("auditCmNtd13.columns.merchantId")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="merchantAccountNumber" label={t("auditCmNtd13.columns.merchantAccountNumber")} rules={[{ required: true }]}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessRegistrationName" label={t("auditCmNtd13.columns.businessRegistrationName")} rules={[{ required: true }]}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="status" label={t("auditCmNtd13.columns.status")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="sampleReason" label={t("auditCmNtd13.columns.sampleReason")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="recommendationType" label={t("auditCmNtd13.columns.recommendationType")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="auditResult" label={t("auditCmNtd13.columns.auditResult")}>
            <Input maxLength={200} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="transactionStaff" label={t("auditCmNtd13.columns.transactionStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="controlUser" label={t("auditCmNtd13.columns.controlUser")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="controlStaff" label={t("auditCmNtd13.columns.controlStaff")}>
                <Input maxLength={120} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="controlStaffTitle" label={t("auditCmNtd13.columns.controlStaffTitle")}>
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
