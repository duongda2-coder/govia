import { useCallback, useEffect, useState } from "react";
import { App, Button, Result, Space, Typography } from "antd";
import { StarOutlined } from "@ant-design/icons";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import type { AuditEngagementItem } from "../../../api/auditEngagement";
import { listDgclEngagements } from "../../../api/auditDgcl";
import { useAuth } from "../../../auth/AuthContext";
import { DgclTeamPage } from "./DgclTeamPage";

/** Phan he "Đánh giá chất lượng" (DGCL_CN.xlsx, sheet logic dong 5-7): danh sach CKT giong man hinh "Quản lý đợt kiểm toán"
 * nhung bo cac chuc nang cua man hinh do, chi co 1 nut moi "Đánh giá chất lượng ĐKT". */
export function DgclEngagementPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.DGCL.VIEW");
  const { getSearchColumnProps } = useClientSearchColumn<AuditEngagementItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditEngagementItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditEngagementItem | null>(null);
  const [openFor, setOpenFor] = useState<AuditEngagementItem | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listDgclEngagements());
    } catch {
      message.error(t("auditDgcl.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  if (openFor) {
    return <DgclTeamPage engagement={openFor} onBack={() => setOpenFor(null)} />;
  }

  const columns: TableProps<AuditEngagementItem>["columns"] = [
    { title: t("auditEngagementMonitoring.columns.code"), width: 150, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditEngagementMonitoring.columns.auditObjectUnitName"), dataIndex: "auditObjectUnitName", render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.name"), ...getSearchColumnProps("name", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.fieldworkStartDate"), dataIndex: "fieldworkStartDate", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.fieldworkEndDate"), dataIndex: "fieldworkEndDate", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.decisionNumber"), dataIndex: "decisionNumber", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.teamLeadEmployee"), dataIndex: "teamLeadEmployeeName", render: (v: string | null) => v ?? "-" },
    {
      title: t("auditEngagementMonitoring.columns.status"),
      dataIndex: "status",
      width: 140,
      render: (v: AuditEngagementItem["status"]) => t(`auditEngagement.status.${v}`),
    },
  ];

  return (
    <div>
      <Typography.Title level={4}>{t("auditDgcl.pageTitle")}</Typography.Title>

      <Space style={{ marginBottom: 12 }}>
        <Button type="primary" icon={<StarOutlined />} disabled={!selected} onClick={() => selected && setOpenFor(selected)}>
          {t("auditDgcl.evaluateTeamButton")}
        </Button>
      </Space>

      <CrudTable<AuditEngagementItem>
        tableId="audit.dgcl.engagements"
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onSelectionChange={(_keys, rows) => setSelected(rows[0] ?? null)}
      />
    </div>
  );
}
