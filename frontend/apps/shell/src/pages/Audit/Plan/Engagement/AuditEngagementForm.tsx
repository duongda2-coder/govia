import { useCallback, useEffect, useState } from "react";
import { App, Button, Col, DatePicker, Divider, Form, Input, InputNumber, Row, Select, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { DeleteOutlined, PlusOutlined, SaveOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { AttachmentPanel } from "@govia/ui-kit";
import { httpClient } from "../../../../api/client";
import {
  addRelatedUnit,
  createAuditEngagement,
  deleteRelatedUnit,
  listRelatedUnits,
  updateAuditEngagement,
  type AuditEngagementItem,
  type AuditEngagementRelatedUnitItem,
  type AuditEngagementRequest,
  type AuditEngagementStatus,
  type AuditObjectUnitOption,
  type EmployeeOption,
} from "../../../../api/auditEngagement";

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);
const STATUSES: AuditEngagementStatus[] = ["DRAFT", "PLANNED", "IN_PROGRESS", "COMPLETED", "CANCELLED"];
const RISK_RANKS = ["Cao", "Trung bình", "Thấp"];
const ENTITY_NAME = "AUDIT_ENGAGEMENT";

interface FormValues {
  unitType?: string;
  auditObjectUnitId?: string;
  year: number;
  expectedMonth: number;
  decisionDate: dayjs.Dayjs;
  teamLeadEmployeeId: string;
  decisionNumber: string;
  status?: AuditEngagementStatus;
  riskRank?: string;
  name?: string;
  objective?: string;
  scope?: string;
  planningStartDate?: dayjs.Dayjs;
  planningEndDate?: dayjs.Dayjs;
  fieldworkStartDate?: dayjs.Dayjs;
  fieldworkEndDate?: dayjs.Dayjs;
  reportStartDate?: dayjs.Dayjs;
  reportEndDate?: dayjs.Dayjs;
  infoCollectionStart?: dayjs.Dayjs;
  infoCollectionEnd?: dayjs.Dayjs;
  sampleRequestStart?: dayjs.Dayjs;
  sampleRequestEnd?: dayjs.Dayjs;
  reportPlanStart?: dayjs.Dayjs;
  reportPlanEnd?: dayjs.Dayjs;
}

export interface AuditEngagementFormProps {
  mode: "create" | "edit" | "view";
  engagement: AuditEngagementItem | null;
  auditObjectUnits: AuditObjectUnitOption[];
  employees: EmployeeOption[];
  onSaved: (item: AuditEngagementItem) => void;
  onCancel: () => void;
  /** Bo qua (undefined) neu nguoi dung khong co quyen EDIT - nut "Sua" se khong hien o che do Xem. */
  onEdit?: () => void;
}

const toDate = (v: dayjs.Dayjs | undefined | null) => (v ? v.format("YYYY-MM-DD") : null);
const toDateTime = (v: dayjs.Dayjs | undefined | null) => (v ? v.format("YYYY-MM-DDTHH:mm:ss") : null);

/** Form dung chung cho ca 3 che do Tao moi/Xem/Sua man hinh "Thong tin cuoc kiem toan" (sheet "khoi tao"). */
export function AuditEngagementForm(props: AuditEngagementFormProps) {
  const { mode, engagement, auditObjectUnits, employees, onSaved, onCancel, onEdit } = props;
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [form] = Form.useForm<FormValues>();
  const readOnly = mode === "view";

  const [selectedUnitType, setSelectedUnitType] = useState<string | undefined>(undefined);
  const [submitting, setSubmitting] = useState(false);

  const [relatedUnits, setRelatedUnits] = useState<AuditEngagementRelatedUnitItem[]>([]);
  const [relatedUnitType, setRelatedUnitType] = useState<string | undefined>(undefined);
  const [relatedUnitId, setRelatedUnitId] = useState<string | undefined>(undefined);
  const [addingRelatedUnit, setAddingRelatedUnit] = useState(false);

  const unitTypes = Array.from(new Set(auditObjectUnits.map((u) => u.unitType))).sort();
  const unitsForType = (type: string | undefined) => (type ? auditObjectUnits.filter((u) => u.unitType === type) : []);

  const loadRelatedUnits = useCallback(async (engagementId: string) => {
    try {
      setRelatedUnits(await listRelatedUnits(engagementId));
    } catch {
      message.error(t("auditEngagement.messages.loadError"));
    }
  }, [message, t]);

  useEffect(() => {
    if (engagement) {
      const unit = auditObjectUnits.find((u) => u.id === engagement.auditObjectUnitId);
      setSelectedUnitType(unit?.unitType ?? engagement.unitType ?? undefined);
      form.setFieldsValue({
        unitType: unit?.unitType ?? engagement.unitType ?? undefined,
        auditObjectUnitId: engagement.auditObjectUnitId,
        year: engagement.year,
        expectedMonth: engagement.expectedMonth,
        decisionDate: dayjs(engagement.decisionDate),
        teamLeadEmployeeId: engagement.teamLeadEmployeeId,
        decisionNumber: engagement.decisionNumber,
        status: engagement.status,
        riskRank: engagement.riskRank ?? undefined,
        name: engagement.name ?? undefined,
        objective: engagement.objective ?? undefined,
        scope: engagement.scope ?? undefined,
        planningStartDate: engagement.planningStartDate ? dayjs(engagement.planningStartDate) : undefined,
        planningEndDate: engagement.planningEndDate ? dayjs(engagement.planningEndDate) : undefined,
        fieldworkStartDate: engagement.fieldworkStartDate ? dayjs(engagement.fieldworkStartDate) : undefined,
        fieldworkEndDate: engagement.fieldworkEndDate ? dayjs(engagement.fieldworkEndDate) : undefined,
        reportStartDate: engagement.reportStartDate ? dayjs(engagement.reportStartDate) : undefined,
        reportEndDate: engagement.reportEndDate ? dayjs(engagement.reportEndDate) : undefined,
        infoCollectionStart: engagement.infoCollectionStart ? dayjs(engagement.infoCollectionStart) : undefined,
        infoCollectionEnd: engagement.infoCollectionEnd ? dayjs(engagement.infoCollectionEnd) : undefined,
        sampleRequestStart: engagement.sampleRequestStart ? dayjs(engagement.sampleRequestStart) : undefined,
        sampleRequestEnd: engagement.sampleRequestEnd ? dayjs(engagement.sampleRequestEnd) : undefined,
        reportPlanStart: engagement.reportPlanStart ? dayjs(engagement.reportPlanStart) : undefined,
        reportPlanEnd: engagement.reportPlanEnd ? dayjs(engagement.reportPlanEnd) : undefined,
      });
      loadRelatedUnits(engagement.id);
    } else {
      form.resetFields();
      form.setFieldsValue({ year: dayjs().year(), status: "DRAFT" });
      setSelectedUnitType(undefined);
      setRelatedUnits([]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [engagement, auditObjectUnits]);

  const handleSubmit = useCallback(async () => {
    if (readOnly) return;
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      const request: AuditEngagementRequest = {
        auditObjectUnitId: values.auditObjectUnitId as string,
        year: values.year,
        expectedMonth: values.expectedMonth,
        decisionDate: toDate(values.decisionDate) as string,
        teamLeadEmployeeId: values.teamLeadEmployeeId,
        decisionNumber: values.decisionNumber,
        status: values.status ?? null,
        riskRank: values.riskRank ?? null,
        name: values.name ?? null,
        objective: values.objective ?? null,
        scope: values.scope ?? null,
        planningStartDate: toDate(values.planningStartDate),
        planningEndDate: toDate(values.planningEndDate),
        fieldworkStartDate: toDate(values.fieldworkStartDate),
        fieldworkEndDate: toDate(values.fieldworkEndDate),
        reportStartDate: toDate(values.reportStartDate),
        reportEndDate: toDate(values.reportEndDate),
        infoCollectionStart: toDateTime(values.infoCollectionStart),
        infoCollectionEnd: toDateTime(values.infoCollectionEnd),
        sampleRequestStart: toDateTime(values.sampleRequestStart),
        sampleRequestEnd: toDateTime(values.sampleRequestEnd),
        reportPlanStart: toDateTime(values.reportPlanStart),
        reportPlanEnd: toDateTime(values.reportPlanEnd),
      };
      let saved: AuditEngagementItem;
      if (engagement) {
        saved = await updateAuditEngagement(engagement.id, request);
        message.success(t("auditEngagement.messages.updateSuccess"));
      } else {
        saved = await createAuditEngagement(request);
        message.success(t("auditEngagement.messages.createSuccess", { code: saved.code }));
      }
      onSaved(saved);
    } catch {
      message.error(t("auditEngagement.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  }, [readOnly, form, engagement, message, t, onSaved]);

  useEffect(() => {
    if (readOnly) return;
    function handleKeyDown(e: KeyboardEvent) {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "s") {
        e.preventDefault();
        handleSubmit();
      }
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [readOnly, handleSubmit]);

  const handleAddRelatedUnit = async () => {
    if (!engagement || !relatedUnitId) return;
    setAddingRelatedUnit(true);
    try {
      await addRelatedUnit(engagement.id, relatedUnitId);
      message.success(t("auditEngagement.messages.relatedUnitAddSuccess"));
      setRelatedUnitType(undefined);
      setRelatedUnitId(undefined);
      await loadRelatedUnits(engagement.id);
    } catch {
      message.error(t("auditEngagement.messages.saveError"));
    } finally {
      setAddingRelatedUnit(false);
    }
  };

  const handleDeleteRelatedUnit = async (rowId: string) => {
    if (!engagement) return;
    try {
      await deleteRelatedUnit(engagement.id, rowId);
      message.success(t("common.deleteSuccess"));
      await loadRelatedUnits(engagement.id);
    } catch {
      message.error(t("auditEngagement.messages.saveError"));
    }
  };

  const relatedUnitColumns: TableProps<AuditEngagementRelatedUnitItem>["columns"] = [
    { title: t("auditEngagement.form.relatedUnitCode"), dataIndex: "auditObjectUnitCode", width: 140 },
    { title: t("auditEngagement.form.relatedUnitType"), dataIndex: "unitType", width: 140 },
    { title: t("auditEngagement.form.relatedUnitName"), dataIndex: "auditObjectUnitName" },
    {
      title: "",
      key: "actions",
      width: 60,
      render: (_v, row) =>
        !readOnly && (
          <Button size="small" danger type="text" icon={<DeleteOutlined />} onClick={() => handleDeleteRelatedUnit(row.id)} />
        ),
    },
  ];

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {engagement
              ? readOnly
                ? t("auditEngagement.form.viewTitle", { code: engagement.code })
                : t("auditEngagement.form.editTitle", { code: engagement.code })
              : t("auditEngagement.form.createTitle")}
          </Typography.Title>
        </Col>
        <Col>
          <Space>
            <Button onClick={onCancel}>{t("common.back")}</Button>
            {readOnly ? (
              onEdit && (
                <Button type="primary" onClick={onEdit}>
                  {t("common.edit")}
                </Button>
              )
            ) : (
              <Button type="primary" icon={<SaveOutlined />} loading={submitting} onClick={handleSubmit}>
                {t("common.save")}
              </Button>
            )}
          </Space>
        </Col>
      </Row>

      <Form<FormValues> form={form} layout="vertical" disabled={readOnly}>
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item label={t("auditEngagement.columns.code")}>
              <Input value={engagement?.code ?? t("auditEngagement.form.codeAutoGenerated")} disabled />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="year" label={t("auditEngagement.columns.year")} rules={[{ required: true }]}>
              <InputNumber style={{ width: "100%" }} min={2000} max={2100} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="expectedMonth" label={t("auditEngagement.columns.expectedMonth")} rules={[{ required: true }]}>
              <Select options={MONTHS.map((m) => ({ value: m, label: m }))} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item name="unitType" label={t("auditEngagement.columns.unitType")} rules={[{ required: true }]}>
              <Select
                showSearch
                options={unitTypes.map((v) => ({ value: v, label: v }))}
                onChange={(v: string) => {
                  setSelectedUnitType(v);
                  form.setFieldValue("auditObjectUnitId", undefined);
                }}
              />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="auditObjectUnitId" label={t("auditEngagement.columns.auditObjectUnit")} rules={[{ required: true }]}>
              <Select
                showSearch
                disabled={readOnly || !selectedUnitType}
                optionFilterProp="label"
                options={unitsForType(selectedUnitType).map((u) => ({ value: u.id, label: `${u.code} - ${u.name}` }))}
              />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="decisionDate" label={t("auditEngagement.columns.decisionDate")} rules={[{ required: true }]}>
              <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item name="teamLeadEmployeeId" label={t("auditEngagement.columns.teamLeadEmployee")} rules={[{ required: true }]}>
              <Select
                showSearch
                optionFilterProp="label"
                options={employees.filter((e) => e.username).map((e) => ({ value: e.id, label: `${e.fullName} (${e.username})` }))}
              />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="decisionNumber" label={t("auditEngagement.columns.decisionNumber")} rules={[{ required: true }]}>
              <Input maxLength={50} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="status" label={t("auditEngagement.columns.status")}>
              <Select options={STATUSES.map((v) => ({ value: v, label: t(`auditEngagement.status.${v}`) }))} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item name="riskRank" label={t("auditEngagement.columns.riskRank")}>
              <Select allowClear options={RISK_RANKS.map((v) => ({ value: v, label: v }))} />
            </Form.Item>
          </Col>
          <Col span={16}>
            <Form.Item name="name" label={t("auditEngagement.columns.name")}>
              <Input maxLength={255} />
            </Form.Item>
          </Col>
        </Row>

        <Divider orientation="left" plain>{t("auditEngagement.form.sectionGeneral")}</Divider>
        <Form.Item name="objective" label={t("auditEngagement.columns.objective")}>
          <Input.TextArea rows={3} />
        </Form.Item>
        <Form.Item name="scope" label={t("auditEngagement.columns.scope")}>
          <Input.TextArea rows={3} />
        </Form.Item>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="planningStartDate" label={t("auditEngagement.form.planningStart")}>
              <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="planningEndDate" label={t("auditEngagement.form.planningEnd")}>
              <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="fieldworkStartDate" label={t("auditEngagement.form.fieldworkStart")}>
              <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="fieldworkEndDate" label={t("auditEngagement.form.fieldworkEnd")}>
              <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="reportStartDate" label={t("auditEngagement.form.reportStart")}>
              <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="reportEndDate" label={t("auditEngagement.form.reportEnd")}>
              <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
            </Form.Item>
          </Col>
        </Row>

        <Divider orientation="left" plain>{t("auditEngagement.form.sectionPrepPlanning")}</Divider>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="infoCollectionStart" label={t("auditEngagement.form.infoCollectionStart")}>
              <DatePicker showTime style={{ width: "100%" }} format="DD.MM.YYYY HH:mm" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="infoCollectionEnd" label={t("auditEngagement.form.infoCollectionEnd")}>
              <DatePicker showTime style={{ width: "100%" }} format="DD.MM.YYYY HH:mm" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="sampleRequestStart" label={t("auditEngagement.form.sampleRequestStart")}>
              <DatePicker showTime style={{ width: "100%" }} format="DD.MM.YYYY HH:mm" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="sampleRequestEnd" label={t("auditEngagement.form.sampleRequestEnd")}>
              <DatePicker showTime style={{ width: "100%" }} format="DD.MM.YYYY HH:mm" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="reportPlanStart" label={t("auditEngagement.form.reportPlanStart")}>
              <DatePicker showTime style={{ width: "100%" }} format="DD.MM.YYYY HH:mm" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="reportPlanEnd" label={t("auditEngagement.form.reportPlanEnd")}>
              <DatePicker showTime style={{ width: "100%" }} format="DD.MM.YYYY HH:mm" />
            </Form.Item>
          </Col>
        </Row>
      </Form>

      <Divider orientation="left" plain>{t("auditEngagement.form.sectionRelatedUnits")}</Divider>
      {engagement ? (
        <>
          {!readOnly && (
            <Row gutter={16} align="bottom" style={{ marginBottom: 12 }}>
              <Col span={8}>
                <Typography.Text type="secondary">{t("auditEngagement.columns.unitType")}</Typography.Text>
                <Select
                  style={{ width: "100%" }}
                  showSearch
                  value={relatedUnitType}
                  options={unitTypes.map((v) => ({ value: v, label: v }))}
                  onChange={(v: string) => {
                    setRelatedUnitType(v);
                    setRelatedUnitId(undefined);
                  }}
                />
              </Col>
              <Col span={10}>
                <Typography.Text type="secondary">{t("auditEngagement.form.relatedUnitBranch")}</Typography.Text>
                <Select
                  style={{ width: "100%" }}
                  showSearch
                  disabled={!relatedUnitType}
                  optionFilterProp="label"
                  value={relatedUnitId}
                  options={unitsForType(relatedUnitType).map((u) => ({ value: u.id, label: `${u.code} - ${u.name}` }))}
                  onChange={(v: string) => setRelatedUnitId(v)}
                />
              </Col>
              <Col span={6}>
                <Space>
                  <Button icon={<PlusOutlined />} loading={addingRelatedUnit} disabled={!relatedUnitId} onClick={handleAddRelatedUnit}>
                    {t("common.add")}
                  </Button>
                </Space>
              </Col>
            </Row>
          )}
          <Table<AuditEngagementRelatedUnitItem>
            size="small"
            rowKey="id"
            pagination={false}
            columns={relatedUnitColumns}
            dataSource={relatedUnits}
          />
        </>
      ) : (
        <Typography.Text type="secondary">{t("auditEngagement.form.relatedUnitsNeedsSave")}</Typography.Text>
      )}

      <Divider orientation="left" plain>{t("auditEngagement.form.sectionAttachment")}</Divider>
      {engagement ? (
        <AttachmentPanel http={httpClient} entityName={ENTITY_NAME} entityId={engagement.id} />
      ) : (
        <Typography.Text type="secondary">{t("auditEngagement.form.attachmentNeedsSave")}</Typography.Text>
      )}
    </div>
  );
}
