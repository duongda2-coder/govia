import { useCallback, useEffect, useState } from "react";
import { App, Button, Space, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import type { AccountStatus, AccountSummary } from "../../api/accounts";
import { exportAccounts, listAccounts } from "../../api/accounts";
import { listRoles, type Role } from "../../api/roles";
import { AssignRolesModal } from "./AssignRolesModal";
import { CopyRolesModal } from "./CopyRolesModal";

const STATUS_COLORS: Record<AccountStatus, string> = {
  ACTIVE: "green",
  LOCKED: "orange",
  DISABLED: "default",
};

const STATUSES: AccountStatus[] = ["ACTIVE", "LOCKED", "DISABLED"];

export function AccountListPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { getSearchColumnProps } = useClientSearchColumn<AccountSummary>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);
  const [assigning, setAssigning] = useState<AccountSummary | null>(null);
  const [copyingInto, setCopyingInto] = useState<AccountSummary | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [accountList, roleList] = await Promise.all([listAccounts(), listRoles()]);
      setAccounts(accountList);
      setRoles(roleList);
    } catch {
      message.error(t("account.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    load();
  }, [load]);

  const columns: TableProps<AccountSummary>["columns"] = [
    { title: t("account.columns.username"), width: 180, ...getSearchColumnProps("username", searchLabels) },
    {
      title: t("account.columns.employeeCode"),
      width: 130,
      render: (v: string | null) => v ?? "-",
      ...getSearchColumnProps("employeeCode", searchLabels),
    },
    {
      title: t("account.columns.employeeName"),
      render: (v: string | null) => v ?? "-",
      ...getSearchColumnProps("employeeName", searchLabels),
    },
    {
      title: t("account.columns.status"),
      dataIndex: "status",
      width: 130,
      filters: STATUSES.map((s) => ({ text: t(`account.status.${s}`), value: s })),
      onFilter: (value, record) => record.status === value,
      render: (status: AccountStatus) => <Tag color={STATUS_COLORS[status]}>{t(`account.status.${status}`)}</Tag>,
    },
    {
      title: t("account.columns.roles"),
      dataIndex: "roleCodes",
      filters: roles.map((r) => ({ text: `${r.code} - ${r.name}`, value: r.code })),
      onFilter: (value, record) => record.roleCodes.includes(value as string),
      render: (roleCodes: string[]) =>
        roleCodes.length > 0 ? (
          roleCodes.map((code) => <Tag key={code}>{code}</Tag>)
        ) : (
          <Tag color="red">{t("account.noRoles")}</Tag>
        ),
    },
    {
      title: t("account.columns.assignRoles"),
      key: "assign",
      width: 260,
      render: (_: unknown, record) => (
        <Space>
          <Button size="small" onClick={() => setAssigning(record)}>
            {t("account.assignRoles.action")}
          </Button>
          <Button size="small" onClick={() => setCopyingInto(record)}>
            {t("account.copyRoles.action")}
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Typography.Title level={4}>{t("account.title")}</Typography.Title>
      <CrudTable<AccountSummary>
        tableId="admin.accounts"
        columns={columns}
        dataSource={accounts}
        rowKey="id"
        loading={loading}
        onExportExcel={exportAccounts}
      />
      <AssignRolesModal
        open={!!assigning}
        account={assigning}
        roles={roles}
        onClose={() => setAssigning(null)}
        onSaved={() => {
          setAssigning(null);
          load();
        }}
      />
      <CopyRolesModal
        open={!!copyingInto}
        targetAccount={copyingInto}
        accounts={accounts}
        onClose={() => setCopyingInto(null)}
        onSaved={() => {
          setCopyingInto(null);
          load();
        }}
      />
    </div>
  );
}
