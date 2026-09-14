import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Button, Checkbox, Form, Input, Modal, Result, Select, Space, Table, Tabs, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { FileExcelOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import {
  confirmAuditKhktBp,
  createAuditKhktBp,
  deleteAuditKhktBp,
  exportAuditKhktBpReport,
  listAuditKhktBp,
  listAuditKhktBpConfirmed,
  updateAuditKhktBp,
  type AuditKhktBpRowItem,
  type AuditKhktSelectionDecision,
  type AuditKhktSourceType,
} from "../../../../api/auditKhktBp";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";
import { AuditKhktObjectPickerModal } from "./AuditKhktObjectPickerModal";

const INSPECTION_TYPES = ["TTCP", "KTNN", "TTGSNH", "KTNB", "GSBKS", "KTGSNB"] as const;
const YEARS_BACK = [5, 4, 3, 2, 1];
const SELECTION_DECISIONS: AuditKhktSelectionDecision[] = ["SELECTED", "NOT_SELECTED"];

interface EditFormValues {
  reviewResult?: string;
  selectionDecision?: AuditKhktSelectionDecision;
  businessSegmentIds: string[];
}

/** "De xuat DTKT nam theo phong" (sheet ZTC_KHKT_BP) - Phan 1 (De xuat, sua duoc) + Phan 2 (Da xac
 * nhan, chi xem) trong 2 tab cua cung 1 man hinh, loc theo Phong NV + Nam o header. Dung antd
 * Table truc tiep (khong qua CrudTable) vi bang co cot nhom long nhau (Lich su KT: 6 nhom x 5 cot
 * T-5..T-1) - CrudTable chi ho tro danh sach cot phang. */
export function AuditKhktBpPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.KHKT_BP.VIEW");
  const canCreate = hasPermission("AUDIT.KHKT_BP.CREATE");
  const canEdit = hasPermission("AUDIT.KHKT_BP.EDIT");
  const canDelete = hasPermission("AUDIT.KHKT_BP.DELETE");

  const [departments, setDepartments] = useState<MasterDataItem[]>([]);
  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [departmentId, setDepartmentId] = useState<string | undefined>(undefined);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [activeTab, setActiveTab] = useState<"candidate" | "confirmed">("candidate");

  const [candidates, setCandidates] = useState<AuditKhktBpRowItem[]>([]);
  const [confirmed, setConfirmed] = useState<AuditKhktBpRowItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditKhktBpRowItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [form] = Form.useForm<EditFormValues>();

  useEffect(() => {
    Promise.all([listMasterDataItems("DEPARTMENT"), listMasterDataItems("YEAR"), listMasterDataItems("BUSINESS_SEGMENT")])
      .then(([deptList, yearList, segmentList]) => {
        setDepartments(deptList);
        setYears(yearList);
        setBusinessSegments(segmentList);
      })
      .catch(() => message.error(t("auditKhktBp.messages.loadError")));
  }, [message, t]);

  const load = useCallback(async () => {
    if (!departmentId || !year) return;
    setLoading(true);
    try {
      const [candidateList, confirmedList] = await Promise.all([
        listAuditKhktBp(departmentId, year),
        listAuditKhktBpConfirmed(departmentId, year),
      ]);
      setCandidates(candidateList);
      setConfirmed(confirmedList);
    } catch {
      message.error(t("auditKhktBp.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [departmentId, year, message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  useEffect(() => {
    setSelectedIds([]);
  }, [activeTab, departmentId, year]);

  const segmentIdByCode = useMemo(() => new Map(businessSegments.map((s) => [s.code, s.id])), [businessSegments]);
  const selectedCandidates = candidates.filter((c) => selectedIds.includes(c.id));

  const handlePick = async (
    rows: { sourceType: AuditKhktSourceType; auditObjectCode: string; auditObjectName: string; auditObjectCategoryCode: string | null; riskScore: number | null; rankLabel: string | null }[],
  ) => {
    if (!departmentId || !year) return;
    try {
      for (const row of rows) {
        await createAuditKhktBp({ departmentId, year, businessSegmentIds: [], ...row });
      }
      message.success(t("auditKhktBp.messages.createSuccess"));
      setPickerOpen(false);
      await load();
    } catch {
      message.error(t("auditKhktBp.messages.saveError"));
    }
  };

  const openEdit = () => {
    const target = selectedCandidates[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      reviewResult: target.reviewResult ?? undefined,
      selectionDecision: target.selectionDecision ?? undefined,
      businessSegmentIds: target.businessSegmentCodes.map((c) => segmentIdByCode.get(c)).filter((v): v is string => !!v),
    });
    setEditModalOpen(true);
  };

  const handleSubmitEdit = async () => {
    if (!editing) return;
    let values: EditFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await updateAuditKhktBp(editing.id, {
        reviewResult: values.reviewResult ?? null,
        selectionDecision: values.selectionDecision ?? null,
        businessSegmentIds: values.businessSegmentIds ?? [],
      });
      message.success(t("auditKhktBp.messages.updateSuccess"));
      setEditModalOpen(false);
      setSelectedIds([]);
      await load();
    } catch {
      message.error(t("auditKhktBp.messages.saveError"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    if (selectedCandidates.length === 0) return;
    modal.confirm({
      title:
        selectedCandidates.length > 1
          ? t("common.deleteConfirmTitleCount", { count: selectedCandidates.length })
          : t("auditKhktBp.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selectedCandidates.map((item) => deleteAuditKhktBp(item.id)));
          message.success(t("auditKhktBp.messages.deleteSuccess"));
          setSelectedIds([]);
          await load();
        } catch {
          message.error(t("auditKhktBp.messages.deleteError"));
        }
      },
    });
  };

  const handleConfirm = () => {
    if (!departmentId || !year) return;
    modal.confirm({
      title: t("auditKhktBp.confirmListTitle"),
      content: t("auditKhktBp.confirmListContent", { count: candidates.length }),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      onOk: async () => {
        setConfirming(true);
        try {
          await confirmAuditKhktBp(departmentId, year);
          message.success(t("auditKhktBp.messages.confirmSuccess"));
          await load();
        } catch {
          message.error(t("auditKhktBp.messages.saveError"));
        } finally {
          setConfirming(false);
        }
      },
    });
  };

  const columns: TableProps<AuditKhktBpRowItem>["columns"] = [
    {
      title: t("auditKhktBp.columns.sourceType"),
      dataIndex: "sourceType",
      width: 90,
      fixed: "left",
      render: (v: AuditKhktSourceType) => <Tag color={v === "BRANCH" ? "blue" : "orange"}>{t(`auditKhktBp.sourceType.${v}`)}</Tag>,
    },
    { title: t("auditKhktBp.columns.auditObjectCode"), dataIndex: "auditObjectCode", width: 100, fixed: "left" },
    { title: t("auditKhktBp.columns.auditObjectName"), dataIndex: "auditObjectName", width: 180, fixed: "left" },
    { title: t("auditKhktBp.columns.riskScore"), dataIndex: "riskScore", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("auditKhktBp.columns.rankLabel"), dataIndex: "rankLabel", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhktBp.columns.onBalanceSheetLoan"), dataIndex: "onBalanceSheetLoan", width: 130, render: (v: number | null) => v ?? "-" },
    { title: t("auditKhktBp.columns.fundingSource"), dataIndex: "fundingSource", width: 130, render: (v: number | null) => v ?? "-" },
    {
      title: t("auditKhktBp.columns.businessSegments"),
      dataIndex: "businessSegmentCodes",
      width: 200,
      render: (codes: string[]) => (codes.length === 0 ? "-" : codes.map((c) => <Tag key={c}>{c}</Tag>)),
    },
    { title: t("auditKhktBp.columns.reviewResult"), dataIndex: "reviewResult", width: 220, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhktBp.columns.selectionDecision"),
      dataIndex: "selectionDecision",
      width: 130,
      render: (v: AuditKhktSelectionDecision | null) =>
        v ? <Tag color={v === "SELECTED" ? "green" : "default"}>{t(`auditKhktBp.selectionDecision.${v}`)}</Tag> : "-",
    },
    ...INSPECTION_TYPES.map((type) => ({
      title: t(`auditKhktBp.inspectionType.${type}`),
      children: YEARS_BACK.map((offset) => ({
        title: `T-${offset}`,
        key: `${type}_T${offset}`,
        width: 42,
        align: "center" as const,
        render: (_: unknown, row: AuditKhktBpRowItem) => (row.inspectionHistory[`${type}_T${offset}`] ? "X" : ""),
      })),
    })),
  ];

  if (!canView) {
    return <Result status="403" title="403" subTitle={t("common.noPermission")} />;
  }

  return (
    <div>
      <Typography.Title level={4} style={{ margin: 0, marginBottom: 16 }}>
        {t("auditKhktBp.title")}
      </Typography.Title>
      <Space style={{ marginBottom: 16 }}>
        <Select
          placeholder={t("auditKhktBp.selectDepartment")}
          style={{ width: 220 }}
          value={departmentId}
          onChange={setDepartmentId}
          showSearch
          optionFilterProp="label"
          options={departments.map((d) => ({ value: d.id, label: `${d.code} - ${d.name}` }))}
        />
        <Select
          placeholder={t("common.selectYear")}
          style={{ width: 120 }}
          value={year}
          onChange={setYear}
          options={years.map((y) => ({ value: Number(y.code), label: y.code }))}
        />
        <Button icon={<FileExcelOutlined />} disabled={!year} onClick={() => year && exportAuditKhktBpReport(year)}>
          {t("auditKhktBp.exportReportButton")}
        </Button>
      </Space>

      {!departmentId || !year ? (
        <Typography.Text type="secondary">{t("auditKhktBp.selectDepartmentAndYearHint")}</Typography.Text>
      ) : (
        <Tabs
          activeKey={activeTab}
          onChange={(k) => setActiveTab(k as "candidate" | "confirmed")}
          items={[
            {
              key: "candidate",
              label: t("auditKhktBp.tabCandidate"),
              children: (
                <>
                  <Space style={{ marginBottom: 12 }}>
                    {canCreate && (
                      <Button type="primary" onClick={() => setPickerOpen(true)}>
                        {t("auditKhktBp.pickObjectButton")}
                      </Button>
                    )}
                    {canEdit && (
                      <Button disabled={selectedCandidates.length !== 1} onClick={openEdit}>
                        {t("common.edit")}
                      </Button>
                    )}
                    {canDelete && (
                      <Button danger disabled={selectedCandidates.length === 0} onClick={handleDelete}>
                        {t("common.delete")}
                      </Button>
                    )}
                    {canCreate && (
                      <Button onClick={handleConfirm} loading={confirming} disabled={candidates.length === 0}>
                        {t("auditKhktBp.confirmListButton")}
                      </Button>
                    )}
                  </Space>
                  <Table<AuditKhktBpRowItem>
                    size="small"
                    columns={columns}
                    dataSource={candidates}
                    rowKey="id"
                    loading={loading}
                    pagination={false}
                    scroll={{ x: "max-content" }}
                    rowSelection={{ selectedRowKeys: selectedIds, onChange: (keys) => setSelectedIds(keys as string[]) }}
                  />
                </>
              ),
            },
            {
              key: "confirmed",
              label: t("auditKhktBp.tabConfirmed"),
              children: (
                <Table<AuditKhktBpRowItem>
                  size="small"
                  columns={columns}
                  dataSource={confirmed}
                  rowKey="id"
                  loading={loading}
                  pagination={false}
                  scroll={{ x: "max-content" }}
                />
              ),
            },
          ]}
        />
      )}

      {departmentId && year && (
        <AuditKhktObjectPickerModal
          open={pickerOpen}
          year={year}
          excludeCodes={candidates.map((c) => c.auditObjectCode)}
          onClose={() => setPickerOpen(false)}
          onPick={handlePick}
        />
      )}

      <Modal
        title={t("auditKhktBp.form.editTitle")}
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onOk={handleSubmitEdit}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
      >
        <Form<EditFormValues> form={form} layout="vertical">
          <Form.Item name="reviewResult" label={t("auditKhktBp.columns.reviewResult")}>
            <Input.TextArea rows={2} maxLength={250} />
          </Form.Item>
          <Form.Item name="selectionDecision" label={t("auditKhktBp.columns.selectionDecision")}>
            <Select allowClear options={SELECTION_DECISIONS.map((v) => ({ value: v, label: t(`auditKhktBp.selectionDecision.${v}`) }))} />
          </Form.Item>
          <Form.Item name="businessSegmentIds" label={t("auditKhktBp.columns.businessSegments")}>
            <Checkbox.Group options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
