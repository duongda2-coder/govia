import { useCallback, useEffect, useState } from "react";
import { App, Button, Result, Select, Space, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable } from "@govia/ui-kit";
import {
  approveAuditTtssRecommendations,
  downloadAuditTtssTemplate,
  listAuditTtssRecords,
  uploadAuditTtssFile,
  type AuditTtssRecordItem,
} from "../../../../../api/auditTtss";
import { listAuditEngagements, type AuditEngagementItem } from "../../../../../api/auditEngagement";
import { useAuth } from "../../../../../auth/AuthContext";
import { RecommendationCatalogModal } from "./RecommendationCatalogModal";
import { LinkRecommendationModal } from "./LinkRecommendationModal";

/** "Màn hình Quản lý TTSS & Kiến nghị" (lựa chọn "C" trong Quản lý công việc) - sheet "Quản lý
 * công việc" trong Tạo CKT (1).xlsx, mục C. */
export function TtssManagementPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { user, hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.TTSS.VIEW");
  const canEdit = hasPermission("AUDIT.TTSS.EDIT");
  const canApprove = hasPermission("AUDIT.TTSS.APPROVE");

  const [engagements, setEngagements] = useState<AuditEngagementItem[]>([]);
  const [engagementId, setEngagementId] = useState<string | undefined>(undefined);
  const [items, setItems] = useState<AuditTtssRecordItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditTtssRecordItem[]>([]);
  const [catalogOpen, setCatalogOpen] = useState(false);
  const [linkOpen, setLinkOpen] = useState(false);

  useEffect(() => {
    if (!canView) return;
    listAuditEngagements().then(setEngagements).catch(() => setEngagements([]));
  }, [canView]);

  const load = useCallback(
    async (selectedEngagementId: string) => {
      setLoading(true);
      try {
        setItems(await listAuditTtssRecords(selectedEngagementId));
      } catch {
        message.error(t("auditTtss.messages.loadError"));
      } finally {
        setLoading(false);
      }
    },
    [message, t],
  );

  useEffect(() => {
    if (canView && engagementId) load(engagementId);
    if (!engagementId) setItems([]);
  }, [canView, engagementId, load]);

  const currentEngagement = engagements.find((e) => e.id === engagementId);
  const isTeamLead = !!user?.employeeCode && !!currentEngagement && user.employeeCode === currentEngagement.teamLeadEmployeeCode;

  const handleApprove = () => {
    if (!engagementId || selected.length === 0) return;
    modal.confirm({
      title: t("auditTtss.approveConfirmTitle", { count: selected.length }),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      onOk: async () => {
        try {
          await approveAuditTtssRecommendations(
            engagementId,
            selected.map((i) => i.id),
          );
          message.success(t("auditTtss.messages.approveSuccess"));
          setSelected([]);
          await load(engagementId);
        } catch {
          message.error(t("auditTtss.messages.approveError"));
        }
      },
    });
  };

  const approveDisabled =
    selected.length === 0 ||
    !isTeamLead ||
    selected.some((i) => !i.teamRecommendationId || i.recommendationApprovalStatus === "APPROVED");

  const columns: TableProps<AuditTtssRecordItem>["columns"] = [
    { title: t("auditTtss.columns.businessSegment"), dataIndex: "businessSegmentCode", width: 100, render: (v) => v ?? "-" },
    { title: t("auditTtss.columns.workItemCode"), dataIndex: "workItemCode", width: 110, render: (v) => v ?? "-" },
    { title: t("auditTtss.columns.processStepSummary"), dataIndex: "processStepSummaryName", width: 200, render: (v) => v ?? "-" },
    { title: t("auditTtss.columns.ttssContent"), dataIndex: "ttssContent", width: 260, render: (v) => v ?? "-" },
    { title: t("auditTtss.columns.findingCode"), dataIndex: "findingCode", width: 120, render: (v) => v ?? "-" },
    { title: t("auditTtss.columns.findingName"), dataIndex: "findingName", width: 220, render: (v) => v ?? "-" },
    {
      title: t("auditTtss.columns.material"),
      dataIndex: "material",
      width: 90,
      render: (v: boolean) => (v ? <Tag color="red">X</Tag> : "-"),
    },
    { title: t("auditTtss.columns.customerName"), dataIndex: "customerName", width: 160, render: (v) => v ?? "-" },
    { title: t("auditTtss.columns.amount"), dataIndex: "amount", width: 130, align: "right", render: (v) => v ?? "-" },
    {
      title: t("auditTtss.columns.teamRecommendation"),
      width: 200,
      render: (_: unknown, item: AuditTtssRecordItem) =>
        item.teamRecommendationCode ? `${item.teamRecommendationCode} - ${item.teamRecommendationContent ?? ""}` : "-",
    },
    {
      title: t("auditTtss.columns.recommendationApprovalStatus"),
      width: 130,
      render: (_: unknown, item: AuditTtssRecordItem) => (
        <Tag color={item.recommendationApprovalStatus === "APPROVED" ? "success" : item.recommendationApprovalStatus === "PENDING" ? "processing" : "default"}>
          {item.recommendationApprovalStatus ? t(`auditWorkManagement.approvalStatus.${item.recommendationApprovalStatus}`) : "-"}
        </Tag>
      ),
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditTtss.title")}</Typography.Title>
      <Space style={{ marginBottom: 16 }}>
        <Typography.Text>{t("auditPlanExecution.engagementFilter")}</Typography.Text>
        <Select
          style={{ width: 220 }}
          showSearch
          optionFilterProp="label"
          placeholder={t("auditPlanExecution.selectEngagement")}
          options={engagements.map((e) => ({ value: e.id, label: e.code }))}
          value={engagementId}
          onChange={setEngagementId}
          allowClear
        />
      </Space>
      <Space style={{ marginBottom: 16 }}>
        <Button disabled={!engagementId} onClick={() => setCatalogOpen(true)}>
          {t("auditRecommendation.title")}
        </Button>
        <Button disabled={!engagementId || !canEdit || selected.length === 0} onClick={() => setLinkOpen(true)}>
          {t("auditTtss.linkRecommendationButton")}
        </Button>
      </Space>
      <CrudTable<AuditTtssRecordItem>
        tableId="audit.plan.execution.ttss"
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onDownloadTemplate={engagementId ? () => downloadAuditTtssTemplate(engagementId) : undefined}
        onImport={
          canEdit && engagementId
            ? async (file) => {
                const result = await uploadAuditTtssFile(engagementId, file);
                await load(engagementId);
                return result;
              }
            : undefined
        }
        onApprove={canApprove && engagementId ? handleApprove : undefined}
        approveDisabled={approveDisabled}
      />

      <RecommendationCatalogModal open={catalogOpen} engagementId={engagementId ?? null} onClose={() => setCatalogOpen(false)} />
      <LinkRecommendationModal
        open={linkOpen}
        engagementId={engagementId ?? null}
        ttssRecordIds={selected.map((i) => i.id)}
        onClose={() => setLinkOpen(false)}
        onLinked={() => {
          setLinkOpen(false);
          setSelected([]);
          if (engagementId) load(engagementId);
        }}
      />
    </div>
  );
}
