import { useCallback, useEffect, useState } from "react";
import { App, DatePicker, Form, Input, InputNumber, Modal, Result, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import type { MasterDataItem, MasterDataItemRequest } from "../../api/positionCatalog";
import {
  createPositionCatalogItem,
  deletePositionCatalogItem,
  exportPositionCatalog,
  importPositionCatalog,
  listPositionCatalog,
  updatePositionCatalogItem,
} from "../../api/positionCatalog";
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

/** "Danh muc Chuc vu" trong Nhan su - thay the han "Chuc danh" cu, du lieu dung chung
 * audit_master_data_item (category=POSITION) nhung permission rieng PEOPLE.POSITION.*. */
export function PositionCatalogPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("PEOPLE.POSITION.VIEW");
  const canCreate = hasPermission("PEOPLE.POSITION.CREATE");
  const canEdit = hasPermission("PEOPLE.POSITION.EDIT");
  const canDelete = hasPermission("PEOPLE.POSITION.DELETE");
  const canExport = hasPermission("PEOPLE.POSITION.EXPORT");
  const canImport = hasPermission("PEOPLE.POSITION.IMPORT");
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
      setItems(await listPositionCatalog());
    } catch {
      message.error(t("position.messages.loadError"));
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
        await updatePositionCatalogItem(editing.id, request);
        message.success(t("position.messages.updateSuccess"));
      } else {
        await createPositionCatalogItem(request);
        message.success(t("position.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("position.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("position.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deletePositionCatalogItem(item.id)));
          message.success(t("position.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("position.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<MasterDataItem>["columns"] = [
    { title: t("position.columns.code"), width: 140, ...getSearchColumnProps("code", searchLabels) },
    { title: t("position.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("position.columns.description"), dataIndex: "description", render: (v: string | null) => v ?? "-" },
    { title: t("position.columns.sortOrder"), dataIndex: "sortOrder", width: 100 },
    {
      title: t("common.active"),
      dataIndex: "active",
      width: 120,
      sorter: (a, b) => Number(a.active) - Number(b.active),
      render: (v: boolean) => (v ? t("common.active") : t("common.inactive")),
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("position.title")}</Typography.Title>
      <CrudTable<MasterDataItem>
        tableId="people.positions"
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
        deleteDisabled={selected.length === 0}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport ? () => exportPositionCatalog("excel") : undefined}
        onExportWord={canExport ? () => exportPositionCatalog("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importPositionCatalog(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("position.form.editTitle") : t("position.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="code" label={t("position.form.code")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="name" label={t("position.form.name")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label={t("position.form.description")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="validFrom" label={t("position.form.validFrom")}>
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="validTo" label={t("position.form.validTo")}>
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="sortOrder" label={t("position.form.sortOrder")}>
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
