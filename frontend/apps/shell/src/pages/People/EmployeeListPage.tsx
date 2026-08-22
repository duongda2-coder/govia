import { useCallback, useEffect, useState } from "react";
import { App, Result, Select, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { CodeWithTooltip, CrudTable, useSearchColumn, useSelectFilterColumn, useServerTable } from "@govia/ui-kit";
import type {
  Employee,
  EmployeeListParams,
  EmployeeStatus,
} from "../../api/employees";
import { changeEmployeeStatus, deleteEmployee, exportEmployees, importEmployees, listEmployees } from "../../api/employees";
import { listOrgUnits, type OrganizationUnit } from "../../api/orgUnits";
import { listPositions, type Position } from "../../api/positions";
import { EmployeeFormDrawer } from "./EmployeeFormDrawer";
import { useAuth } from "../../auth/AuthContext";

const STATUS_COLORS: Record<EmployeeStatus, string> = {
  ACTIVE: "green",
  ON_LEAVE: "orange",
  TERMINATED: "default",
};

const STATUSES: EmployeeStatus[] = ["ACTIVE", "ON_LEAVE", "TERMINATED"];

/** dataIndex cua cot -> duong dan sort ma backend hieu (cot tham chieu can join sang bang khac). */
const SORT_FIELD_MAP: Record<string, string> = {
  orgUnitName: "orgUnit.name",
  positionName: "position.name",
  managerName: "manager.fullName",
};

export function EmployeeListPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("PEOPLE.EMPLOYEE.VIEW");
  const canCreate = hasPermission("PEOPLE.EMPLOYEE.CREATE");
  const canEdit = hasPermission("PEOPLE.EMPLOYEE.EDIT");
  const canDelete = hasPermission("PEOPLE.EMPLOYEE.DELETE");
  const canExport = hasPermission("PEOPLE.EMPLOYEE.EXPORT");
  const canImport = hasPermission("PEOPLE.EMPLOYEE.IMPORT");
  const { getSearchColumnProps } = useSearchColumn<Employee>();
  const { getSelectFilterColumnProps } = useSelectFilterColumn<Employee>();
  const { query, filters, handleChange, pagination } = useServerTable<Employee>(SORT_FIELD_MAP);
  const filterLabels = { confirmText: t("common.confirm"), resetText: t("common.reset") };
  const searchLabels = { confirmText: t("common.search"), resetText: t("common.reset") };

  const [employees, setEmployees] = useState<Employee[]>([]);
  /** Danh sach nhan vien day du (khong phan trang) - dung cho dropdown "Quan ly" trong form, khac voi
   * `employees` o tren (chi 1 trang) dung de hien thi bang. */
  const [allEmployees, setAllEmployees] = useState<Employee[]>([]);
  const [orgUnits, setOrgUnits] = useState<OrganizationUnit[]>([]);
  const [positions, setPositions] = useState<Position[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<Employee[]>([]);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<Employee | null>(null);

  /**
   * Org unit/position/danh sach nhan vien day du CHI dung cho dropdown trong form Them/Sua -
   * khong dung de hien thi bang (bang da co san orgUnitName/positionName/managerName tren tung dong).
   * Vi vay chi can goi khi user co the mo form (canCreate/canEdit), va tung API doc lap
   * (Promise.allSettled) de 1 API bi thieu quyen (vd chua duoc gan PEOPLE.POSITION.VIEW) khong
   * lam loi ca man hinh - chi dropdown tuong ung se rong.
   */
  const loadLookups = useCallback(async () => {
    const [unitsResult, positionResult, employeeResult] = await Promise.allSettled([
      listOrgUnits(),
      listPositions(),
      listEmployees({ page: 0, size: 500 }),
    ]);
    if (unitsResult.status === "fulfilled") setOrgUnits(unitsResult.value);
    if (positionResult.status === "fulfilled") setPositions(positionResult.value);
    if (employeeResult.status === "fulfilled") setAllEmployees(employeeResult.value.content);
  }, []);

  const loadEmployees = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listEmployees(query as EmployeeListParams);
      setEmployees(result.content);
      setTotal(result.totalElements);
    } catch {
      message.error(t("employee.messages.loadError"));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [JSON.stringify(query), message, t]);

  useEffect(() => {
    if (canCreate || canEdit) loadLookups();
  }, [canCreate, canEdit, loadLookups]);

  useEffect(() => {
    if (canView) loadEmployees();
  }, [canView, loadEmployees]);

  const handleStatusChange = async (employee: Employee, newStatus: EmployeeStatus) => {
    try {
      await changeEmployeeStatus(employee.id, newStatus);
      message.success(t("employee.messages.statusUpdateSuccess"));
      await loadEmployees();
    } catch {
      message.error(t("employee.messages.saveError"));
    }
  };

  const handleDelete = () => {
    const target = selected[0];
    if (!target) return;
    modal.confirm({
      title: t("employee.form.deleteConfirmTitle"),
      content: t("employee.form.deleteConfirmContent"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deleteEmployee(target.id);
          message.success(t("employee.messages.deleteSuccess"));
          setSelected([]);
          await loadEmployees();
        } catch {
          message.error(t("employee.messages.deleteError"));
        }
      },
    });
  };

  const columns: TableProps<Employee>["columns"] = [
    {
      title: t("employee.columns.employeeCode"),
      dataIndex: "employeeCode",
      width: 110,
      sorter: true,
      ...getSearchColumnProps("employeeCode", filters.employeeCode, searchLabels),
    },
    {
      title: t("employee.columns.fullName"),
      dataIndex: "fullName",
      sorter: true,
      ...getSearchColumnProps("fullName", filters.fullName, searchLabels),
    },
    {
      title: t("employee.columns.position"),
      dataIndex: "positionName",
      width: 120,
      sorter: true,
      ...getSearchColumnProps("positionName", filters.positionName, searchLabels),
      render: (_: string | null, record) => <CodeWithTooltip code={record.positionCode} name={record.positionName} />,
    },
    {
      title: t("employee.columns.orgUnit"),
      dataIndex: "orgUnitName",
      width: 120,
      sorter: true,
      ...getSearchColumnProps("orgUnitName", filters.orgUnitName, searchLabels),
      render: (_: string | null, record) => <CodeWithTooltip code={record.orgUnitCode} name={record.orgUnitName} />,
    },
    {
      title: t("employee.columns.manager"),
      dataIndex: "managerName",
      width: 120,
      sorter: true,
      ...getSearchColumnProps("managerName", filters.managerName, searchLabels),
      render: (_: string | null, record) => <CodeWithTooltip code={record.managerCode} name={record.managerName} />,
    },
    {
      title: t("employee.columns.phone"),
      dataIndex: "phone",
      width: 130,
      sorter: true,
      ...getSearchColumnProps("phone", filters.phone, searchLabels),
    },
    {
      title: t("employee.columns.email"),
      dataIndex: "email",
      sorter: true,
      ...getSearchColumnProps("email", filters.email, searchLabels),
    },
    {
      title: t("employee.columns.status"),
      dataIndex: "status",
      width: 150,
      ...getSelectFilterColumnProps(
        "status",
        STATUSES.map((s) => ({ value: s, text: t(`employee.status.${s}`) })),
        filters.status,
        filterLabels,
      ),
      render: (value: EmployeeStatus, record) => (
        <Select
          size="small"
          value={value}
          variant="borderless"
          style={{ width: 130 }}
          onChange={(next) => handleStatusChange(record, next)}
          options={STATUSES.map((s) => ({
            value: s,
            label: <Tag color={STATUS_COLORS[s]}>{t(`employee.status.${s}`)}</Tag>,
          }))}
        />
      ),
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("employee.title")}</Typography.Title>

      <CrudTable<Employee>
        columns={columns}
        dataSource={employees}
        rowKey="id"
        loading={loading}
        onAdd={
          canCreate
            ? () => {
                setEditing(null);
                setDrawerOpen(true);
              }
            : undefined
        }
        onEdit={
          canEdit
            ? () => {
                setEditing(selected[0]);
                setDrawerOpen(true);
              }
            : undefined
        }
        editDisabled={selected.length !== 1}
        onDelete={canDelete ? handleDelete : undefined}
        deleteDisabled={selected.length !== 1}
        onExportExcel={canExport ? () => exportEmployees("excel", filters) : undefined}
        onExportWord={canExport ? () => exportEmployees("word", filters) : undefined}
        onImport={
          canImport
            ? async (file) => {
                const result = await importEmployees(file);
                await loadEmployees();
                await loadLookups();
                return result;
              }
            : undefined
        }
        onSelectionChange={(_keys, rows) => setSelected(rows)}
        onChange={handleChange}
        pagination={pagination(total)}
      />

      <EmployeeFormDrawer
        open={drawerOpen}
        employee={editing}
        orgUnits={orgUnits}
        positions={positions}
        employees={allEmployees}
        onClose={() => setDrawerOpen(false)}
        onSaved={() => {
          setDrawerOpen(false);
          loadEmployees();
          loadLookups();
        }}
      />
    </div>
  );
}
