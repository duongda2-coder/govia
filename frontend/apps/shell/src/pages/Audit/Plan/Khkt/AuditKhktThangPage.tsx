import { useCallback, useEffect, useState } from "react";
import { App, Button, Checkbox, Form, Input, Modal, Result, Select, Space, Table, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { listAuditKhktThang, updateAuditKhktThang, type AuditKhktThangRowItem } from "../../../../api/auditKhktThang";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

interface EditFormValues {
  months: number[];
  note?: string;
}

/** "Khai bao so thang kiem toan trong nam" (sheet ZTC_KHKT_THANG) - doc song danh sach doi tuong da
 * xac nhan (TH2), chi cho sua phan thang 1-12 + ghi chu (xem AuditKhktThangService). */
export function AuditKhktThangPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.KHKT_THANG.VIEW");
  const canEdit = hasPermission("AUDIT.KHKT_THANG.EDIT");

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [rows, setRows] = useState<AuditKhktThangRowItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedCode, setSelectedCode] = useState<string | null>(null);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<EditFormValues>();

  useEffect(() => {
    listMasterDataItems("YEAR")
      .then(setYears)
      .catch(() => message.error(t("auditKhktThang.messages.loadError")));
  }, [message, t]);

  const load = useCallback(async () => {
    if (!year) return;
    setLoading(true);
    try {
      setRows(await listAuditKhktThang(year));
    } catch {
      message.error(t("auditKhktThang.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [year, message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  useEffect(() => {
    setSelectedCode(null);
  }, [year]);

  const selectedRow = rows.find((r) => r.auditObjectCode === selectedCode) ?? null;

  const openEdit = () => {
    if (!selectedRow) return;
    const months = MONTHS.filter((m) => selectedRow[`month${m}` as keyof AuditKhktThangRowItem] as boolean);
    form.setFieldsValue({ months, note: selectedRow.note ?? undefined });
    setEditModalOpen(true);
  };

  const handleSubmitEdit = async () => {
    if (!selectedRow || !year) return;
    let values: EditFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      const monthSet = new Set(values.months ?? []);
      await updateAuditKhktThang(year, selectedRow.auditObjectCode, {
        month1: monthSet.has(1),
        month2: monthSet.has(2),
        month3: monthSet.has(3),
        month4: monthSet.has(4),
        month5: monthSet.has(5),
        month6: monthSet.has(6),
        month7: monthSet.has(7),
        month8: monthSet.has(8),
        month9: monthSet.has(9),
        month10: monthSet.has(10),
        month11: monthSet.has(11),
        month12: monthSet.has(12),
        note: values.note ?? null,
      });
      message.success(t("auditKhktThang.messages.updateSuccess"));
      setEditModalOpen(false);
      await load();
    } catch {
      message.error(t("auditKhktThang.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const monthColumns: NonNullable<TableProps<AuditKhktThangRowItem>["columns"]> = MONTHS.map((m) => ({
    title: t("auditKhktThang.columns.month", { month: m }),
    dataIndex: `month${m}`,
    width: 56,
    align: "center" as const,
    render: (v: boolean) => (v ? "X" : ""),
  }));

  const columns: TableProps<AuditKhktThangRowItem>["columns"] = [
    { title: t("auditKhktThang.columns.auditObjectCode"), dataIndex: "auditObjectCode", width: 100, fixed: "left" },
    { title: t("auditKhktThang.columns.auditObjectName"), dataIndex: "auditObjectName", width: 180, fixed: "left" },
    {
      title: t("auditKhktThang.columns.businessSegments"),
      dataIndex: "businessSegmentCodes",
      width: 160,
      render: (codes: string[]) => (codes.length === 0 ? "-" : codes.map((c) => <Tag key={c}>{c}</Tag>)),
    },
    { title: t("auditKhktThang.columns.auditObjectCategoryCode"), dataIndex: "auditObjectCategoryCode", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhktThang.columns.rankLabel"), dataIndex: "rankLabel", width: 100, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhktThang.columns.onBalanceSheetLoan"),
      dataIndex: "onBalanceSheetLoan",
      width: 130,
      align: "right",
      render: (v: number | null) => (v == null ? "-" : v.toLocaleString()),
    },
    {
      title: t("auditKhktThang.columns.fundingSource"),
      dataIndex: "fundingSource",
      width: 130,
      align: "right",
      render: (v: number | null) => (v == null ? "-" : v.toLocaleString()),
    },
    { title: t("auditKhktThang.columns.creditScale"), dataIndex: "creditScale", width: 100, align: "center", render: (v: number | null) => v ?? "-" },
    { title: t("auditKhktThang.columns.fundingScale"), dataIndex: "fundingScale", width: 110, align: "center", render: (v: number | null) => v ?? "-" },
    { title: t("auditKhktThang.columns.geographicArea"), dataIndex: "geographicArea", width: 100, render: (v: string | null) => v ?? "-" },
    ...monthColumns,
    { title: t("auditKhktThang.columns.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4} style={{ margin: 0, marginBottom: 16 }}>
        {t("auditKhktThang.title")}
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
          <Button disabled={!selectedRow} onClick={openEdit}>
            {t("common.edit")}
          </Button>
        )}
      </Space>

      {!year ? (
        <Typography.Text type="secondary">{t("auditKhktThang.selectYearHint")}</Typography.Text>
      ) : (
        <Table<AuditKhktThangRowItem>
          size="small"
          columns={columns}
          dataSource={rows}
          rowKey="auditObjectCode"
          loading={loading}
          pagination={false}
          scroll={{ x: "max-content" }}
          rowSelection={{
            type: "radio",
            selectedRowKeys: selectedCode ? [selectedCode] : [],
            onChange: (keys) => setSelectedCode((keys[0] as string) ?? null),
          }}
        />
      )}

      <Modal
        title={t("auditKhktThang.form.editTitle")}
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onOk={handleSubmitEdit}
        confirmLoading={submitting}
        destroyOnClose
        width={520}
      >
        <Form<EditFormValues> form={form} layout="vertical">
          <Form.Item name="months" label={t("auditKhktThang.form.months")}>
            <Checkbox.Group options={MONTHS.map((m) => ({ value: m, label: t("auditKhktThang.columns.month", { month: m }) }))} />
          </Form.Item>
          <Form.Item name="note" label={t("auditKhktThang.columns.note")}>
            <Input.TextArea rows={2} maxLength={250} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
