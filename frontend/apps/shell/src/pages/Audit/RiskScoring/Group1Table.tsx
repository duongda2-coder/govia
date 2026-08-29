import { useCallback, useEffect, useState } from "react";
import { App, DatePicker, Form, Input, InputNumber, Modal, Select, Switch } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  group1Api,
  type Group1Item,
  type Group1Request,
} from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";
import { useAuditObjectOptions } from "./useAuditObjectOptions";

interface FormValues {
  auditObjectCategoryId: string;
  code: string;
  name: string;
  weight?: number;
  validFrom?: dayjs.Dayjs;
  validTo?: dayjs.Dayjs;
  active: boolean;
}

/** Danh muc "Nhom chi tieu cap 1" (sheet ZTC_DGRR_Group1). */
export function Group1Table() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<Group1Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };
  const { options: auditObjectCategoryOptions } = useAuditObjectOptions();

  const [items, setItems] = useState<Group1Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<Group1Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Group1Item | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await group1Api.list());
    } catch {
      message.error(t("riskScoring.messages.loadError"));
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
      auditObjectCategoryId: target.auditObjectCategoryId,
      code: target.code,
      name: target.name,
      weight: target.weight ?? undefined,
      validFrom: target.validFrom ? dayjs(target.validFrom) : undefined,
      validTo: target.validTo ? dayjs(target.validTo) : undefined,
      active: target.active,
    });
    setModalOpen(true);
  };

  const openCopy = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(null);
    form.setFieldsValue({
      auditObjectCategoryId: target.auditObjectCategoryId,
      code: "",
      name: target.name,
      weight: target.weight ?? undefined,
      validFrom: target.validFrom ? dayjs(target.validFrom) : undefined,
      validTo: target.validTo ? dayjs(target.validTo) : undefined,
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
      const request: Group1Request = {
        auditObjectCategoryId: values.auditObjectCategoryId,
        code: values.code,
        name: values.name,
        weight: values.weight ?? null,
        validFrom: values.validFrom ? values.validFrom.format("YYYY-MM-DD") : null,
        validTo: values.validTo ? values.validTo.format("YYYY-MM-DD") : null,
        active: values.active,
      };
      if (editing) {
        await group1Api.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await group1Api.create(request);
        message.success(t("riskScoring.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("riskScoring.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("riskScoring.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => group1Api.remove(item.id)));
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<Group1Item>["columns"] = [
    {
      title: t("riskScoring.columns.auditObjectCategory"),
      width: 220,
      ...getSearchColumnProps("auditObjectCategoryCode", searchLabels),
      render: (_: unknown, record: Group1Item) =>
        record.auditObjectCategoryCode ? `${record.auditObjectCategoryCode} - ${record.auditObjectCategoryName}` : "-",
    },
    { title: t("riskScoring.columns.code"), width: 120, ...getSearchColumnProps("code", searchLabels) },
    { title: t("riskScoring.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("riskScoring.columns.weight"), dataIndex: "weight", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.validFrom"), dataIndex: "validFrom", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoring.columns.validTo"), dataIndex: "validTo", width: 120, render: (v: string | null) => v ?? "-" },
    {
      title: t("common.active"),
      dataIndex: "active",
      width: 110,
      sorter: (a, b) => Number(a.active) - Number(b.active),
      render: (v: boolean) => (v ? t("common.active") : t("common.inactive")),
    },
  ];

  if (!canView) {
    return null;
  }

  return (
    <div>
      <CrudTable<Group1Item>
        tableId="riskScoring.group1"
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
        onExportExcel={canExport ? () => group1Api.exportFile("excel") : undefined}
        onExportWord={canExport ? () => group1Api.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await group1Api.importExcel(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("riskScoring.form.editTitle") : t("riskScoring.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="auditObjectCategoryId" label={t("riskScoring.columns.auditObjectCategory")} rules={[{ required: true }]}>
            <Select options={auditObjectCategoryOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="code" label={t("riskScoring.columns.code")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="name" label={t("riskScoring.columns.name")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="weight" label={t("riskScoring.columns.weight")}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="validFrom" label={t("riskScoring.columns.validFrom")}>
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="validTo" label={t("riskScoring.columns.validTo")}>
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
