import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Button, DatePicker, Form, Input, Modal, Result, Select, Space, Table, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";
import {
  updateAuditKhnsNam,
  listAuditKhnsNam,
  type AuditKhnsNamRowItem,
  type AuditKhnsNamUpdateRequest,
  type AuditKhnsRoleInTeam,
} from "../../../../api/auditKhnsNam";
import { listAuditKhktThConfirmed } from "../../../../api/auditKhktTh";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);
const ROLES: AuditKhnsRoleInTeam[] = ["TEAM_LEAD", "GROUP_LEAD", "MEMBER", "SUPPORT"];

interface EditFormValues {
  roleInTeam?: AuditKhnsRoleInTeam;
  otherDuties?: string;
  auditObjectCodes: string[];
  decisionNumber?: string;
  decisionDate?: dayjs.Dayjs;
  expectedBatch?: string;
  note?: string;
  months: Record<number, string | undefined>;
}

/** "Dự kiến nhân sự thực hiện kiểm toán năm, đợt" (sheet ZTC_KHNS_NAM) - liet ke TOAN BO nhan vien
 * cua nam, cho sua truc tiep (khong them/xoa dong - xem AuditKhnsNamService o BE). "Chi tiet dot"
 * (12 thang, moi thang chon 1 doi tuong KT) gop vao chung modal Sua thay vi man hinh rieng. */
