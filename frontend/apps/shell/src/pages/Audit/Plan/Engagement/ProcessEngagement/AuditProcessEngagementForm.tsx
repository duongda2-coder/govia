import { useCallback, useEffect, useState } from "react";
import { App, Button, Col, DatePicker, Divider, Form, Input, InputNumber, Row, Select, Space, Typography } from "antd";
import { SaveOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { AttachmentPanel } from "@govia/ui-kit";
import { httpClient } from "../../../../../api/client";
import type { MasterDataItem } from "../../../../../api/auditMasterData";
import {
  createAuditProcessEngagement,
  updateAuditProcessEngagement,
  type AuditProcessEngagementItem,
  type AuditProcessEngagementObjectType,
  type AuditProcessEngagementRequest,
  type TeamLeadOption,
} from "../../../../../api/auditProcessEngagement";

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);
const ENTITY_NAME = "AUDIT_PROCESS_ENGAGEMENT";
const OBJECT_TYPES: AuditProcessEngagementObjectType[] = ["QT", "HD"];

interface FormValues {
  objectType: AuditProcessEngagementObjectType;
  businessSegmentId: string;
  year: number;
  expectedMonth: number;
  decisionDate: dayjs.Dayjs;
  teamLeadEmployeeId: string;
  decisionNumber: string;
  name?: string;
  workSetCode?: string;
}

export interface AuditProcessEngagementFormProps {
  mode: "create" | "edit" | "view";
  engagement: AuditProcessEngagementItem | null;
  businessSegments: MasterDataItem[];
  teamLeads: TeamLeadOption[];
  onSaved: (item: AuditProcessEngagementItem) => void;
  onCancel: () => void;
  /** Bo qua (undefined) neu nguoi dung khong co quyen EDIT - nut "Sua" se khong hien o che do Xem. */
  onEdit?: () => void;
}

const toDate = (v: dayjs.Dayjs | undefined | null) => (v ? v.format("YYYY-MM-DD") : null);

/** Form dung chung cho ca 3 che do Tao moi/Xem/Sua man hinh "Tao CKT quy trinh" (sheet "man hinh tao
 * CKT quy trinh" cua Tao CKT (3).xlsx) - cung bo cuc voi AuditEngagementForm (sibling CKT chi nhanh). */
export function AuditProcessEngagementForm(props: AuditProcessEngagementFormProps) {
  const { mode, engagement, businessSegments, teamLeads, onSaved, onCancel, onEdit } = props;
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [form] = Form.useForm<FormValues>();
  const readOnly = mode === "view";

  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (engagement) {
      form.setFieldsValue({
        objectType: engagement.objectType,
        businessSegmentId: engagement.businessSegmentId,
        year: engagement.year,
        expectedMonth: engagement.expectedMonth,
        decisionDate: dayjs(engagement.decisionDate),
        teamLeadEmployeeId: engagement.teamLeadEmployeeId,
        decisionNumber: engagement.decisionNumber,
        name: engagement.name ?? undefined,
        workSetCode: engagement.workSetCode ?? undefined,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ year: dayjs().year(), objectType: "QT" });
    }
  }, [engagement, form]);

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
      const request: AuditProcessEngagementRequest = {
        objectType: values.objectType,
        businessSegmentId: values.businessSegmentId,
        year: values.year,
        expectedMonth: values.expectedMonth,
        decisionDate: toDate(values.decisionDate) as string,
        teamLeadEmployeeId: values.teamLeadEmployeeId,
        decisionNumber: values.decisionNumber,
        name: values.name ?? null,
        workSetCode: values.workSetCode ?? null,
      };
      let saved: AuditProcessEngagementItem;
      if (engagement) {
        saved = await updateAuditProcessEngagement(engagement.id, request);
        message.success(t("auditProcessEngagement.messages.updateSuccess"));
      } else {
        saved = await createAuditProcessEngagement(request);
        message.success(t("auditProcessEngagement.messages.createSuccess", { code: saved.code }));
      }
      onSaved(saved);
    } catch {
      message.error(t("auditProcessEngagement.messages.saveError"));
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

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {engagement
              ? readOnly
                ? t("auditProcessEngagement.form.viewTitle", { code: engagement.code })
                : t("auditProcessEngagement.form.editTitle", { code: engagement.code })
              : t("auditProcessEngagement.form.createTitle")}
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
            <Form.Item label={t("auditProcessEngagement.columns.code")}>
              <Input value={engagement?.code ?? t("auditProcessEngagement.form.codeAutoGenerated")} disabled />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="objectType" label={t("auditProcessEngagement.columns.objectType")} rules={[{ required: true }]}>
              <Select disabled={readOnly || !!engagement} options={OBJECT_TYPES.map((v) => ({ value: v, label: t(`auditProcessEngagement.objectType.${v}`) }))} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="businessSegmentId" label={t("auditProcessEngagement.columns.businessSegment")} rules={[{ required: true }]}>
              <Select
                showSearch
                optionFilterProp="label"
                options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))}
              />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="year" label={t("auditProcessEngagement.columns.year")} rules={[{ required: true }]}>
              <InputNumber style={{ width: "100%" }} min={2000} max={2100} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item name="expectedMonth" label={t("auditProcessEngagement.columns.expectedMonth")} rules={[{ required: true }]}>
              <Select options={MONTHS.map((m) => ({ value: m, label: m }))} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="decisionDate" label={t("auditProcessEngagement.columns.decisionDate")} rules={[{ required: true }]}>
              <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="teamLeadEmployeeId" label={t("auditProcessEngagement.columns.teamLeadEmployee")} rules={[{ required: true }]}>
              <Select
                showSearch
                optionFilterProp="label"
                // Luon giu lai lua chon hien tai (co the la nhan vien chua/khong con duoc tick "Truong
                // doan" o danh muc Kha nang dam nhan linh vuc) de khong lam "mat" gia tri da luu khi mo
                // lai form Sua - chi rang buoc lua chon MOI theo dung kha nang, giong AuditEngagementForm.
                options={teamLeads
                  .filter((e) => e.truongDoanCapable || e.id === engagement?.teamLeadEmployeeId)
                  .map((e) => ({ value: e.id, label: `${e.fullName} (${e.employeeCode})` }))}
              />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item name="decisionNumber" label={t("auditProcessEngagement.columns.decisionNumber")} rules={[{ required: true }]}>
              <Input maxLength={50} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="workSetCode" label={t("auditProcessEngagement.columns.workSetCode")}>
              <Input maxLength={50} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="name" label={t("auditProcessEngagement.columns.name")}>
              <Input maxLength={255} />
            </Form.Item>
          </Col>
        </Row>
      </Form>

      <Divider orientation="left" plain>{t("auditProcessEngagement.form.sectionAttachment")}</Divider>
      {engagement ? (
        <AttachmentPanel http={httpClient} entityName={ENTITY_NAME} entityId={engagement.id} />
      ) : (
        <Typography.Text type="secondary">{t("auditProcessEngagement.form.attachmentNeedsSave")}</Typography.Text>
      )}
    </div>
  );
}
