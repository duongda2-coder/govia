import { useCallback, useEffect, useState } from "react";
import { App, Alert, Button, Checkbox, Drawer, Table } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import {
  listSupervisionCandidates,
  saveSupervisionTeam,
  type AuditSupervisionCandidateItem,
} from "../../../../api/auditSupervisionTeam";

export interface AuditSupervisionTeamDrawerProps {
  open: boolean;
  engagementId: string;
  engagementCode: string;
  /** Chi nguoi tao ra CKT nay moi duoc chon/thay doi to giam sat - xem AuditEngagementPage
   * (readOnly = selected engagement's createdBy !== user hien tai). */
  readOnly: boolean;
  onClose: () => void;
}

/** "Chon to giam sat" (sheet "To giam sat" cua Tao CKT (4).xlsx) - danh sach nhan vien co kha
 * nang dam nhiem to giam sat (AuditEmployeeCapability.toGiamSatCapable), tich chon roi Luu se
 * thay the TOAN BO danh sach to giam sat hien tai cua CKT. */
export function AuditSupervisionTeamDrawer({ open, engagementId, engagementCode, readOnly, onClose }: AuditSupervisionTeamDrawerProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();

  const [items, setItems] = useState<AuditSupervisionCandidateItem[]>([]);
  const [checkedIds, setCheckedIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const candidates = await listSupervisionCandidates(engagementId);
      setItems(candidates);
      setCheckedIds(new Set(candidates.filter((c) => c.selected).map((c) => c.employeeId)));
    } catch {
      message.error(t("auditSupervisionTeam.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [engagementId, message, t]);

  useEffect(() => {
    if (open) load();
  }, [open, load]);

  const toggle = (employeeId: string, checked: boolean) => {
    setCheckedIds((prev) => {
      const next = new Set(prev);
      if (checked) next.add(employeeId);
      else next.delete(employeeId);
      return next;
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await saveSupervisionTeam(engagementId, Array.from(checkedIds));
      message.success(t("auditSupervisionTeam.messages.saveTeamSuccess"));
      onClose();
    } catch {
      message.error(t("auditSupervisionTeam.messages.saveTeamError"));
    } finally {
      setSaving(false);
    }
  };

  const columns: TableProps<AuditSupervisionCandidateItem>["columns"] = [
    { title: t("auditSupervisionTeam.columns.employeeCode"), dataIndex: "employeeCode", width: 110 },
    { title: t("auditSupervisionTeam.columns.employeeName"), dataIndex: "employeeName" },
    { title: t("auditSupervisionTeam.columns.username"), dataIndex: "username", width: 140, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditSupervisionTeam.columns.selected"),
      key: "selected",
      width: 90,
      render: (_v, row) => (
        <Checkbox disabled={readOnly} checked={checkedIds.has(row.employeeId)} onChange={(e) => toggle(row.employeeId, e.target.checked)} />
      ),
    },
  ];

  return (
    <Drawer
      title={t("auditSupervisionTeam.chooseTeamTitle", { code: engagementCode })}
      open={open}
      onClose={onClose}
      width={720}
      destroyOnClose
      footer={
        !readOnly && (
          <Button type="primary" loading={saving} onClick={handleSave}>
            {t("common.save")}
          </Button>
        )
      }
    >
      {readOnly && <Alert type="info" showIcon style={{ marginBottom: 16 }} message={t("auditSupervisionTeam.readOnlyWarning")} />}
      <Table<AuditSupervisionCandidateItem> rowKey="employeeId" loading={loading} dataSource={items} columns={columns} pagination={false} />
    </Drawer>
  );
}
