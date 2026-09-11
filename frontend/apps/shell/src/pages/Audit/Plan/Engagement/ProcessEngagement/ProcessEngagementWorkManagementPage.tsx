import { useEffect, useState } from "react";
import { App, Button, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { listProcessEngagementWorkManagement } from "../../../../../api/auditProcessEngagement";
import type { AuditWorkManagementItem } from "../../../../../api/auditWorkManagement";

export interface ProcessEngagementWorkManagementPageProps {
  processEngagementId: string;
  processEngagementCode: string;
  onBack: () => void;
}

/** "3. Quản lý công việc" (man hinh "QL CKT quy trinh") - CHI XEM, tong hop cong viec cua TAT CA
 * CKT con (ca CBKT/THKT/DCKT) - xem AuditProcessEngagementAggregateService.workManagement. */
export function ProcessEngagementWorkManagementPage({ processEngagementId, processEngagementCode, onBack }: ProcessEngagementWorkManagementPageProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [items, setItems] = useState<AuditWorkManagementItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    listProcessEngagementWorkManagement(processEngagementId)
      .then((data) => {
        if (!cancelled) setItems(data);
      })
      .catch(() => message.error(t("auditProcessEngagement.messages.loadError")))
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [processEngagementId, message, t]);

  const columns: TableProps<AuditWorkManagementItem>["columns"] = [
    { title: t("auditProcessEngagement.aggregate.workManagement.engagementCode"), dataIndex: "engagementCode", width: 150, render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.workManagement.employeeCode"), dataIndex: "employeeCode", width: 100, render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.workManagement.engagementName"), dataIndex: "engagementName", render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.workManagement.businessSegmentCode"), dataIndex: "businessSegmentCode", width: 110, render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.workManagement.workItemCode"), dataIndex: "workItemCode", width: 120, render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.workManagement.workItemName"), dataIndex: "workItemName", render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.workManagement.employeeUsername"), dataIndex: "employeeUsername", width: 130, render: (v) => v ?? "-" },
    {
      title: t("auditProcessEngagement.aggregate.workManagement.status"),
      dataIndex: "status",
      width: 130,
      render: (v: string) => t(`auditWorkManagement.status.${v}`),
    },
    {
      title: t("auditProcessEngagement.aggregate.workManagement.approvalStatus"),
      dataIndex: "approvalStatus",
      width: 130,
      render: (v: string | null) => (v ? t(`auditWorkManagement.approvalStatus.${v}`) : "-"),
    },
    { title: t("auditProcessEngagement.aggregate.workManagement.note"), dataIndex: "note", render: (v) => v ?? "-" },
    { title: t("auditProcessEngagement.aggregate.workManagement.groupCode"), dataIndex: "groupCode", width: 110, render: (v) => v ?? "-" },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16, width: "100%", justifyContent: "space-between" }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {t("auditProcessEngagement.aggregate.workManagement.title", { code: processEngagementCode })}
        </Typography.Title>
        <Button onClick={onBack}>{t("common.back")}</Button>
      </Space>
      <Table<AuditWorkManagementItem>
        rowKey="assignmentId"
        loading={loading}
        dataSource={items}
        columns={columns}
        scroll={{ x: true }}
        pagination={{ pageSize: 50 }}
      />
    </div>
  );
}