export function AuditKhnsNamPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.KHNS_NAM.VIEW");
  const canEdit = hasPermission("AUDIT.KHNS_NAM.EDIT");

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [rows, setRows] = useState<AuditKhnsNamRowItem[]>([]);
  const [thObjects, setThObjects] = useState<{ value: string; label: string }[]>([]);
  const [loading, setLoading] = useState(false);
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
      const [namRows, thRows] = await Promise.all([listAuditKhnsNam(year), listAuditKhktThConfirmed(year)]);
      setRows(namRows);
      setThObjects(thRows.map((r) => ({ value: r.auditObjectCode, label: `${r.auditObjectCode} - ${r.auditObjectName}` })));
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

  const openEdit = () => {
    if (!selectedRow) return;
    const months: Record<number, string | undefined> = {};
    MONTHS.forEach((m) => {
      months[m] = (selectedRow[`month${m}AuditObjectCode` as keyof AuditKhnsNamRowItem] as string | null) ?? undefined;
    });
    form.setFieldsValue({
      roleInTeam: selectedRow.roleInTeam ?? undefined,
      otherDuties: selectedRow.otherDuties ?? undefined,
      auditObjectCodes: selectedRow.auditObjectCodes,
      decisionNumber: selectedRow.decisionNumber ?? undefined,
      decisionDate: selectedRow.decisionDate ? dayjs(selectedRow.decisionDate) : undefined,
      expectedBatch: selectedRow.expectedBatch ?? undefined,
      note: selectedRow.note ?? undefined,
      months,
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
      const request: AuditKhnsNamUpdateRequest = {
        roleInTeam: values.roleInTeam ?? null,
        otherDuties: values.otherDuties ?? null,
        auditObjectCodes: values.auditObjectCodes ?? [],
        decisionNumber: values.decisionNumber ?? null,
        decisionDate: values.decisionDate ? values.decisionDate.format("YYYY-MM-DD") : null,
        expectedBatch: values.expectedBatch ?? null,
        note: values.note ?? null,
        month1AuditObjectCode: values.months?.[1] ?? null,
        month2AuditObjectCode: values.months?.[2] ?? null,
        month3AuditObjectCode: values.months?.[3] ?? null,
        month4AuditObjectCode: values.months?.[4] ?? null,
        month5AuditObjectCode: values.months?.[5] ?? null,
        month6AuditObjectCode: values.months?.[6] ?? null,
        month7AuditObjectCode: values.months?.[7] ?? null,
        month8AuditObjectCode: values.months?.[8] ?? null,
        month9AuditObjectCode: values.months?.[9] ?? null,
        month10AuditObjectCode: values.months?.[10] ?? null,
        month11AuditObjectCode: values.months?.[11] ?? null,
        month12AuditObjectCode: values.months?.[12] ?? null,
      };
      await updateAuditKhnsNam(selectedRow.employeeId, year, request);
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
    { title: t("auditKhnsNam.columns.employeeCode"), dataIndex: "employeeCode", width: 100, fixed: "left" },
    { title: t("auditKhnsNam.columns.employeeName"), dataIndex: "employeeName", width: 180, fixed: "left" },
    { title: t("auditKhnsNam.columns.positionName"), dataIndex: "positionName", width: 140, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhnsNam.columns.departmentCode"), dataIndex: "departmentCode", width: 100, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhnsNam.columns.auditorClassification"),
      dataIndex: "auditorClassification",
      width: 110,
      render: (v: string | null) => (v ? t(`auditKhnsNam.classification.${v}`) : "-"),
    },
    { title: t("auditKhnsNam.columns.businessSegmentCode"), dataIndex: "businessSegmentCode", width: 100, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhnsNam.columns.roleInTeam"),
      dataIndex: "roleInTeam",
      width: 120,
      render: (v: AuditKhnsRoleInTeam | null) => (v ? t(`auditKhnsNam.role.${v}`) : "-"),
    },
    {
      title: t("auditKhnsNam.columns.auditObjectCodes"),
      dataIndex: "auditObjectCodes",
      width: 200,
      render: (codes: string[]) => (codes.length === 0 ? "-" : codes.map((c) => <Tag key={c}>{c}</Tag>)),
    },
    { title: t("auditKhnsNam.columns.decisionNumber"), dataIndex: "decisionNumber", width: 120, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhnsNam.columns.decisionDate"),
      dataIndex: "decisionDate",
      width: 120,
      render: (v: string | null) => (v ? dayjs(v).format("DD.MM.YYYY") : "-"),
    },
    { title: t("auditKhnsNam.columns.expectedBatch"), dataIndex: "expectedBatch", width: 130, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhnsNam.columns.totalTeamsCount"), dataIndex: "totalTeamsCount", width: 90, align: "center" },
    { title: t("auditKhnsNam.columns.note"), dataIndex: "note", render: (v: string | null) => v ?? "-" },
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4} style={{ margin: 0, marginBottom: 16 }}>
        {t("auditKhnsNam.title")}
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
        <Typography.Text type="secondary">{t("auditKhnsNam.selectYearHint")}</Typography.Text>
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
        title={t("auditKhnsNam.form.editTitle")}
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
        width={720}
      >
        <Form<EditFormValues> form={form} layout="vertical">
          <Form.Item name="roleInTeam" label={t("auditKhnsNam.columns.roleInTeam")}>
            <Select allowClear options={ROLES.map((r) => ({ value: r, label: t(`auditKhnsNam.role.${r}`) }))} />
          </Form.Item>
          <Form.Item name="otherDuties" label={t("auditKhnsNam.columns.otherDuties")}>
            <Input maxLength={120} />
          </Form.Item>
          <Form.Item name="auditObjectCodes" label={t("auditKhnsNam.columns.auditObjectCodes")}>
            <Select mode="multiple" options={thObjects} showSearch optionFilterProp="label" />
          </Form.Item>
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
          <Typography.Title level={5}>{t("auditKhnsNam.form.monthDetail")}</Typography.Title>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(160px, 1fr))", gap: 8 }}>
            {MONTHS.map((m) => (
              <Form.Item key={m} name={["months", m]} label={t("auditKhktThang.columns.month", { month: m })} style={{ marginBottom: 8 }}>
                <Select allowClear showSearch optionFilterProp="label" options={thObjects} />
              </Form.Item>
            ))}
          </div>
        </Form>
      </Modal>
    </div>
  );
}
