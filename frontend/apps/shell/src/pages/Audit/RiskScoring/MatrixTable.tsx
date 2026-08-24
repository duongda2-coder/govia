import { useCallback, useEffect, useState } from "react";
import { App, Form, Input, InputNumber, Modal, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import { matrixApi, type MatrixItem, type MatrixRequest } from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  frequencyLevel: number;
  frequencyLabel: string;
  scoreLowSeverity?: number;
  scoreMediumSeverity?: number;
  scoreHighSeverity?: number;
  active: boolean;
}

/** Danh muc "Ma tran quy doi diem rui ro" (sheet ztc_mtrr_dt) - 1 dong / 1 muc tan suat (1-5). */
export function MatrixTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<MatrixItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<MatrixItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<MatrixItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<MatrixItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await matrixApi.list());
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
      frequencyLevel: target.frequencyLevel,
      frequencyLabel: target.frequencyLabel,
      scoreLowSeverity: target.scoreLowSeverity ?? undefined,
      scoreMediumSeverity: target.scoreMediumSeverity ?? undefined,
      scoreHighSeverity: target.scoreHighSeverity ?? undefined,
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
      const request: MatrixRequest = {
        frequencyLevel: values.frequencyLevel,
        frequencyLabel: values.frequencyLabel,
        scoreLowSeverity: values.scoreLowSeverity ?? null,
        scoreMediumSeverity: values.scoreMediumSeverity ?? null,
        scoreHighSeverity: values.scoreHighSeverity ?? null,
        active: values.active,
      };
      if (editing) {
        await matrixApi.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await matrixApi.create(request);
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
          await matrixApi.remove(target.id);
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<MatrixItem>["columns"] = [
    { title: t("riskScoring.columns.frequencyLevel"), dataIndex: "frequencyLevel", width: 110 },
    { title: t("riskScoring.columns.frequencyLabel"), ...getSearchColumnProps("frequencyLabel", searchLabels) },
    { title: t("riskScoring.columns.scoreLow"), dataIndex: "scoreLowSeverity", width: 110, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.scoreMedium"), dataIndex: "scoreMediumSeverity", width: 130, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.scoreHigh"), dataIndex: "scoreHighSeverity", width: 110, render: (v: number | null) => v ?? "-" },
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
      <CrudTable<MatrixItem>
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
        onExportExcel={canExport ? () => matrixApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => matrixApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await matrixApi.importExcel(file);
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
          <Form.Item name="frequencyLevel" label={t("riskScoring.columns.frequencyLevel")} rules={[{ required: true }]}>
            <InputNumber style={{ width: "100%" }} min={1} max={5} />
          </Form.Item>
          <Form.Item name="frequencyLabel" label={t("riskScoring.columns.frequencyLabel")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="scoreLowSeverity" label={t("riskScoring.columns.scoreLow")}>
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="scoreMediumSeverity" label={t("riskScoring.columns.scoreMedium")}>
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="scoreHighSeverity" label={t("riskScoring.columns.scoreHigh")}>
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
