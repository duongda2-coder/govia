import { useCallback, useEffect, useState } from "react";
import { App, Card, Col, DatePicker, Form, Input, Modal, Result, Row, Select, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createPhbcRecommendation,
  createPhbcReport,
  deletePhbcRecommendation,
  deletePhbcReport,
  exportPhbcRecommendations,
  exportPhbcReports,
  importPhbcRecommendations,
  importPhbcReports,
  listPhbcRecommendations,
  listPhbcReports,
  updatePhbcRecommendation,
  updatePhbcReport,
  type PhbcIssuingUnit,
  type PhbcRecommendationCode,
  type PhbcRecommendationItem,
  type PhbcRecommendationRequest,
  type PhbcRecommendationTarget,
  type PhbcReportItem,
  type PhbcReportRequest,
} from "../../../api/auditPhbc";
import { useAuth } from "../../../auth/AuthContext";
import { filterOption, fromApiDate, recommendationCodeOptions, renderDate, renderText, targetOptions, toApiDate, usePhbcLookups } from "./phbcShared";
import type dayjs from "dayjs";

interface ReportFormValues {
  reportNumber: string;
  reportDate?: dayjs.Dayjs;
  reportName: string;
  summaryContent?: string;
  issuingUnit?: PhbcIssuingUnit;
}

interface RecommendationFormValues {
  recommendationCode: PhbcRecommendationCode;
  content: string;
  businessSegmentId?: string;
  target?: PhbcRecommendationTarget;
  executingUnitId?: string;
  executingUnitName?: string;
  deadline?: dayjs.Dayjs;
}

const ISSUING_UNIT_VALUES: PhbcIssuingUnit[] = ["KTNB", "BKS"];

