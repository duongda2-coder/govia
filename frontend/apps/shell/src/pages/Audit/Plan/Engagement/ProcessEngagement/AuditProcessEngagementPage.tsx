import { useCallback, useEffect, useState } from "react";
import { App, Button, Card, Radio, Result, Space, Typography } from "antd";
import type { TableProps } from "antd";
import { EyeOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn, useScreenLock } from "@govia/ui-kit";
import {
  deleteAuditProcessEngagement,
  exportAuditProcessEngagements,
  importAuditProcessEngagements,
  listAuditProcessEngagements,
  listTeamLeadOptions,
  type AuditProcessEngagementItem,
  type TeamLeadOption,
} from "../../../../../api/auditProcessEngagement";
import { listMasterDataItems, type MasterDataItem } from "../../../../../api/auditMasterData";
import { listAuditObjectUnitOptions, listEmployeeOptions, type AuditObjectUnitOption, type EmployeeOption } from "../../../../../api/auditEngagement";
import { httpClient } from "../../../../../api/client";
import { useAuth } from "../../../../../auth/AuthContext";
import { AuditProcessEngagementForm } from "./AuditProcessEngagementForm";
import { AuditEngagementForm } from "../AuditEngagementForm";
import { ProcessEngagementChildrenPage } from "./ProcessEngagementChildrenPage";
import { ProcessEngagementWorkManagementPage } from "./ProcessEngagementWorkManagementPage";
import { ProcessEngagementTtssPage } from "./ProcessEngagementTtssPage";
import { ProcessEngagementRecommendationsPage } from "./ProcessEngagementRecommendationsPage";
import { ProcessEngagementReportFilesDrawer } from "./ProcessEngagementReportFilesDrawer";

const SCREEN_KEY = "audit.plan.engagement.process";

type Detail =
  | { kind: "view" | "edit"; item: AuditProcessEngagementItem }
  | { kind: "createChild"; item: AuditProcessEngagementItem }
  | { kind: "children" | "workManagement" | "ttss" | "recommendations"; item: AuditProcessEngagementItem }
  | null;

/** Man hinh "Tao CKT quy trinh" (sheet "man hinh tao CKT quy trinh" cua file "Tao CKT (3).xlsx") -
 * sibling nhe hon cua AuditEngagementPage (CKT theo don vi/chi nhanh): cung bo cuc Radio Tao moi/
 * Danh sach + form full-page + dinh kem, nhung gan CKT truc tiep voi 1 "Nghiep vu" (BUSINESS_SEGMENT)
 * thay vi chon Doi tuong kiem toan (don vi/chi nhanh). Tu "Tao CKT (4).xlsx" sheet "QL CKT quy
 * trinh": man hinh nay cung la "CKT quy trinh" CHA cua nhieu CKT con (AuditEngagement), voi 6 chuc
 * nang tren toolbar khi chon 1 dong: Tao CKT con, File BC khac, Quan ly cong viec/TTSS/KN (tong hop
 * doc-only tren tat ca CKT con), Chi tiet doan. */
