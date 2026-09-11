import { useEffect, useState } from "react";
import { App, Button, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { listProcessEngagementChildren, listProcessEngagementRecommendations } from "../../../../../api/auditProcessEngagement";
import type { AuditRecommendationItem } from "../../../../../api/auditRecommendation";

export interface ProcessEngagementRecommendationsPageProps {
  processEngagementId: string;
  processEngagementCode: string;
  onBack: () => void;
}

/** "5. Quản lý kiến nghị" (man hinh "QL CKT quy trinh") - CHI XEM, "Tương tự chức năng quản lý
 * TTSS" (dac ta) - tong hop kien nghi cua TAT CA CKT con. */
export function ProcessEngagementRecommendationsPage({ processEngagementId, processEngagementCode, onBack }: ProcessEngagementRecommendationsPageProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [items, setItems] = useState<AuditRecommendationItem[]>([]);
  const [engagementCodeById, setEngagementCodeById] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([listProcessEngagementRecommendations(processEngagementId), listProcessEngagementChildren(processEngagementId)])
      .then(([recommendations, children]) => {
        if (cancelled) return;
        setItems(recommendations);
        setEngagementCodeById(Object.fromEntries(children.map((c) => [c.id, c.code])));
      })
      .catch(() => message.error(t("auditProcessEngagement.messages.loadError")))
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [processEngagementId, message, t]);

  const columns: TableProps<AuditRecommendationItem>["columns"] = [
    {
      title: t("auditProcessEngagement.aggregate.recommendations.engagementCode"),
      dataIndex: "engagementId",
      width: 150,
      render: (v: string) => engagementCodeById[v] ?? "-",
    },
    { title: t("auditProcessEngagement.aggregate.recommendations.code"), dataIndex: "code", width: 130 },
    { title: t("auditProcessEngagement.aggregate.recommendations.businessSegmentCode"), dataIndex: "businessSegmentCode", width: 130, render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.recommendations.content"), dataIndex: "content" },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16, width: "100%", justifyContent: "space-between" }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {t("auditProcessEngagement.aggregate.recommendations.title", { code: processEngagementCode })}
        </Typography.Title>
        <Button onClick={onBack}>{t("common.back")}</Button>
      </Space>
      <Table<AuditRecommendationItem> rowKey="id" loading={loading} dataSource={items} columns={columns} pagination={{ pageSize: 50 }} />
    </div>
  );
}
