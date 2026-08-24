import { useCallback, useEffect, useState } from "react";
import { App, Form, Input, InputNumber, Modal, Select, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  criteriaQuantitativeApi,
  group1Api,
  group2Api,
  OBJECT_TYPE_OPTIONS,
  type CriteriaQuantitativeItem,
  type CriteriaQuantitativeRequest,
  type Group1Item,
  type Group2Item,
  type ObjectType,
} from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  objectType: ObjectType;
  group1Id: string;
  group2Id?: string;
  code: string;
  name: string;
  criteriaType?: number;
  businessThreshold?: number;
  viewThreshold?: number;
  score20?: number;
  score40?: number;
  score60?: number;
  score80?: number;
  score100?: number;
  scoringGuide?: string;
  includeCurrentYear: boolean;
  active: boolean;
}

/** Danh muc "Chi tieu danh gia rui ro dinh luong" (sheet ZTC_CTDGRR_DL), co 5 moc diem 20/40/60/80/100. */
export function CriteriaQuantitativeTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<CriteriaQuantitativeItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<CriteriaQuantitativeItem[]>([]);
  const [group1Options, setGroup1Options] = useState<Group1Item[]>([]);
  const [group2Options, setGroup2Options] = useState<Group2Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<CriteriaQuantitativeItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<CriteriaQuantitativeItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, group1List, group2List] = await Promise.all([
        criteriaQuantitativeApi.list(),
        group1Api.list(),
        group2Api.list(),
      ]);
      setItems(list);
      setGroup1Options(group1List);
      setGroup2Options(group2List);
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
    form.setFieldsValue({ active: true, includeCurrentYear: true });
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      objectType: target.objectType,
      group1Id: target.group1Id,
      group2Id: target.group2Id ?? undefined,
      code: target.code,
      name: target.name,
      criteriaType: target.criteriaType ?? undefined,
      businessThreshold: target.businessThreshold ?? undefined,
      viewThreshold: target.viewThreshold ?? undefined,
      score20: target.score20 ?? undefined,
      score40: target.score40 ?? undefined,
      score60: target.score60 ?? undefined,
      score80: target.score80 ?? undefined,
      score100: target.score100 ?? undefined,
      scoringGuide: target.scoringGuide ?? undefined,
      includeCurrentYear: target.includeCurrentYear,
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
      const request: CriteriaQuantitativeRequest = {
        objectType: values.objectType,
        group1Id: values.group1Id,
        group2Id: values.group2Id ?? null,
        code: values.code,
        name: values.name,
        criteriaType: values.criteriaType ?? null,
        businessThreshold: values.businessThreshold ?? null,
        viewThreshold: values.viewThreshold ?? null,
        score20: values.score20 ?? null,
        score40: values.score40 ?? null,
        score60: values.score60 ?? null,
        score80: values.score80 ?? null,
        score100: values.score100 ?? null,
        scoringGuide: values.scoringGuide ?? null,
        includeCurrentYear: values.includeCurrentYear,
        active: values.active,
      };
      if (editing) {
        await criteriaQuantitativeApi.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await criteriaQuantitativeApi.create(request);
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
          await criteriaQuantitativeApi.remove(target.id);
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<CriteriaQuantitativeItem>["columns"] = [
    { title: t("riskScoring.columns.objectType"), dataIndex: "objectType", width: 140 },
    { title: t("riskScoring.columns.group1"), dataIndex: "group1Code", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoring.columns.group2"), dataIndex: "group2Code", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoring.columns.code"), width: 110, ...getSearchColumnProps("code", searchLabels) },
    { title: t("riskScoring.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("riskScoring.columns.criteriaType"), dataIndex: "criteriaType", width: 90, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.score20"), dataIndex: "score20", width: 80, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.score40"), dataIndex: "score40", width: 80, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.score60"), dataIndex: "score60", width: 80, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.score80"), dataIndex: "score80", width: 80, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.score100"), dataIndex: "score100", width: 90, render: (v: number | null) => v ?? "-" },
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
      <CrudTable<CriteriaQuantitativeItem>
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
        onExportExcel={canExport ? () => criteriaQuantitativeApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => criteriaQuantitativeApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await criteriaQuantitativeApi.importExcel(file);
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
        width={720}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="objectType" label={t("riskScoring.columns.objectType")} rules={[{ required: true }]}>
            <Select options={OBJECT_TYPE_OPTIONS.map((o) => ({ value: o.value, label: o.label }))} />
          </Form.Item>
          <Form.Item name="group1Id" label={t("riskScoring.columns.group1")} rules={[{ required: true }]}>
            <Select options={group1Options.map((g) => ({ value: g.id, label: `${g.code} - ${g.name}` }))} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="group2Id" label={t("riskScoring.columns.group2")}>
            <Select
              allowClear
              options={group2Options.map((g) => ({ value: g.id, label: `${g.code} - ${g.name}` }))}
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="code" label={t("riskScoring.columns.code")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="name" label={t("riskScoring.columns.name")} rules={[{ required: true }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="criteriaType" label={t("riskScoring.columns.criteriaType")}>
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="businessThreshold" label={t("riskScoring.columns.businessThreshold")}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="viewThreshold" label={t("riskScoring.columns.viewThreshold")}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="score20" label={t("riskScoring.columns.score20")}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="score40" label={t("riskScoring.columns.score40")}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="score60" label={t("riskScoring.columns.score60")}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="score80" label={t("riskScoring.columns.score80")}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="score100" label={t("riskScoring.columns.score100")}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="scoringGuide" label={t("riskScoring.columns.scoringGuide")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="includeCurrentYear" label={t("riskScoring.columns.includeCurrentYear")} valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
