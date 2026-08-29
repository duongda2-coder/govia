import { useCallback, useEffect, useState } from "react";
import { App, Form, Input, InputNumber, Modal, Select, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  groupHOApi,
  riskTypeHOApi,
  type GroupHOItem,
  type RiskTypeHOItem,
  type RiskTypeHORequest,
} from "../../../api/riskScoringExec";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  groupHoId: string;
  code: string;
  name: string;
  weight?: number;
  active: boolean;
}

/** Danh muc "Loai rui ro HO" (sheet ZTC_RR_HO, tcode ztc_rr_ho), thuoc ve 1 nhom rui ro HO. */
export function RiskTypeHOTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING_EXEC.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING_EXEC.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING_EXEC.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING_EXEC.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING_EXEC.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<RiskTypeHOItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<RiskTypeHOItem[]>([]);
  const [groupOptions, setGroupOptions] = useState<GroupHOItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<RiskTypeHOItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RiskTypeHOItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, groups] = await Promise.all([riskTypeHOApi.list(), groupHOApi.list()]);
      setItems(list);
      setGroupOptions(groups);
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
      groupHoId: target.groupHoId,
      code: target.code,
      name: target.name,
      weight: target.weight ?? undefined,
      active: target.active,
    });
    setModalOpen(true);
  };

  const openCopy = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(null);
    form.setFieldsValue({
      groupHoId: target.groupHoId,
      code: "",
      name: target.name,
      weight: target.weight ?? undefined,
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
      const request: RiskTypeHORequest = {
        groupHoId: values.groupHoId,
        code: values.code,
        name: values.name,
        weight: values.weight ?? null,
        active: values.active,
      };
      if (editing) {
        await riskTypeHOApi.update(editing.id, request);
        message.success(t("riskScoringExec.messages.updateSuccess"));
      } else {
        await riskTypeHOApi.create(request);
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
          await Promise.all(selected.map((item) => riskTypeHOApi.remove(item.id)));
          message.success(t("riskScoringExec.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoringExec.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<RiskTypeHOItem>["columns"] = [
    {
      title: t("riskScoringExec.columns.groupHo"),
      width: 160,
      ...getSearchColumnProps("groupHoCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("riskScoringExec.columns.riskTypeCode"), dataIndex: "code", width: 120, ...getSearchColumnProps("code", searchLabels) },
    { title: t("riskScoringExec.columns.riskTypeName"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("riskScoring.columns.weight"), dataIndex: "weight", width: 100, render: (v: number | null) => v ?? "-" },
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
      <CrudTable<RiskTypeHOItem>
        tableId="riskScoringExec.riskTypeHo"
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
        onExportExcel={canExport ? () => riskTypeHOApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => riskTypeHOApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await riskTypeHOApi.importExcel(file);
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
          <Form.Item name="groupHoId" label={t("riskScoringExec.columns.groupHo")} rules={[{ required: true }]}>
            <Select options={groupOptions.map((g) => ({ value: g.id, label: `${g.code} - ${g.name}` }))} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item
            name="code"
            label={t("riskScoringExec.columns.riskTypeCode")}
            rules={[{ required: true }, { max: 2, message: t("riskScoringExec.form.riskTypeCodeMaxLength") }]}
          >
            <Input maxLength={2} />
          </Form.Item>
          <Form.Item name="name" label={t("riskScoringExec.columns.riskTypeName")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="weight" label={t("riskScoring.columns.weight")}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
