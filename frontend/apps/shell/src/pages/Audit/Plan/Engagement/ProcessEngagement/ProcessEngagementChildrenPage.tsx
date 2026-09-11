import { useEffect, useState } from "react";
import { App, Button, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { listProcessEngagementChildren } from "../../../../../api/auditProcessEngagement";
import type { AuditEngagementMonitoringItem } from "../../../../../api/auditEngagementMonitoring";
import { AuditEngagementTeamDetailPage } from "../AuditEngagementTeamDetailPage";

export interface ProcessEngagementChildrenPageProps {
  processEngagementId: string;
  processEngagementCode: string;
  onBack: () => void;
}

/** "6. Chi tiết đoàn" (man hinh "QL CKT quy trinh") - 1 dong / CKT con voi so lieu tong hop, nut
 * "Chi tiết đoàn KT" dieu huong sang man hinh "Quản lý đợt kiểm toán" cho dung CKT con do (tai su
 * dung nguyen AuditEngagementTeamDetailPage - xem javadoc AuditProcessEngagementAggregateService). */
export function ProcessEngagementChildrenPage({ processEngagementId, processEngagementCode, onBack }: ProcessEngagementChildrenPageProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [items, setItems] = useState<AuditEngagementMonitoringItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditEngagementMonitoringItem | null>(null);

  const load = () => {
    setLoading(true);
    listProcessEngagementChildren(processEngagementId)
      .then(setItems)
      .catch(() => message.error(t("auditProcessEngagement.messages.loadError")))
      .finally(() => setLoading(false));
  };

  useEffect(load, [processEngagementId]); // eslint-disable-line react-hooks/exhaustive-deps

  if (selected) {
    return <AuditEngagementTeamDetailPage engagement={selected} onBack={() => setSelected(null)} />;
  }

  const columns: TableProps<AuditEngagementMonitoringItem>["columns"] = [
    { title: t("auditProcessEngagement.aggregate.children.code"), dataIndex: "code", width: 150 },
    { title: t("auditProcessEngagement.aggregate.children.auditObjectUnitName"), dataIndex: "auditObjectUnitName", render: (v: string | null) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.children.auditObjectUnitCode"), dataIndex: "auditObjectUnitCode", width: 110, render: (v: string | null) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.children.fieldworkStartDate"), dataIndex: "fieldworkStartDate", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.children.fieldworkEndDate"), dataIndex: "fieldworkEndDate", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.children.decisionNumber"), dataIndex: "decisionNumber", width: 130 },
    { title: t("auditProcessEngagement.aggregate.children.teamLeadEmployeeName"), dataIndex: "teamLeadEmployeeName", render: (v: string | null) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.children.memberCount"), dataIndex: "memberCount", width: 90 },
    { title: t("auditProcessEngagement.aggregate.children.businessSegmentCount"), dataIndex: "businessSegmentCount", width: 90 },
    { title: t("auditProcessEngagement.aggregate.children.totalFindings"), dataIndex: "totalFindings", width: 110 },
    { title: t("auditProcessEngagement.aggregate.children.totalMaterialFindings"), dataIndex: "totalMaterialFindings", width: 110 },
    { title: t("auditProcessEngagement.aggregate.children.recommendationCount"), dataIndex: "recommendationCount", width: 100 },
    {
      title: "",
      key: "actions",
      width: 140,
      render: (_: unknown, item: AuditEngagementMonitoringItem) => (
        <Button size="small" onClick={() => setSelected(item)}>
          {t("auditProcessEngagement.aggregate.children.viewDetail")}
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16, width: "100%", justifyContent: "space-between" }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {t("auditProcessEngagement.aggregate.children.title", { code: processEngagementCode })}
        </Typography.Title>
        <Button onClick={onBack}>{t("common.back")}</Button>
      </Space>
      <Table<AuditEngagementMonitoringItem>
        rowKey="id"
        loading={loading}
        dataSource={items}
        columns={columns}
        scroll={{ x: true }}
        pagination={{ pageSize: 50 }}
      />
    </div>
  );
}
