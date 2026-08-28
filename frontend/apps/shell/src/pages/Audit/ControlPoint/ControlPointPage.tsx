import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, Modal, Result, Row, Select, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditControlPoint,
  deleteAuditControlPoint,
  exportAuditControlPoints,
  importAuditControlPoints,
  listAuditControlPoints,
  updateAuditControlPoint,
  type AuditControlPointItem,
  type AuditControlPointRequest,
  type AuditControlType,
  type AuditLevel,
} from "../../../api/auditControlPoint";
import { listMasterDataItems, type MasterDataItem } from "../../../api/auditMasterData";
import { useAuth } from "../../../auth/AuthContext";

interface FormValues {
  businessSegmentId?: string;
  code: string;
  name: string;
  possibleRisk?: string;
  controlPointByStep?: string;
  actualControl?: string;
  controlType?: AuditControlType;
  controlFrequency?: AuditLevel;
  auditProcedure?: string;
  residualRiskAssessment?: string;
  processRegulation?: string;
  referenceClause?: string;
  processEffectiveness?: string;
  controlEffectivenessAssessment?: string;
  controlEfficiencyAssessment?: string;
  active: boolean;
}

const CONTROL_TYPES: AuditControlType[] = ["MANUAL", "AUTOMATIC"];
const LEVELS: AuditLevel[] = ["HIGH", "MEDIUM", "LOW"];

/** Danh muc "Chot kiem soat" (sheet ZTC_CKS) - trong nhom "Danh muc" cua module Kiem toan noi bo. */
export function ControlPointPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CONTROL_POINT.VIEW");
  const canCreate = hasPermission("AUDIT.CONTROL_POINT.CREATE");
  const canEdit = hasPermission("AUDIT.CONTROL_POINT.EDIT");
  const canDelete = hasPermission("AUDIT.CONTROL_POINT.DELETE");
  const canExport = hasPermission("AUDIT.CONTROL_POINT.EXPORT");
  const canImport = hasPermission("AUDIT.CONTROL_POINT.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditControlPointItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditControlPointItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditControlPointItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditControlPointItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList] = await Promise.all([listAuditControlPoints(), listMasterDataItems("BUSINESS_SEGMENT")]);
      setItems(list);
      setBusinessSegments(segmentList);
    } catch {
      message.error(t("auditControlPoint.messages.loadError"));
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
      businessSegmentId: target.businessSegmentId ?? undefined,
      code: target.code,
      name: target.name,
      possibleRisk: target.possibleRisk ?? undefined,
      controlPointByStep: target.controlPointByStep ?? undefined,
      actualControl: target.actualControl ?? undefined,
      controlType: target.controlType ?? undefined,
      controlFrequency: target.controlFrequency ?? undefined,
      auditProcedure: target.auditProcedure ?? undefined,
      residualRiskAssessment: target.residualRiskAssessment ?? undefined,
      processRegulation: target.processRegulation ?? undefined,
      referenceClause: target.referenceClause ?? undefined,
      processEffectiveness: target.processEffectiveness ?? undefined,
      controlEffectivenessAssessment: target.controlEffectivenessAssessment ?? undefined,
      controlEfficiencyAssessment: target.controlEfficiencyAssessment ?? undefined,
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
      const request: AuditControlPointRequest = {
        businessSegmentId: values.businessSegmentId ?? null,
        code: values.code,
        name: values.name,
        possibleRisk: values.possibleRisk ?? null,
        controlPointByStep: values.controlPointByStep ?? null,
        actualControl: values.actualControl ?? null,
        controlType: values.controlType ?? null,
        controlFrequency: values.controlFrequency ?? null,
        auditProcedure: values.auditProcedure ?? null,
        residualRiskAssessment: values.residualRiskAssessment ?? null,
        processRegulation: values.processRegulation ?? null,
        referenceClause: values.referenceClause ?? null,
        processEffectiveness: values.processEffectiveness ?? null,
        controlEffectivenessAssessment: values.controlEffectivenessAssessment ?? null,
        controlEfficiencyAssessment: values.controlEfficiencyAssessment ?? null,
        active: values.active,
      };
      if (editing) {
        await updateAuditControlPoint(editing.id, request);
        message.success(t("auditControlPoint.messages.updateSuccess"));
      } else {
        await createAuditControlPoint(request);
        message.success(t("auditControlPoint.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditControlPoint.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title:
        selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditControlPoint.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditControlPoint(item.id)));
          message.success(t("auditControlPoint.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditControlPoint.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditControlPointItem>["columns"] = [
    { title: t("auditControlPoint.columns.code"), width: 130, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditControlPoint.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    {
      title: t("auditControlPoint.columns.businessSegment"),
      dataIndex: "businessSegmentName",
      width: 160,
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditControlPoint.columns.possibleRisk"), dataIndex: "possibleRisk", render: (v: string | null) => v ?? "-" },
    {
      title: t("auditControlPoint.columns.controlType"),
      dataIndex: "controlType",
      width: 130,
      render: (v: AuditControlType | null) => (v ? t(`auditControlPoint.controlType.${v}`) : "-"),
    },
    {
      title: t("auditControlPoint.columns.controlFrequency"),
      dataIndex: "controlFrequency",
      width: 150,
      render: (v: AuditLevel | null) => (v ? t(`auditControlPoint.level.${v}`) : "-"),
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
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditControlPoint.title")}</Typography.Title>
      <CrudTable<AuditControlPointItem>
        tableId="audit.controlPoint"
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length === 0}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport ? () => exportAuditControlPoints("excel") : undefined}
        onExportWord={canExport ? () => exportAuditControlPoints("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditControlPoints(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditControlPoint.form.editTitle") : t("auditControlPoint.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={720}
      >
        <Form<FormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="code" label={t("auditControlPoint.columns.code")} rules={[{ required: true }]}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t("auditControlPoint.columns.businessSegment")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="name" label={t("auditControlPoint.columns.name")} rules={[{ required: true }]}>
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item name="possibleRisk" label={t("auditControlPoint.columns.possibleRisk")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="controlPointByStep" label={t("auditControlPoint.columns.controlPointByStep")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="actualControl" label={t("auditControlPoint.columns.actualControl")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="controlType" label={t("auditControlPoint.columns.controlType")}>
                <Select allowClear options={CONTROL_TYPES.map((v) => ({ value: v, label: t(`auditControlPoint.controlType.${v}`) }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="controlFrequency" label={t("auditControlPoint.columns.controlFrequency")}>
                <Select allowClear options={LEVELS.map((v) => ({ value: v, label: t(`auditControlPoint.level.${v}`) }))} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="auditProcedure" label={t("auditControlPoint.columns.auditProcedure")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="residualRiskAssessment" label={t("auditControlPoint.columns.residualRiskAssessment")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="processRegulation" label={t("auditControlPoint.columns.processRegulation")}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="referenceClause" label={t("auditControlPoint.columns.referenceClause")}>
                <Input />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="processEffectiveness" label={t("auditControlPoint.columns.processEffectiveness")}>
            <Input />
          </Form.Item>
          <Form.Item name="controlEffectivenessAssessment" label={t("auditControlPoint.columns.controlEffectivenessAssessment")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="controlEfficiencyAssessment" label={t("auditControlPoint.columns.controlEfficiencyAssessment")}>
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
