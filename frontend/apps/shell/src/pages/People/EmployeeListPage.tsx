import { useCallback, useEffect, useState } from "react";
import { App, Result, Select, Tag, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { CodeWithTooltip, CrudTable, useSearchColumn, useSelectFilterColumn, useServerTable } from "@govia/ui-kit";
import type { CrudColumn } from "@govia/ui-kit";
import type {
  Employee,
  EmployeeListParams,
  EmployeeStatus,
} from "../../api/employees";
import { changeEmployeeStatus, deleteEmployee, exportEmployees, importEmployees, listEmployees } from "../../api/employees";
import { listOrgUnits, type OrganizationUnit } from "../../api/orgUnits";
import { listPositionCatalog, type MasterDataItem as PositionItem } from "../../api/positionCatalog";
import { listMasterDataItems, type MasterDataItem } from "../../api/auditMasterData";
import { auditObjectUnitApi, type AuditObjectUnitItem } from "../../api/riskScoring";
import { EmployeeFormDrawer } from "./EmployeeFormDrawer";
import { useAuth } from "../../auth/AuthContext";

const STATUS_COLORS: Record<EmployeeStatus, string> = {
  ACTIVE: "green",
  ON_LEAVE: "orange",
  TERMINATED: "default",
  PENDING_APPROVAL: "gold",
  REJECTED: "red",
};

const STATUSES: EmployeeStatus[] = ["ACTIVE", "ON_LEAVE", "TERMINATED", "PENDING_APPROVAL", "REJECTED"];

/** dataIndex cua cot -> duong dan sort ma backend hieu (cot tham chieu can join sang bang khac).
 * positionName/departmentName KHONG co trong map nay - Employee.positionId/departmentId tro toi
 * AuditMasterDataItem (khac module, khong co quan he JPA de backend sort qua join), nen 2 cot nay
 * chi ho tro loc, khong sort. */
const SORT_FIELD_MAP: Record<string, string> = {
  orgUnitName: "orgUnit.name",
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
  const [positions, setPositions] = useState<PositionItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [departments, setDepartments] = useState<MasterDataItem[]>([]);
  const [branches, setBranches] = useState<AuditObjectUnitItem[]>([]);
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
    const [unitsResult, positionResult, employeeResult, businessSegmentResult, departmentResult, branchResult] = await Promise.allSettled([
      listOrgUnits(),
      listPositionCatalog(),
      listEmployees({ page: 0, size: 500 }),
      listMasterDataItems("BUSINESS_SEGMENT"),
      listMasterDataItems("DEPARTMENT"),
      auditObjectUnitApi.list(),
    ]);
    if (unitsResult.status === "fulfilled") setOrgUnits(unitsResult.value);
    if (positionResult.status === "fulfilled") setPositions(positionResult.value);
    if (employeeResult.status === "fulfilled") setAllEmployees(employeeResult.value.content);
    if (businessSegmentResult.status === "fulfilled") setBusinessSegments(businessSegmentResult.value);
    if (departmentResult.status === "fulfilled") setDepartments(departmentResult.value);
    if (branchResult.status === "fulfilled") setBranches(branchResult.value.filter((b) => b.unitType === "CN"));
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
    if (selected.length === 0) return;
    modal.confirm({
      title: selected.length > 1 ? t("common.deleteConfirmTitleCount", { count: selected.length }) : t("employee.form.deleteConfirmTitle"),
      content: t("employee.form.deleteConfirmContent"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selected.map((item) => deleteEmployee(item.id)));
          message.success(t("employee.messages.deleteSuccess"));
          setSelected([]);
          await loadEmployees();
        } catch {
          message.error(t("employee.messages.deleteError"));
        }
      },
    });
  };

  const columns: CrudColumn<Employee>[] = [
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
      title: t("employee.columns.department"),
      dataIndex: "departmentName",
      width: 120,
      ...getSearchColumnProps("departmentName", filters.departmentName, searchLabels),
      render: (_: string | null, record) => <CodeWithTooltip code={record.departmentCode} name={record.departmentName} />,
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
      title: t("employee.columns.username"),
      dataIndex: "username",
      width: 130,
      render: (v: string | null) => v ?? "-",
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
          style={{ width: 150 }}
          disabled={value === "PENDING_APPROVAL"}
          onChange={(next) => handleStatusChange(record, next)}
          options={STATUSES.map((s) => ({
            value: s,
            label: <Tag color={STATUS_COLORS[s]}>{t(`employee.status.${s}`)}</Tag>,
          }))}
        />
      ),
    },
    // Cac cot con lai an mac dinh (defaultHidden) - du lieu chi tiet it dung thuong xuyen, nguoi
    // dung tu bat len qua nut "Tuy chinh cot" khi can thay vi lam chat bang mac dinh.
    { title: t("employee.columns.personalEmail"), dataIndex: "personalEmail", defaultHidden: true, render: (v: string | null) => v ?? "-" },
    { title: t("employee.columns.hireDate"), dataIndex: "hireDate", width: 120, defaultHidden: true, render: (v: string | null) => v ?? "-" },
    { title: t("employee.columns.dateOfBirth"), dataIndex: "dateOfBirth", width: 120, defaultHidden: true, render: (v: string | null) => v ?? "-" },
    {
      title: t("employee.columns.gender"),
      dataIndex: "gender",
      width: 100,
      defaultHidden: true,
      render: (v: Employee["gender"]) => (v ? t(`employee.gender.${v}`) : "-"),
    },
    { title: t("employee.columns.idNumber"), dataIndex: "idNumber", width: 130, defaultHidden: true, render: (v: string | null) => v ?? "-" },
    { title: t("employee.columns.rankLevel"), dataIndex: "rankLevel", width: 90, defaultHidden: true, render: (v: string | null) => v ?? "-" },
    { title: t("employee.columns.ethnicity"), dataIndex: "ethnicity", width: 120, defaultHidden: true, render: (v: string | null) => v ?? "-" },
    { title: t("employee.columns.hometown"), dataIndex: "hometown", defaultHidden: true, render: (v: string | null) => v ?? "-" },
    {
      title: t("employee.columns.businessSegment"),
      dataIndex: "businessSegmentName",
      width: 150,
      defaultHidden: true,
      render: (_: string | null, record) => <CodeWithTooltip code={record.businessSegmentCode} name={record.businessSegmentName} />,
    },
    { title: t("employee.columns.partyJoinDate"), dataIndex: "partyJoinDate", width: 130, defaultHidden: true, render: (v: string | null) => v ?? "-" },
    {
      title: t("employee.columns.auditDeptJoinDate"),
      dataIndex: "auditDeptJoinDate",
      width: 150,
      defaultHidden: true,
      render: (v: string | null) => v ?? "-",
    },
    { title: t("employee.columns.priorWorkHistory"), dataIndex: "priorWorkHistory", defaultHidden: true, render: (v: string | null) => v ?? "-" },
    {
      title: t("employee.columns.educationLevel"),
      dataIndex: "educationLevel",
      width: 150,
      defaultHidden: true,
      render: (v: Employee["educationLevel"]) => (v ? t(`employee.educationLevel.${v}`) : "-"),
    },
    {
      title: t("employee.columns.politicalLevel"),
      dataIndex: "politicalLevel",
      width: 130,
      defaultHidden: true,
      render: (v: Employee["politicalLevel"]) => (v ? t(`employee.politicalLevel.${v}`) : "-"),
    },
    {
      title: t("employee.columns.foreignLanguageLevel"),
      dataIndex: "foreignLanguageLevel",
      width: 130,
      defaultHidden: true,
      render: (v: string | null) => v ?? "-",
    },
    { title: t("employee.columns.itSkillLevel"), dataIndex: "itSkillLevel", width: 120, defaultHidden: true, render: (v: string | null) => v ?? "-" },
    {
      title: t("employee.columns.auditorClassification"),
      dataIndex: "auditorClassification",
      width: 150,
      defaultHidden: true,
      render: (v: Employee["auditorClassification"]) => (v ? t(`employee.auditorClassification.${v}`) : "-"),
    },
    {
      title: t("employee.columns.teamLeadCapable"),
      dataIndex: "teamLeadCapable",
      width: 130,
      defaultHidden: true,
      render: (v: boolean) => (v ? t("common.yes") : t("common.no")),
    },
    { title: t("employee.columns.auditedBranches"), dataIndex: "auditedBranches", defaultHidden: true, render: (v: string | null) => v ?? "-" },
    { title: t("employee.columns.otherDuties"), dataIndex: "otherDuties", defaultHidden: true, render: (v: string | null) => v ?? "-" },
    {
      title: t("employee.columns.relatedPersonBranches"),
      dataIndex: "relatedPersonBranches",
      defaultHidden: true,
      render: (v: string | null) => v ?? "-",
    },
    {
      title: t("employee.columns.onLeave"),
      dataIndex: "onLeave",
      width: 130,
      defaultHidden: true,
      render: (v: boolean) => (v ? t("common.yes") : t("common.no")),
    },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4}>{t("employee.title")}</Typography.Title>

      <CrudTable<Employee>
        tableId="people.employees"
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
        deleteDisabled={selected.length === 0}
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
        businessSegments={businessSegments}
        departments={departments}
        branches={branches}
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
