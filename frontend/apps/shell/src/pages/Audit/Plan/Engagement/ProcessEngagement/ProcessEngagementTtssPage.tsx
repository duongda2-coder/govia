import { useEffect, useState } from "react";
import { App, Button, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { listProcessEngagementChildren, listProcessEngagementTtss } from "../../../../../api/auditProcessEngagement";
import type { AuditTtssRecordItem } from "../../../../../api/auditTtss";

export interface ProcessEngagementTtssPageProps {
  processEngagementId: string;
  processEngagementCode: string;
  onBack: () => void;
}

/** "4. Quản lý TTSS" (man hinh "QL CKT quy trinh") - CHI XEM, tong hop TTSS cua TAT CA CKT con. */
export function ProcessEngagementTtssPage({ processEngagementId, processEngagementCode, onBack }: ProcessEngagementTtssPageProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [items, setItems] = useState<AuditTtssRecordItem[]>([]);
  const [engagementCodeById, setEngagementCodeById] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([listProcessEngagementTtss(processEngagementId), listProcessEngagementChildren(processEngagementId)])
      .then(([ttss, children]) => {
        if (cancelled) return;
        setItems(ttss);
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

  const columns: TableProps<AuditTtssRecordItem>["columns"] = [
    {
      title: t("auditProcessEngagement.aggregate.ttss.engagementCode"),
      dataIndex: "engagementId",
      width: 150,
      render: (v: string) => engagementCodeById[v] ?? "-",
    },
    { title: t("auditProcessEngagement.aggregate.ttss.businessSegmentCode"), dataIndex: "businessSegmentCode", width: 110, render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.ttss.recordUsername"), dataIndex: "recordUsername", width: 130, render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.ttss.workItemCode"), dataIndex: "workItemCode", width: 110, render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.ttss.processStepSummaryName"), dataIndex: "processStepSummaryName", render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.ttss.processStepDetailCode"), dataIndex: "processStepDetailCode", width: 130, render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.ttss.findingCode"), dataIndex: "findingCode", width: 120, render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.ttss.findingName"), dataIndex: "findingName", render: (v) => v ?? "-" },
    {
      title: t("auditProcessEngagement.aggregate.ttss.material"),
      dataIndex: "material",
      width: 100,
      render: (v: boolean) => (v ? t("common.yes") : t("common.no")),
    },
    { title: t("auditProcessEngagement.aggregate.ttss.referenceNumber"), dataIndex: "referenceNumber", width: 120, render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.ttss.referenceNumber2"), dataIndex: "referenceNumber2", width: 120, render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.ttss.customerCode"), dataIndex: "customerCode", width: 110, render: (v) => v ?? "-" },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16, width: "100%", justifyContent: "space-between" }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {t("auditProcessEngagement.aggregate.ttss.title", { code: processEngagementCode })}
        </Typography.Title>
        <Button onClick={onBack}>{t("common.back")}</Button>
      </Space>
      <Table<AuditTtssRecordItem> rowKey="id" loading={loading} dataSource={items} columns={columns} scroll={{ x: true }} pagination={{ pageSize: 50 }} />
    </div>
  );
}
