import { useCallback, useEffect, useState } from "react";
import { App, Form, Input, InputNumber, Modal, Select, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  riskCriteriaOtherApi,
  riskCriteriaOtherScaleApi,
  type RiskCriteriaOtherItem,
  type RiskCriteriaOtherScaleItem,
  type RiskCriteriaOtherScaleRequest,
} from "../../../api/riskScoringExec";
import { useAuth } from "../../../auth/AuthContext";
import { useAuditObjectOptions } from "../RiskScoring/useAuditObjectOptions";

interface FormValues {
  auditObjectCategoryId: string;
  criteriaOtherId: string;
  scaleScore: number;
  ratingLevel: string;
  description?: string;
  active: boolean;
}

/** Danh muc "Thang diem cua chi tieu danh gia rui ro HO, CNTT, Du an, Dich vu thue ngoai..." (sheet ZTC_CTRR_KHAC_TD). */
export function RiskCriteriaOtherScaleTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING_EXEC.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING_EXEC.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING_EXEC.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING_EXEC.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING_EXEC.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<RiskCriteriaOtherScaleItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };
  const { options: auditObjectCategoryOptions } = useAuditObjectOptions();

  const [items, setItems] = useState<RiskCriteriaOtherScaleItem[]>([]);
  const [criteriaOptions, setCriteriaOptions] = useState<RiskCriteriaOtherItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<RiskCriteriaOtherScaleItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RiskCriteriaOtherScaleItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();
  const selectedCategoryId = Form.useWatch("auditObjectCategoryId", form);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, criteria] = await Promise.all([riskCriteriaOtherScaleApi.list(), riskCriteriaOtherApi.list()]);
      setItems(list);
      setCriteriaOptions(criteria);
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
      criteriaOtherId: target.criteriaOtherId,
      scaleScore: target.scaleScore,
      ratingLevel: target.ratingLevel,
      description: target.description ?? undefined,
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
      criteriaOtherId: target.criteriaOtherId,
      scaleScore: target.scaleScore,
      ratingLevel: target.ratingLevel,
      description: target.description ?? undefined,
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
      const request: RiskCriteriaOtherScaleRequest = {
        auditObjectCategoryId: values.auditObjectCategoryId,
        criteriaOtherId: values.criteriaOtherId,
        scaleScore: values.scaleScore,
        ratingLevel: values.ratingLevel,
        description: values.description || null,
        active: values.active,
      };
      if (editing) {
        await riskCriteriaOtherScaleApi.update(editing.id, request);
        message.success(t("riskScoringExec.messages.updateSuccess"));
      } else {
        await riskCriteriaOtherScaleApi.create(request);
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
          await Promise.all(selected.map((item) => riskCriteriaOtherScaleApi.remove(item.id)));
          message.success(t("riskScoringExec.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoringExec.messages.deleteError"));
        }
      },
    });
  };

  const filteredCriteriaOptions = criteriaOptions
    .filter((c) => !selectedCategoryId || c.auditObjectCategoryId === selectedCategoryId)
    .map((c) => ({ value: c.id, label: `${c.code} - ${c.name}` }));

  const columns: TableProps<RiskCriteriaOtherScaleItem>["columns"] = [
    { title: t("riskScoring.columns.auditObjectCategory"), dataIndex: "auditObjectCategoryCode", width: 160, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoringExec.columns.criteriaCode"), dataIndex: "criteriaOtherCode", width: 120, ...getSearchColumnProps("criteriaOtherCode", searchLabels) },
    { title: t("riskScoringExec.columns.criteriaName"), dataIndex: "criteriaOtherName", render: (v: string | null) => v ?? "-" },
    { title: t("riskScoringExec.columns.scaleScore"), dataIndex: "scaleScore", width: 110, sorter: (a, b) => a.scaleScore - b.scaleScore },
    { title: t("riskScoringExec.columns.ratingLevel"), dataIndex: "ratingLevel", width: 160, ...getSearchColumnProps("ratingLevel", searchLabels) },
    { title: t("riskScoringExec.columns.description"), dataIndex: "description", render: (v: string | null) => v ?? "-" },
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
      <CrudTable<RiskCriteriaOtherScaleItem>
        tableId="riskScoringExec.criteriaOtherScale"
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
        onExportExcel={canExport ? () => riskCriteriaOtherScaleApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => riskCriteriaOtherScaleApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await riskCriteriaOtherScaleApi.importExcel(file);
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
            if (changed.auditObjectCategoryId) {
              form.setFieldValue("criteriaOtherId", undefined);
            }
          }}
        >
          <Form.Item name="auditObjectCategoryId" label={t("riskScoring.columns.auditObjectCategory")} rules={[{ required: true }]}>
            <Select options={auditObjectCategoryOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="criteriaOtherId" label={t("riskScoringExec.columns.criteriaCode")} rules={[{ required: true }]}>
            <Select options={filteredCriteriaOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="scaleScore" label={t("riskScoringExec.columns.scaleScore")} rules={[{ required: true }]}>
            <InputNumber style={{ width: "100%" }} min={0} max={999} />
          </Form.Item>
          <Form.Item name="ratingLevel" label={t("riskScoringExec.columns.ratingLevel")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label={t("riskScoringExec.columns.description")}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
