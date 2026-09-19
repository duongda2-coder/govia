import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Button, DatePicker, Form, Input, Modal, Result, Select, Space, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { SyncOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import {
  listAuditKhnsNam,
  syncAuditKhnsNamList,
  updateAuditKhnsNamInfo,
  type AuditKhnsNamRowItem,
} from "../../../../api/auditKhnsNam";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

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
      dataIndex: "positions",
      width: 300,
      render: (positions: string[]) => (positions.length === 0 ? "-" : positions.map((p) => t(`auditKhnsPb.position.${p}`)).join(", ")),
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
