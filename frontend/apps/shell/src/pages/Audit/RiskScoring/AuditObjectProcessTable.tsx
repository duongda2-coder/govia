import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, Modal, Row, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  auditObjectProcessApi,
  type AuditObjectProcessItem,
  type AuditObjectProcessRequest,
} from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  segmentCode?: string;
  code: string;
  name: string;
  referenceDocument?: string;
  auditResult?: string;
  eventNote?: string;
  incidentNote?: string;
  reviewResult?: string;
  active: boolean;
}

/** Danh muc "Doi tuong kiem toan - Quy trinh" (sheet ZTC_DTKT4). */
export function AuditObjectProcessTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditObjectProcessItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditObjectProcessItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditObjectProcessItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditObjectProcessItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await auditObjectProcessApi.list());
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
      segmentCode: target.segmentCode ?? undefined,
      code: target.code,
      name: target.name,
      referenceDocument: target.referenceDocument ?? undefined,
      auditResult: target.auditResult ?? undefined,
      eventNote: target.eventNote ?? undefined,
      incidentNote: target.incidentNote ?? undefined,
      reviewResult: target.reviewResult ?? undefined,
      active: target.active,
    });
    setModalOpen(true);
  };

  const openCopy = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(null);
    form.setFieldsValue({
      segmentCode: target.segmentCode ?? undefined,
      code: "",
      name: target.name,
      referenceDocument: target.referenceDocument ?? undefined,
      auditResult: target.auditResult ?? undefined,
      eventNote: target.eventNote ?? undefined,
      incidentNote: target.incidentNote ?? undefined,
      reviewResult: target.reviewResult ?? undefined,
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
      const request: AuditObjectProcessRequest = {
        segmentCode: values.segmentCode ?? null,
        code: values.code,
        name: values.name,
        referenceDocument: values.referenceDocument ?? null,
        auditResult: values.auditResult ?? null,
        eventNote: values.eventNote ?? null,
        incidentNote: values.incidentNote ?? null,
        reviewResult: values.reviewResult ?? null,
        active: values.active,
      };
      if (editing) {
        await auditObjectProcessApi.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await auditObjectProcessApi.create(request);
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
          await auditObjectProcessApi.remove(target.id);
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditObjectProcessItem>["columns"] = [
    { title: t("riskScoring.columns.segmentCodeProcess"), dataIndex: "segmentCode", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoring.columns.code"), width: 110, ...getSearchColumnProps("code", searchLabels) },
    { title: t("riskScoring.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("riskScoring.columns.referenceDocument"), dataIndex: "referenceDocument", width: 180, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoring.columns.reviewResult"), dataIndex: "reviewResult", width: 160, render: (v: string | null) => v ?? "-" },
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
      <CrudTable<AuditObjectProcessItem>
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
        onExportExcel={canExport ? () => auditObjectProcessApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => auditObjectProcessApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await auditObjectProcessApi.importExcel(file);
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
        width={680}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="segmentCode" label={t("riskScoring.columns.segmentCodeProcess")}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="code" label={t("riskScoring.columns.code")} rules={[{ required: true }]}>
                <Input maxLength={20} placeholder="1.1.1" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="name" label={t("riskScoring.columns.name")} rules={[{ required: true }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="referenceDocument" label={t("riskScoring.columns.referenceDocument")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="auditResult" label={t("riskScoring.columns.auditResult")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="eventNote" label={t("riskScoring.columns.eventNote")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="incidentNote" label={t("riskScoring.columns.incidentNote")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="reviewResult" label={t("riskScoring.columns.reviewResult")}>
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
