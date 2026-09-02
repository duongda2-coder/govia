import { useCallback, useEffect, useState } from "react";
import { App, Button, Card, Radio, Result, Space, Typography } from "antd";
import type { TableProps } from "antd";
import { ApartmentOutlined, EyeOutlined, TeamOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn, useScreenLock } from "@govia/ui-kit";
import {
  deleteAuditEngagement,
  exportAuditEngagements,
  importAuditEngagements,
  listAuditEngagements,
  listAuditObjectUnitOptions,
  listEmployeeOptions,
  type AuditEngagementItem,
  type AuditObjectUnitOption,
  type EmployeeOption,
} from "../../../../api/auditEngagement";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { httpClient } from "../../../../api/client";
import { useAuth } from "../../../../auth/AuthContext";
import { AuditEngagementForm } from "./AuditEngagementForm";
import { AuditEngagementGroupsDrawer } from "./AuditEngagementGroupsDrawer";
import { AuditEngagementAssignmentPage } from "./AuditEngagementAssignmentPage";

const SCREEN_KEY = "audit.plan.engagement";

type Detail = { kind: "view" | "edit"; item: AuditEngagementItem } | null;

/** Man hinh "Khoi tao va quan ly cuoc kiem toan" - gop ca 2 sheet "khoi tao" + "quan ly DKT" cua
 * file "Tao CKT.xlsx" trong 1 route duy nhat, dieu huong bang radio + cac hanh dong tren danh sach. */
