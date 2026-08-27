import { useCallback, useEffect, useState } from "react";
import { App, Form, Input, Modal, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import { groupHOApi, type GroupHOItem, type GroupHORequest } from "../../../api/riskScoringExec";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  code: string;
  name: string;
  note?: string;
  active: boolean;
}

/** Danh muc "Nhom rui ro HO theo tuyen bao ve" (sheet ZTC_Nhom_DGRR_HO, tcode ztc_nhom_dgrr_ho). */
export function GroupHOTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING_EXEC.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING_EXEC.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING_EXEC.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING_EXEC.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING_EXEC.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<GroupHOItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<GroupHOItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<GroupHOItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<GroupHOItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await groupHOApi.list());
    } catch {
      message.error(t("riskScoringExec.messages.loadError"));
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
      note: target.note ?? undefined,
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
      note: target.note ?? undefined,
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
      const request: GroupHORequest = {
        code: values.code,
        name: values.name,
        note: values.note || null,
        active: values.active,
      };
      if (editing) {
        await groupHOApi.update(editing.id, request);
        message.success(t("riskScoringExec.messages.updateSuccess"));
      } else {
        await groupHOApi.create(request);
        message.success(t("riskScoringExec.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("riskScoringExec.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("riskScoringExec.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => groupHOApi.remove(item.id)));
          message.success(t("riskScoringExec.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoringExec.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<GroupHOItem>["columns"] = [
    { title: t("riskScoringExec.columns.code"), width: 120, ...getSearchColumnProps("code", searchLabels) },
    { title: t("riskScoringExec.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("riskScoringExec.columns.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
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
      <CrudTable<GroupHOItem>
        tableId="riskScoringExec.groupHO"
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
        onExportExcel={canExport ? () => groupHOApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => groupHOApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await groupHOApi.importExcel(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("riskScoringExec.form.editTitle") : t("riskScoringExec.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="code" label={t("riskScoringExec.columns.code")} rules={[{ required: true }, { max: 2, message: t("riskScoringExec.form.codeMaxLength") }]}>
            <Input maxLength={2} />
          </Form.Item>
          <Form.Item name="name" label={t("riskScoringExec.columns.name")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="note" label={t("riskScoringExec.columns.note")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
