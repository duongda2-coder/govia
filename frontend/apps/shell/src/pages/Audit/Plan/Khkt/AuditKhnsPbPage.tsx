import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Button, Form, Modal, Result, Select, Space, Table, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { EditOutlined, FileExcelOutlined, PlusOutlined, TeamOutlined } from "@ant-design/icons";
import { isAxiosError } from "axios";
import { useTranslation } from "react-i18next";
import {
  allocateAuditKhnsPb,
  exportAuditKhnsNamMonthlyReport,
  listAuditKhnsNam,
  listAuditKhnsPbRows,
  updateAuditKhnsNam,
  type AuditKhnsNamRowItem,
  type AuditKhnsNamUpdateRequest,
  type AuditKhnsPbRowItem,
  type AuditKhnsRoleInTeam,
} from "../../../../api/auditKhnsNam";
import { listAuditKhktThang, type AuditKhktThangRowItem } from "../../../../api/auditKhktThang";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);
const ROLES: AuditKhnsRoleInTeam[] = ["TEAM_LEAD", "GROUP_LEAD", "MEMBER", "SUPPORT"];

interface AllocationFormValues {
  employeeId?: string;
  roleInTeam?: AuditKhnsRoleInTeam;
  months: Record<number, string | undefined>;
}

const rowKeyOf = (row: AuditKhnsPbRowItem) => `${row.employeeId}|${row.auditObjectCode}`;

/** "Phân bổ cán bộ cho chi nhánh theo tháng" (sheet ZTC_KHNS_PB) - moi dong = 1 can bo duoc phan bo vao 1
 * don vi (doi tuong kiem toan), cot theo dac ta: Ma/Ten don vi, Linh vuc kiem toan, Quy mo tin dung, Quy mo
 * huy dong von (tu KHKT_THANG / DS DTKT nam), Ten can bo, Username, Chuc vu (phan bo tu dong theo kha nang
 * dam nhan), Thang. CHI gom can bo da duoc phan bo. Nut "Phan bo nhan su" phan bo tu dong (ngau nhien theo
 * kha nang dam nhan linh vuc) cho CA NAM - logic o backend (AuditKhnsPbService); sau do nguoi dung tu chinh
 * lai bang "Sua phan bo" / "Them can bo" (ghi qua updateAuditKhnsNam, giu nguyen so/ngay quyet dinh, dot). */
