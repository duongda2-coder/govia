import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Button, Form, Input, Modal, Result, Select, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { FileExcelOutlined, TeamOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import {
  exportAuditKhnsNamMonthlyReport,
  listAuditKhnsNam,
  updateAuditKhnsNam,
  updateAuditKhnsNamNote,
  type AuditKhnsNamRowItem,
  type AuditKhnsNamUpdateRequest,
} from "../../../../api/auditKhnsNam";
import { listAuditKhktThConfirmed } from "../../../../api/auditKhktTh";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

interface NoteFormValues {
  note?: string;
}

/** Tra ve gia tri cot "thang X" cua 1 dong KHNS_NAM (dung chung cho hien thi va tinh toan phan bo). */
function monthAuditObjectCode(row: AuditKhnsNamRowItem, month: number): string | null {
  return row[`month${month}AuditObjectCode` as keyof AuditKhnsNamRowItem] as string | null;
}

/** "Phân bổ tự động cán bộ cho chi nhánh theo tháng" (sheet ZTC_KHNS_PB) - hien thi bao cao tong hop
 * tu du lieu da nhap o KHNS_NAM, CONG THEM nut "Phan bo nhan su": chon 1 doi tuong KT + 1 thang, tick
 * chon nhieu can bo de gan/huy gan cung luc (ghi qua lai API cap nhat KHNS_NAM cho tung can bo, giu
 * nguyen cac truong khac cua ho) - thay vi phai mo tung dong o man KHNS_NAM de sua rieng le. Cot
 * "Ghi chú" cua man hinh nay van sua rieng qua updateAuditKhnsNamNote nhu truoc. */
export function AuditKhnsPbPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.KHNS_NAM.VIEW");
  const canEdit = hasPermission("AUDIT.KHNS_NAM.EDIT");

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [month, setMonth] = useState<number>(1);
  const [rows, setRows] = useState<AuditKhnsNamRowItem[]>([]);
  const [objectNameByCode, setObjectNameByCode] = useState<Map<string, string>>(new Map());
  const [auditObjectOptions, setAuditObjectOptions] = useState<{ value: string; label: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [noteModalOpen, setNoteModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<NoteFormValues>();

  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [assignObjectCode, setAssignObjectCode] = useState<string | undefined>(undefined);
  const [assignMonth, setAssignMonth] = useState<number>(1);
  const [assignEmployeeIds, setAssignEmployeeIds] = useState<string[]>([]);
  const [assignSubmitting, setAssignSubmitting] = useState(false);

  useEffect(() => {
    listMasterDataItems("YEAR")
      .then(setYears)
      .catch(() => message.error(t("auditKhnsPb.messages.loadError")));
  }, [message, t]);

  const load = useCallback(async () => {
    if (!year) return;
    setLoading(true);
    try {
      const [namRows, thRows] = await Promise.all([listAuditKhnsNam(year), listAuditKhktThConfirmed(year)]);
      setRows(namRows);
      setObjectNameByCode(new Map(thRows.map((r) => [r.auditObjectCode, r.auditObjectName])));
      setAuditObjectOptions(thRows.map((r) => ({ value: r.auditObjectCode, label: `${r.auditObjectCode} - ${r.auditObjectName}` })));
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
    setSelectedId(null);
  }, [year]);

  useEffect(() => {
    if (!assignModalOpen || !assignObjectCode) {
      setAssignEmployeeIds([]);
      return;
    }
    setAssignEmployeeIds(rows.filter((r) => monthAuditObjectCode(r, assignMonth) === assignObjectCode).map((r) => r.employeeId));
  }, [assignModalOpen, assignObjectCode, assignMonth, rows]);

  const selectedRow = useMemo(() => rows.find((r) => r.employeeId === selectedId) ?? null, [rows, selectedId]);

  const openEditNote = () => {
    if (!selectedRow) return;
    form.setFieldsValue({ note: selectedRow.note ?? undefined });
    setNoteModalOpen(true);
  };

  const handleSubmitNote = async () => {
    if (!selectedRow || !year) return;
    let values: NoteFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await updateAuditKhnsNamNote(selectedRow.employeeId, year, values.note ?? null);
      message.success(t("auditKhnsPb.messages.updateSuccess"));
      setNoteModalOpen(false);
      await load();
    } catch {
      message.error(t("auditKhnsPb.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const openAssignModal = () => {
    setAssignObjectCode(undefined);
    setAssignMonth(month);
    setAssignEmployeeIds([]);
    setAssignModalOpen(true);
  };

  const handleSubmitAssign = async () => {
    if (!year || !assignObjectCode) {
      message.error(t("auditKhnsPb.messages.assignObjectRequired"));
      return;
    }
    setAssignSubmitting(true);
    try {
      const selectedSet = new Set(assignEmployeeIds);
      const changedRows = rows.filter((r) => (monthAuditObjectCode(r, assignMonth) === assignObjectCode) !== selectedSet.has(r.employeeId));
      await Promise.all(
        changedRows.map((r) => {
          const nowSelected = selectedSet.has(r.employeeId);
          const newCode = nowSelected ? assignObjectCode : null;
          const request: AuditKhnsNamUpdateRequest = {
            roleInTeam: r.roleInTeam,
            otherDuties: r.otherDuties,
            auditObjectCodes: r.auditObjectCodes,
            decisionNumber: r.decisionNumber,
            decisionDate: r.decisionDate,
            expectedBatch: r.expectedBatch,
            note: r.note,
            month1AuditObjectCode: assignMonth === 1 ? newCode : r.month1AuditObjectCode,
            month2AuditObjectCode: assignMonth === 2 ? newCode : r.month2AuditObjectCode,
            month3AuditObjectCode: assignMonth === 3 ? newCode : r.month3AuditObjectCode,
            month4AuditObjectCode: assignMonth === 4 ? newCode : r.month4AuditObjectCode,
            month5AuditObjectCode: assignMonth === 5 ? newCode : r.month5AuditObjectCode,
            month6AuditObjectCode: assignMonth === 6 ? newCode : r.month6AuditObjectCode,
            month7AuditObjectCode: assignMonth === 7 ? newCode : r.month7AuditObjectCode,
            month8AuditObjectCode: assignMonth === 8 ? newCode : r.month8AuditObjectCode,
            month9AuditObjectCode: assignMonth === 9 ? newCode : r.month9AuditObjectCode,
            month10AuditObjectCode: assignMonth === 10 ? newCode : r.month10AuditObjectCode,
            month11AuditObjectCode: assignMonth === 11 ? newCode : r.month11AuditObjectCode,
            month12AuditObjectCode: assignMonth === 12 ? newCode : r.month12AuditObjectCode,
          };
          return updateAuditKhnsNam(r.employeeId, year, request);
        }),
      );
      message.success(t("auditKhnsPb.messages.assignSuccess"));
      setAssignModalOpen(false);
      await load();
    } catch {
      message.error(t("auditKhnsPb.messages.assignError"));
    } finally {
      setAssignSubmitting(false);
    }
  };

  const monthColumns: NonNullable<TableProps<AuditKhnsNamRowItem>["columns"]> = MONTHS.map((m) => ({
    title: t("auditKhktThang.columns.month", { month: m }),
    dataIndex: `month${m}AuditObjectCode`,
    width: 130,
    render: (code: string | null) => (code ? (objectNameByCode.get(code) ?? code) : "-"),
  }));

  const assignModalColumns: NonNullable<TableProps<AuditKhnsNamRowItem>["columns"]> = [
    { title: t("auditKhnsPb.columns.employeeCode"), dataIndex: "employeeCode", width: 100 },
    { title: t("auditKhnsPb.columns.employeeName"), dataIndex: "employeeName", width: 180 },
    { title: t("auditKhnsPb.columns.departmentCode"), dataIndex: "departmentCode", width: 100, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhnsPb.columns.auditorClassification"),
      dataIndex: "auditorClassification",
      width: 110,
      render: (v: string | null) => (v ? t(`auditKhnsNam.classification.${v}`) : "-"),
    },
    {
      title: t("auditKhnsPb.columns.currentAssignment"),
      width: 160,
      render: (_: unknown, r: AuditKhnsNamRowItem) => {
        const code = monthAuditObjectCode(r, assignMonth);
        return code ? (objectNameByCode.get(code) ?? code) : "-";
      },
    },
  ];

  const columns: TableProps<AuditKhnsNamRowItem>["columns"] = [
    { title: t("auditKhnsPb.columns.employeeCode"), dataIndex: "employeeCode", width: 100, fixed: "left" },
    { title: t("auditKhnsPb.columns.employeeName"), dataIndex: "employeeName", width: 180, fixed: "left" },
    { title: t("auditKhnsPb.columns.positionName"), dataIndex: "positionName", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhnsPb.columns.departmentCode"), dataIndex: "departmentCode", width: 100, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhnsPb.columns.auditorClassification"),
      dataIndex: "auditorClassification",
      width: 110,
      render: (v: string | null) => (v ? t(`auditKhnsNam.classification.${v}`) : "-"),
    },
    { title: t("auditKhnsPb.columns.businessSegmentCode"), dataIndex: "businessSegmentCode", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhnsPb.columns.otherDuties"), dataIndex: "otherDuties", width: 160, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhnsPb.columns.totalTeamsCount"), dataIndex: "totalTeamsCount", width: 90, align: "center" },
    ...monthColumns,
    { title: t("auditKhnsPb.columns.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4} style={{ margin: 0, marginBottom: 16 }}>
        {t("auditKhnsPb.title")}
      </Typography.Title>
      <Space style={{ marginBottom: 16 }}>
        <Select
          placeholder={t("common.selectYear")}
          style={{ width: 120 }}
          value={year}
          onChange={setYear}
          options={years.map((y) => ({ value: Number(y.code), label: y.code }))}
        />
        {canEdit && (
          <Button disabled={!selectedRow} onClick={openEditNote}>
            {t("auditKhnsPb.editNoteButton")}
          </Button>
        )}
        {canEdit && (
          <Button icon={<TeamOutlined />} disabled={!year} onClick={openAssignModal}>
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
        <Table<AuditKhnsNamRowItem>
          size="small"
          columns={columns}
          dataSource={rows}
          rowKey="employeeId"
          loading={loading}
          pagination={{ pageSize: 50 }}
          scroll={{ x: "max-content" }}
          rowSelection={{
            type: "radio",
            selectedRowKeys: selectedId ? [selectedId] : [],
            onChange: (keys) => setSelectedId((keys[0] as string) ?? null),
          }}
        />
      )}

      <Modal
        title={t("auditKhnsPb.editNoteButton")}
        open={noteModalOpen}
        onCancel={() => setNoteModalOpen(false)}
        onOk={handleSubmitNote}
        confirmLoading={submitting}
        destroyOnClose
        width={480}
      >
        <Form<NoteFormValues> form={form} layout="vertical">
          <Form.Item name="note" label={t("auditKhnsPb.columns.note")}>
            <Input.TextArea rows={3} maxLength={120} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={t("auditKhnsPb.assignModalTitle")}
        open={assignModalOpen}
        onCancel={() => setAssignModalOpen(false)}
        onOk={handleSubmitAssign}
        confirmLoading={assignSubmitting}
        destroyOnClose
        width={760}
      >
        <Space style={{ marginBottom: 12 }}>
          <Select
            style={{ width: 320 }}
            placeholder={t("auditKhnsPb.assignObjectPlaceholder")}
            value={assignObjectCode}
            onChange={setAssignObjectCode}
            options={auditObjectOptions}
            showSearch
            optionFilterProp="label"
          />
          <Select
            style={{ width: 140 }}
            value={assignMonth}
            onChange={setAssignMonth}
            options={MONTHS.map((m) => ({ value: m, label: t("auditKhktThang.columns.month", { month: m }) }))}
          />
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
          {t("auditKhnsPb.assignEmployeesHint")}
        </Typography.Paragraph>
        <Table<AuditKhnsNamRowItem>
          size="small"
          columns={assignModalColumns}
          dataSource={rows}
          rowKey="employeeId"
          pagination={{ pageSize: 10 }}
          scroll={{ y: 360 }}
          rowSelection={{
            type: "checkbox",
            selectedRowKeys: assignEmployeeIds,
            onChange: (keys) => setAssignEmployeeIds(keys as string[]),
          }}
        />
      </Modal>
    </div>
  );
}
