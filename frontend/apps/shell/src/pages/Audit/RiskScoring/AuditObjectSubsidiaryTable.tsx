import { useCallback, useEffect, useState } from "react";
import { App, Col, DatePicker, Form, Input, InputNumber, Modal, Row, Switch } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  auditObjectSubsidiaryApi,
  type AuditObjectSubsidiaryItem,
  type AuditObjectSubsidiaryRequest,
} from "../../../api/riskScoring";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  code: string;
  name: string;
  companyType?: string;
  establishedDate?: dayjs.Dayjs;
  staffCount?: number;
  leaderCount?: number;
  inspectionYear?: number;
  inspectionResult?: string;
  inspectionRecommendation?: string;
  auditYear?: number;
  auditResult?: string;
  auditRecommendation?: string;
  revenue?: number;
  cost?: number;
  profit?: number;
  salaryFund?: number;
  active: boolean;
}

/** Danh muc "Doi tuong kiem toan - Cong ty con" (sheet ZTC_DTKT2). */
export function AuditObjectSubsidiaryTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canCreate = hasPermission("AUDIT.RISK_SCORING.CREATE");
  const canEdit = hasPermission("AUDIT.RISK_SCORING.EDIT");
  const canDelete = hasPermission("AUDIT.RISK_SCORING.DELETE");
  const canExport = hasPermission("AUDIT.RISK_SCORING.EXPORT");
  const canImport = hasPermission("AUDIT.RISK_SCORING.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditObjectSubsidiaryItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditObjectSubsidiaryItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditObjectSubsidiaryItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditObjectSubsidiaryItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await auditObjectSubsidiaryApi.list());
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
      companyType: target.companyType ?? undefined,
      establishedDate: target.establishedDate ? dayjs(target.establishedDate) : undefined,
      staffCount: target.staffCount ?? undefined,
      leaderCount: target.leaderCount ?? undefined,
      inspectionYear: target.inspectionYear ?? undefined,
      inspectionResult: target.inspectionResult ?? undefined,
      inspectionRecommendation: target.inspectionRecommendation ?? undefined,
      auditYear: target.auditYear ?? undefined,
      auditResult: target.auditResult ?? undefined,
      auditRecommendation: target.auditRecommendation ?? undefined,
      revenue: target.revenue ?? undefined,
      cost: target.cost ?? undefined,
      profit: target.profit ?? undefined,
      salaryFund: target.salaryFund ?? undefined,
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
      companyType: target.companyType ?? undefined,
      establishedDate: target.establishedDate ? dayjs(target.establishedDate) : undefined,
      staffCount: target.staffCount ?? undefined,
      leaderCount: target.leaderCount ?? undefined,
      inspectionYear: target.inspectionYear ?? undefined,
      inspectionResult: target.inspectionResult ?? undefined,
      inspectionRecommendation: target.inspectionRecommendation ?? undefined,
      auditYear: target.auditYear ?? undefined,
      auditResult: target.auditResult ?? undefined,
      auditRecommendation: target.auditRecommendation ?? undefined,
      revenue: target.revenue ?? undefined,
      cost: target.cost ?? undefined,
      profit: target.profit ?? undefined,
      salaryFund: target.salaryFund ?? undefined,
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
      const request: AuditObjectSubsidiaryRequest = {
        code: values.code,
        name: values.name,
        companyType: values.companyType ?? null,
        establishedDate: values.establishedDate ? values.establishedDate.format("YYYY-MM-DD") : null,
        staffCount: values.staffCount ?? null,
        leaderCount: values.leaderCount ?? null,
        inspectionYear: values.inspectionYear ?? null,
        inspectionResult: values.inspectionResult ?? null,
        inspectionRecommendation: values.inspectionRecommendation ?? null,
        auditYear: values.auditYear ?? null,
        auditResult: values.auditResult ?? null,
        auditRecommendation: values.auditRecommendation ?? null,
        revenue: values.revenue ?? null,
        cost: values.cost ?? null,
        profit: values.profit ?? null,
        salaryFund: values.salaryFund ?? null,
        active: values.active,
      };
      if (editing) {
        await auditObjectSubsidiaryApi.update(editing.id, request);
        message.success(t("riskScoring.messages.updateSuccess"));
      } else {
        await auditObjectSubsidiaryApi.create(request);
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
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("riskScoring.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => auditObjectSubsidiaryApi.remove(item.id)));
          message.success(t("riskScoring.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("riskScoring.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditObjectSubsidiaryItem>["columns"] = [
    { title: t("riskScoring.columns.code"), width: 100, ...getSearchColumnProps("code", searchLabels) },
    { title: t("riskScoring.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    {
      title: t("riskScoring.columns.companyType"),
      width: 140,
      ...getSearchColumnProps("companyType", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("riskScoring.columns.staffCount"), dataIndex: "staffCount", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.leaderCount"), dataIndex: "leaderCount", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.inspectionYear"), dataIndex: "inspectionYear", width: 110, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.auditYear"), dataIndex: "auditYear", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.revenue"), dataIndex: "revenue", width: 130, render: (v: number | null) => v ?? "-" },
    { title: t("riskScoring.columns.profit"), dataIndex: "profit", width: 130, render: (v: number | null) => v ?? "-" },
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
      <CrudTable<AuditObjectSubsidiaryItem>
        tableId="riskScoring.auditObjectSubsidiary"
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
        onExportExcel={canExport ? () => auditObjectSubsidiaryApi.exportFile("excel") : undefined}
        onExportWord={canExport ? () => auditObjectSubsidiaryApi.exportFile("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await auditObjectSubsidiaryApi.importExcel(file);
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
              <Form.Item name="companyType" label={t("riskScoring.columns.companyType")}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="establishedDate" label={t("riskScoring.columns.establishedDate")}>
                <DatePicker style={{ width: "100%" }} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="staffCount" label={t("riskScoring.columns.staffCount")}>
                <InputNumber style={{ width: "100%" }} min={0} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="leaderCount" label={t("riskScoring.columns.leaderCount")}>
                <InputNumber style={{ width: "100%" }} min={0} />
              </Form.Item>
            </Col>
          </Row>
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
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="revenue" label={t("riskScoring.columns.revenue")}>
                <InputNumber style={{ width: "100%" }} min={0} step={0.01} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="cost" label={t("riskScoring.columns.cost")}>
                <InputNumber style={{ width: "100%" }} min={0} step={0.01} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="profit" label={t("riskScoring.columns.profit")}>
                <InputNumber style={{ width: "100%" }} step={0.01} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="salaryFund" label={t("riskScoring.columns.salaryFund")}>
                <InputNumber style={{ width: "100%" }} min={0} step={0.01} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