export function AuditKhnsPbPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.KHNS_NAM.VIEW");
  const canEdit = hasPermission("AUDIT.KHNS_NAM.EDIT");

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [month, setMonth] = useState<number>(1);
  const [pbRows, setPbRows] = useState<AuditKhnsPbRowItem[]>([]);
  const [khnsRows, setKhnsRows] = useState<AuditKhnsNamRowItem[]>([]);
  const [thangRows, setThangRows] = useState<AuditKhktThangRowItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [allocating, setAllocating] = useState(false);
  const [allocModalOpen, setAllocModalOpen] = useState(false);
  const [allocAddMode, setAllocAddMode] = useState(false);
  const [unallocatedRows, setUnallocatedRows] = useState<AuditKhnsNamRowItem[]>([]);
  const [currentMonthCodes, setCurrentMonthCodes] = useState<Record<number, string | undefined>>({});
  const [allocForm] = Form.useForm<AllocationFormValues>();

  useEffect(() => {
    listMasterDataItems("YEAR")
      .then(setYears)
      .catch(() => message.error(t("auditKhnsPb.messages.loadError")));
  }, [message, t]);

  const load = useCallback(async () => {
    if (!year) return;
    setLoading(true);
    try {
      const [rows, plans, thang] = await Promise.all([listAuditKhnsPbRows(year), listAuditKhnsNam(year, true), listAuditKhktThang(year)]);
      setPbRows(rows);
      setKhnsRows(plans);
      setThangRows(thang);
    } catch {
      message.error(t("auditKhnsPb.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [year, message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  useEffect(() => {
    setSelectedKey(null);
  }, [year]);

  const selectedRow = useMemo(() => pbRows.find((r) => rowKeyOf(r) === selectedKey) ?? null, [pbRows, selectedKey]);
  const selectedPlan = useMemo(() => khnsRows.find((r) => r.employeeId === selectedRow?.employeeId) ?? null, [khnsRows, selectedRow]);

  /** Doi tuong chon duoc cho tung thang = doi tuong da khai bao di kiem toan vao thang do (KHKT_THANG); giu them
   * doi tuong dang duoc phan bo san (neu khong con khop khai bao) de khong bi mat hien thi khi sua. */
  const objectOptionsForMonth = (m: number) => {
    const options = thangRows
      .filter((r) => r[`month${m}` as keyof AuditKhktThangRowItem] === true)
      .map((r) => ({ value: r.auditObjectCode, label: `${r.auditObjectCode} - ${r.auditObjectName}` }));
    const current = currentMonthCodes[m];
    if (current && !options.some((o) => o.value === current)) {
      const known = thangRows.find((r) => r.auditObjectCode === current);
      options.push({ value: current, label: known ? `${current} - ${known.auditObjectName}` : current });
    }
    return options;
  };

  const monthsOf = (plan: AuditKhnsNamRowItem): Record<number, string | undefined> => {
    const months: Record<number, string | undefined> = {};
    MONTHS.forEach((m) => {
      months[m] = (plan[`month${m}AuditObjectCode` as keyof AuditKhnsNamRowItem] as string | null) ?? undefined;
    });
    return months;
  };

  const openEditAllocation = () => {
    if (!selectedPlan) return;
    const months = monthsOf(selectedPlan);
    setCurrentMonthCodes(months);
    allocForm.setFieldsValue({ employeeId: selectedPlan.employeeId, roleInTeam: selectedPlan.roleInTeam ?? undefined, months });
    setAllocAddMode(false);
    setAllocModalOpen(true);
  };

  const openAddEmployee = async () => {
    if (!year) return;
    try {
      const all = await listAuditKhnsNam(year);
      const allocatedIds = new Set(khnsRows.map((r) => r.employeeId));
      setUnallocatedRows(all.filter((r) => !allocatedIds.has(r.employeeId)));
    } catch {
      message.error(t("auditKhnsPb.messages.loadError"));
      return;
    }
    allocForm.resetFields();
    setCurrentMonthCodes({});
    setAllocAddMode(true);
    setAllocModalOpen(true);
  };

  const handleSubmitAllocation = async () => {
    if (!year) return;
    let values: AllocationFormValues;
    try {
      values = await allocForm.validateFields();
    } catch {
      return;
    }
    const target = allocAddMode ? unallocatedRows.find((r) => r.employeeId === values.employeeId) : selectedPlan;
    if (!target) return;
    const monthCode = (m: number) => values.months?.[m] ?? null;
    const request: AuditKhnsNamUpdateRequest = {
      roleInTeam: values.roleInTeam ?? null,
      otherDuties: target.otherDuties,
      auditObjectCodes: Array.from(new Set(MONTHS.map(monthCode).filter((c): c is string => !!c))),
      decisionNumber: target.decisionNumber,
      decisionDate: target.decisionDate,
      expectedBatch: target.expectedBatch,
      note: target.note,
      month1AuditObjectCode: monthCode(1),
      month2AuditObjectCode: monthCode(2),
      month3AuditObjectCode: monthCode(3),
      month4AuditObjectCode: monthCode(4),
      month5AuditObjectCode: monthCode(5),
      month6AuditObjectCode: monthCode(6),
      month7AuditObjectCode: monthCode(7),
      month8AuditObjectCode: monthCode(8),
      month9AuditObjectCode: monthCode(9),
      month10AuditObjectCode: monthCode(10),
      month11AuditObjectCode: monthCode(11),
      month12AuditObjectCode: monthCode(12),
    };
    setSubmitting(true);
    try {
      await updateAuditKhnsNam(target.employeeId, year, request);
      message.success(t("auditKhnsPb.messages.updateSuccess"));
      setAllocModalOpen(false);
      setSelectedKey(null);
      await load();
    } catch (err) {
      const code = isAxiosError<{ errorCode?: string }>(err) ? err.response?.data?.errorCode : undefined;
      message.error(code === "AUDIT_KHNS_NAM_TEAM_LEAD_CONFLICT" ? t("auditKhnsPb.messages.teamLeadConflict") : t("auditKhnsPb.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const runAllocation = async () => {
    if (!year) return;
    setAllocating(true);
    try {
      const result = await allocateAuditKhnsPb(year);
      setSelectedKey(null);
      await load();
      if (result.objectCount === 0) {
        message.warning(result.warnings[0]);
        return;
      }
      if (result.warnings.length === 0) {
        message.success(
          t("auditKhnsPb.messages.assignSuccess", { staffed: result.fullyStaffedCount, total: result.objectCount, employees: result.employeesAssigned }),
        );
        return;
      }
      modal.warning({
        title: t("auditKhnsPb.messages.assignPartialTitle", { staffed: result.fullyStaffedCount, total: result.objectCount, employees: result.employeesAssigned }),
        width: 640,
        content: (
          <div style={{ maxHeight: 360, overflowY: "auto" }}>
            <Typography.Paragraph type="secondary">{t("auditKhnsPb.messages.assignPartialHint")}</Typography.Paragraph>
            <ul style={{ paddingLeft: 20, margin: 0 }}>
              {result.warnings.map((w) => (
                <li key={w}>{w}</li>
              ))}
            </ul>
          </div>
        ),
      });
    } catch {
      message.error(t("auditKhnsPb.messages.assignError"));
    } finally {
      setAllocating(false);
    }
  };

  const confirmAllocation = () => {
    modal.confirm({
      title: t("auditKhnsPb.assignConfirmTitle"),
      content: t("auditKhnsPb.assignConfirmContent", { year }),
      okText: t("auditKhnsPb.assignButton"),
      onOk: runAllocation,
    });
  };

  const columns: TableProps<AuditKhnsPbRowItem>["columns"] = [
    { title: t("auditKhnsPb.columns.auditObjectCode"), dataIndex: "auditObjectCode", width: 110 },
    { title: t("auditKhnsPb.columns.auditObjectName"), dataIndex: "auditObjectName", width: 200 },
    {
      title: t("auditKhnsPb.columns.businessSegments"),
      dataIndex: "businessSegmentCodes",
      width: 170,
      render: (codes: string[]) => (codes.length === 0 ? "-" : codes.map((c) => <Tag key={c}>{c}</Tag>)),
    },
    { title: t("auditKhnsPb.columns.creditScale"), dataIndex: "creditScale", width: 110, align: "center", render: (v: number | null) => v ?? "-" },
    { title: t("auditKhnsPb.columns.fundingScale"), dataIndex: "fundingScale", width: 130, align: "center", render: (v: number | null) => v ?? "-" },
    { title: t("auditKhnsPb.columns.employeeName"), dataIndex: "employeeName", width: 200 },
    { title: t("auditKhnsPb.columns.username"), dataIndex: "username", width: 130, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhnsPb.columns.position"),
      dataIndex: "roleInTeam",
      width: 220,
      render: (_: unknown, row) => {
        if (!row.roleInTeam) return "-";
        const role = t(`auditKhnsNam.role.${row.roleInTeam}`);
        // Trưởng đoàn giữ nguyên; các vai trò còn lại ghi rõ đang làm nghiệp vụ nào (1 hoặc nhiều)
        return row.roleInTeam === "TEAM_LEAD" || row.segmentNames.length === 0 ? role : `${role} ${row.segmentNames.join(", ")}`;
      },
    },
    { title: t("auditKhnsPb.columns.months"), dataIndex: "months", width: 100, render: (months: number[]) => months.join(", ") },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4} style={{ margin: 0, marginBottom: 16 }}>
        {t("auditKhnsPb.title")}
      </Typography.Title>
      <Space style={{ marginBottom: 16 }} wrap>
        <Select
          placeholder={t("common.selectYear")}
          style={{ width: 120 }}
          value={year}
          onChange={setYear}
          options={years.map((y) => ({ value: Number(y.code), label: y.code }))}
        />
        {canEdit && (
          <Button icon={<EditOutlined />} disabled={!selectedPlan} onClick={openEditAllocation}>
            {t("auditKhnsPb.editAllocationButton")}
          </Button>
        )}
        {canEdit && (
          <Button icon={<PlusOutlined />} disabled={!year} onClick={openAddEmployee}>
            {t("auditKhnsPb.addEmployeeButton")}
          </Button>
        )}
        {canEdit && (
          <Button icon={<TeamOutlined />} disabled={!year} loading={allocating} onClick={confirmAllocation}>
            {t("auditKhnsPb.assignButton")}
          </Button>
        )}
        <Select style={{ width: 140 }} value={month} onChange={setMonth} options={MONTHS.map((m) => ({ value: m, label: t("auditKhktThang.columns.month", { month: m }) }))} />
        <Button icon={<FileExcelOutlined />} disabled={!year} onClick={() => year && exportAuditKhnsNamMonthlyReport(year, month)}>
          {t("auditKhnsPb.exportReportButton")}
        </Button>
      </Space>

      {!year ? (
        <Typography.Text type="secondary">{t("auditKhnsPb.selectYearHint")}</Typography.Text>
      ) : (
        <Table<AuditKhnsPbRowItem>
          size="small"
          columns={columns}
          dataSource={pbRows}
          rowKey={rowKeyOf}
          loading={loading}
          locale={{ emptyText: t("auditKhnsPb.emptyHint") }}
          pagination={{ pageSize: 50 }}
          scroll={{ x: "max-content" }}
          rowSelection={{
            type: "radio",
            selectedRowKeys: selectedKey ? [selectedKey] : [],
            onChange: (keys) => setSelectedKey((keys[0] as string) ?? null),
          }}
        />
      )}

      <Modal
        title={allocAddMode ? t("auditKhnsPb.addEmployeeTitle") : t("auditKhnsPb.editAllocationTitle")}
        open={allocModalOpen}
        onCancel={() => setAllocModalOpen(false)}
        onOk={handleSubmitAllocation}
        confirmLoading={submitting}
        destroyOnClose
        width={720}
      >
        <Form<AllocationFormValues> form={allocForm} layout="vertical">
          {allocAddMode ? (
            <Form.Item name="employeeId" label={t("auditKhnsPb.employeeField")} rules={[{ required: true }]}>
              <Select
                showSearch
                optionFilterProp="label"
                options={unallocatedRows.map((r) => ({ value: r.employeeId, label: `${r.employeeCode} - ${r.employeeName}` }))}
              />
            </Form.Item>
          ) : (
            <Typography.Paragraph strong>
              {selectedPlan?.employeeCode} - {selectedPlan?.employeeName}
            </Typography.Paragraph>
          )}
          <Form.Item name="roleInTeam" label={t("auditKhnsPb.roleField")}>
            <Select allowClear options={ROLES.map((r) => ({ value: r, label: t(`auditKhnsNam.role.${r}`) }))} />
          </Form.Item>
          <Typography.Paragraph type="secondary">{t("auditKhnsPb.monthsHint")}</Typography.Paragraph>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: 8 }}>
            {MONTHS.map((m) => (
              <Form.Item key={m} name={["months", m]} label={t("auditKhktThang.columns.month", { month: m })} style={{ marginBottom: 8 }}>
                <Select allowClear showSearch optionFilterProp="label" options={objectOptionsForMonth(m)} />
              </Form.Item>
            ))}
          </div>
        </Form>
      </Modal>
    </div>
  );
}