export function AuditProcessEngagementPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { user, hasPermission } = useAuth();
  const lock = useScreenLock(SCREEN_KEY, httpClient, user?.userId);
  const canView = hasPermission("AUDIT.PLAN_ENGAGEMENT_PROCESS.VIEW");
  const canCreate = hasPermission("AUDIT.PLAN_ENGAGEMENT_PROCESS.CREATE");
  const canEdit = hasPermission("AUDIT.PLAN_ENGAGEMENT_PROCESS.EDIT");
  const canDelete = hasPermission("AUDIT.PLAN_ENGAGEMENT_PROCESS.DELETE");
  const canExport = hasPermission("AUDIT.PLAN_ENGAGEMENT_PROCESS.EXPORT");
  const canImport = hasPermission("AUDIT.PLAN_ENGAGEMENT_PROCESS.IMPORT");
  const canCreateChild = hasPermission("AUDIT.PLAN_ENGAGEMENT.CREATE");
  const { getSearchColumnProps } = useClientSearchColumn<AuditProcessEngagementItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [topChoice, setTopChoice] = useState<"create" | "list">("list");
  const [detail, setDetail] = useState<Detail>(null);
  const [reportFilesFor, setReportFilesFor] = useState<AuditProcessEngagementItem | null>(null);

  const [items, setItems] = useState<AuditProcessEngagementItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [teamLeads, setTeamLeads] = useState<TeamLeadOption[]>([]);
  const [auditObjectUnits, setAuditObjectUnits] = useState<AuditObjectUnitOption[]>([]);
  const [employees, setEmployees] = useState<EmployeeOption[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditProcessEngagementItem[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [engagements, segments, leads, units, employeeOptions] = await Promise.all([
        listAuditProcessEngagements(),
        listMasterDataItems("BUSINESS_SEGMENT"),
        listTeamLeadOptions(),
        listAuditObjectUnitOptions(),
        listEmployeeOptions(),
      ]);
      setItems(engagements);
      setBusinessSegments(segments);
      setTeamLeads(leads);
      setAuditObjectUnits(units);
      setEmployees(employeeOptions);
    } catch {
      message.error(t("auditProcessEngagement.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  const startCreate = async () => {
    const { ok, status } = await lock.acquire();
    if (!ok) {
      message.warning(t("common.screenLockedBy", { name: status.lockedByName, time: new Date(status.lockedAt ?? "").toLocaleString() }));
      setTopChoice("list");
      return;
    }
    setTopChoice("create");
  };

  const startEdit = async (item: AuditProcessEngagementItem) => {
    const { ok, status } = await lock.acquire();
    if (!ok) {
      message.warning(t("common.screenLockedBy", { name: status.lockedByName, time: new Date(status.lockedAt ?? "").toLocaleString() }));
      return;
    }
    setDetail({ kind: "edit", item });
  };

  const openView = () => {
    const target = selected[0];
    if (!target) return;
    setDetail({ kind: "view", item: target });
  };

  const backToList = async () => {
    await lock.release();
    setDetail(null);
    setTopChoice("list");
    setSelected([]);
    load();
  };

  const handleSaved = (item: AuditProcessEngagementItem) => {
    load();
    setDetail({ kind: "edit", item });
    setTopChoice("list");
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditProcessEngagement.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditProcessEngagement(item.id)));
          message.success(t("common.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditProcessEngagement.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditProcessEngagementItem>["columns"] = [
    { title: t("auditProcessEngagement.columns.code"), width: 150, ...getSearchColumnProps("code", searchLabels) },
    {
      title: t("auditProcessEngagement.columns.objectType"),
      dataIndex: "objectType",
      width: 110,
      render: (v: AuditProcessEngagementItem["objectType"]) => t(`auditProcessEngagement.objectType.${v}`),
    },
    { title: t("auditProcessEngagement.columns.businessSegment"), width: 160, ...getSearchColumnProps("businessSegmentCode", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditProcessEngagement.columns.name"), ...getSearchColumnProps("name", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditProcessEngagement.columns.year"), dataIndex: "year", width: 90, sorter: (a, b) => a.year - b.year },
    { title: t("auditProcessEngagement.columns.expectedMonth"), dataIndex: "expectedMonth", width: 130, sorter: (a, b) => a.expectedMonth - b.expectedMonth },
    { title: t("auditProcessEngagement.columns.decisionDate"), width: 120, ...getSearchColumnProps("decisionDate", searchLabels) },
    { title: t("auditProcessEngagement.columns.decisionNumber"), width: 150, ...getSearchColumnProps("decisionNumber", searchLabels) },
    { title: t("auditProcessEngagement.columns.childCount"), dataIndex: "childCount", width: 110 },
    { title: t("auditProcessEngagement.columns.memberCount"), dataIndex: "memberCount", width: 100 },
    { title: t("auditProcessEngagement.columns.teamLeadEmployee"), ...getSearchColumnProps("teamLeadEmployeeName", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditProcessEngagement.columns.teamLeadUsername"), dataIndex: "teamLeadUsername", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditProcessEngagement.columns.createdBy"), dataIndex: "createdBy", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditProcessEngagement.columns.workSetCode"), width: 150, ...getSearchColumnProps("workSetCode", searchLabels), render: (v: string | null) => v ?? "-" },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  if (detail?.kind === "view" || detail?.kind === "edit") {
    return (
      <AuditProcessEngagementForm
        mode={detail.kind}
        engagement={detail.item}
        businessSegments={businessSegments}
        teamLeads={teamLeads}
        onCancel={backToList}
        onEdit={canEdit ? () => startEdit(detail.item) : undefined}
        onSaved={handleSaved}
      />
    );
  }

  if (detail?.kind === "createChild") {
    const parent = detail.item;
    return (
      <AuditEngagementForm
        mode="create"
        engagement={null}
        auditObjectUnits={auditObjectUnits}
        employees={employees}
        onCancel={() => setDetail(null)}
        onSaved={() => {
          message.success(t("auditProcessEngagement.messages.childCreateSuccess"));
          setDetail(null);
          load();
        }}
        prefillFromProcessEngagement={{
          processEngagementId: parent.id,
          processEngagementCode: parent.code,
          year: parent.year,
          expectedMonth: parent.expectedMonth,
          decisionDate: parent.decisionDate,
          teamLeadEmployeeId: parent.teamLeadEmployeeId,
          decisionNumber: parent.decisionNumber,
          name: parent.name,
        }}
      />
    );
  }

  if (detail?.kind === "children") {
    return <ProcessEngagementChildrenPage processEngagementId={detail.item.id} processEngagementCode={detail.item.code} onBack={() => setDetail(null)} />;
  }

  if (detail?.kind === "workManagement") {
    return (
      <ProcessEngagementWorkManagementPage processEngagementId={detail.item.id} processEngagementCode={detail.item.code} onBack={() => setDetail(null)} />
    );
  }

  if (detail?.kind === "ttss") {
    return <ProcessEngagementTtssPage processEngagementId={detail.item.id} processEngagementCode={detail.item.code} onBack={() => setDetail(null)} />;
  }

  if (detail?.kind === "recommendations") {
    return (
      <ProcessEngagementRecommendationsPage processEngagementId={detail.item.id} processEngagementCode={detail.item.code} onBack={() => setDetail(null)} />
    );
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditProcessEngagement.pageTitle")}</Typography.Title>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Typography.Text strong>{t("auditProcessEngagement.form.parameter")}</Typography.Text>
        <div style={{ marginTop: 8 }}>
          <Radio.Group
            value={topChoice}
            onChange={async (e) => {
              const value = e.target.value as "create" | "list";
              if (value === "create") {
                await startCreate();
              } else {
                await lock.release();
                setTopChoice("list");
              }
            }}
          >
            {canCreate && <Radio value="create">{t("auditProcessEngagement.form.optionCreate")}</Radio>}
            <Radio value="list">{t("auditProcessEngagement.form.optionList")}</Radio>
          </Radio.Group>
        </div>
      </Card>

      {topChoice === "create" ? (
        <AuditProcessEngagementForm
          mode="create"
          engagement={null}
          businessSegments={businessSegments}
          teamLeads={teamLeads}
          onCancel={backToList}
          onSaved={handleSaved}
        />
      ) : (
        <>
          <Space style={{ marginBottom: 12 }} wrap>
            <Button icon={<EyeOutlined />} onClick={openView} disabled={selected.length !== 1}>
              {t("common.view")}
            </Button>
            {canCreateChild && (
              <Button disabled={selected.length !== 1} onClick={() => setDetail({ kind: "createChild", item: selected[0] })}>
                {t("auditProcessEngagement.aggregate.createChild")}
              </Button>
            )}
            <Button disabled={selected.length !== 1} onClick={() => setReportFilesFor(selected[0])}>
              {t("auditProcessEngagement.aggregate.reportFiles")}
            </Button>
            <Button disabled={selected.length !== 1} onClick={() => setDetail({ kind: "workManagement", item: selected[0] })}>
              {t("auditProcessEngagement.aggregate.workManagement.button")}
            </Button>
            <Button disabled={selected.length !== 1} onClick={() => setDetail({ kind: "ttss", item: selected[0] })}>
              {t("auditProcessEngagement.aggregate.ttss.button")}
            </Button>
            <Button disabled={selected.length !== 1} onClick={() => setDetail({ kind: "recommendations", item: selected[0] })}>
              {t("auditProcessEngagement.aggregate.recommendations.button")}
            </Button>
            <Button disabled={selected.length !== 1} onClick={() => setDetail({ kind: "children", item: selected[0] })}>
              {t("auditProcessEngagement.aggregate.children.button")}
            </Button>
          </Space>
          <CrudTable<AuditProcessEngagementItem>
            tableId={SCREEN_KEY}
            screenLock={{ screenKey: SCREEN_KEY, httpClient, currentUserId: user?.userId }}
            columns={columns}
            dataSource={items}
            rowKey="id"
            loading={loading}
            onDelete={canDelete ? handleDelete : undefined}
            deleteDisabled={selected.length === 0}
            onSelectionChange={(_keys, rows) => setSelected(rows)}
            onExportExcel={canExport ? () => exportAuditProcessEngagements("excel") : undefined}
            onExportWord={canExport ? () => exportAuditProcessEngagements("word") : undefined}
            onImport={
              canImport
                ? async (file) => {
                    const result = await importAuditProcessEngagements(file);
                    await load();
                    return result;
                  }
                : undefined
            }
          />
          <ProcessEngagementReportFilesDrawer
            open={reportFilesFor != null}
            processEngagementId={reportFilesFor?.id ?? null}
            onClose={() => setReportFilesFor(null)}
          />
        </>
      )}
    </div>
  );
}
