import { useCallback, useEffect, useState } from "react";
import { App, Drawer, Table, Tag, Typography } from "antd";
import { CheckCircleFilled } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import type { AccountSummary } from "../../api/accounts";
import { getAccountPermissions } from "../../api/accounts";
import type { Permission } from "../../api/roles";
import { listPermissions } from "../../api/roles";

export interface AccountPermissionsDrawerProps {
  open: boolean;
  account: AccountSummary | null;
  onClose: () => void;
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

/** Parse "PEOPLE.EMPLOYEE.VIEW" -> { resource: "EMPLOYEE", action: "VIEW" } - giong RolePermissionsDrawer. */
function parseCode(code: string): { resource: string; action: string } | null {
  const parts = code.split(".");
  if (parts.length !== 3) return null;
  return { resource: parts[1], action: parts[2] };
}

/**
 * Xem quyen HIEU LUC (hop nhat tu MOI vai tro dang gan cho tai khoan nay) - chi doc, danh cho
 * Super Admin kiem tra nhanh 1 tai khoan THUC SU lam duoc gi ma khong phai lat tung vai tro
 * rieng le trong "Vai tro & Phan quyen". Cung ma tran Module/Man hinh x Hanh dong nhu
 * RolePermissionsDrawer, nhung khong cho chinh sua (khong co Luu/Import).
 */
export function AccountPermissionsDrawer({ open, account, onClose }: AccountPermissionsDrawerProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [catalog, setCatalog] = useState<Permission[]>([]);
  const [granted, setGranted] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);

  const load = useCallback(() => {
    if (!account) return;
    setLoading(true);
    Promise.all([listPermissions(), getAccountPermissions(account.id)])
      .then(([permissions, codes]) => {
        setCatalog(permissions);
        setGranted(new Set(codes));
      })
      .catch(() => message.error(t("account.viewPermissions.loadError")))
      .finally(() => setLoading(false));
  }, [account, message, t]);

  useEffect(() => {
    if (open) load();
  }, [open, load]);

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

  return (
    <Drawer title={t("account.viewPermissions.title", { username: account?.username })} open={open} onClose={onClose} width={720} destroyOnClose>
      {account && (
        <Typography.Paragraph type="secondary">
          {account.roleCodes.length > 0 ? (
            <>
              {t("account.viewPermissions.rolesPrefix")} {account.roleCodes.map((code) => <Tag key={code}>{code}</Tag>)}
            </>
          ) : (
            t("account.noRoles")
          )}
        </Typography.Paragraph>
      )}
      <Table<ResourceRow>
        rowKey="resource"
        loading={loading}
        dataSource={rows}
        pagination={false}
        columns={[
          { title: t("common.module"), dataIndex: "module", width: 110 },
          { title: t("role.screen"), dataIndex: "label" },
          ...ACTIONS.map((action) => ({
            title: t(ACTION_LABEL_KEYS[action]),
            key: action,
            width: 90,
            align: "center" as const,
            render: (_: unknown, row: ResourceRow) => {
              const code = row.codes[action];
              if (!code) return null;
              return granted.has(code) ? <CheckCircleFilled style={{ color: "#16a34a" }} /> : null;
            },
          })),
        ]}
      />
    </Drawer>
  );
}
