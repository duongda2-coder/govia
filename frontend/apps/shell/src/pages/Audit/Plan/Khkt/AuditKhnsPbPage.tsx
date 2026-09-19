import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Button, Form, Input, Modal, Result, Select, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { FileExcelOutlined, TeamOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import {
  allocateAuditKhnsPb,
  exportAuditKhnsNamMonthlyReport,
  listAuditKhnsNam,
  updateAuditKhnsNamNote,
  type AuditKhnsNamRowItem,
} from "../../../../api/auditKhnsNam";
import { listAuditKhktThConfirmed } from "../../../../api/auditKhktTh";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

interface NoteFormValues {
  note?: string;
}

/** "Phân bổ tự động cán bộ cho chi nhánh theo tháng" (sheet ZTC_KHNS_PB) - hien thi bao cao tong hop
 * tu du lieu KHNS_NAM, CONG THEM nut "Phan bo nhan su": phan bo tu dong can bo cho CA NAM dua tren
 * "Khai bao so thang kiem toan trong nam" (KHKT_THANG) va nguyen tac bo tri nhan su 1 doan kiem toan
 * (logic o backend - AuditKhnsPbService), ghi de phan bo thang/doi tuong cu cua nam. Cot "Ghi chú" cua
 * man hinh nay van sua rieng qua updateAuditKhnsNamNote nhu truoc. */
export function AuditKhnsPbPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.KHNS_NAM.VIEW");
  const canEdit = hasPermission("AUDIT.KHNS_NAM.EDIT");

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [month, setMonth] = useState<number>(1);
  const [rows, setRows] = useState<AuditKhnsNamRowItem[]>([]);
  const [objectNameByCode, setObjectNameByCode] = useState<Map<string, string>>(new Map());
  const [loading, setLoading] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [noteModalOpen, setNoteModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<NoteFormValues>();
  const [allocating, setAllocating] = useState(false);

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

  const runAllocation = async () => {
    if (!year) return;
    setAllocating(true);
    try {
      const result = await allocateAuditKhnsPb(year);
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

  const monthColumns: NonNullable<TableProps<AuditKhnsNamRowItem>["columns"]> = MONTHS.map((m) => ({
    title: t("auditKhktThang.columns.month", { month: m }),
    dataIndex: `month${m}AuditObjectCode`,
    width: 130,
    render: (code: string | null) => (code ? (objectNameByCode.get(code) ?? code) : "-"),
  }));

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
    </div>
  );
}
