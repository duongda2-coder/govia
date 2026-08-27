import { useCallback, useEffect, useState } from "react";
import { App, Result, Switch, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CodeWithTooltip, CrudTable, useClientSearchColumn } from "@govia/ui-kit";
import type { OrganizationUnit } from "../../api/orgUnits";
import { exportOrgUnits, importOrgUnits, listOrgUnits, setOrgUnitActive } from "../../api/orgUnits";
import { listEmployees, type Employee } from "../../api/employees";
import { OrganizationUnitFormModal } from "./OrganizationUnitFormModal";
import { useAuth } from "../../auth/AuthContext";

export function OrganizationUnitListPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("PEOPLE.ORGUNIT.VIEW");
  const canCreate = hasPermission("PEOPLE.ORGUNIT.CREATE");
  const canEdit = hasPermission("PEOPLE.ORGUNIT.EDIT");
  const canExport = hasPermission("PEOPLE.ORGUNIT.EXPORT");
  const canImport = hasPermission("PEOPLE.ORGUNIT.IMPORT");
  const { getSearchColumnProps } = useClientSearchColumn<OrganizationUnit>();
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [orgUnits, setOrgUnits] = useState<OrganizationUnit[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<OrganizationUnit[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<OrganizationUnit | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [units, employeePage] = await Promise.all([listOrgUnits(), listEmployees({ page: 0, size: 500 })]);
      setOrgUnits(units);
      setEmployees(employeePage.content);
    } catch {
      message.error(t("orgUnit.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  const handleActiveChange = async (unit: OrganizationUnit, active: boolean) => {
    try {
      await setOrgUnitActive(unit.id, active);
      message.success(t("orgUnit.messages.activeUpdateSuccess"));
      await load();
    } catch {
      message.error(t("orgUnit.messages.saveError"));
    }
  };

  const unitById = new Map(orgUnits.map((u) => [u.id, u]));
  const employeeById = new Map(employees.map((e) => [e.id, e]));

  const parentCode = (u: OrganizationUnit) => (u.parentId ? (unitById.get(u.parentId)?.code ?? "") : "");
  const managerCode = (u: OrganizationUnit) => (u.managerEmployeeId ? (employeeById.get(u.managerEmployeeId)?.employeeCode ?? "") : "");

  const columns: TableProps<OrganizationUnit>["columns"] = [
    { title: t("orgUnit.columns.code"), width: 140, ...getSearchColumnProps("code", searchLabels) },
    { title: t("orgUnit.columns.name"), ...getSearchColumnProps("name", searchLabels) },
    {
      title: t("orgUnit.columns.level"),
      dataIndex: "levelCode",
      width: 160,
      sorter: (a, b) => (a.levelCode ?? "").localeCompare(b.levelCode ?? ""),
      render: (levelCode: string | null) => (levelCode ? t(`orgUnit.level.${levelCode}`) : "-"),
    },
    {
      title: t("orgUnit.columns.parent"),
      dataIndex: "parentId",
      width: 140,
      sorter: (a, b) => parentCode(a).localeCompare(parentCode(b)),
      render: (parentId: string | null) => {
        const parent = parentId ? unitById.get(parentId) : undefined;
        return <CodeWithTooltip code={parent?.code} name={parent?.name} />;
      },
    },
    {
      title: t("orgUnit.columns.manager"),
      dataIndex: "managerEmployeeId",
      width: 140,
      sorter: (a, b) => managerCode(a).localeCompare(managerCode(b)),
      render: (managerEmployeeId: string | null, record) => (
        <CodeWithTooltip code={managerEmployeeId ? employeeById.get(managerEmployeeId)?.employeeCode : undefined} name={record.managerEmployeeName} />
      ),
    },
    {
      title: t("orgUnit.columns.active"),
      dataIndex: "active",
      width: 130,
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
      <Typography.Title level={4}>{t("orgUnit.title")}</Typography.Title>
      <CrudTable<OrganizationUnit>
        tableId="people.orgUnits"
        columns={columns}
        dataSource={orgUnits}
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
        onExportExcel={canExport ? () => exportOrgUnits("excel") : undefined}
        onExportWord={canExport ? () => exportOrgUnits("word") : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importOrgUnits(file);
                await load();
                return result;
              }
            : undefined
        }
      />
      <OrganizationUnitFormModal
        open={modalOpen}
        orgUnit={editing}
        orgUnits={orgUnits}
        employees={employees}
        onClose={() => setModalOpen(false)}
        onSaved={() => {
          setModalOpen(false);
          load();
        }}
      />
    </div>
  );
}
