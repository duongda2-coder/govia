import { useCallback, useEffect, useState } from "react";
import { App, Button, Form, Input, Modal, Select, Table } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import {
  createAuditRecommendation,
  listAuditRecommendations,
  type AuditRecommendationItem,
} from "../../../../../api/auditRecommendation";
import { listMasterDataItems, type MasterDataItem } from "../../../../../api/auditMasterData";

export interface RecommendationCatalogModalProps {
  open: boolean;
  engagementId: string | null;
  onClose: () => void;
  /** Bao lai cho man hinh cha khi catalog thay doi (vd de load lai options cho modal Gan kien nghi). */
  onChanged?: () => void;
}

interface FormValues {
  businessSegmentId?: string;
  content: string;
}

/** "3. Thêm kiến nghị" - quan ly catalog "Lưu mã kiến nghị" cua 1 cuoc kiem toan (luon co san dong
 * mac dinh KNKT000 - xem AuditRecommendationService). */
export function RecommendationCatalogModal({ open, engagementId, onClose, onChanged }: RecommendationCatalogModalProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();

  const [items, setItems] = useState<AuditRecommendationItem[]>([]);
  const [segments, setSegments] = useState<MasterDataItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  const load = useCallback(async () => {
    if (!engagementId) return;
    setLoading(true);
    try {
      setItems(await listAuditRecommendations(engagementId));
    } catch {
      message.error(t("auditRecommendation.loadError"));
    } finally {
      setLoading(false);
    }
  }, [engagementId, message, t]);

  useEffect(() => {
    if (open) {
      load();
      listMasterDataItems("BUSINESS_SEGMENT").then(setSegments).catch(() => setSegments([]));
    }
  }, [open, load]);

  const handleSubmit = async () => {
    if (!engagementId) return;
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await createAuditRecommendation(engagementId, { businessSegmentId: values.businessSegmentId ?? null, content: values.content });
      message.success(t("auditRecommendation.createSuccess"));
      form.resetFields();
      await load();
      onChanged?.();
    } catch {
      message.error(t("auditRecommendation.createError"));
    } finally {
      setSubmitting(false);
    }
  };

  const columns: TableProps<AuditRecommendationItem>["columns"] = [
    { title: t("auditRecommendation.columns.code"), dataIndex: "code", width: 110 },
    { title: t("auditRecommendation.columns.businessSegment"), dataIndex: "businessSegmentCode", width: 120, render: (v) => v ?? "-" },
    { title: t("auditRecommendation.columns.content"), dataIndex: "content" },
  ];

  return (
    <Modal title={t("auditRecommendation.title")} open={open} onCancel={onClose} footer={null} width={700} destroyOnClose>
      <Form<FormValues> form={form} layout="inline" onFinish={handleSubmit} style={{ marginBottom: 16 }}>
        <Form.Item name="businessSegmentId" style={{ minWidth: 160 }}>
          <Select
            allowClear
            placeholder={t("auditRecommendation.columns.businessSegment")}
            options={segments.map((s) => ({ value: s.id, label: s.code }))}
          />
        </Form.Item>
        <Form.Item name="content" rules={[{ required: true }]} style={{ flex: 1, minWidth: 200 }}>
          <Input placeholder={t("auditRecommendation.columns.content")} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={submitting}>
            {t("common.add")}
          </Button>
        </Form.Item>
      </Form>
      <Table<AuditRecommendationItem> rowKey="id" loading={loading} dataSource={items} columns={columns} pagination={false} />
    </Modal>
  );
}
