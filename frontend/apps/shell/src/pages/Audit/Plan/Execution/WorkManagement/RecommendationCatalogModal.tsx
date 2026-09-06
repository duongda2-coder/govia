import { useCallback, useEffect, useState } from "react";
import { App, Button, Form, Input, Modal, Select, Table } from "antd";
import type { TableProps } from "antd";
import { DeleteOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { getApiErrorMessage } from "@govia/ui-kit";
import {
  createAuditRecommendation,
  deleteAuditRecommendation,
  listAuditRecommendations,
  type AuditRecommendationItem,
} from "../../../../../api/auditRecommendation";
import { listMasterDataItems, type MasterDataItem } from "../../../../../api/auditMasterData";

/** Ma mac dinh luon co san cho moi engagement, khong duoc phep xoa - khop DEFAULT_CODE ben
 * AuditRecommendationService (backend cung chan neu co goi xoa). */
const DEFAULT_RECOMMENDATION_CODE = "KNKT000";

export interface RecommendationCatalogModalProps {
  open: boolean;
  engagementId: string | null;
  onClose: () => void;
  /** Bao lai cho man hinh cha khi catalog thay doi (vd de load lai options cho modal Gan kien nghi). */
  onChanged?: () => void;
}

interface FormValues {
  code: string;
  businessSegmentId?: string;
  content: string;
}

/** "3. Thêm kiến nghị" - quan ly catalog "Lưu mã kiến nghị" cua 1 cuoc kiem toan (luon co san dong
 * mac dinh KNKT000 - xem AuditRecommendationService). */
export function RecommendationCatalogModal({ open, engagementId, onClose, onChanged }: RecommendationCatalogModalProps) {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();

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
      await createAuditRecommendation(engagementId, {
        code: values.code.trim().toUpperCase(),
        businessSegmentId: values.businessSegmentId ?? null,
        content: values.content,
      });
      message.success(t("auditRecommendation.createSuccess"));
      form.resetFields();
      await load();
      onChanged?.();
    } catch (err) {
      message.error(getApiErrorMessage(err, t("auditRecommendation.createError")));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = (item: AuditRecommendationItem) => {
    if (!engagementId) return;
    modal.confirm({
      title: t("auditRecommendation.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      onOk: async () => {
        try {
          await deleteAuditRecommendation(engagementId, item.id);
          message.success(t("auditRecommendation.deleteSuccess"));
          await load();
          onChanged?.();
        } catch (err) {
          message.error(getApiErrorMessage(err, t("auditRecommendation.deleteError")));
        }
      },
    });
  };

  const columns: TableProps<AuditRecommendationItem>["columns"] = [
    { title: t("auditRecommendation.columns.code"), dataIndex: "code", width: 110 },
    { title: t("auditRecommendation.columns.businessSegment"), dataIndex: "businessSegmentCode", width: 120, render: (v) => v ?? "-" },
    { title: t("auditRecommendation.columns.content"), dataIndex: "content" },
    {
      title: "",
      key: "actions",
      width: 60,
      render: (_: unknown, item: AuditRecommendationItem) =>
        item.code === DEFAULT_RECOMMENDATION_CODE ? null : (
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(item)} />
        ),
    },
  ];

  return (
    <Modal title={t("auditRecommendation.title")} open={open} onCancel={onClose} footer={null} width={700} destroyOnClose>
      <Form<FormValues> form={form} layout="inline" onFinish={handleSubmit} style={{ marginBottom: 16 }}>
        <Form.Item
          name="code"
          style={{ width: 130 }}
          rules={[
            { required: true, message: t("auditRecommendation.columns.code") },
            { pattern: /^KNKT\d{3,}$/i, message: t("auditRecommendation.codeFormatError") },
          ]}
        >
          <Input placeholder="KNKT001" />
        </Form.Item>
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
