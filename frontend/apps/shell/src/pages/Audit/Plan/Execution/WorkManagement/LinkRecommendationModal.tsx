import { useEffect, useState } from "react";
import { App, Form, Modal, Select } from "antd";
import { useTranslation } from "react-i18next";
import { linkAuditTtssRecommendation } from "../../../../../api/auditTtss";
import { listAuditRecommendations, type AuditRecommendationItem } from "../../../../../api/auditRecommendation";

export interface LinkRecommendationModalProps {
  open: boolean;
  engagementId: string | null;
  ttssRecordIds: string[];
  onClose: () => void;
  onLinked: () => void;
}

interface FormValues {
  recommendationId: string;
}

/** "4. Gắn kiến nghị" - chon 1 kien nghi tu catalog de gan cho cac dong TTSS dang duoc chon. */
export function LinkRecommendationModal({ open, engagementId, ttssRecordIds, onClose, onLinked }: LinkRecommendationModalProps) {
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
      await linkAuditTtssRecommendation(engagementId, ttssRecordIds, values.recommendationId);
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
      <Form<FormValues> form={form} layout="vertical">
        <Form.Item name="recommendationId" label={t("auditRecommendation.title")} rules={[{ required: true }]}>
          <Select
            showSearch
            optionFilterProp="label"
            options={recommendations.map((r) => ({ value: r.id, label: `${r.code} - ${r.content}` }))}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
