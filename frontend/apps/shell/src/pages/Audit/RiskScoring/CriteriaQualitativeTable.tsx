import { useCallback, useEffect, useState } from "react";
import { App, Form, Input, InputNumber, Modal, Select, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  criteriaQualitativeApi,
  group1Api,
  group2Api,
  type CriteriaQualitativeItem,
  type CriteriaQualitativeRequest,
  type Group1Item,
  type Group2Item,
} from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";
import { auditObjectRefValue, parseAuditObjectRefValue, useAuditObjectOptions } from "./useAuditObjectOptions";

interface FormValues {
  auditObjectRef: string;
  group1Id: string;
  group2Id?: string;
  code: string;
  name: string;
  weight?: number;
  impactLevel?: number;
  likelihoodLevel?: number;
  includeCurrentYear: boolean;
  active: boolean;
}

/** Danh muc "Chi tieu danh gia rui ro dinh tinh" (sheet ZTC_CTDGRR_DT). */
export function CriteriaQualitativeTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<CriteriaQualitativeItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };
  const { groups: auditObjectGroups } = useAuditObjectOptions();

  const [items, setItems] = useState<CriteriaQualitativeItem[]>([]);
  const [group1Options, setGroup1Options] = useState<Group1Item[]>([]);
  const [group2Options, setGroup2Options] = useState<Group2Item[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<CriteriaQualitativeItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<CriteriaQualitativeItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();
  const auditObjectRefWatch = Form.useWatch("auditObjectRef", form);
  const group1IdWatch = Form.useWatch("group1Id", form);
  const group1OptionsForAuditObject = auditObjectRefWatch
    ? group1Options.filter((g) => {
        const { type, id } = parseAuditObjectRefValue(auditObjectRefWatch);
        return g.auditObjectType === type && g.auditObjectId === id;
      })
    : [];
  const group2OptionsForGroup1 = group2Options.filter((g) => g.group1Id === group1IdWatch);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, group1List, group2List] = await Promise.all([
        criteriaQualitativeApi.list(),
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
      auditObjectRef: auditObjectRefValue(target.auditObjectType, target.auditObjectId),
      group1Id: target.group1Id,
      group2Id: target.group2Id ?? undefined,
      code: target.code,
      name: target.name,
      weight: target.weight ?? undefined,
      impactLevel: target.impactLevel ?? undefined,
      likelihoodLevel: target.likelihoodLevel ?? undefined,
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
      const { type: auditObjectType, id: auditObjectId } = parseAuditObjectRefValue(values.auditObjectRef);
      const request: CriteriaQualitativeRequest = {
        auditObjectType,
        auditObjectId,
        group1Id: values.group1Id,
        group2Id: values.group2Id ?? null,
        code: values.code,
        name: values.name,
        weight: values.weight ?? null,
        impactLevel: values.impactLevel ?? null,
        likelihoodLevel: values.likelihoodLevel ?? null,
        includeCurrentYear: values.includeCurrentYear,
        active: values.active,
      };
      if (editing) {
        await criteriaQualitativeApi.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await criteriaQualitativeApi.create(request);
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
          await criteriaQualitativeApi.remove(target.id);
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<CriteriaQualitativeItem>["columns"] = [
    {
      title: t("riskScoring.columns.auditObject"),
      width: 200,
      render: (_: unknown, record: CriteriaQualitativeItem) => (record.auditObjectCode ? `${record.auditObjectCode} - ${record.auditObjectName}` : "-"),
    },
    { title: t("riskScoring.columns.group1"), dataIndex: "group1Code", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoring.columns.group2"), dataIndex: "group2Code", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoring.columns.code"), width: 110, ...getSearchColumnProps("code", searchLabels) },
    { title: t("riskScoring.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("riskScoring.columns.weight"), dataIndex: "weight", width: 90, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.impactLevel"), dataIndex: "impactLevel", width: 110, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.likelihoodLevel"), dataIndex: "likelihoodLevel", width: 110, render: (v: number | null) => v ?? "-" },
    {
      title: t("riskScoring.columns.includeCurrentYear"),
      dataIndex: "includeCurrentYear",
      width: 130,
      render: (v: boolean) => (v ? t("common.yes") : t("common.no")),
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
      <CrudTable<CriteriaQualitativeItem>
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
        onExportExcel={canExport ? () => criteriaQualitativeApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => criteriaQualitativeApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await criteriaQualitativeApi.importExcel(file);
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
        width={640}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Form.Item name="auditObjectRef" label={t("riskScoring.columns.auditObject")} rules={[{ required: true }]}>
            <Select
              options={auditObjectGroups}
              showSearch
              optionFilterProp="label"
              onChange={(value) => {
                const currentGroup1 = form.getFieldValue("group1Id") as string | undefined;
                if (!currentGroup1) return;
                const { type, id } = parseAuditObjectRefValue(value);
                const stillValid = group1Options.some((g) => g.id === currentGroup1 && g.auditObjectType === type && g.auditObjectId === id);
                if (!stillValid) {
                  form.setFieldValue("group1Id", undefined);
                  form.setFieldValue("group2Id", undefined);
                }
              }}
            />
          </Form.Item>
          <Form.Item name="group1Id" label={t("riskScoring.columns.group1")} rules={[{ required: true }]}>
            <Select
              disabled={!auditObjectRefWatch}
              placeholder={!auditObjectRefWatch ? t("riskScoring.form.selectAuditObjectFirst") : undefined}
              options={group1OptionsForAuditObject.map((g) => ({ value: g.id, label: `${g.code} - ${g.name}` }))}
              showSearch
              optionFilterProp="label"
              onChange={(value) => {
                const current = form.getFieldValue("group2Id") as string | undefined;
                if (current && !group2Options.some((g) => g.id === current && g.group1Id === value)) {
                  form.setFieldValue("group2Id", undefined);
                }
              }}
            />
          </Form.Item>
          <Form.Item name="group2Id" label={t("riskScoring.columns.group2")}>
            <Select
              allowClear
              disabled={!group1IdWatch}
              placeholder={!group1IdWatch ? t("riskScoring.form.selectGroup1First") : undefined}
              options={group2OptionsForGroup1.map((g) => ({ value: g.id, label: `${g.code} - ${g.name}` }))}
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
          <Form.Item name="weight" label={t("riskScoring.columns.weight")}>
            <InputNumber style={{ width: "100%" }} step={0.01} />
          </Form.Item>
          <Form.Item name="impactLevel" label={t("riskScoring.columns.impactLevel")}>
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="likelihoodLevel" label={t("riskScoring.columns.likelihoodLevel")}>
            <InputNumber style={{ width: "100%" }} />
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
