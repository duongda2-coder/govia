import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Button, DatePicker, Dropdown, Form, Input, Modal, Result, Select, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { FileExcelOutlined, FileWordOutlined, SyncOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import {
  exportAuditKhnsNamDecision,
  exportAuditKhnsNamMonthlyReport,
  listAuditKhnsNam,
  listAuditKhnsPbRows,
  syncAuditKhnsNamList,
  updateAuditKhnsNamInfo,
  type AuditKhnsNamDecisionType,
  type AuditKhnsNamRowItem,
  type AuditKhnsPbRowItem,
} from "../../../../api/auditKhnsNam";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";
import { formatKhnsPositions } from "./khnsPositionLabel";

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

interface ExportFormValues {
  decisionNumber?: string;
  decisionDate?: dayjs.Dayjs;
  periods?: Record<string, string>;
}

interface EditFormValues {
  otherDuties?: string;
  decisionNumber?: string;
  decisionDate?: dayjs.Dayjs;
  expectedBatch?: string;
  note?: string;
}

/** "Dự kiến nhân sự thực hiện kiểm toán năm, đợt" (sheet ZTC_KHNS_NAM) - danh sach can bo LAY TU man hinh "Phan bo can bo cho chi
 * nhanh theo thang" (KHNS_PB) qua nut "Cap nhat danh sach can bo": Chuc vu, Thang 1..12 (ten doi tuong KT), Tong so doan deu doc
 * tu phan bo do; chi "Cac cong viec khac dang dam nhan" la NSD tu nhap. So/ngay quyet dinh, dot du kien, ghi chu khong phai
 * cot theo dac ta nhung "Chuyen thong tin KHTH" van can nen giu o form Sua. */
export function AuditKhnsNamPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.KHNS_NAM.VIEW");
  const canEdit = hasPermission("AUDIT.KHNS_NAM.EDIT");

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [rows, setRows] = useState<AuditKhnsNamRowItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<EditFormValues>();
  const [exportForm] = Form.useForm<ExportFormValues>();
  const [exportMonth, setExportMonth] = useState<number | null>(null);
  const [exportUnits, setExportUnits] = useState<{ code: string; name: string }[]>([]);
  const [exporting, setExporting] = useState(false);
  const [decisionType, setDecisionType] = useState<AuditKhnsNamDecisionType | null>(null);
  const [decisionRows, setDecisionRows] = useState<AuditKhnsPbRowItem[]>([]);
  const [decisionMonth, setDecisionMonth] = useState<number | undefined>(undefined);
  const [decisionBranch, setDecisionBranch] = useState<string | undefined>(undefined);
  const [exportingDecision, setExportingDecision] = useState(false);

  useEffect(() => {
    listMasterDataItems("YEAR")
      .then(setYears)
      .catch(() => message.error(t("auditKhnsNam.messages.loadError")));
  }, [message, t]);

  const load = useCallback(async () => {
    if (!year) return;
    setLoading(true);
    try {
      setRows(await listAuditKhnsNam(year, false, true));
    } catch {
      message.error(t("auditKhnsNam.messages.loadError"));
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

  const handleSync = async () => {
    if (!year) return;
    setSyncing(true);
    try {
      const count = await syncAuditKhnsNamList(year);
      setSelectedId(null);
      await load();
      if (count === 0) {
        message.warning(t("auditKhnsNam.messages.syncEmpty"));
      } else {
        message.success(t("auditKhnsNam.messages.syncSuccess", { count }));
      }
    } catch {
      message.error(t("auditKhnsNam.messages.syncError"));
    } finally {
      setSyncing(false);
    }
  };

  /** Nút "Xuất báo cáo theo đợt": chọn tháng rồi nhập số/ngày quyết định + thời gian kiểm toán từng đơn vị (NSD nhập theo file mẫu ZTC_BC_DOT). */
  const openExport = async (month: number) => {
    if (!year) return;
    exportForm.resetFields();
    setExportUnits([]);
    setExportMonth(month);
    try {
      const rows = await listAuditKhnsPbRows(year);
      const units = new Map<string, string>();
      rows.filter((r) => r.months.includes(month)).forEach((r) => units.set(r.auditObjectCode, r.auditObjectName));
      setExportUnits(Array.from(units, ([code, name]) => ({ code, name })).sort((a, b) => a.name.localeCompare(b.name, "vi")));
    } catch {
      message.error(t("auditKhnsNam.messages.loadError"));
    }
  };

  const handleExport = async () => {
    if (!year || !exportMonth) return;
    const values = exportForm.getFieldsValue();
    setExporting(true);
    try {
      await exportAuditKhnsNamMonthlyReport(year, exportMonth, {
        decisionNumber: values.decisionNumber?.trim() || null,
        decisionDate: values.decisionDate ? values.decisionDate.format("YYYY-MM-DD") : null,
        unitPeriods: exportUnits
          .map((u) => ({ auditObjectCode: u.code, period: values.periods?.[u.code]?.trim() ?? "" }))
          .filter((u) => u.period),
      });
      setExportMonth(null);
    } catch {
      message.error(t("auditKhnsNam.exportDialog.error"));
    } finally {
      setExporting(false);
    }
  };

  /** Nút "Xuất QĐ thành lập đoàn" / "Xuất QĐ kiểm kê": chọn tháng (đợt) + chi nhánh đi kiểm toán; danh sách cán bộ là đoàn của chi nhánh đó ở KHNS_PB. */
  const openDecision = async (type: AuditKhnsNamDecisionType) => {
    if (!year) return;
    setDecisionMonth(undefined);
    setDecisionBranch(undefined);
    setDecisionRows([]);
    setDecisionType(type);
    try {
      setDecisionRows(await listAuditKhnsPbRows(year));
    } catch {
      message.error(t("auditKhnsNam.messages.loadError"));
    }
  };

  const decisionBranches = useMemo(() => {
    if (!decisionMonth) return [];
    const branches = new Map<string, string>();
    decisionRows.filter((r) => r.months.includes(decisionMonth)).forEach((r) => branches.set(r.auditObjectCode, r.auditObjectName));
    return Array.from(branches, ([code, name]) => ({ value: code, label: name })).sort((a, b) => a.label.localeCompare(b.label, "vi"));
  }, [decisionRows, decisionMonth]);

  const handleDecisionExport = async () => {
    if (!year || !decisionType || !decisionMonth || !decisionBranch) return;
    setExportingDecision(true);
    try {
      const branchName = decisionBranches.find((b) => b.value === decisionBranch)?.label ?? decisionBranch;
      await exportAuditKhnsNamDecision(year, decisionMonth, decisionBranch, decisionType, branchName);
      setDecisionType(null);
    } catch {
      message.error(t("auditKhnsNam.decisionDialog.error"));
    } finally {
      setExportingDecision(false);
    }
  };

  const openEdit = () => {
    if (!selectedRow) return;
    form.setFieldsValue({
      otherDuties: selectedRow.otherDuties ?? undefined,
      decisionNumber: selectedRow.decisionNumber ?? undefined,
      decisionDate: selectedRow.decisionDate ? dayjs(selectedRow.decisionDate) : undefined,
      expectedBatch: selectedRow.expectedBatch ?? undefined,
      note: selectedRow.note ?? undefined,
    });
    setEditModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!selectedRow || !year) return;
    let values: EditFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await updateAuditKhnsNamInfo(selectedRow.employeeId, year, {
        otherDuties: values.otherDuties ?? null,
        decisionNumber: values.decisionNumber ?? null,
        decisionDate: values.decisionDate ? values.decisionDate.format("YYYY-MM-DD") : null,
        expectedBatch: values.expectedBatch ?? null,
        note: values.note ?? null,
      });
      message.success(t("auditKhnsNam.messages.updateSuccess"));
      setEditModalOpen(false);
      await load();
    } catch {
      message.error(t("auditKhnsNam.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const columns: TableProps<AuditKhnsNamRowItem>["columns"] = [
    { title: t("auditKhnsNam.columns.year"), dataIndex: "year", width: 90, align: "center" },
    { title: t("auditKhnsNam.columns.employeeCode"), dataIndex: "employeeCode", width: 110 },
    { title: t("auditKhnsNam.columns.employeeName"), dataIndex: "employeeName", width: 200 },
    { title: t("auditKhnsNam.columns.positionName"), dataIndex: "positionName", width: 150, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhnsNam.columns.departmentCode"), dataIndex: "departmentCode", width: 120, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhnsNam.columns.auditorClassification"),
      dataIndex: "auditorClassification",
      width: 130,
      render: (v: string | null) => (v ? t(`auditKhnsNam.classification.${v}`) : "-"),
    },
    { title: t("auditKhnsNam.columns.businessSegmentCode"), dataIndex: "businessSegmentCode", width: 150, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhnsNam.columns.positions"),
      key: "positions",
      width: 320,
      // y nguyên cột "Chức vụ" của màn Phân bổ cán bộ cho chi nhánh theo tháng; cán bộ đi nhiều đơn vị có chức vụ khác nhau thì mỗi dòng 1 kiểu
      render: (_: unknown, row: AuditKhnsNamRowItem) => {
        const texts = Array.from(new Set(row.positionDetails.map((d) => formatKhnsPositions(t, d.positions, d.segmentNames, row.roleInTeam))));
        if (texts.length === 0) return formatKhnsPositions(t, [], [], row.roleInTeam);
        return texts.map((text) => <div key={text}>{text}</div>);
      },
    },
    { title: t("auditKhnsNam.columns.otherDuties"), dataIndex: "otherDuties", width: 200, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhnsNam.columns.totalTeamsCount"), dataIndex: "totalTeamsCount", width: 140, align: "center" },
    ...MONTHS.map((m) => ({
      title: t("auditKhktThang.columns.month", { month: m }),
      key: `month${m}`,
      width: 170,
      render: (_: unknown, row: AuditKhnsNamRowItem) => row.monthAuditObjectNames?.[m - 1] ?? "-",
    })),
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4} style={{ margin: 0, marginBottom: 16 }}>
        {t("auditKhnsNam.title")}
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
          <Button icon={<SyncOutlined />} disabled={!year} loading={syncing} onClick={handleSync}>
            {t("auditKhnsNam.syncButton")}
          </Button>
        )}
        {canEdit && (
          <Button disabled={!selectedRow} onClick={openEdit}>
            {t("common.edit")}
          </Button>
        )}
        <Dropdown
          disabled={!year}
          trigger={["click"]}
          menu={{
            items: MONTHS.map((m) => ({ key: String(m), label: t("auditKhktThang.columns.month", { month: m }) })),
            onClick: ({ key }) => openExport(Number(key)),
          }}
        >
          <Button icon={<FileExcelOutlined />} disabled={!year}>
            {t("auditKhnsNam.exportReportButton")}
          </Button>
        </Dropdown>
        <Button icon={<FileWordOutlined />} disabled={!year} onClick={() => openDecision("TEAM")}>
          {t("auditKhnsNam.exportTeamDecisionButton")}
        </Button>
        <Button icon={<FileWordOutlined />} disabled={!year} onClick={() => openDecision("INVENTORY")}>
          {t("auditKhnsNam.exportInventoryDecisionButton")}
        </Button>
      </Space>

      {!year ? (
        <Typography.Text type="secondary">{t("auditKhnsNam.selectYearHint")}</Typography.Text>
      ) : (
        <Table<AuditKhnsNamRowItem>
          size="small"
          columns={columns}
          dataSource={rows}
          rowKey="employeeId"
          loading={loading}
          locale={{ emptyText: t("auditKhnsNam.emptyHint") }}
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
        title={t("auditKhnsNam.exportDialog.title", { month: exportMonth, year })}
        open={exportMonth !== null}
        onCancel={() => setExportMonth(null)}
        onOk={handleExport}
        okText={t("auditKhnsNam.exportDialog.submit")}
        confirmLoading={exporting}
        destroyOnClose
        width={560}
      >
        <Form<ExportFormValues> form={exportForm} layout="vertical">
          <Typography.Paragraph type="secondary">{t("auditKhnsNam.exportDialog.hint")}</Typography.Paragraph>
          <Form.Item name="decisionNumber" label={t("auditKhnsNam.exportDialog.decisionNumber")}>
            <Input maxLength={25} />
          </Form.Item>
          <Form.Item name="decisionDate" label={t("auditKhnsNam.exportDialog.decisionDate")}>
            <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
          </Form.Item>
          <Typography.Text strong>{t("auditKhnsNam.exportDialog.units")}</Typography.Text>
          {exportUnits.length === 0 ? (
            <Typography.Paragraph type="secondary" style={{ marginTop: 8 }}>
              {t("auditKhnsNam.exportDialog.noUnits")}
            </Typography.Paragraph>
          ) : (
            <div style={{ marginTop: 8 }}>
              {exportUnits.map((u) => (
                <Form.Item key={u.code} name={["periods", u.code]} label={u.name} style={{ marginBottom: 12 }}>
                  <Input maxLength={100} placeholder={t("auditKhnsNam.exportDialog.periodPlaceholder")} />
                </Form.Item>
              ))}
            </div>
          )}
        </Form>
      </Modal>

      <Modal
        title={t(decisionType === "INVENTORY" ? "auditKhnsNam.decisionDialog.titleInventory" : "auditKhnsNam.decisionDialog.titleTeam", { year })}
        open={decisionType !== null}
        onCancel={() => setDecisionType(null)}
        onOk={handleDecisionExport}
        okText={t("auditKhnsNam.decisionDialog.submit")}
        okButtonProps={{ disabled: !decisionMonth || !decisionBranch }}
        confirmLoading={exportingDecision}
        destroyOnClose
        width={520}
      >
        <Typography.Paragraph type="secondary">{t("auditKhnsNam.decisionDialog.hint")}</Typography.Paragraph>
        <Form layout="vertical">
          <Form.Item label={t("auditKhnsNam.decisionDialog.month")}>
            <Select
              value={decisionMonth}
              onChange={(m: number) => {
                setDecisionMonth(m);
                setDecisionBranch(undefined);
              }}
              options={MONTHS.map((m) => ({ value: m, label: t("auditKhktThang.columns.month", { month: m }) }))}
            />
          </Form.Item>
          <Form.Item label={t("auditKhnsNam.decisionDialog.branch")}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder={t("auditKhnsNam.decisionDialog.branchPlaceholder")}
              value={decisionBranch}
              onChange={setDecisionBranch}
              disabled={!decisionMonth}
              options={decisionBranches}
              notFoundContent={decisionMonth ? t("auditKhnsNam.decisionDialog.noBranches") : null}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={t("auditKhnsNam.form.editTitle")}
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
      >
        <Form<EditFormValues> form={form} layout="vertical">
          <Form.Item name="otherDuties" label={t("auditKhnsNam.columns.otherDuties")}>
            <Input maxLength={120} />
          </Form.Item>
          <Typography.Paragraph type="secondary">{t("auditKhnsNam.form.decisionHint")}</Typography.Paragraph>
          <Form.Item name="decisionNumber" label={t("auditKhnsNam.columns.decisionNumber")}>
            <Input maxLength={25} />
          </Form.Item>
          <Form.Item name="decisionDate" label={t("auditKhnsNam.columns.decisionDate")}>
            <DatePicker style={{ width: "100%" }} format="DD.MM.YYYY" />
          </Form.Item>
          <Form.Item name="expectedBatch" label={t("auditKhnsNam.columns.expectedBatch")}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="note" label={t("auditKhnsNam.columns.note")}>
            <Input.TextArea rows={2} maxLength={120} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
