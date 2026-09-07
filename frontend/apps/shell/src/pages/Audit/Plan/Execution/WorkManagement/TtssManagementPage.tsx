import { useCallback, useEffect, useState } from "react";
import { App, Button, Descriptions, Modal, Result, Select, Space, Tag, Tooltip, Typography } from "antd";
import type { DescriptionsProps, TableProps } from "antd";
import { EyeOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { CrudTable, getApiErrorMessage, useClientSearchColumn } from "@govia/ui-kit";
import {
  approveAuditTtssRecommendations,
  deleteAuditTtssRecord,
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
  const { getSearchColumnProps } = useClientSearchColumn<AuditTtssRecordItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [engagements, setEngagements] = useState<AuditEngagementItem[]>([]);
  const [engagementId, setEngagementId] = useState<string | undefined>(undefined);
  const [items, setItems] = useState<AuditTtssRecordItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditTtssRecordItem[]>([]);
  const [catalogOpen, setCatalogOpen] = useState(false);
  const [linkOpen, setLinkOpen] = useState(false);
  const [detailItem, setDetailItem] = useState<AuditTtssRecordItem | null>(null);

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
        } catch (err) {
          message.error(getApiErrorMessage(err, t("auditTtss.messages.approveError")));
        }
      },
    });
  };

  const handleDownloadTemplate = async () => {
    if (!engagementId) return;
    try {
      await downloadAuditTtssTemplate(engagementId);
    } catch (err) {
      message.error(getApiErrorMessage(err, t("auditTtss.messages.downloadTemplateError")));
    }
  };

  const handleDelete = () => {
    if (!engagementId || selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditTtss.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditTtssRecord(engagementId, item.id)));
          message.success(t("auditTtss.messages.deleteSuccess"));
          setSelected([]);
          await load(engagementId);
        } catch (err) {
          message.error(getApiErrorMessage(err, t("auditTtss.messages.deleteError")));
        }
      },
    });
  };

  const approveDisabled =
    selected.length === 0 ||
    !isTeamLead ||
    selected.some((i) => !i.teamRecommendationId || i.recommendationApprovalStatus === "APPROVED");

  const columns: TableProps<AuditTtssRecordItem>["columns"] = [
    {
      title: "",
      key: "detail",
      width: 44,
      ellipsis: false,
      render: (_: unknown, item: AuditTtssRecordItem) => (
        <Tooltip title={t("auditTtss.detail.buttonTooltip")}>
          <Button type="link" icon={<EyeOutlined />} onClick={() => setDetailItem(item)} />
        </Tooltip>
      ),
    },
    {
      title: t("auditTtss.columns.businessSegment"),
      width: 100,
      ...getSearchColumnProps("businessSegmentCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditTtss.columns.workItemCode"), dataIndex: "workItemCode", width: 110, render: (v) => v ?? "-" },
    {
      title: t("auditTtss.columns.processStepSummaryCode"),
      width: 130,
      ...getSearchColumnProps("processStepSummaryCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditTtss.columns.processStepSummary"), dataIndex: "processStepSummaryName", width: 200, render: (v) => v ?? "-" },
    {
      title: t("auditTtss.columns.processStepDetailCode"),
      width: 130,
      ...getSearchColumnProps("processStepDetailCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("auditTtss.columns.ttssContent"),
      dataIndex: "ttssContent",
      width: 260,
      // Tat native title-tooltip (chu rat nho, kho doc) cua CrudTable, dung Tooltip cua antd (co
      // chu binh thuong, ro rang hon) de hien noi dung day du khi ren chuot vao dong bi cat.
      ellipsis: false,
      render: (v: string | null) => (v ? <Typography.Text ellipsis={{ tooltip: v }}>{v}</Typography.Text> : "-"),
    },
    {
      title: t("auditTtss.columns.findingCode"),
      width: 120,
      ...getSearchColumnProps("findingCode", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditTtss.columns.findingName"), dataIndex: "findingName", width: 220, render: (v) => v ?? "-" },
    {
      title: t("auditTtss.columns.material"),
      dataIndex: "material",
      width: 90,
      render: (v: boolean) => (v ? <Tag color="red">X</Tag> : "-"),
    },
    { title: t("auditTtss.columns.customerName"), dataIndex: "customerName", width: 160, render: (v) => v ?? "-" },
    { title: t("auditTtss.columns.amount"), dataIndex: "amount", width: 130, align: "right", render: (v) => v ?? "-" },
    { title: t("auditTtss.columns.appendix"), dataIndex: "appendix", width: 160, render: (v) => v ?? "-" },
    {
      title: t("auditTtss.columns.recordUsername"),
      width: 140,
      ...getSearchColumnProps("recordUsername", searchLabels),
      render: (v: string | null) => v ?? "-",
    },
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

  // Man hinh chi tiet 1 dong TTSS: liet ke TOAN BO cac truong cua ban ghi (khong chi cac cot dang
  // hien tren bang) theo chieu doc (column: 1) de hien day du ky tu cua cac truong noi dung dai
  // (ttssContent, transactionContent...) ma bang bi cat bot vi gioi han do rong cot.
  const detailItems: DescriptionsProps["items"] = detailItem
    ? [
        { key: "recordUsername", label: t("auditTtss.columns.recordUsername"), children: detailItem.recordUsername ?? "-" },
        { key: "businessSegmentCode", label: t("auditTtss.columns.businessSegment"), children: detailItem.businessSegmentCode ?? "-" },
        { key: "workItemCode", label: t("auditTtss.columns.workItemCode"), children: detailItem.workItemCode ?? "-" },
        { key: "processStepSummaryCode", label: t("auditTtss.columns.processStepSummaryCode"), children: detailItem.processStepSummaryCode ?? "-" },
        { key: "processStepSummaryName", label: t("auditTtss.columns.processStepSummary"), children: detailItem.processStepSummaryName ?? "-" },
        { key: "processStepDetailCode", label: t("auditTtss.columns.processStepDetailCode"), children: detailItem.processStepDetailCode ?? "-" },
        { key: "findingCode", label: t("auditTtss.columns.findingCode"), children: detailItem.findingCode ?? "-" },
        { key: "findingName", label: t("auditTtss.columns.findingName"), children: detailItem.findingName ?? "-" },
        { key: "material", label: t("auditTtss.columns.material"), children: detailItem.material ? <Tag color="red">X</Tag> : "-" },
        { key: "ttssContent", label: t("auditTtss.columns.ttssContent"), children: detailItem.ttssContent ?? "-" },
        { key: "referenceNumber", label: t("auditTtss.columns.referenceNumber"), children: detailItem.referenceNumber ?? "-" },
        { key: "referenceNumber2", label: t("auditTtss.columns.referenceNumber2"), children: detailItem.referenceNumber2 ?? "-" },
        { key: "customerCode", label: t("auditTtss.columns.customerCode"), children: detailItem.customerCode ?? "-" },
        { key: "customerName", label: t("auditTtss.columns.customerName"), children: detailItem.customerName ?? "-" },
        { key: "amount", label: t("auditTtss.columns.amount"), children: detailItem.amount ?? "-" },
        { key: "performingUser", label: t("auditTtss.columns.performingUser"), children: detailItem.performingUser ?? "-" },
        { key: "transactionContent", label: t("auditTtss.columns.transactionContent"), children: detailItem.transactionContent ?? "-" },
        { key: "uploaderRecommendationCode", label: t("auditTtss.columns.uploaderRecommendationCode"), children: detailItem.uploaderRecommendationCode ?? "-" },
        { key: "uploaderRecommendationName", label: t("auditTtss.columns.uploaderRecommendationName"), children: detailItem.uploaderRecommendationName ?? "-" },
        { key: "exceptionDate", label: t("auditTtss.columns.exceptionDate"), children: detailItem.exceptionDate ?? "-" },
        { key: "ttssPerformerName", label: t("auditTtss.columns.ttssPerformerName"), children: detailItem.ttssPerformerName ?? "-" },
        { key: "relatedStaff", label: t("auditTtss.columns.relatedStaff"), children: detailItem.relatedStaff ?? "-" },
        { key: "approverName", label: t("auditTtss.columns.approverName"), children: detailItem.approverName ?? "-" },
        { key: "controllerName", label: t("auditTtss.columns.controllerName"), children: detailItem.controllerName ?? "-" },
        { key: "appendix", label: t("auditTtss.columns.appendix"), children: detailItem.appendix ?? "-" },
        { key: "teamRecommendationCode", label: t("auditTtss.columns.teamRecommendationCode"), children: detailItem.teamRecommendationCode ?? "-" },
        { key: "teamRecommendationContent", label: t("auditTtss.columns.teamRecommendationContent"), children: detailItem.teamRecommendationContent ?? "-" },
        {
          key: "recommendationApprovalStatus",
          label: t("auditTtss.columns.recommendationApprovalStatus"),
          children: detailItem.recommendationApprovalStatus ? (
            <Tag color={detailItem.recommendationApprovalStatus === "APPROVED" ? "success" : detailItem.recommendationApprovalStatus === "PENDING" ? "processing" : "default"}>
              {t(`auditWorkManagement.approvalStatus.${detailItem.recommendationApprovalStatus}`)}
            </Tag>
          ) : (
            "-"
          ),
        },
        { key: "recommendationApprovedBy", label: t("auditTtss.columns.recommendationApprovedBy"), children: detailItem.recommendationApprovedBy ?? "-" },
        { key: "recommendationApprovedAt", label: t("auditTtss.columns.recommendationApprovedAt"), children: detailItem.recommendationApprovedAt ?? "-" },
      ]
    : [];

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
        onDelete={canEdit && engagementId ? handleDelete : undefined}
        deleteDisabled={selected.length === 0}
        onDownloadTemplate={engagementId ? handleDownloadTemplate : undefined}
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

      <Modal title={t("auditTtss.detail.title")} open={!!detailItem} onCancel={() => setDetailItem(null)} footer={null} width={720} destroyOnClose>
        <Descriptions column={1} bordered size="small" items={detailItems} />
      </Modal>

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
