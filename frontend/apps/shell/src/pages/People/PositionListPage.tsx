import { useCallback, useEffect, useState } from "react";
import { App, Result, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import type { Position } from "../../api/positions";
import { exportPositions, importPositions, listPositions, setPositionActive } from "../../api/positions";
import { PositionFormModal } from "./PositionFormModal";
import { useAuth } from "../../auth/AuthContext";

export function PositionListPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("PEOPLE.POSITION.VIEW");
  const canCreate = hasPermission("PEOPLE.POSITION.CREATE");
  const canEdit = hasPermission("PEOPLE.POSITION.EDIT");
  const canExport = hasPermission("PEOPLE.POSITION.EXPORT");
  const canImport = hasPermission("PEOPLE.POSITION.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<Position>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [positions, setPositions] = useState<Position[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<Position[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Position | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setPositions(await listPositions());
    } catch {
      message.error(t("position.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  const handleActiveChange = async (position: Position, active: boolean) => {
    try {
      await setPositionActive(position.id, active);
      message.success(t("position.messages.activeUpdateSuccess"));
      await load();
    } catch {
      message.error(t("position.messages.saveError"));
    }
  };

  const columns: TableProps<Position>["columns"] = [
    { title: t("position.columns.code"), width: 160, ...getSearchColumnProps("code", searchLabels) },
    { title: t("position.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    {
      title: t("position.columns.active"),
      dataIndex: "active",
      width: 150,
      sorter: (a, b) => Number(a.active) - Number(b.active),
      render: (active: boolean, record) => (
        <Switch checked={active} disabled={!canEdit} onChange={(checked) => handleActiveChange(record, checked)} />
      ),
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("position.title")}</Typography.Title>
      <CrudTable<Position>
        tableId="people.positions"
        columns={columns}
        dataSource={positions}
        rowKey="id"
        loading={loading}
        onAdd={
          canCreate
            ? () => {
                setEditing(null);
                setModalOpen(true);
              }
            : undefined
        }
        onEdit={
          canEdit
            ? () => {
                setEditing(selected[0]);
                setModalOpen(true);
              }
            : undefined
        }
        editDisabled={selected.length !== 1}
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onExportExcel={canExport ? () => exportPositions("excel") : undefined}
        onExportWord={canExport ? () => exportPositions("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importPositions(file);
                await load();
                return result;
              }
            : undefined
        }
      />
      <PositionFormModal
        open={modalOpen}
        position={editing}
        onClose={() => setModalOpen(false)}
        onSaved={() => {
          setModalOpen(false);
          load();
        }}
      />
    </div>
  );
}