/** Màn hình "Phát hành báo cáo" (ztc_phbc): danh sách báo cáo đã phát hành + kiến nghị đính kèm từng báo cáo. */
export function PhbcReportPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.PHBC.VIEW");
  const canCreate = hasPermission("AUDIT.PHBC.CREATE");
  const canEdit = hasPermission("AUDIT.PHBC.EDIT");
  const canDelete = hasPermission("AUDIT.PHBC.DELETE");
  const canExport = hasPermission("AUDIT.PHBC.EXPORT");
  const canImport = hasPermission("AUDIT.PHBC.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<PhbcReportItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };
  const lookups = usePhbcLookups(() => message.error(t("auditPhbc.messages.lookupError")));

  const [reports, setReports] = useState<PhbcReportItem[]>([]);
  const [recommendations, setRecommendations] = useState<PhbcRecommendationItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [recommendationLoading, setRecommendationLoading] = useState(false);
  const [selected, setSelected] = useState<PhbcReportItem[]>([]);
  const [selectedRecommendations, setSelectedRecommendations] = useState<PhbcRecommendationItem[]>([]);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<PhbcReportItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<ReportFormValues>();

  const [recommendationModalOpen, setRecommendationModalOpen] = useState(false);
  const [editingRecommendation, setEditingRecommendation] = useState<PhbcRecommendationItem | null>(null);
  const [recommendationSubmitting, setRecommendationSubmitting] = useState(false);
  const [recommendationForm] = Form.useForm<RecommendationFormValues>();

  const current = selected.length === 1 ? selected[0] : null;

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setReports(await listPhbcReports());
    } catch {
      message.error(t("auditPhbc.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  const loadRecommendations = useCallback(
    async (reportIssuanceId: string | undefined) => {
      if (!reportIssuanceId) {
        setRecommendations([]);
        return;
      }
      setRecommendationLoading(true);
      try {
        setRecommendations(await listPhbcRecommendations(reportIssuanceId));
      } catch {
        message.error(t("auditPhbc.messages.loadError"));
      } finally {
        setRecommendationLoading(false);
      }
    },
    [message, t],
  );

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  useEffect(() => {
    setSelectedRecommendations([]);
    loadRecommendations(current?.id);
  }, [current?.id, loadRecommendations]);

  // ---------- 1. báo cáo phát hành ----------

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = () => {
    if (!current) return;
    setEditing(current);
    form.setFieldsValue({
      reportNumber: current.reportNumber,
      reportDate: fromApiDate(current.reportDate),
      reportName: current.reportName,
      summaryContent: current.summaryContent ?? undefined,
      issuingUnit: current.issuingUnit ?? undefined,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    let values: ReportFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      const request: PhbcReportRequest = {
        reportNumber: values.reportNumber,
        reportDate: toApiDate(values.reportDate),
        reportName: values.reportName,
        summaryContent: values.summaryContent ?? null,
        issuingUnit: values.issuingUnit ?? null,
      };
      if (editing) {
        await updatePhbcReport(editing.id, request);
        message.success(t("auditPhbc.messages.updateSuccess"));
      } else {
        await createPhbcReport(request);
        message.success(t("auditPhbc.messages.createSuccess"));
      }
      setModalOpen(false);
      setSelected([]);
      await load();
    } catch {
      message.error(t("auditPhbc.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: t("auditPhbc.report.deleteConfirmTitle", { count: selected.length }),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deletePhbcReport(item.id)));
          message.success(t("auditPhbc.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditPhbc.messages.deleteError"));
        }
      },
    });
  };

  // ---------- 2. kiến nghị trong báo cáo ----------

  const openRecommendationCreate = () => {
    setEditingRecommendation(null);
    recommendationForm.resetFields();
    setRecommendationModalOpen(true);
  };

  const openRecommendationEdit = () => {
    const target = selectedRecommendations[0];
    if (!target) return;
    setEditingRecommendation(target);
    recommendationForm.setFieldsValue({
      recommendationCode: target.recommendationCode,
      content: target.content,
      businessSegmentId: target.businessSegmentId ?? undefined,
      target: target.target ?? undefined,
      executingUnitId: target.executingUnitId ?? undefined,
      executingUnitName: target.executingUnitName ?? undefined,
      deadline: fromApiDate(target.deadline),
    });
    setRecommendationModalOpen(true);
  };

  const handleRecommendationSubmit = async () => {
    if (!current) return;
    let values: RecommendationFormValues;
    try {
      values = await recommendationForm.validateFields();
    } catch {
      return;
    }
    setRecommendationSubmitting(true);
    try {
      const request: PhbcRecommendationRequest = {
        reportIssuanceId: current.id,
        recommendationCode: values.recommendationCode,
        content: values.content,
        businessSegmentId: values.businessSegmentId ?? null,
        target: values.target ?? null,
        executingUnitId: values.executingUnitId ?? null,
        executingUnitName: values.executingUnitName ?? null,
        deadline: toApiDate(values.deadline),
      };
      if (editingRecommendation) {
        await updatePhbcRecommendation(editingRecommendation.id, request);
        message.success(t("auditPhbc.messages.updateSuccess"));
      } else {
        await createPhbcRecommendation(request);
        message.success(t("auditPhbc.messages.createSuccess"));
      }
      setRecommendationModalOpen(false);
      setSelectedRecommendations([]);
      await Promise.all([loadRecommendations(current.id), load()]);
    } catch {
      message.error(t("auditPhbc.messages.saveError"));
    } finally {
      setRecommendationSubmitting(false);
    }
  };

  const handleRecommendationDelete = () => {
    if (selectedRecommendations.length === 0) return;
    modal.confirm({
      title: t("common.deleteConfirmTitleCount", { count: selectedRecommendations.length }),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selectedRecommendations.map((item) => deletePhbcRecommendation(item.id)));
          message.success(t("auditPhbc.messages.deleteSuccess"));
          setSelectedRecommendations([]);
          await Promise.all([loadRecommendations(current?.id), load()]);
        } catch {
          message.error(t("auditPhbc.messages.deleteError"));
        }
      },
    });
  };

  // ---------- khi chọn Đơn vị thực hiện: gợi ý điền sẵn Tên đơn vị thực hiện (vẫn cho sửa tự do) ----------
  const handleExecutingUnitChange = (unitId?: string) => {
    if (!unitId) return;
    const unit = lookups.executingUnits.find((u) => u.id === unitId);
    if (unit && !recommendationForm.getFieldValue("executingUnitName")) {
      recommendationForm.setFieldsValue({ executingUnitName: unit.name });
    }
  };

  const c = "auditPhbc.report.columns";
  const columns: TableProps<PhbcReportItem>["columns"] = [
    { title: t(`${c}.reportNumber`), width: 150, ...getSearchColumnProps("reportNumber", searchLabels) },
    { title: t(`${c}.reportDate`), dataIndex: "reportDate", width: 130, sorter: (a, b) => (a.reportDate ?? "").localeCompare(b.reportDate ?? ""), render: renderDate },
    { title: t(`${c}.reportName`), width: 280, ...getSearchColumnProps("reportName", searchLabels) },
    { title: t(`${c}.summaryContent`), dataIndex: "summaryContent", width: 320, render: renderText },
    { title: t(`${c}.issuingUnit`), dataIndex: "issuingUnitLabel", width: 160, render: renderText },
    { title: t(`${c}.recommendationCount`), dataIndex: "recommendationCount", width: 130, align: "center" },
  ];

  const r = "auditPhbc.report.recommendationColumns";
  const recommendationColumns: TableProps<PhbcRecommendationItem>["columns"] = [
    { title: t(`${r}.recommendationCode`), dataIndex: "recommendationCode", width: 110, align: "center" },
    { title: t(`${r}.recommendationName`), dataIndex: "recommendationName", width: 300, render: renderText },
    { title: t(`${r}.content`), dataIndex: "content", width: 320, render: renderText },
    { title: t(`${r}.businessSegment`), dataIndex: "businessSegmentCode", width: 140, render: renderText },
    { title: t(`${r}.target`), dataIndex: "targetLabel", width: 120, render: renderText },
    { title: t(`${r}.executingUnit`), dataIndex: "executingUnitCode", width: 140, render: renderText },
    { title: t(`${r}.executingUnitName`), dataIndex: "executingUnitName", width: 200, render: renderText },
    { title: t(`${r}.deadline`), dataIndex: "deadline", width: 150, sorter: (a, b) => (a.deadline ?? "").localeCompare(b.deadline ?? ""), render: renderDate },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditPhbc.report.title")}</Typography.Title>
      <CrudTable<PhbcReportItem>
        tableId="audit.phbc.report"
        columns={columns}
        dataSource={reports}
        rowKey="id"
        loading={loading}
        onAdd={canCreate ? openCreate : undefined}
        onEdit={canEdit ? openEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length === 0}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport ? () => exportPhbcReports() : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importPhbcReports(file);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Typography.Title level={5} style={{ marginTop: 24 }}>
        {t("auditPhbc.report.recommendationTitle")}
        {current ? ` - ${current.reportNumber}` : ""}
      </Typography.Title>
      {!current && (
        <Card size="small" style={{ marginBottom: 12 }}>
          <Typography.Text type="secondary">{t("auditPhbc.report.selectReportHint")}</Typography.Text>
        </Card>
      )}
      <CrudTable<PhbcRecommendationItem>
        tableId="audit.phbc.recommendation"
        columns={recommendationColumns}
        dataSource={recommendations}
        rowKey="id"
        loading={recommendationLoading}
        onAdd={canCreate ? openRecommendationCreate : undefined}
        addDisabled={!current}
        onEdit={canEdit ? openRecommendationEdit : undefined}
        editDisabled={selectedRecommendations.length !== 1}
        onDelete={canDelete ? handleRecommendationDelete : undefined}
        deleteDisabled={selectedRecommendations.length === 0}
        onSelectionChange={(_keys, rows) => setSelectedRecommendations(rows)}
        onExportExcel={canExport ? () => exportPhbcRecommendations() : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importPhbcRecommendations(file);
                await loadRecommendations(current?.id);
                await load();
                return result;
              }
            : undefined
        }
      />

      <Modal
        title={editing ? `${t("auditPhbc.report.form.editTitle")} - ${editing.reportNumber}` : t("auditPhbc.report.form.createTitle")}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={720}
      >
        <Form<ReportFormValues> form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="reportNumber" label={t(`${c}.reportNumber`)} rules={[{ required: true }]}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="reportDate" label={t(`${c}.reportDate`)}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="reportName" label={t(`${c}.reportName`)} rules={[{ required: true }]}>
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item name="summaryContent" label={t(`${c}.summaryContent`)}>
            <Input.TextArea rows={3} maxLength={1000} showCount />
          </Form.Item>
          <Form.Item name="issuingUnit" label={t(`${c}.issuingUnit`)}>
            <Select allowClear options={ISSUING_UNIT_VALUES.map((v) => ({ value: v, label: t(`auditPhbc.common.issuingUnit.${v}`) }))} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingRecommendation ? t("auditPhbc.report.recommendationForm.editTitle") : t("auditPhbc.report.recommendationForm.createTitle")}
        open={recommendationModalOpen}
        onCancel={() => setRecommendationModalOpen(false)}
        onOk={handleRecommendationSubmit}
        confirmLoading={recommendationSubmitting}
        destroyOnClose
        width={720}
      >
        <Form<RecommendationFormValues> form={recommendationForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="recommendationCode" label={t(`${r}.recommendationCode`)} rules={[{ required: true }]}>
                <Select options={recommendationCodeOptions(t)} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="target" label={t(`${r}.target`)}>
                <Select allowClear options={targetOptions(t)} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="content" label={t(`${r}.content`)} rules={[{ required: true }]}>
            <Input.TextArea rows={3} maxLength={1000} showCount />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="businessSegmentId" label={t(`${r}.businessSegment`)}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  filterOption={filterOption}
                  options={lookups.businessSegments.map((o) => ({ value: o.id, label: `${o.code} - ${o.name}` }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="deadline" label={t(`${r}.deadline`)}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="executingUnitId" label={t(`${r}.executingUnit`)}>
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  filterOption={filterOption}
                  options={lookups.executingUnits.map((o) => ({ value: o.id, label: `${o.code} - ${o.name}` }))}
                  onChange={handleExecutingUnitChange}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="executingUnitName" label={t(`${r}.executingUnitName`)}>
                <Input maxLength={100} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}
