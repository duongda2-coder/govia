import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, Modal, Result, Row, Select, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditControlPointQt,
  deleteAuditControlPointQt,
  exportAuditControlPointsQt,
  importAuditControlPointsQt,
  listAuditControlPointsQt,
  updateAuditControlPointQt,
  type AuditControlPointQtItem,
  type AuditControlPointQtRequest,
} from "../../../../api/auditControlPointQt";
import type { AuditControlType, AuditLevel } from "../../../../api/auditControlPoint";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

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

/** Danh muc "Chot kiem soat quy trinh" (sheet ZTC_CKS_QT) - trong nhom "Danh muc Kiem toan quy
 * trinh" cua "Lap ke hoach". Mo phong AuditControlPoint (ControlPointPage), tach bang rieng cho
 * quy trinh. */
export function ControlPointQtPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.CONTROL_POINT_QT.VIEW");
  const canCreate = hasPermission("AUDIT.CONTROL_POINT_QT.CREATE");
  const canEdit = hasPermission("AUDIT.CONTROL_POINT_QT.EDIT");
  const canDelete = hasPermission("AUDIT.CONTROL_POINT_QT.DELETE");
  const canExport = hasPermission("AUDIT.CONTROL_POINT_QT.EXPORT");
  const canImport = hasPermission("AUDIT.CONTROL_POINT_QT.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditControlPointQtItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditControlPointQtItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditControlPointQtItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditControlPointQtItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList] = await Promise.all([listAuditControlPointsQt(), listMasterDataItems("BUSINESS_SEGMENT")]);
      setItems(list);
      setBusinessSegments(segmentList);
    } catch {
      message.error(t("auditControlPointQt.messages.loadError"));
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
      const request: AuditControlPointQtRequest = {
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
        await updateAuditControlPointQt(editing.id, request);
        message.success(t("auditControlPointQt.messages.updateSuccess"));
      } else {
        await createAuditControlPointQt(request);
        message.success(t("auditControlPointQt.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditControlPointQt.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title:
        selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditControlPointQt.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditControlPointQt(item.id)));
          message.success(t("auditControlPointQt.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditControlPointQt.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditControlPointQtItem>["columns"] = [
    { title: t("auditControlPointQt.columns.code"), width: 130, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditControlPointQt.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    {
      title: t("auditControlPointQt.columns.businessSegment"),
      width: 160,
      ...getSearchColumnProps("businessSegmentName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditControlPointQt.columns.possibleRisk"), dataIndex: "possibleRisk", render: (v: string | null) => v ?? "-" },
    {
      title: t("auditControlPointQt.columns.controlType"),
      dataIndex: "controlType",
      width: 130,
      render: (v: AuditControlType | null) => (v ? t(`auditControlPoint.controlType.${v}`) : "-"),
    },
    {
      title: t("auditControlPointQt.columns.controlFrequency"),
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
      <Typography.Title level={4}>{t("auditControlPointQt.title")}</Typography.Title>
      <CrudTable<AuditControlPointQtItem>
        tableId="audit.plan.controlPointQt"
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
        onExportExcel={canExport ? () => exportAuditControlPointsQt("excel") : undefined}
        onExportWord={canExport ? () => exportAuditControlPointsQt("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditControlPointsQt(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditControlPointQt.form.editTitle") : t("auditControlPointQt.form.createTitle")}
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
              <Form.Item name="code" label={t("auditControlPointQt.columns.code")} rules={[{ required: true }]}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t("auditControlPointQt.columns.businessSegment")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="name" label={t("auditControlPointQt.columns.name")} rules={[{ required: true }]}>
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item name="possibleRisk" label={t("auditControlPointQt.columns.possibleRisk")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="controlPointByStep" label={t("auditControlPointQt.columns.controlPointByStep")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="actualControl" label={t("auditControlPointQt.columns.actualControl")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="controlType" label={t("auditControlPointQt.columns.controlType")}>
                <Select allowClear options={CONTROL_TYPES.map((v) => ({ value: v, label: t(`auditControlPoint.controlType.${v}`) }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="controlFrequency" label={t("auditControlPointQt.columns.controlFrequency")}>
                <Select allowClear options={LEVELS.map((v) => ({ value: v, label: t(`auditControlPoint.level.${v}`) }))} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="auditProcedure" label={t("auditControlPointQt.columns.auditProcedure")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="residualRiskAssessment" label={t("auditControlPointQt.columns.residualRiskAssessment")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="processRegulation" label={t("auditControlPointQt.columns.processRegulation")}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="referenceClause" label={t("auditControlPointQt.columns.referenceClause")}>
                <Input />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="processEffectiveness" label={t("auditControlPointQt.columns.processEffectiveness")}>
            <Input />
          </Form.Item>
          <Form.Item name="controlEffectivenessAssessment" label={t("auditControlPointQt.columns.controlEffectivenessAssessment")}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="controlEfficiencyAssessment" label={t("auditControlPointQt.columns.controlEfficiencyAssessment")}>
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
