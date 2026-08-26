import { useCallback, useEffect, useState } from "react";
import { App, DatePicker, Form, Input, InputNumber, Modal, Switch } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import type { MasterDataItem, MasterDataItemRequest } from "../../api/auditMasterData";
import {
  createMasterDataItem,
  deleteMasterDataItem,
  exportMasterDataItems,
  importMasterDataItems,
  listMasterDataItems,
  updateMasterDataItem,
} from "../../api/auditMasterData";
import { useAuth } from "../../auth/AuthContext";

interface FormValues {
  code: string;
  name: string;
  description?: string;
  validFrom?: dayjs.Dayjs;
  validTo?: dayjs.Dayjs;
  sortOrder?: number;
  active: boolean;
}

/**
 * Man hinh danh muc DUNG CHUNG cho toan bo cac loai danh muc cua module Kiem toan noi bo (xem
 * AuditMasterDataCategory o backend) - 1 component duy nhat, chi khac nhau o "category" truyen vao,
 * thay vi viet lai man hinh CRUD cho tung danh muc.
 */
export function MasterDataCatalogTable({ category, label }: { category: string; label: string }) {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.MASTER_DATA.VIEW");
  const canCreate = hasPermission("AUDIT.MASTER_DATA.CREATE");
  const canEdit = hasPermission("AUDIT.MASTER_DATA.EDIT");
  const canDelete = hasPermission("AUDIT.MASTER_DATA.DELETE");
  const canExport = hasPermission("AUDIT.MASTER_DATA.EXPORT");
  const canImport = hasPermission("AUDIT.MASTER_DATA.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<MasterDataItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<MasterDataItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<MasterDataItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<MasterDataItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listMasterDataItems(category));
    } catch {
      message.error(t("auditMasterData.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [category, message, t]);

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
      code: target.code,
      name: target.name,
      description: target.description ?? undefined,
      validFrom: target.validFrom ? dayjs(target.validFrom) : undefined,
      validTo: target.validTo ? dayjs(target.validTo) : undefined,
      sortOrder: target.sortOrder ?? undefined,
      active: target.active,
    });
    setModalOpen(true);
  };

  const openCopy = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(null);
    form.setFieldsValue({
      code: "",
      name: target.name,
      description: target.description ?? undefined,
      validFrom: target.validFrom ? dayjs(target.validFrom) : undefined,
      validTo: target.validTo ? dayjs(target.validTo) : undefined,
      sortOrder: target.sortOrder ?? undefined,
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
      const request: MasterDataItemRequest = {
        code: values.code,
        name: values.name,
        description: values.description || null,
        validFrom: values.validFrom ? values.validFrom.format("YYYY-MM-DD") : null,
        validTo: values.validTo ? values.validTo.format("YYYY-MM-DD") : null,
        sortOrder: values.sortOrder ?? null,
        active: values.active,
      };
      if (editing) {
        await updateMasterDataItem(category, editing.id, request);
        message.success(t("auditMasterData.messages.updateSuccess"));
      } else {
        await createMasterDataItem(category, request);
        message.success(t("auditMasterData.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditMasterData.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    const target = selected[0];
    if (!target) return;
    modal.confirm({
      title: t("auditMasterData.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deleteMasterDataItem(category, target.id);
          message.success(t("auditMasterData.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditMasterData.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<MasterDataItem>["columns"] = [
    { title: t("auditMasterData.columns.code"), width: 140, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditMasterData.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("auditMasterData.columns.description"), dataIndex: "description", render: (v: string | null) => v ?? "-" },
    { title: t("auditMasterData.columns.sortOrder"), dataIndex: "sortOrder", width: 100 },
    {
      title: t("common.active"),
      dataIndex: "active",
      width: 120,
      sorter: (a, b) => Number(a.active) - Number(b.active),
      render: (v: boolean) => (v ? t("common.active") : t("common.inactive")),
    },
  ];

  if (!canView) {
    return null;
  }

  return (
    <div>
      <CrudTable<MasterDataItem>
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onCopy={canCreate ? openCopy : undefined}
        copyDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length !== 1}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport ? () => exportMasterDataItems(category, "excel") : undefined}
        onExportWord={canExport ? () => exportMasterDataItems(category, "word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importMasterDataItems(category, file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditMasterData.form.editTitle", { label }) : t("auditMasterData.form.createTitle", { label })}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="code" label={t("auditMasterData.form.code")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="name" label={t("auditMasterData.form.name")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label={t("auditMasterData.form.description")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="validFrom" label={t("auditMasterData.form.validFrom")}>
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="validTo" label={t("auditMasterData.form.validTo")}>
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="sortOrder" label={t("auditMasterData.form.sortOrder")}>
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
