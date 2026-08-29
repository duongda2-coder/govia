import { useCallback, useEffect, useState } from "react";
import { App, Form, Input, InputNumber, Modal, Select, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  groupHOApi,
  riskCriteriaOtherApi,
  riskTypeHOApi,
  type GroupHOItem,
  type RiskCriteriaOtherItem,
  type RiskCriteriaOtherRequest,
  type RiskTypeHOItem,
} from "../../../api/riskScoringExec";
import { useAuth } from "../../../auth/AuthContext";
import { useAuditObjectOptions } from "../RiskScoring/useAuditObjectOptions";

interface FormValues {
  auditObjectCategoryId: string;
  code: string;
  name: string;
  weight?: number;
  groupHoId?: string;
  riskTypeHoId?: string;
  active: boolean;
}

/** Danh muc "Chi tieu danh gia rui ro HO, CNTT, Du an, Dich vu thue ngoai..." (sheet ZTC_CTDGRR_KHAC). */
export function RiskCriteriaOtherTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING_EXEC.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING_EXEC.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING_EXEC.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING_EXEC.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING_EXEC.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<RiskCriteriaOtherItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };
  const { options: auditObjectCategoryOptions } = useAuditObjectOptions();

  const [items, setItems] = useState<RiskCriteriaOtherItem[]>([]);
  const [groupOptions, setGroupOptions] = useState<GroupHOItem[]>([]);
  const [typeOptions, setTypeOptions] = useState<RiskTypeHOItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<RiskCriteriaOtherItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RiskCriteriaOtherItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();
  const selectedGroupHoId = Form.useWatch("groupHoId", form);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, groups, types] = await Promise.all([riskCriteriaOtherApi.list(), groupHOApi.list(), riskTypeHOApi.list()]);
      setItems(list);
      setGroupOptions(groups);
      setTypeOptions(types);
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
      auditObjectCategoryId: target.auditObjectCategoryId,
      code: target.code,
      name: target.name,
      weight: target.weight ?? undefined,
      groupHoId: target.groupHoId ?? undefined,
      riskTypeHoId: target.riskTypeHoId ?? undefined,
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
      groupHoId: target.groupHoId ?? undefined,
      riskTypeHoId: target.riskTypeHoId ?? undefined,
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
      const request: RiskCriteriaOtherRequest = {
        auditObjectCategoryId: values.auditObjectCategoryId,
        code: values.code,
        name: values.name,
        weight: values.weight ?? null,
        groupHoId: values.groupHoId,
        riskTypeHoId: values.riskTypeHoId,
        active: values.active,
      };
      if (editing) {
        await riskCriteriaOtherApi.update(editing.id, request);
        message.success(t("riskScoringExec.messages.updateSuccess"));
      } else {
        await riskCriteriaOtherApi.create(request);
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
          await Promise.all(selected.map((item) => riskCriteriaOtherApi.remove(item.id)));
          message.success(t("riskScoringExec.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoringExec.messages.deleteError"));
        }
      },
    });
  };

  const filteredTypeOptions = typeOptions
    .filter((rt) => !selectedGroupHoId || rt.groupHoId === selectedGroupHoId)
    .map((rt) => ({ value: rt.id, label: `${rt.code} - ${rt.name}` }));

  const columns: TableProps<RiskCriteriaOtherItem>["columns"] = [
    {
      title: t("riskScoring.columns.auditObjectCategory"),
      width: 160,
      ...getSearchColumnProps("auditObjectCategoryCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("riskScoringExec.columns.criteriaCode"), dataIndex: "code", width: 120, ...getSearchColumnProps("code", searchLabels) },
    { title: t("riskScoringExec.columns.criteriaName"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("riskScoring.columns.weight"), dataIndex: "weight", width: 100, render: (v: number | null) => v ?? "-" },
    {
      title: t("riskScoringExec.columns.groupHo"),
      width: 160,
      ...getSearchColumnProps("groupHoCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("riskScoringExec.columns.riskType"),
      width: 160,
      ...getSearchColumnProps("riskTypeHoCode", searchLabels),
      render: (v: string | null) => v ?? "-",
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
    return null;
  }

  return (
    <div>
      <CrudTable<RiskCriteriaOtherItem>
        tableId="riskScoringExec.criteriaOther"
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
        onExportExcel={canExport ? () => riskCriteriaOtherApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => riskCriteriaOtherApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await riskCriteriaOtherApi.importExcel(file);
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
        <Form<FormValues>
          form={form}
          layout="vertical"
          onValuesChange={(changed) => {
            if (changed.groupHoId) {
              form.setFieldValue("riskTypeHoId", undefined);
            }
          }}
        >
          <Form.Item name="auditObjectCategoryId" label={t("riskScoring.columns.auditObjectCategory")} rules={[{ required: true }]}>
            <Select options={auditObjectCategoryOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item
            name="code"
            label={t("riskScoringExec.columns.criteriaCode")}
            rules={[{ required: true }, { max: 2, message: t("riskScoringExec.form.criteriaCodeMaxLength") }]}
          >
            <Input maxLength={2} />
          </Form.Item>
          <Form.Item name="name" label={t("riskScoringExec.columns.criteriaName")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="weight" label={t("riskScoring.columns.weight")}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="groupHoId" label={t("riskScoringExec.columns.groupHo")}>
            <Select options={groupOptions.map((g) => ({ value: g.id, label: `${g.code} - ${g.name}` }))} showSearch optionFilterProp="label" allowClear />
          </Form.Item>
          <Form.Item name="riskTypeHoId" label={t("riskScoringExec.columns.riskType")}>
            <Select options={filteredTypeOptions} showSearch optionFilterProp="label" allowClear />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