export function AuditEngagementPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { user, hasPermission } = useAuth();
  const lock = useScreenLock(SCREEN_KEY, httpClient, user?.userId);
  const canView = hasPermission("AUDIT.PLAN_ENGAGEMENT.VIEW");
  const canCreate = hasPermission("AUDIT.PLAN_ENGAGEMENT.CREATE");
  const canEdit = hasPermission("AUDIT.PLAN_ENGAGEMENT.EDIT");
  const canDelete = hasPermission("AUDIT.PLAN_ENGAGEMENT.DELETE");
  const canExport = hasPermission("AUDIT.PLAN_ENGAGEMENT.EXPORT");
  const canImport = hasPermission("AUDIT.PLAN_ENGAGEMENT.IMPORT");
  const canViewTeam = hasPermission("AUDIT.PLAN_ENGAGEMENT_TEAM.VIEW");
  const { getSearchColumnProps } = useClientSearchColumn<AuditEngagementItem>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [topChoice, setTopChoice] = useState<"create" | "list">("list");
  const [detail, setDetail] = useState<Detail>(null);
  const [assignmentItem, setAssignmentItem] = useState<AuditEngagementItem | null>(null);
  const [groupsFor, setGroupsFor] = useState<AuditEngagementItem | null>(null);

  const [items, setItems] = useState<AuditEngagementItem[]>([]);
  const [auditObjectUnits, setAuditObjectUnits] = useState<AuditObjectUnitOption[]>([]);
  const [employees, setEmployees] = useState<EmployeeOption[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<AuditEngagementItem[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [engagements, units, emps, segments] = await Promise.all([
        listAuditEngagements(),
        listAuditObjectUnitOptions(),
        listEmployeeOptions(),
        listMasterDataItems("BUSINESS_SEGMENT"),
      ]);
      setItems(engagements);
      setAuditObjectUnits(units);
      setEmployees(emps);
      setBusinessSegments(segments);
    } catch {
      message.error(t("auditEngagement.messages.loadError"));
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

  const startEdit = async (item: AuditEngagementItem) => {
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

  const handleSaved = (item: AuditEngagementItem) => {
    load();
    setDetail({ kind: "edit", item });
    setTopChoice("list");
  };

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("auditEngagement.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteAuditEngagement(item.id)));
          message.success(t("common.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("auditEngagement.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<AuditEngagementItem>["columns"] = [
    { title: t("auditEngagement.columns.code"), width: 150, ...getSearchColumnProps("code", searchLabels) },
    { title: t("auditEngagement.columns.name"), ...getSearchColumnProps("name", searchLabels), render: (v: string | null) => v ?? "-" },
    { title: t("auditEngagement.columns.year"), dataIndex: "year", width: 90 },
    { title: t("auditEngagement.columns.unitType"), dataIndex: "unitType", width: 110 },
    { title: t("auditEngagement.columns.auditObjectUnitCode"), dataIndex: "auditObjectUnitCode", width: 110 },
    { title: t("auditEngagement.columns.expectedMonth"), dataIndex: "expectedMonth", width: 90 },
    { title: t("auditEngagement.columns.status"), dataIndex: "status", width: 140, render: (v: AuditEngagementItem["status"]) => t(`auditEngagement.status.${v}`) },
    { title: t("auditEngagement.columns.decisionDate"), dataIndex: "decisionDate", width: 120 },
    { title: t("auditEngagement.columns.teamLeadEmployee"), dataIndex: "teamLeadEmployeeName", render: (v: string | null) => v ?? "-" },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  if (assignmentItem) {
    return <AuditEngagementAssignmentPage engagement={assignmentItem} onBack={() => setAssignmentItem(null)} />;
  }

  if (detail) {
    return (
      <AuditEngagementForm
        mode={detail.kind}
        engagement={detail.item}
        auditObjectUnits={auditObjectUnits}
        employees={employees}
        onCancel={backToList}
        onEdit={canEdit ? () => startEdit(detail.item) : undefined}
        onSaved={handleSaved}
      />
    );
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditEngagement.pageTitle")}</Typography.Title>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Typography.Text strong>{t("auditEngagement.form.parameter")}</Typography.Text>
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
            {canCreate && <Radio value="create">{t("auditEngagement.form.optionCreate")}</Radio>}
            <Radio value="list">{t("auditEngagement.form.optionList")}</Radio>
          </Radio.Group>
        </div>
      </Card>

      {topChoice === "create" ? (
        <AuditEngagementForm
          mode="create"
          engagement={null}
          auditObjectUnits={auditObjectUnits}
          employees={employees}
          onCancel={backToList}
          onSaved={handleSaved}
        />
      ) : (
        <>
          <Space style={{ marginBottom: 12 }}>
            <Button icon={<EyeOutlined />} onClick={openView} disabled={selected.length !== 1}>
              {t("common.view")}
            </Button>
            <Button icon={<TeamOutlined />} disabled={selected.length !== 1 || !canViewTeam} onClick={() => selected[0] && setGroupsFor(selected[0])}>
              {t("auditEngagement.form.groupsButton")}
            </Button>
            <Button icon={<ApartmentOutlined />} disabled={selected.length !== 1 || !canViewTeam} onClick={() => selected[0] && setAssignmentItem(selected[0])}>
              {t("auditEngagement.form.assignmentButton")}
            </Button>
          </Space>
          <CrudTable<AuditEngagementItem>
            tableId={SCREEN_KEY}
            screenLock={{ screenKey: SCREEN_KEY, httpClient, currentUserId: user?.userId }}
            columns={columns}
            dataSource={items}
            rowKey="id"
            loading={loading}
            onDelete={canDelete ? handleDelete : undefined}
            deleteDisabled={selected.length === 0}
            onSelectionChange={(_keys, rows) => setSelected(rows)}
            onExportExcel={canExport ? () => exportAuditEngagements("excel") : undefined}
            onExportWord={canExport ? () => exportAuditEngagements("word") : undefined}
            onImport={
              canImport
                ? async (file) => {
                    const result = await importAuditEngagements(file);
                    await load();
                    return result;
                  }
                : undefined
            }
          />
        </>
      )}

      {groupsFor && (
        <AuditEngagementGroupsDrawer
          open={!!groupsFor}
          engagementId={groupsFor.id}
          engagementCode={groupsFor.code}
          employees={employees}
          businessSegments={businessSegments}
          onClose={() => setGroupsFor(null)}
        />
      )}
    </div>
  );
}
