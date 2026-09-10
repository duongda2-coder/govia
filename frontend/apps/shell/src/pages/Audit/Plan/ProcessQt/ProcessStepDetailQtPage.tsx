import { useCallback, useEffect, useState } from "react";
import { App, Col, Form, Input, InputNumber, Modal, Result, Row, Select, Space, Switch, Typography } from "antd";
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
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

interface FormValues {
  businessSegmentId?: string;
  processStepSummaryId?: string;
  code: string;
  applicableYear?: number;
  active: boolean;
}

/** Danh muc "Buoc quy trinh chi tiet" (sheet ZTC_BQT_MAP_QT) - trong nhom "Danh muc Kiem toan
 * quy trinh" cua "Lap ke hoach". Mo phong AuditProcessStepDetail (ProcessStepDetailPage). Da bo
 * cot lien ket Chot kiem soat quy trinh (giong migration 095 cua ban goc): "Ma BQT chi tiet"
 * chinh la ma CKS nen cot rieng la thua. */
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
  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [selectedYear, setSelectedYear] = useState<number | undefined>(undefined);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditProcessStepDetailQtItem[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditProcessStepDetailQtItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, segmentList, summaryList, yearList] = await Promise.all([
        listAuditProcessStepDetailsQt(),
        listMasterDataItems("BUSINESS_SEGMENT"),
        listAuditProcessStepSummariesQt(),
        listMasterDataItems("YEAR"),
      ]);
      setItems(list);
      setBusinessSegments(segmentList);
      setSummaries(summaryList);
      setYears(yearList);
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
    form.setFieldsValue({ active: true, applicableYear: selectedYear });
    setModalOpen(true);
  };

  const openEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      businessSegmentId: target.businessSegmentId ?? undefined,
      processStepSummaryId: target.processStepSummaryId ?? undefined,
      code: target.code,
      applicableYear: target.applicableYear ?? undefined,
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
        code: values.code,
        applicableYear: values.applicableYear ?? null,
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

  const displayedItems = selectedYear == null ? items : items.filter((i) => i.applicableYear === selectedYear);

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
    { title: t("auditProcessStepDetailQt.columns.code"), width: 160, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditProcessStepDetailQt.columns.applicableYear"), dataIndex: "applicableYear", width: 100, render: (v: number | null) => v ?? "-" },
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
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {t("auditProcessStepDetailQt.title")}
        </Typography.Title>
        <Space>
          <Select
            allowClear
            placeholder={t("common.selectYear")}
            style={{ width: 120 }}
            value={selectedYear}
            onChange={(v) => setSelectedYear(v)}
            options={years.map((y) => ({ value: Number(y.code), label: y.code }))}
          />
        </Space>
      </div>
      <CrudTable<AuditProcessStepDetailQtItem>
        tableId="audit.plan.processStepDetailQt"
        columns={columns}
        dataSource={displayedItems}
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
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="applicableYear" label={t("auditProcessStepDetailQt.columns.applicableYear")}>
                <InputNumber style={{ width: "100%" }} min={2000} max={2100} />
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
