import { useCallback, useEffect, useState } from "react";
import { App, Form, Input, InputNumber, Modal, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import { weightByBusinessApi, type WeightByBusinessItem, type WeightByBusinessRequest } from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  businessCode: string;
  qualitativeWeight?: number;
  quantitativeWeight?: number;
  fromYear?: number;
  toYear?: number;
  active: boolean;
}

/** Danh muc "Ty trong Dinh tinh/Dinh luong theo Nghiep vu" (sheet ZTC_DLDT_TT). */
export function WeightByBusinessTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<WeightByBusinessItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<WeightByBusinessItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<WeightByBusinessItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<WeightByBusinessItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();
  const qualitativeWeightWatch = Form.useWatch("qualitativeWeight", form);
  const quantitativeWeightWatch = Form.useWatch("quantitativeWeight", form);
  const weightSum =
    qualitativeWeightWatch != null && quantitativeWeightWatch != null ? qualitativeWeightWatch + quantitativeWeightWatch : null;

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await weightByBusinessApi.list());
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
      businessCode: target.businessCode,
      qualitativeWeight: target.qualitativeWeight ?? undefined,
      quantitativeWeight: target.quantitativeWeight ?? undefined,
      fromYear: target.fromYear ?? undefined,
      toYear: target.toYear ?? undefined,
      active: target.active,
    });
    setModalOpen(true);
  };

  const openCopy = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(null);
    form.setFieldsValue({
      businessCode: "",
      qualitativeWeight: target.qualitativeWeight ?? undefined,
      quantitativeWeight: target.quantitativeWeight ?? undefined,
      fromYear: target.fromYear ?? undefined,
      toYear: target.toYear ?? undefined,
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
      const request: WeightByBusinessRequest = {
        businessCode: values.businessCode,
        qualitativeWeight: values.qualitativeWeight ?? null,
        quantitativeWeight: values.quantitativeWeight ?? null,
        fromYear: values.fromYear ?? null,
        toYear: values.toYear ?? null,
        active: values.active,
      };
      if (editing) {
        await weightByBusinessApi.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await weightByBusinessApi.create(request);
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
          await weightByBusinessApi.remove(target.id);
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<WeightByBusinessItem>["columns"] = [
    { title: t("riskScoring.columns.businessCode"), width: 130, ...getSearchColumnProps("businessCode", searchLabels) },
    { title: t("riskScoring.columns.qualitativeWeight"), dataIndex: "qualitativeWeight", width: 130, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.quantitativeWeight"), dataIndex: "quantitativeWeight", width: 130, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.fromYear"), dataIndex: "fromYear", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.toYear"), dataIndex: "toYear", width: 100, render: (v: number | null) => v ?? "-" },
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
      <CrudTable<WeightByBusinessItem>
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
        onExportExcel={canExport ? () => weightByBusinessApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => weightByBusinessApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await weightByBusinessApi.importExcel(file);
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
          <Form.Item name="businessCode" label={t("riskScoring.columns.businessCode")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="qualitativeWeight" label={t("riskScoring.columns.qualitativeWeight")}>
            <InputNumber style={{ width: "100%" }} step={0.01} min={0} max={1} />
          </Form.Item>
          <Form.Item
            name="quantitativeWeight"
            label={t("riskScoring.columns.quantitativeWeight")}
            dependencies={["qualitativeWeight"]}
            extra={
              weightSum != null && (
                <span style={{ color: Math.abs(weightSum - 1) > 0.001 ? "#dc2626" : "#16a34a" }}>
                  {t("riskScoring.messages.weightSumHint", { percent: Math.round(weightSum * 100) })}
                </span>
              )
            }
            rules={[
              {
                validator: async (_, value) => {
                  const qualitative = form.getFieldValue("qualitativeWeight");
                  if (qualitative != null && value != null && Math.abs(qualitative + value - 1) > 0.001) {
                    throw new Error(t("riskScoring.messages.weightSumInvalid"));
                  }
                },
              },
            ]}
          >
            <InputNumber style={{ width: "100%" }} step={0.01} min={0} max={1} />
          </Form.Item>
          <Form.Item name="fromYear" label={t("riskScoring.columns.fromYear")}>
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="toYear" label={t("riskScoring.columns.toYear")}>
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
