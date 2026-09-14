import { useCallback, useEffect, useState } from "react";
import { App, Form, Input, InputNumber, Modal, Result, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditKhktScale,
  deleteAuditKhktScale,
  exportAuditKhktScales,
  importAuditKhktScales,
  listAuditKhktScales,
  updateAuditKhktScale,
  type AuditKhktScaleItem,
  type AuditKhktScaleRequest,
} from "../../../../api/auditKhktScale";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  sortOrder: number;
  creditThreshold?: number;
  fundingThreshold?: number;
  scaleName: string;
  active: boolean;
}

/** Danh muc "Quy mo tin dung va huy dong von cua chi nhanh" (sheet ZTC_KHKT_QM) - module Ke hoach
 * kiem toan (KHKT) dung de tra Quy mo tin dung/huy dong von cua 1 don vi theo nguong Du no noi
 * bang/Nguon von, so sanh theo sortOrder tang dan (xem AuditKhktScaleService o BE). */
export function AuditKhktScalePage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.KHKT_SCALE.VIEW");
  const canCreate = hasPermission("AUDIT.KHKT_SCALE.CREATE");
  const canEdit = hasPermission("AUDIT.KHKT_SCALE.EDIT");
  const canDelete = hasPermission("AUDIT.KHKT_SCALE.DELETE");
  const canExport = hasPermission("AUDIT.KHKT_SCALE.EXPORT");
  const canImport = hasPermission("AUDIT.KHKT_SCALE.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditKhktScaleItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditKhktScaleItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditKhktScaleItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditKhktScaleItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listAuditKhktScales());
    } catch {
      message.error(t("auditKhktScale.messages.loadError"));
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
    form.setFieldsValue({ active: true, sortOrder: items.length + 1 });
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      sortOrder: target.sortOrder,
      creditThreshold: target.creditThreshold ?? undefined,
      fundingThreshold: target.fundingThreshold ?? undefined,
      scaleName: target.scaleName,
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
      const request: AuditKhktScaleRequest = {
        sortOrder: values.sortOrder,
        creditThreshold: values.creditThreshold ?? null,
        fundingThreshold: values.fundingThreshold ?? null,
        scaleName: values.scaleName,
        active: values.active,
      };
      if (editing) {
        await updateAuditKhktScale(editing.id, request);
        message.success(t("auditKhktScale.messages.updateSuccess"));
      } else {
        await createAuditKhktScale(request);
        message.success(t("auditKhktScale.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditKhktScale.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditKhktScale.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditKhktScale(item.id)));
          message.success(t("auditKhktScale.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditKhktScale.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditKhktScaleItem>["columns"] = [
    { title: t("auditKhktScale.columns.sortOrder"), dataIndex: "sortOrder", width: 90, sorter: (a, b) => a.sortOrder - b.sortOrder },
    { title: t("auditKhktScale.columns.scaleName"), ...getSearchColumnProps("scaleName", searchLabels) },
    {
      title: t("auditKhktScale.columns.creditThreshold"),
      dataIndex: "creditThreshold",
      width: 160,
      render: (v: number | null) => v ?? "-",
    },
    {
      title: t("auditKhktScale.columns.fundingThreshold"),
      dataIndex: "fundingThreshold",
      width: 160,
      render: (v: number | null) => v ?? "-",
    },
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
      <Typography.Title level={4} style={{ margin: 0, marginBottom: 16 }}>
        {t("auditKhktScale.title")}
      </Typography.Title>
      <CrudTable<AuditKhktScaleItem>
        tableId="audit.plan.khktScale"
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
        onExportExcel={canExport ? () => exportAuditKhktScales("excel") : undefined}
        onExportWord={canExport ? () => exportAuditKhktScales("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditKhktScales(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditKhktScale.form.editTitle") : t("auditKhktScale.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={520}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="sortOrder" label={t("auditKhktScale.columns.sortOrder")} rules={[{ required: true }]}>
            <InputNumber style={{ width: "100%" }} min={1} />
          </Form.Item>
          <Form.Item name="scaleName" label={t("auditKhktScale.columns.scaleName")} rules={[{ required: true }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="creditThreshold" label={t("auditKhktScale.columns.creditThreshold")}>
            <InputNumber style={{ width: "100%" }} min={0} />
          </Form.Item>
          <Form.Item name="fundingThreshold" label={t("auditKhktScale.columns.fundingThreshold")}>
            <InputNumber style={{ width: "100%" }} min={0} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
