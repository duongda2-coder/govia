import { useCallback, useEffect, useState } from "react";
import { App, Button, Form, Input, Modal, Result, Space, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import {
  listAuditEngagementMonitoring,
  updateAuditEngagementTeamRanking,
  type AuditEngagementMonitoringItem,
} from "../../../../api/auditEngagementMonitoring";
import type { AuditEngagementItem } from "../../../../api/auditEngagement";
import { useAuth } from "../../../../auth/AuthContext";
import { AuditEngagementTeamDetailPage } from "./AuditEngagementTeamDetailPage";

const SCREEN_KEY = "audit.plan.engagement.monitoring";

interface RankingFormValues {
  teamRanking: string | null;
}

/** Man hinh "Quản lý đợt kiểm toán" (nguon: "Tao CKT (2).xlsx", sheet cung ten) - dashboard giam
 * sat tien do doan kiem toan theo tung CKT, khac voi man hinh CRUD "Khoi tao va quan ly cuoc kiem
 * toan": chi xem so lieu tong hop + cap nhat "Xếp loại đoàn", khong tao/sua/xoa CKT o day. Phan
 * quyen theo dac ta ("Truong/Pho KTNB, BKS xem tat ca; nhan vien phong ban chi xem CKT da tham
 * gia") duoc ap dung o backend (AuditEngagementMonitoringService), FE chi hien thi ket qua da loc. */
export function AuditEngagementMonitoringPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.PLAN_ENGAGEMENT.VIEW");
  const canEdit = hasPermission("AUDIT.PLAN_ENGAGEMENT.EDIT");
  const { getSearchColumnProps } = useClientSearchColumn<AuditEngagementMonitoringItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [items, setItems] = useState<AuditEngagementMonitoringItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditEngagementMonitoringItem[]>([]);
  const [detailFor, setDetailFor] = useState<AuditEngagementMonitoringItem | null>(null);
  const [rankingOpen, setRankingOpen] = useState(false);
  const [rankingForm] = Form.useForm<RankingFormValues>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await listAuditEngagementMonitoring());
    } catch {
      message.error(t("auditEngagementMonitoring.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  const openRanking = () => {
    const target = selected[0];
    if (!target) return;
    rankingForm.setFieldsValue({ teamRanking: target.teamRanking });
    setRankingOpen(true);
  };

  const submitRanking = async () => {
    const target = selected[0];
    if (!target) return;
    try {
      const values = await rankingForm.validateFields();
      await updateAuditEngagementTeamRanking(target.id, values.teamRanking ?? null);
      message.success(t("auditEngagementMonitoring.messages.updateRankingSuccess"));
      setRankingOpen(false);
      await load();
    } catch (err) {
      if (err instanceof Error) message.error(t("auditEngagementMonitoring.messages.updateRankingError"));
    }
  };

  const columns: TableProps<AuditEngagementMonitoringItem>["columns"] = [
    { title: t("auditEngagementMonitoring.columns.code"), width: 150, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditEngagementMonitoring.columns.auditObjectUnitName"), dataIndex: "auditObjectUnitName", render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.name"), ...getSearchColumnProps("name", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.fieldworkStartDate"), dataIndex: "fieldworkStartDate", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.fieldworkEndDate"), dataIndex: "fieldworkEndDate", width: 120, render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.decisionNumber"), dataIndex: "decisionNumber", width: 130 },
    { title: t("auditEngagementMonitoring.columns.teamLeadEmployee"), dataIndex: "teamLeadEmployeeName", render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagementMonitoring.columns.memberCount"), dataIndex: "memberCount", width: 100 },
    { title: t("auditEngagementMonitoring.columns.businessSegmentCount"), dataIndex: "businessSegmentCount", width: 100 },
    { title: t("auditEngagementMonitoring.columns.totalFindings"), dataIndex: "totalFindings", width: 110 },
    { title: t("auditEngagementMonitoring.columns.totalMaterialFindings"), dataIndex: "totalMaterialFindings", width: 110 },
    { title: t("auditEngagementMonitoring.columns.recommendationCount"), dataIndex: "recommendationCount", width: 110 },
    {
      title: t("auditEngagementMonitoring.columns.status"),
      dataIndex: "status",
      width: 140,
      render: (v: AuditEngagementItem["status"]) => t(`auditEngagement.status.${v}`),
    },
    { title: t("auditEngagementMonitoring.columns.teamRanking"), dataIndex: "teamRanking", width: 130, render: (v: string | null) => v ?? "-" },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  if (detailFor) {
    return <AuditEngagementTeamDetailPage engagement={detailFor} onBack={() => setDetailFor(null)} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditEngagementMonitoring.pageTitle")}</Typography.Title>

      <Space style={{ marginBottom: 12 }}>
        <Button disabled={selected.length !== 1} onClick={() => selected[0] && setDetailFor(selected[0])}>
          {t("auditEngagementMonitoring.teamDetailButton")}
        </Button>
        {canEdit && (
          <Button disabled={selected.length !== 1} onClick={openRanking}>
            {t("auditEngagementMonitoring.updateRankingButton")}
          </Button>
        )}
      </Space>

      <CrudTable<AuditEngagementMonitoringItem>
        tableId={SCREEN_KEY}
        columns={columns}
        dataSource={items}
        rowKey="id"
        loading={loading}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
      />

      <Modal
        title={t("auditEngagementMonitoring.updateRankingTitle", { code: selected[0]?.code ?? "" })}
        open={rankingOpen}
        onCancel={() => setRankingOpen(false)}
        onOk={submitRanking}
        destroyOnClose
      >
        <Form<RankingFormValues> form={rankingForm} layout="vertical">
          <Form.Item name="teamRanking" label={t("auditEngagementMonitoring.columns.teamRanking")}>
            <Input maxLength={50} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
