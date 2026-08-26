import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, InputNumber, Modal, Row, Switch } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  auditObjectProjectApi,
  type AuditObjectProjectItem,
  type AuditObjectProjectRequest,
} from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  code: string;
  name: string;
  projectType?: string;
  approvalAuthority?: string;
  purpose?: string;
  investmentValue?: number;
  provider?: string;
  relatedParties?: string;
  inspectionYear?: number;
  inspectionResult?: string;
  inspectionRecommendation?: string;
  auditYear?: number;
  auditResult?: string;
  auditRecommendation?: string;
  active: boolean;
}

/** Danh muc "Doi tuong kiem toan - Du an/Dich vu thue ngoai" (sheet ZTC_DTKT3). */
export function AuditObjectProjectTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditObjectProjectItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditObjectProjectItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditObjectProjectItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditObjectProjectItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await auditObjectProjectApi.list());
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
      code: target.code,
      name: target.name,
      projectType: target.projectType ?? undefined,
      approvalAuthority: target.approvalAuthority ?? undefined,
      purpose: target.purpose ?? undefined,
      investmentValue: target.investmentValue ?? undefined,
      provider: target.provider ?? undefined,
      relatedParties: target.relatedParties ?? undefined,
      inspectionYear: target.inspectionYear ?? undefined,
      inspectionResult: target.inspectionResult ?? undefined,
      inspectionRecommendation: target.inspectionRecommendation ?? undefined,
      auditYear: target.auditYear ?? undefined,
      auditResult: target.auditResult ?? undefined,
      auditRecommendation: target.auditRecommendation ?? undefined,
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
      projectType: target.projectType ?? undefined,
      approvalAuthority: target.approvalAuthority ?? undefined,
      purpose: target.purpose ?? undefined,
      investmentValue: target.investmentValue ?? undefined,
      provider: target.provider ?? undefined,
      relatedParties: target.relatedParties ?? undefined,
      inspectionYear: target.inspectionYear ?? undefined,
      inspectionResult: target.inspectionResult ?? undefined,
      inspectionRecommendation: target.inspectionRecommendation ?? undefined,
      auditYear: target.auditYear ?? undefined,
      auditResult: target.auditResult ?? undefined,
      auditRecommendation: target.auditRecommendation ?? undefined,
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
      const request: AuditObjectProjectRequest = {
        code: values.code,
        name: values.name,
        projectType: values.projectType ?? null,
        approvalAuthority: values.approvalAuthority ?? null,
        purpose: values.purpose ?? null,
        investmentValue: values.investmentValue ?? null,
        provider: values.provider ?? null,
        relatedParties: values.relatedParties ?? null,
        inspectionYear: values.inspectionYear ?? null,
        inspectionResult: values.inspectionResult ?? null,
        inspectionRecommendation: values.inspectionRecommendation ?? null,
        auditYear: values.auditYear ?? null,
        auditResult: values.auditResult ?? null,
        auditRecommendation: values.auditRecommendation ?? null,
        active: values.active,
      };
      if (editing) {
        await auditObjectProjectApi.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await auditObjectProjectApi.create(request);
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
          await auditObjectProjectApi.remove(target.id);
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditObjectProjectItem>["columns"] = [
    { title: t("riskScoring.columns.code"), width: 100, ...getSearchColumnProps("code", searchLabels) },
    { title: t("riskScoring.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("riskScoring.columns.projectType"), dataIndex: "projectType", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoring.columns.approvalAuthority"), dataIndex: "approvalAuthority", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoring.columns.investmentValue"), dataIndex: "investmentValue", width: 140, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.provider"), dataIndex: "provider", width: 160, render: (v: string | null) => v ?? "-" },
    { title: t("riskScoring.columns.inspectionYear"), dataIndex: "inspectionYear", width: 110, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.auditYear"), dataIndex: "auditYear", width: 100, render: (v: number | null) => v ?? "-" },
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
      <CrudTable<AuditObjectProjectItem>
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
        onExportExcel={canExport ? () => auditObjectProjectApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => auditObjectProjectApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await auditObjectProjectApi.importExcel(file);
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
              <Form.Item name="code" label={t("riskScoring.columns.code")} rules={[{ required: true }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="name" label={t("riskScoring.columns.name")} rules={[{ required: true }]}>
                <Input />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="projectType" label={t("riskScoring.columns.projectType")}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="approvalAuthority" label={t("riskScoring.columns.approvalAuthority")}>
                <Input />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="investmentValue" label={t("riskScoring.columns.investmentValue")}>
                <InputNumber style={{ width: "100%" }} min={0} step={0.01} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="provider" label={t("riskScoring.columns.provider")}>
                <Input />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="purpose" label={t("riskScoring.columns.purpose")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="relatedParties" label={t("riskScoring.columns.relatedParties")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="inspectionYear" label={t("riskScoring.columns.inspectionYear")}>
                <InputNumber style={{ width: "100%" }} min={1900} max={2100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="auditYear" label={t("riskScoring.columns.auditYear")}>
                <InputNumber style={{ width: "100%" }} min={1900} max={2100} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="inspectionResult" label={t("riskScoring.columns.inspectionResult")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="inspectionRecommendation" label={t("riskScoring.columns.inspectionRecommendation")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="auditResult" label={t("riskScoring.columns.auditResult")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="auditRecommendation" label={t("riskScoring.columns.auditRecommendation")}>
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
