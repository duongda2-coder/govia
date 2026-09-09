import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, Modal, Result, Row, Select, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createAuditProcessStepDetailQt,
  deleteAuditProcessStepDetailQt,
  exportAuditProcessStepDetailsQt,
  importAuditProcessStepDetailsQt,
  listAuditProcessStepDetailsQt,
  listAuditProcessStepSummariesQt,
  updateAuditProcessStepDetailQt,
  type AuditProcessStepDetailQtItem,
  type AuditProcessStepDetailQtRequest,
  type AuditProcessStepSummaryQtItem,
} from "../../../../api/auditProcessStepQt";
import { listAuditControlPointsQt, type AuditControlPointQtItem } from "../../../../api/auditControlPointQt";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  businessSegmentId?: string;
  processStepSummaryId?: string;
  controlPointId?: string;
  code: string;
  active: boolean;
}

/** Danh muc "Buoc quy trinh chi tiet" (sheet ZTC_BQT_MAP_QT) - trong nhom "Danh muc Kiem toan
 * quy trinh" cua "Lap ke hoach". Mo phong AuditProcessStepDetail (ProcessStepDetailPage), nhung
 * (khac ban goc) giu lai lien ket toi Chot kiem soat quy trinh (ControlPointQtPage/ZTC_CKS_QT)
 * vi sheet nguon yeu cau ro "Ma BQT chi tiet - Link tu ztc_cks". */
export function ProcessStepDetailQtPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.PROCESS_STEP_DETAIL_QT.VIEW");
  const canCreate = hasPermission("AUDIT.PROCESS_STEP_DETAIL_QT.CREATE");
  const canEdit = hasPermission("AUDIT.PROCESS_STEP_DETAIL_QT.EDIT");
  const canDelete = hasPermission("AUDIT.PROCESS_STEP_DETAIL_QT.DELETE");
  const canExport = hasPermission("AUDIT.PROCESS_STEP_DETAIL_QT.EXPORT");
  const canImport = hasPermission("AUDIT.PROCESS_STEP_DETAIL_QT.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditProcessStepDetailQtItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditProcessStepDetailQtItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [summaries, setSummaries] = useState<AuditProcessStepSummaryQtItem[]>([]);
  const [controlPoints, setControlPoints] = useState<AuditControlPointQtItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditProcessStepDetailQtItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditProcessStepDetailQtItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList, summaryList, controlPointList] = await Promise.all([
        listAuditProcessStepDetailsQt(),
        listMasterDataItems("BUSINESS_SEGMENT"),
        listAuditProcessStepSummariesQt(),
        listAuditControlPointsQt(),
      ]);
      setItems(list);
      setBusinessSegments(segmentList);
      setSummaries(summaryList);
      setControlPoints(controlPointList);
    } catch {
      message.error(t("auditProcessStepDetailQt.messages.loadError"));
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
      processStepSummaryId: target.processStepSummaryId ?? undefined,
      controlPointId: target.controlPointId ?? undefined,
      code: target.code,
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
      const request: AuditProcessStepDetailQtRequest = {
        businessSegmentId: values.businessSegmentId ?? null,
        processStepSummaryId: values.processStepSummaryId ?? null,
        controlPointId: values.controlPointId ?? null,
        code: values.code,
        active: values.active,
      };
      if (editing) {
        await updateAuditProcessStepDetailQt(editing.id, request);
        message.success(t("auditProcessStepDetailQt.messages.updateSuccess"));
      } else {
        await createAuditProcessStepDetailQt(request);
        message.success(t("auditProcessStepDetailQt.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditProcessStepDetailQt.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title:
        selected.length > 1
          ? t("common.deleteConfirmTitleCount", { count: selected.length })
          : t("auditProcessStepDetailQt.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditProcessStepDetailQt(item.id)));
          message.success(t("auditProcessStepDetailQt.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditProcessStepDetailQt.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditProcessStepDetailQtItem>["columns"] = [
    {
      title: t("auditProcessStepDetailQt.columns.businessSegment"),
      width: 160,
      ...getSearchColumnProps("businessSegmentName", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("auditProcessStepDetailQt.columns.processStepSummary"),
      width: 220,
      render: (_, record) => (record.processStepSummaryCode ? `${record.processStepSummaryCode} - ${record.processStepSummaryName}` : "-"),
    },
    {
      title: t("auditProcessStepDetailQt.columns.controlPoint"),
      width: 220,
      render: (_, record) => (record.controlPointCode ? `${record.controlPointCode} - ${record.controlPointName}` : "-"),
    },
    { title: t("auditProcessStepDetailQt.columns.code"), width: 160, ...getSearchColumnProps("code", searchLabels) },
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
      <Typography.Title level={4}>{t("auditProcessStepDetailQt.title")}</Typography.Title>
      <CrudTable<AuditProcessStepDetailQtItem>
        tableId="audit.plan.processStepDetailQt"
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
        onExportExcel={canExport ? () => exportAuditProcessStepDetailsQt("excel") : undefined}
        onExportWord={canExport ? () => exportAuditProcessStepDetailsQt("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importAuditProcessStepDetailsQt(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? t("auditProcessStepDetailQt.form.editTitle") : t("auditProcessStepDetailQt.form.createTitle")}
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
              <Form.Item name="code" label={t("auditProcessStepDetailQt.columns.code")} rules={[{ required: true }]}>
                <Input maxLength={50} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t("auditProcessStepDetailQt.columns.businessSegment")}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="processStepSummaryId" label={t("auditProcessStepDetailQt.columns.processStepSummary")}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              options={summaries.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
            />
          </Form.Item>
          <Form.Item name="controlPointId" label={t("auditProcessStepDetailQt.columns.controlPoint")}>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              options={controlPoints.map((c) => ({ value: c.id, label: `${c.code} - ${c.name}` }))}
            />
          </Form.Item>
          <Form.Item name="active" label={t("common.active")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
