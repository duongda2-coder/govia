import { useCallback, useEffect, useState } from "react";
import { App, DatePicker, Form, Input, InputNumber, Modal, Select, Switch } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import { group1Api, group2Api, type Group1Item, type Group2Item, type Group2Request } from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  group1Id: string;
  code: string;
  name: string;
  weight?: number;
  validFrom?: dayjs.Dayjs;
  validTo?: dayjs.Dayjs;
  active: boolean;
}

/** Danh muc "Nhom chi tieu cap 2" (sheet ZTC_DGRR_Group2), thuoc ve 1 nhom cap 1. */
export function Group2Table() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<Group2Item>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<Group2Item[]>([]);
  const [group1Options, setGroup1Options] = useState<Group1Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<Group2Item[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Group2Item | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, group1List] = await Promise.all([group2Api.list(), group1Api.list()]);
      setItems(list);
      setGroup1Options(group1List);
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
      group1Id: target.group1Id,
      code: target.code,
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
      const request: Group2Request = {
        group1Id: values.group1Id,
        code: values.code,
        name: values.name,
        weight: values.weight ?? null,
        validFrom: values.validFrom ? values.validFrom.format("YYYY-MM-DD") : null,
        validTo: values.validTo ? values.validTo.format("YYYY-MM-DD") : null,
        active: values.active,
      };
      if (editing) {
        await group2Api.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await group2Api.create(request);
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
    const target = selected[0];
    if (!target) return;
    modal.confirm({
      title: t("riskScoring.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await group2Api.remove(target.id);
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<Group2Item>["columns"] = [
    { title: t("riskScoring.columns.group1"), dataIndex: "group1Code", width: 120, render: (v: string | null) => v ?? "-" },
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
      <CrudTable<Group2Item>
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length !== 1}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport ? () => group2Api.exportFile("excel") : undefined}
        onExportWord={canExport ? () => group2Api.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await group2Api.importExcel(file);
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
          <Form.Item name="group1Id" label={t("riskScoring.columns.group1")} rules={[{ required: true }]}>
            <Select options={group1Options.map((g) => ({ value: g.id, label: `${g.code} - ${g.name}` }))} showSearch optionFilterProp="label" />
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
