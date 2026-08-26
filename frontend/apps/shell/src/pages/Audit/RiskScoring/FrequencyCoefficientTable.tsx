import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, InputNumber, Modal, Row, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  frequencyCoefficientApi,
  type FrequencyCoefficientItem,
  type FrequencyCoefficientRequest,
} from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  code: string;
  fromYear?: number;
  toYear?: number;
  label: string;
  value?: number;
  bonusPoint?: number;
  repeat: boolean;
  repeatCount?: string;
  repeatRiskPoint?: number;
  active: boolean;
}

/** Danh muc "He so tan suat xuat hien sai pham" (sheet ZTC_HSSP_DT). */
export function FrequencyCoefficientTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<FrequencyCoefficientItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<FrequencyCoefficientItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<FrequencyCoefficientItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<FrequencyCoefficientItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await frequencyCoefficientApi.list());
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
    form.setFieldsValue({ active: true, repeat: false });
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      code: target.code,
      fromYear: target.fromYear ?? undefined,
      toYear: target.toYear ?? undefined,
      label: target.label,
      value: target.value ?? undefined,
      bonusPoint: target.bonusPoint ?? undefined,
      repeat: target.repeat,
      repeatCount: target.repeatCount ?? undefined,
      repeatRiskPoint: target.repeatRiskPoint ?? undefined,
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
      fromYear: target.fromYear ?? undefined,
      toYear: target.toYear ?? undefined,
      label: target.label,
      value: target.value ?? undefined,
      bonusPoint: target.bonusPoint ?? undefined,
      repeat: target.repeat,
      repeatCount: target.repeatCount ?? undefined,
      repeatRiskPoint: target.repeatRiskPoint ?? undefined,
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
      const request: FrequencyCoefficientRequest = {
        code: values.code,
        fromYear: values.fromYear ?? null,
        toYear: values.toYear ?? null,
        label: values.label,
        value: values.value ?? null,
        bonusPoint: values.bonusPoint ?? null,
        repeat: values.repeat,
        repeatCount: values.repeatCount ?? null,
        repeatRiskPoint: values.repeatRiskPoint ?? null,
        active: values.active,
      };
      if (editing) {
        await frequencyCoefficientApi.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await frequencyCoefficientApi.create(request);
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
          await frequencyCoefficientApi.remove(target.id);
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<FrequencyCoefficientItem>["columns"] = [
    { title: t("riskScoring.columns.code"), width: 110, ...getSearchColumnProps("code", searchLabels) },
    { title: t("riskScoring.columns.label"), ...getSearchColumnProps("label", searchLabels) },
    { title: t("riskScoring.columns.fromYear"), dataIndex: "fromYear", width: 90, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.toYear"), dataIndex: "toYear", width: 90, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.value"), dataIndex: "value", width: 90, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.bonusPoint"), dataIndex: "bonusPoint", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.repeat"), dataIndex: "repeat", width: 90, render: (v: boolean) => (v ? t("common.yes") : t("common.no")) },
    { title: t("riskScoring.columns.repeatCount"), dataIndex: "repeatCount", width: 110, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoring.columns.repeatRiskPoint"), dataIndex: "repeatRiskPoint", width: 120, render: (v: number | null) => v ?? "-" },
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
      <CrudTable<FrequencyCoefficientItem>
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
        onExportExcel={canExport ? () => frequencyCoefficientApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => frequencyCoefficientApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await frequencyCoefficientApi.importExcel(file);
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
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="code" label={t("riskScoring.columns.code")} rules={[{ required: true }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="label" label={t("riskScoring.columns.label")} rules={[{ required: true }]}>
                <Input />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="fromYear" label={t("riskScoring.columns.fromYear")}>
                <InputNumber style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="toYear" label={t("riskScoring.columns.toYear")}>
                <InputNumber style={{ width: "100%" }} placeholder="9999" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="value" label={t("riskScoring.columns.value")}>
                <InputNumber style={{ width: "100%" }} step={0.01} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="bonusPoint" label={t("riskScoring.columns.bonusPoint")}>
                <InputNumber style={{ width: "100%" }} step={0.01} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="repeat" label={t("riskScoring.columns.repeat")} valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="repeatCount" label={t("riskScoring.columns.repeatCount")}>
                <Input placeholder=">=5" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="repeatRiskPoint" label={t("riskScoring.columns.repeatRiskPoint")}>
                <InputNumber style={{ width: "100%" }} step={0.01} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="active" label={t("common.active")} valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}
