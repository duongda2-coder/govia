import { useCallback, useEffect, useState } from "react";
import { App, Button, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import type { Role } from "../../api/roles";
import { deleteRole, exportRoles, listRoles } from "../../api/roles";
import { RoleFormModal } from "./RoleFormModal";
import { RolePermissionsDrawer } from "./RolePermissionsDrawer";

export function RoleListPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { getSearchColumnProps } = useClientSearchColumn<Role>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<Role[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Role | null>(null);
  const [permissionsRole, setPermissionsRole] = useState<Role | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRoles(await listRoles());
    } catch {
      message.error(t("role.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    load();
  }, [load]);

  const handleDelete = () => {
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("role.form.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteRole(item.id)));
          message.success(t("role.messages.deleteSuccess"));
          setSelected([]);
          await load();
        } catch {
          message.error(t("role.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<Role>["columns"] = [
    { title: t("role.columns.code"), width: 200, ...getSearchColumnProps("code", searchLabels) },
    { title: t("role.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    { title: t("role.columns.description"), dataIndex: "description", render: (v: string | null) => v ?? "-" },
    {
      title: t("role.columns.systemDefined"),
      dataIndex: "systemDefined",
      width: 130,
      render: (systemDefined: boolean) => (systemDefined ? <Tag color="blue">{t("role.systemDefinedYes")}</Tag> : null),
    },
    {
      title: t("role.columns.permissions"),
      key: "permissions",
      width: 140,
      render: (_: unknown, record) => (
        <Button size="small" onClick={() => setPermissionsRole(record)}>
          {t("role.assignPermissions")}
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Typography.Title level={4}>{t("role.title")}</Typography.Title>
      <CrudTable<Role>
        tableId="admin.roles"
        columns={columns}
        dataSource={roles}
        rowKey="id"
        loading={loading}
        onAdd={() => {
          setEditing(null);
          setModalOpen(true);
        }}
        onEdit={() => {
          setEditing(selected[0]);
          setModalOpen(true);
        }}
        editDisabled={selected.length !== 1 || selected[0]?.systemDefined}
        onDelete={handleDelete}
        deleteDisabled={selected.length === 0 || selected.some((r) => r.systemDefined)}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={exportRoles}
      />
      <RoleFormModal
        open={modalOpen}
        role={editing}
        onClose={() => setModalOpen(false)}
        onSaved={() => {
          setModalOpen(false);
          load();
        }}
      />
      <RolePermissionsDrawer
        open={!!permissionsRole}
        role={permissionsRole}
        onClose={() => setPermissionsRole(null)}
        onSaved={() => setPermissionsRole(null)}
      />
    </div>
  );
}
