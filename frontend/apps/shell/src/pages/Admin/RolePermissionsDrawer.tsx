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

const ACTIONS = ["VIEW", "CREATE", "EDIT", "DELETE", "EXPORT", "IMPORT"] as const;

const ACTION_LABEL_KEYS: Record<(typeof ACTIONS)[number], string> = {
  VIEW: "common.view",
  CREATE: "common.add",
  EDIT: "common.edit",
  DELETE: "common.delete",
  EXPORT: "common.export",
  IMPORT: "common.import",
};

interface ResourceRow {
  resource: string;
  module: string;
  label: string;
  codes: Partial<Record<(typeof ACTIONS)[number], string>>;
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
      acc[parsed.resource].codes[parsed.action as (typeof ACTIONS)[number]] = permission.code;
      return acc;
    }, {}),
  )
    .map(([, row]) => row)
    .sort((a, b) => a.module.localeCompare(b.module) || a.label.localeCompare(b.label));

  /**
   * Moi hanh dong (Them/Sua/Xoa/Xuat/Import) deu thao tac TU man hinh danh sach, nen khong co
   * y nghia neu thieu quyen Xem: bat 1 hanh dong khac se tu dong bat kem Xem; tat Xem se tat
   * luon cac hanh dong khac cua man hinh do (tranh gan quyen "mo khoa" ma khong vao duoc man hinh).
   */
  const toggle = (row: ResourceRow, action: (typeof ACTIONS)[number], value: boolean) => {
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
          ACTIONS.forEach((a) => {
            const otherCode = row.codes[a];
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
          ...ACTIONS.map((action) => ({
            title: t(ACTION_LABEL_KEYS[action]),
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
