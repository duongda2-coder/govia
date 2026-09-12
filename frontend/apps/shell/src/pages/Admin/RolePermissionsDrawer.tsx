import { useCallback, useEffect, useState } from "react";
import { App, Checkbox, Drawer, Space, Button, Table, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { StandardToolbar } from "@govia/ui-kit";
import type { Permission, Role } from "../../api/roles";
import {
  exportRolePermissions,
  getRolePermissions,
  importRolePermissions,
  listPermissions,
  setRolePermissions,
} from "../../api/roles";

export interface RolePermissionsDrawerProps {
  open: boolean;
  role: Role | null;
  onClose: () => void;
  onSaved: () => void;
}

/**
 * Thu tu uu tien cho 6 hanh dong CRUD pho bien - luon hien truoc de giao dien on dinh. Cac
 * module (Workflow, Audit Supervision Team, ...) co the dinh nghia them hanh dong KHAC voi ten
 * bat ky (vd MANAGE, EVALUATE, APPROVE, DEPLOY, START, CANCEL, COMPLETE, VIEW_ALL) - nhung hanh
 * dong nay PHAI van hien thi duoc o day, neu khong Super Admin khong co cach nao gan quyen do cho
 * vai tro (nut/chuc nang lien quan se "an" voi user thuong dau da duoc "phan quyen day du" tren
 * cac hanh dong da biet). Xem PRIMARY_ACTIONS/ACTION_LABEL_KEYS/actionLabel() ben duoi.
 */
const PRIMARY_ACTIONS = ["VIEW", "CREATE", "EDIT", "DELETE", "EXPORT", "IMPORT"] as const;

/** Nhan hien thi cho cac hanh dong da biet - hanh dong la nhung khong co trong danh sach nay se
 * hien thi nguyen ten (xem actionLabel()) thay vi bi an di. */
const ACTION_LABEL_KEYS: Record<string, string> = {
  VIEW: "common.view",
  CREATE: "common.add",
  EDIT: "common.edit",
  DELETE: "common.delete",
  EXPORT: "common.export",
  IMPORT: "common.import",
  APPROVE: "common.approve",
  MANAGE: "common.manage",
  EVALUATE: "common.evaluate",
  DEPLOY: "common.deploy",
  START: "common.start",
  CANCEL: "common.cancel",
  COMPLETE: "common.complete",
  VIEW_ALL: "common.viewAll",
};

interface ResourceRow {
  resource: string;
  module: string;
  label: string;
  codes: Partial<Record<string, string>>;
}

/** Parse "PEOPLE.EMPLOYEE.VIEW" -> { resource: "EMPLOYEE", action: "VIEW" }. */
function parseCode(code: string): { resource: string; action: string } | null {
  const parts = code.split(".");
  if (parts.length !== 3) return null;
  return { resource: parts[1], action: parts[2] };
}

export function RolePermissionsDrawer({ open, role, onClose, onSaved }: RolePermissionsDrawerProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [catalog, setCatalog] = useState<Permission[]>([]);
  const [checked, setChecked] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    if (!role) return;
    setLoading(true);
    Promise.all([listPermissions(), getRolePermissions(role.id)])
      .then(([permissions, granted]) => {
        setCatalog(permissions);
        setChecked(new Set(granted));
      })
      .catch(() => message.error(t("role.messages.loadPermissionsError")))
      .finally(() => setLoading(false));
  }, [role, message, t]);

  useEffect(() => {
    if (open) load();
  }, [open, load]);

  /**
   * Nhom theo module truoc, sau do theo man hinh (resource) - nhan man hinh lay tu
   * permission.resourceLabel do BE tra ve (khai bao trong Liquibase seed cua tung module),
   * KHONG hardcode danh sach man hinh trong code FE nen them man hinh moi khong can sua FE.
   */
  const rows: ResourceRow[] = Object.entries(
    catalog.reduce<Record<string, ResourceRow>>((acc, permission) => {
      const parsed = parseCode(permission.code);
      if (!parsed) return acc;
      if (!acc[parsed.resource]) {
        acc[parsed.resource] = {
          resource: parsed.resource,
          module: permission.module,
          label: permission.resourceLabel ?? `${permission.module}.${parsed.resource}`,
          codes: {},
        };
      }
      acc[parsed.resource].codes[parsed.action] = permission.code;
      return acc;
    }, {}),
  )
    .map(([, row]) => row)
    .sort((a, b) => a.module.localeCompare(b.module) || a.label.localeCompare(b.label));

  /**
   * Danh sach cot hanh dong hien thi: 6 hanh dong CRUD pho bien truoc (theo dung thu tu cu de
   * khong xao tron giao dien), sau do la cac hanh dong "khac" (MANAGE/EVALUATE/APPROVE/...) thuc
   * su co trong danh muc quyen, xep theo bang chu cai - PHAI tinh dong tu `rows`, khong hardcode,
   * neu khong quyen nao dung ten hanh dong moi se khong co cach nao gan duoc (loi da gap: nut
   * "Danh gia" cua To giam sat khong hien voi user thuong du da "phan quyen day du" - vi
   * AUDIT.SUPERVISION_TEAM.EVALUATE/MANAGE khong co checkbox de gan).
   */
  const actions = (() => {
    const found = new Set<string>();
    rows.forEach((row) => Object.keys(row.codes).forEach((a) => found.add(a)));
    const primary = PRIMARY_ACTIONS.filter((a) => found.has(a));
    const extra = Array.from(found)
      .filter((a) => !(PRIMARY_ACTIONS as readonly string[]).includes(a))
      .sort((a, b) => a.localeCompare(b));
    return [...primary, ...extra];
  })();

  const actionLabel = (action: string) => (ACTION_LABEL_KEYS[action] ? t(ACTION_LABEL_KEYS[action]) : action);

  /**
   * Moi hanh dong deu thao tac TU man hinh danh sach, nen khong co y nghia neu thieu quyen Xem:
   * bat 1 hanh dong khac se tu dong bat kem Xem; tat Xem se tat luon TAT CA hanh dong khac cua
   * man hinh do - ke ca cac hanh dong "khong chuan" nhu MANAGE/EVALUATE (tranh gan quyen "mo khoa"
   * ma khong vao duoc man hinh).
   */
  const toggle = (row: ResourceRow, action: string, value: boolean) => {
    const code = row.codes[action];
    if (!code) return;
    setChecked((prev) => {
      const next = new Set(prev);
      if (value) {
        next.add(code);
        if (action !== "VIEW" && row.codes.VIEW) next.add(row.codes.VIEW);
      } else {
        next.delete(code);
        if (action === "VIEW") {
          Object.values(row.codes).forEach((otherCode) => {
            if (otherCode) next.delete(otherCode);
          });
        }
      }
      return next;
    });
  };

  const handleSave = async () => {
    if (!role) return;
    setSaving(true);
    try {
      await setRolePermissions(role.id, Array.from(checked));
      message.success(t("role.messages.permissionsSaved"));
      onSaved();
    } catch {
      message.error(t("role.messages.saveError"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Drawer
      title={t("role.permissionsTitle", { name: role?.name })}
      open={open}
      onClose={onClose}
      width={720}
      destroyOnClose
      extra={
        <Space>
          <Button onClick={onClose}>{t("employee.form.cancel")}</Button>
          <Button type="primary" onClick={handleSave} loading={saving} disabled={role?.systemDefined}>
            {t("employee.form.save")}
          </Button>
        </Space>
      }
    >
      {role?.systemDefined && (
        <Typography.Paragraph type="secondary">{t("role.messages.systemDefinedReadOnly")}</Typography.Paragraph>
      )}
      <div style={{ marginBottom: 16 }}>
        <StandardToolbar
          onExportExcel={role ? () => exportRolePermissions(role.id) : undefined}
          onImport={
            role && !role.systemDefined
              ? async (file) => {
                  const result = await importRolePermissions(role.id, file);
                  load();
                  return result;
                }
              : undefined
          }
        />
      </div>
      <Table<ResourceRow>
        rowKey="resource"
        loading={loading}
        dataSource={rows}
        pagination={false}
        columns={[
          {
            title: t("common.module"),
            dataIndex: "module",
            width: 110,
          },
          {
            title: t("role.screen"),
            dataIndex: "label",
          },
          ...actions.map((action) => ({
            title: actionLabel(action),
            key: action,
            width: 90,
            align: "center" as const,
            render: (_: unknown, row: ResourceRow) => {
              const code = row.codes[action];
              if (!code) return null;
              return (
                <Checkbox
                  checked={checked.has(code)}
                  disabled={role?.systemDefined}
                  onChange={(e) => toggle(row, action, e.target.checked)}
                />
              );
            },
          })),
        ]}
      />
    </Drawer>
  );
}
