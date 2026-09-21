import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Button, Card, Col, DatePicker, Descriptions, Form, Input, InputNumber, Modal, Radio, Result, Row, Select, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { EyeOutlined, FileExcelOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import dayjs from "dayjs";
import { AttachmentPanel, CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  createTdkpReportArchive,
  deleteTdkpReportArchive,
  exportTdkpReport,
  listTdkpReportArchive,
  previewTdkpReport,
  updateTdkpReportArchive,
  type TdkpReportArchiveItem,
  type TdkpReportArchiveRequest,
  type TdkpReportData,
  type TdkpReportRequest,
  type TdkpReportType,
} from "../../../api/auditTdkp";
import { httpClient } from "../../../api/client";
import { useAuth } from "../../../auth/AuthContext";
import { filterOption, fromApiDate, renderDate, renderText, toApiDate, useTdkpLookups } from "./tdkpShared";

const REPORT_TYPES: TdkpReportType[] = ["BC01", "BC02", "BC03", "BC04", "BC05"];
const ARCHIVE_ENTITY = "AUDIT_TDKP_REPORT";

interface FilterValues {
  asOfDate: dayjs.Dayjs;
  year?: number;
  branchUnitId?: string;
  attachedReportNumber?: string;
  attachedReportDate?: dayjs.Dayjs;
  minutesDate?: dayjs.Dayjs;
  decisionNumber?: string;
  decisionDate?: dayjs.Dayjs;
}

interface ArchiveFormValues {
  reportType: TdkpReportType;
  title: string;
  asOfDate?: dayjs.Dayjs;
  note?: string;
}

/** Màn hình ZTC_TDKP_BC: chọn 1 trong 5 báo cáo (Template_BC_01..05), xem trước dạng bảng, xuất Excel theo mẫu, và lưu trữ báo cáo đã xuất. */
export function TdkpReportPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.TDKP_BC.VIEW");
  const canExport = hasPermission("AUDIT.TDKP_BC.EXPORT");
  const canUpload = hasPermission("AUDIT.TDKP_BC.UPLOAD");
  const canDelete = hasPermission("AUDIT.TDKP_BC.DELETE");
  const lookups = useTdkpLookups(() => message.error(t("auditTdkp.messages.lookupError")));
  const { getSearchColumnProps } = useClientSearchColumn<TdkpReportArchiveItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [type, setType] = useState<TdkpReportType>("BC01");
  const [filterForm] = Form.useForm<FilterValues>();
  const [preview, setPreview] = useState<TdkpReportData | null>(null);
  const [previewing, setPreviewing] = useState(false);
  const [exporting, setExporting] = useState(false);

  const [archives, setArchives] = useState<TdkpReportArchiveItem[]>([]);
  const [archiveLoading, setArchiveLoading] = useState(false);
  const [selected, setSelected] = useState<TdkpReportArchiveItem[]>([]);
  const [archiveModalOpen, setArchiveModalOpen] = useState(false);
  const [editing, setEditing] = useState<TdkpReportArchiveItem | null>(null);
  const [archiveSubmitting, setArchiveSubmitting] = useState(false);
  const [archiveForm] = Form.useForm<ArchiveFormValues>();
  const [attachmentTarget, setAttachmentTarget] = useState<TdkpReportArchiveItem | null>(null);

  const loadArchives = useCallback(async () => {
    setArchiveLoading(true);
    try {
      setArchives(await listTdkpReportArchive());
    } catch {
      message.error(t("auditTdkp.messages.loadError"));
    } finally {
      setArchiveLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) loadArchives();
  }, [canView, loadArchives]);

  useEffect(() => {
    filterForm.setFieldsValue({ asOfDate: dayjs() });
  }, [filterForm]);

  // đổi báo cáo -> bỏ kết quả xem trước cũ
  useEffect(() => {
    setPreview(null);
  }, [type]);

  const buildRequest = async (): Promise<TdkpReportRequest | null> => {
    let values: FilterValues;
    try {
      values = await filterForm.validateFields();
    } catch {
      return null;
    }
    return {
      type,
      asOfDate: toApiDate(values.asOfDate),
      year: values.year ?? null,
      branchUnitId: values.branchUnitId ?? null,
      attachedReportNumber: values.attachedReportNumber ?? null,
      attachedReportDate: toApiDate(values.attachedReportDate),
      minutesDate: toApiDate(values.minutesDate),
      decisionNumber: values.decisionNumber ?? null,
      decisionDate: toApiDate(values.decisionDate),
    };
  };

  const handlePreview = async () => {
    const request = await buildRequest();
    if (!request) return;
    setPreviewing(true);
    try {
      setPreview(await previewTdkpReport(request));
    } catch {
      message.error(t("auditTdkp.report.previewError"));
    } finally {
      setPreviewing(false);
    }
  };

  const handleExport = async () => {
    const request = await buildRequest();
    if (!request) return;
    setExporting(true);
    try {
      await exportTdkpReport(request);
    } catch {
      message.error(t("auditTdkp.report.exportError"));
    } finally {
      setExporting(false);
    }
  };

  const previewColumns: TableProps<string[]>["columns"] = useMemo(
    () => (preview?.headers ?? []).map((header, index) => ({ title: header, key: `c${index}`, dataIndex: index, width: index === 0 ? 70 : 160 })),
    [preview],
  );

  // ---------- báo cáo lưu trữ ----------

  const openArchiveCreate = () => {
    setEditing(null);
    archiveForm.resetFields();
    archiveForm.setFieldsValue({ reportType: type, asOfDate: dayjs() });
    setArchiveModalOpen(true);
  };

  const openArchiveEdit = () => {
    const target = selected[0];
    if (!target) return;
    setEditing(target);
    archiveForm.setFieldsValue({ reportType: target.reportType, title: target.title, asOfDate: fromApiDate(target.asOfDate), note: target.note ?? undefined });
    setArchiveModalOpen(true);
  };

  const handleArchiveSubmit = async () => {
    let values: ArchiveFormValues;
    try {
      values = await archiveForm.validateFields();
    } catch {
      return;
    }
    setArchiveSubmitting(true);
    try {
      const request: TdkpReportArchiveRequest = { reportType: values.reportType, title: values.title, asOfDate: toApiDate(values.asOfDate), note: values.note ?? null };
      if (editing) {
        await updateTdkpReportArchive(editing.id, request);
        message.success(t("auditTdkp.messages.updateSuccess"));
      } else {
        const created = await createTdkpReportArchive(request);
        message.success(t("auditTdkp.report.archive.createdUploadHint"));
        setAttachmentTarget(created);
      }
      setArchiveModalOpen(false);
      setSelected([]);
      await loadArchives();
    } catch {
      message.error(t("auditTdkp.messages.saveError"));
    } finally {
      setArchiveSubmitting(false);
    }
  };

  const handleArchiveDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: t("common.deleteConfirmTitleCount", { count: selected.length }),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteTdkpReportArchive(item.id)));
          message.success(t("auditTdkp.messages.deleteSuccess"));
          setSelected([]);
          await loadArchives();
        } catch {
          message.error(t("auditTdkp.messages.deleteError"));
        }
      },
    });
  };

  const archiveColumns: TableProps<TdkpReportArchiveItem>["columns"] = [
    { title: t("auditTdkp.report.archive.columns.reportType"), dataIndex: "reportType", width: 110, render: (v: TdkpReportType) => v },
    { title: t("auditTdkp.report.archive.columns.reportTypeTitle"), dataIndex: "reportTypeTitle", width: 320 },
    { title: t("auditTdkp.report.archive.columns.title"), width: 260, ...getSearchColumnProps("title", searchLabels) },
    { title: t("auditTdkp.report.archive.columns.asOfDate"), dataIndex: "asOfDate", width: 130, render: renderDate },
    { title: t("auditTdkp.report.archive.columns.attachmentCount"), dataIndex: "attachmentCount", width: 120, align: "center" },
    { title: t("auditTdkp.report.archive.columns.createdBy"), dataIndex: "createdBy", width: 140, render: renderText },
    { title: t("auditTdkp.report.archive.columns.note"), dataIndex: "note", width: 240, render: renderText },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  const showBranch = type === "BC02" || type === "BC03";
  const showYear = type === "BC02" || type === "BC03" || type === "BC05";
  const showAttached = type === "BC01" || type === "BC03" || type === "BC04";

  return (
    <div>
      <Typography.Title level={4}>{t("auditTdkp.report.title")}</Typography.Title>

      <Card title={t("auditTdkp.report.selectReport")} style={{ marginBottom: 16 }}>
        <Radio.Group value={type} onChange={(e) => setType(e.target.value as TdkpReportType)} style={{ display: "flex", flexDirection: "column", gap: 8 }}>
          {REPORT_TYPES.map((value) => (
            <Radio key={value} value={value}>
              <b>{value}:</b> {t(`auditTdkp.report.types.${value}`)}
            </Radio>
          ))}
        </Radio.Group>

        <Form<FilterValues> form={filterForm} layout="vertical" style={{ marginTop: 20 }}>
          <Row gutter={16}>
            <Col xs={24} md={6}>
              <Form.Item name="asOfDate" label={t("auditTdkp.report.filters.asOfDate")} rules={[{ required: true }]}>
                <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
              </Form.Item>
            </Col>
            {showYear && (
              <Col xs={24} md={6}>
                <Form.Item name="year" label={type === "BC05" ? t("auditTdkp.report.filters.resolutionYear") : t("auditTdkp.report.filters.auditYear")}>
                  <InputNumber style={{ width: "100%" }} min={2000} max={2100} />
                </Form.Item>
              </Col>
            )}
            {showBranch && (
              <Col xs={24} md={12}>
                <Form.Item name="branchUnitId" label={t("auditTdkp.report.filters.branch")} rules={type === "BC02" ? [{ required: true }] : []}>
                  <Select
                    allowClear={type === "BC03"}
                    showSearch
                    optionFilterProp="label"
                    filterOption={filterOption}
                    options={lookups.units.map((u) => ({ value: u.id, label: `${u.code} - ${u.name}` }))}
                  />
                </Form.Item>
              </Col>
            )}
            {showAttached && (
              <>
                <Col xs={24} md={6}>
                  <Form.Item name="attachedReportNumber" label={t("auditTdkp.report.filters.attachedReportNumber")}>
                    <Input maxLength={50} />
                  </Form.Item>
                </Col>
                {type !== "BC03" && (
                  <Col xs={24} md={6}>
                    <Form.Item name="attachedReportDate" label={t("auditTdkp.report.filters.attachedReportDate")}>
                      <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
                    </Form.Item>
                  </Col>
                )}
              </>
            )}
            {type === "BC02" && (
              <>
                <Col xs={24} md={6}>
                  <Form.Item name="minutesDate" label={t("auditTdkp.report.filters.minutesDate")}>
                    <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={6}>
                  <Form.Item name="decisionNumber" label={t("auditTdkp.report.filters.decisionNumber")}>
                    <Input maxLength={50} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={6}>
                  <Form.Item name="decisionDate" label={t("auditTdkp.report.filters.decisionDate")}>
                    <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
                  </Form.Item>
                </Col>
              </>
            )}
          </Row>
        </Form>
        <Space wrap>
          <Button icon={<EyeOutlined />} loading={previewing} onClick={handlePreview}>
            {t("auditTdkp.report.previewButton")}
          </Button>
          {canExport && (
            <Button type="primary" icon={<FileExcelOutlined />} loading={exporting} onClick={handleExport}>
              {t("common.exportExcel")}
            </Button>
          )}
        </Space>
      </Card>

      {preview && (
        <Card title={t("auditTdkp.report.previewTitle", { type })} style={{ marginBottom: 16 }}>
          {preview.subtitle && <Typography.Paragraph type="secondary">{preview.subtitle}</Typography.Paragraph>}
          <Table<string[]>
            size="small"
            bordered
            columns={previewColumns}
            dataSource={preview.rows}
            rowKey={(_row, index) => String(index)}
            pagination={{ pageSize: 20, showSizeChanger: false }}
            scroll={{ x: "max-content" }}
            summary={
              preview.totals
                ? () => (
                    <Table.Summary.Row>
                      {preview.totals!.map((value, index) => (
                        <Table.Summary.Cell key={index} index={index}>
                          <b>{value}</b>
                        </Table.Summary.Cell>
                      ))}
                    </Table.Summary.Row>
                  )
                : undefined
            }
          />
          {preview.stats && (
            <Descriptions size="small" bordered column={1} style={{ marginTop: 16, maxWidth: 560 }}>
              {preview.stats.map((stat) => (
                <Descriptions.Item key={stat[0]} label={stat[0]}>
                  {stat[1]}
                  {stat[2] ? ` (${stat[2]})` : ""}
                </Descriptions.Item>
              ))}
            </Descriptions>
          )}
        </Card>
      )}

      <Typography.Title level={5}>{t("auditTdkp.report.archive.title")}</Typography.Title>
      <CrudTable<TdkpReportArchiveItem>
        tableId="audit.tdkp.reportArchive"
        columns={archiveColumns}
        dataSource={archives}
        rowKey="id"
        loading={archiveLoading}
        onAdd={canUpload ? openArchiveCreate : undefined}
        onEdit={canUpload ? openArchiveEdit : undefined}
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleArchiveDelete : undefined}
        deleteDisabled={selected.length === 0}
        onAttachment={() => setAttachmentTarget(selected[0] ?? null)}
        attachmentDisabled={selected.length !== 1}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
      />

      <Modal
        title={editing ? t("auditTdkp.report.archive.form.editTitle") : t("auditTdkp.report.archive.form.createTitle")}
        open={archiveModalOpen}
        onCancel={() => setArchiveModalOpen(false)}
        onOk={handleArchiveSubmit}
        confirmLoading={archiveSubmitting}
        destroyOnClose
      >
        <Form<ArchiveFormValues> form={archiveForm} layout="vertical">
          <Form.Item name="reportType" label={t("auditTdkp.report.archive.columns.reportType")} rules={[{ required: true }]}>
            <Select options={REPORT_TYPES.map((value) => ({ value, label: `${value}: ${t(`auditTdkp.report.types.${value}`)}` }))} />
          </Form.Item>
          <Form.Item name="title" label={t("auditTdkp.report.archive.columns.title")} rules={[{ required: true }]}>
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item name="asOfDate" label={t("auditTdkp.report.archive.columns.asOfDate")}>
            <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
          </Form.Item>
          <Form.Item name="note" label={t("auditTdkp.report.archive.columns.note")}>
            <Input.TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={attachmentTarget ? `${t("auditTdkp.report.archive.attachmentTitle")} - ${attachmentTarget.title}` : ""}
        open={attachmentTarget != null}
        onCancel={() => {
          setAttachmentTarget(null);
          loadArchives();
        }}
        footer={null}
        destroyOnClose
        width={640}
      >
        {attachmentTarget && <AttachmentPanel http={httpClient} entityName={ARCHIVE_ENTITY} entityId={attachmentTarget.id} />}
      </Modal>
    </div>
  );
}
