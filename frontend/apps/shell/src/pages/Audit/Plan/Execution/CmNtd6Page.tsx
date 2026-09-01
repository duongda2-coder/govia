import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, Modal, Result, Row, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditCmNtd6,
  deleteAuditCmNtd6,
  exportAuditCmNtd6,
  importAuditCmNtd6,
  listAuditCmNtd6,
  updateAuditCmNtd6,
  type AuditCmNtd6Item,
  type AuditCmNtd6Request,
} from "../../../../api/auditCmNtd6";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  branchCode: string;
  staffCode?: string;
  staffName: string;
  ipcasUser: string;
  adUser?: string;
  securityDevice?: string;
  sampleReason?: string;
  sampleCode?: string;
  auditResult?: string;
  active: boolean;
}

/** Man hinh "Danh sach chon mau User Ipcas AD, KPI CNTT" (sheet ZTC_CM_NTD6) - trong nhom "Thuc hien
 * kiem toan" cua "Lap ke hoach". Cac cot IPCAS (ma chi nhanh/ma can bo) la text tu do, khong FK. */
export function CmNtd6Page() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CM_NTD6.VIEW");
  const canCreate = hasPermission("AUDIT.CM_NTD6.CREATE");
  const canEdit = hasPermission("AUDIT.CM_NTD6.EDIT");
  const canDelete = hasPermission("AUDIT.CM_NTD6.DELETE");
  const canExport = hasPermission("AUDIT.CM_NTD6.EXPORT");
  const canImport = hasPermission("AUDIT.CM_NTD6.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditCmNtd6Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditCmNtd6Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditCmNtd6Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditCmNtd6Item | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listAuditCmNtd6());
    } catch {
      message.error(t("auditCmNtd6.messages.loadError"));
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
      staffCode: target.staffCode ?? undefined,
      staffName: target.staffName,
      ipcasUser: target.ipcasUser,
      adUser: target.adUser ?? undefined,
      securityDevice: target.securityDevice ?? undefined,
      sampleReason: target.sampleReason ?? undefined,
      sampleCode: target.sampleCode ?? undefined,
      auditResult: target.auditResult ?? undefined,
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
      const request: AuditCmNtd6Request = {
        branchCode: values.branchCode,
        staffCode: values.staffCode ?? null,
        staffName: values.staffName,
        ipcasUser: values.ipcasUser,
        adUser: values.adUser ?? null,
        securityDevice: values.securityDevice ?? null,
        sampleReason: values.sampleReason ?? null,
        sampleCode: values.sampleCode ?? null,
        auditResult: values.auditResult ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditCmNtd6(editing.id, request);
        message.success(t("auditCmNtd6.messages.updateSuccess"));
      } else {
        await createAuditCmNtd6(request);
        message.success(t("auditCmNtd6.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditCmNtd6.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditCmNtd6.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditCmNtd6(item.id)));
          message.success(t("auditCmNtd6.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditCmNtd6.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditCmNtd6Item>["columns"] = [
    { title: t("auditCmNtd6.columns.branchCode"), width: 110, ...getSearchColumnProps("branchCode", searchLabels) },
    { title: t("auditCmNtd6.columns.staffCode"), dataIndex: "staffCode", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd6.columns.staffName"), ...getSearchColumnProps("staffName", searchLabels) },
    { title: t("auditCmNtd6.columns.ipcasUser"), dataIndex: "ipcasUser", width: 130 },
    { title: t("auditCmNtd6.columns.adUser"), dataIndex: "adUser", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd6.columns.securityDevice"), dataIndex: "securityDevice", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd6.columns.sampleReason"), dataIndex: "sampleReason", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd6.columns.sampleCode"), dataIndex: "sampleCode", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditCmNtd6.columns.auditResult"), dataIndex: "auditResult", render: (v: string | null) => v ?? "-" },
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
      <Typography.Title level={4}>{t("auditCmNtd6.title")}</Typography.Title>
      <CrudTable<AuditCmNtd6Item>
        tableId="audit.plan.execution.cmNtd6"
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
        onExportExcel={canExport ? () => exportAuditCmNtd6("excel") : undefined}
        onExportWord={canExport ? () => exportAuditCmNtd6("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditCmNtd6(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditCmNtd6.form.editTitle") : t("auditCmNtd6.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={640}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="branchCode" label={t("auditCmNtd6.columns.branchCode")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="staffCode" label={t("auditCmNtd6.columns.staffCode")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="staffName" label={t("auditCmNtd6.columns.staffName")} rules={[{ required: true }]}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="ipcasUser" label={t("auditCmNtd6.columns.ipcasUser")} rules={[{ required: true }]}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="adUser" label={t("auditCmNtd6.columns.adUser")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="securityDevice" label={t("auditCmNtd6.columns.securityDevice")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="sampleReason" label={t("auditCmNtd6.columns.sampleReason")}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="sampleCode" label={t("auditCmNtd6.columns.sampleCode")}>
                <Input maxLength={20} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="auditResult" label={t("auditCmNtd6.columns.auditResult")}>
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
