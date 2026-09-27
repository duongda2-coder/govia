import { useEffect, useState } from "react";
import { App, Form, Modal, Select } from "antd";
import { useTranslation } from "react-i18next";
import { linkAuditTtssRecommendation } from "../../../../../api/auditTtss";
import { listAuditRecommendations, type AuditRecommendationItem } from "../../../../../api/auditRecommendation";

export interface LinkRecommendationModalProps {
  open: boolean;
  engagementId: string | null;
  ttssRecordIds: string[];
  /** Kien nghi dang gan (dien san khi chi chon 1 dong TTSS). */
  initialRecommendationIds?: string[];
  onClose: () => void;
  onLinked: () => void;
}

interface FormValues {
  recommendationIds: string[];
}

/** "4. Gắn kiến nghị" - chon 1 hoac NHIEU kien nghi tu catalog de gan cho cac dong TTSS dang duoc chon
 * (test25.9: 1 TTSS gan nhieu kien nghi truong doan). Danh sach chon THAY THE kien nghi da gan truoc do. */
export function LinkRecommendationModal({ open, engagementId, ttssRecordIds, initialRecommendationIds, onClose, onLinked }: LinkRecommendationModalProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [recommendations, setRecommendations] = useState<AuditRecommendationItem[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();

  useEffect(() => {
    if (open && engagementId) {
      listAuditRecommendations(engagementId).then(setRecommendations).catch(() => setRecommendations([]));
    }
  }, [open, engagementId]);

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
      await linkAuditTtssRecommendation(engagementId, ttssRecordIds, values.recommendationIds);
      message.success(t("auditTtss.linkRecommendationSuccess"));
      form.resetFields();
      onLinked();
    } catch {
      message.error(t("auditTtss.linkRecommendationError"));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={t("auditTtss.linkRecommendationTitle", { count: ttssRecordIds.length })}
      open={open}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={submitting}
      destroyOnClose
    >
      <Form<FormValues> form={form} layout="vertical" preserve={false} initialValues={{ recommendationIds: initialRecommendationIds ?? [] }}>
        <Form.Item name="recommendationIds" label={t("auditRecommendation.title")} rules={[{ required: true }]}>
          <Select
            mode="multiple"
            showSearch
            optionFilterProp="label"
            options={recommendations.map((r) => ({ value: r.id, label: `${r.code} - ${r.content}` }))}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
