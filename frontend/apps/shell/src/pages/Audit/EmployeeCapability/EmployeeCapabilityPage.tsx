import { useCallback, useEffect, useState } from "react";
import { App, Button, Checkbox, Result, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import { StandardToolbar } from "@govia/ui-kit";
import {
  approveAuditEmployeeCapability,
  bulkUpdateAuditEmployeeCapabilities,
  exportAuditEmployeeCapabilities,
  importAuditEmployeeCapabilities,
  listAuditEmployeeCapabilities,
  type AuditEmployeeCapabilityItem,
  type CapabilityFlagKey,
} from "../../../api/auditEmployeeCapability";
import { useAuth } from "../../../auth/AuthContext";

const CAPABILITY_COLUMNS: { key: CapabilityFlagKey; labelKey: string }[] = [
  { key: "theCapable", labelKey: "auditEmployeeCapability.columns.theCapable" },
  { key: "qtdhCapable", labelKey: "auditEmployeeCapability.columns.qtdhCapable" },
  { key: "hdvCapable", labelKey: "auditEmployeeCapability.columns.hdvCapable" },
  { key: "tcktCapable", labelKey: "auditEmployeeCapability.columns.tcktCapable" },
  { key: "cnttCapable", labelKey: "auditEmployeeCapability.columns.cnttCapable" },
  { key: "ttkqCapable", labelKey: "auditEmployeeCapability.columns.ttkqCapable" },
  { key: "pcrtCapable", labelKey: "auditEmployeeCapability.columns.pcrtCapable" },
  { key: "ttqtCapable", labelKey: "auditEmployeeCapability.columns.ttqtCapable" },
  { key: "xdcbCapable", labelKey: "auditEmployeeCapability.columns.xdcbCapable" },
  { key: "tdCapable", labelKey: "auditEmployeeCapability.columns.tdCapable" },
  { key: "truongDoanCapable", labelKey: "auditEmployeeCapability.columns.truongDoanCapable" },
  { key: "truongNhomCapable", labelKey: "auditEmployeeCapability.columns.truongNhomCapable" },
  { key: "toGiamSatCapable", labelKey: "auditEmployeeCapability.columns.toGiamSatCapable" },
  { key: "dgclCapable", labelKey: "auditEmployeeCapability.columns.dgclCapable" },
];

/**
 * Man hinh "Khai bao kha nang dam nhan linh vuc cua nhan vien" (sheet ZTC_KNDN) - trong nhom
 * "Danh muc" cua module Kiem toan noi bo. KHONG dung CrudTable: danh sach luon la TAT CA nhan vien
 * (khong them/xoa dong thu cong), NSD tich chon 14 co truc tiep tren luoi roi bam "Luu thay doi"
 * 1 lan (giong RolePermissionsDrawer) thay vi mo modal tung dong; "Phe duyet" la nut rieng cho tung
 * dong, bam la luu ngay (khong gom vao "Luu thay doi").
 */
export function EmployeeCapabilityPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.EMPLOYEE_CAPABILITY.VIEW");
  const canEdit = hasPermission("AUDIT.EMPLOYEE_CAPABILITY.EDIT");
  const canExport = hasPermission("AUDIT.EMPLOYEE_CAPABILITY.EXPORT");
  const canImport = hasPermission("AUDIT.EMPLOYEE_CAPABILITY.IMPORT");

  const [rows, setRows] = useState<AuditEmployeeCapabilityItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [approvingId, setApprovingId] = useState<string | null>(null);
  const [dirty, setDirty] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const list = await listAuditEmployeeCapabilities();
      setRows(list);
      setDirty(false);
    } catch {
      message.error(t("auditEmployeeCapability.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  const toggle = (employeeId: string, key: CapabilityFlagKey) => {
    setRows((prev) => prev.map((r) => (r.employeeId === employeeId ? { ...r, [key]: !r[key] } : r)));
    setDirty(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const updated = await bulkUpdateAuditEmployeeCapabilities(
        rows.map((r) => ({
          employeeId: r.employeeId,
          theCapable: r.theCapable,
          qtdhCapable: r.qtdhCapable,
          hdvCapable: r.hdvCapable,
          tcktCapable: r.tcktCapable,
          cnttCapable: r.cnttCapable,
          ttkqCapable: r.ttkqCapable,
          pcrtCapable: r.pcrtCapable,
          ttqtCapable: r.ttqtCapable,
          xdcbCapable: r.xdcbCapable,
          tdCapable: r.tdCapable,
          truongDoanCapable: r.truongDoanCapable,
          truongNhomCapable: r.truongNhomCapable,
          toGiamSatCapable: r.toGiamSatCapable,
          dgclCapable: r.dgclCapable,
        })),
      );
      setRows(updated);
      setDirty(false);
      message.success(t("auditEmployeeCapability.messages.saveSuccess"));
    } catch {
      message.error(t("auditEmployeeCapability.messages.saveError"));
    } finally {
      setSaving(false);
    }
  };

  const handleApprove = async (employeeId: string) => {
    setApprovingId(employeeId);
    try {
      const updated = await approveAuditEmployeeCapability(employeeId);
      setRows((prev) => prev.map((r) => (r.employeeId === employeeId ? updated : r)));
      message.success(t("auditEmployeeCapability.messages.approveSuccess"));
    } catch {
      message.error(t("auditEmployeeCapability.messages.approveError"));
    } finally {
      setApprovingId(null);
    }
  };

  const formatDate = (v: string | null) => (v ? dayjs(v).format("DD/MM/YYYY HH:mm") : "-");

  const columns: TableProps<AuditEmployeeCapabilityItem>["columns"] = [
    {
      title: t("auditEmployeeCapability.columns.fullName"),
      dataIndex: "fullName",
      fixed: "left",
      width: 200,
    },
    {
      title: t("auditEmployeeCapability.columns.username"),
      dataIndex: "username",
      width: 130,
      render: (v: string | null) => v ?? "-",
    },
    ...CAPABILITY_COLUMNS.map((col) => ({
      title: t(col.labelKey),
      key: col.key,
      width: 70,
      align: "center" as const,
      render: (_: unknown, row: AuditEmployeeCapabilityItem) => (
        <Checkbox checked={row[col.key]} disabled={!canEdit} onChange={() => toggle(row.employeeId, col.key)} />
      ),
    })),
    {
      title: t("auditEmployeeCapability.columns.enteredBy"),
      dataIndex: "enteredBy",
      width: 110,
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("auditEmployeeCapability.columns.updatedAt"),
      dataIndex: "updatedAt",
      width: 150,
      render: formatDate,
    },
    {
      title: t("auditEmployeeCapability.columns.approved"),
      dataIndex: "approved",
      width: 90,
      align: "center" as const,
      render: (v: boolean) => <Checkbox checked={v} disabled />,
    },
    {
      title: t("auditEmployeeCapability.columns.approvedBy"),
      dataIndex: "approvedBy",
      width: 110,
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("auditEmployeeCapability.columns.approvedAt"),
      dataIndex: "approvedAt",
      width: 150,
      render: formatDate,
    },
    {
      title: t("common.actions"),
      key: "actions",
      fixed: "right",
      width: 110,
      render: (_: unknown, row: AuditEmployeeCapabilityItem) => (
        <Button
          size="small"
          type="link"
          disabled={!canEdit || row.approved}
          loading={approvingId === row.employeeId}
          onClick={() => handleApprove(row.employeeId)}
        >
          {t("auditEmployeeCapability.approve")}
        </Button>
      ),
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("auditEmployeeCapability.title")}</Typography.Title>
      <Typography.Paragraph type="secondary">{t("auditEmployeeCapability.description")}</Typography.Paragraph>

      <Space style={{ marginBottom: 16 }} wrap>
        <StandardToolbar
          onExportExcel={canExport ? () => exportAuditEmployeeCapabilities("excel") : undefined}
          onExportWord={canExport ? () => exportAuditEmployeeCapabilities("word") : undefined}
          onImport={
            canImport
              ? async (file) => {
                  const result = await importAuditEmployeeCapabilities(file);
                  await load();
                  return result;
                }
              : undefined
          }
        />
        {canEdit && (
          <Button type="primary" onClick={handleSave} loading={saving} disabled={!dirty}>
            {t("auditEmployeeCapability.saveChanges")}
          </Button>
        )}
      </Space>

      <Table<AuditEmployeeCapabilityItem>
        rowKey="employeeId"
        loading={loading}
        dataSource={rows}
        columns={columns}
        pagination={{ pageSize: 20 }}
        scroll={{ x: "max-content" }}
      />
    </div>
  );
}
