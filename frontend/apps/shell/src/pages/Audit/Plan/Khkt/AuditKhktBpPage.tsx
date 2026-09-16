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
  setAuditKhktBpApprovalStatus,
  updateAuditKhktBp,
  type AuditKhktApprovalStatus,
  type AuditKhktBpRowItem,
  type AuditKhktSelectionChoice,
  type AuditKhktSelectionDecision,
  type AuditKhktSourceType,
} from "../../../../api/auditKhktBp";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";
import { AuditKhktObjectPickerModal } from "./AuditKhktObjectPickerModal";

const INSPECTION_TYPES = ["TTCP", "KTNN", "TTGSNH", "KTNB", "GSBKS", "KTGSNB"] as const;
const YEARS_BACK = [5, 4, 3, 2, 1];
const SELECTION_DECISIONS: AuditKhktSelectionDecision[] = ["SELECTED", "NOT_SELECTED"];
const SELECTION_CHOICES: AuditKhktSelectionChoice[] = ["KT", "GS"];

interface EditFormValues {
  reviewResult?: string;
  selectionDecision?: AuditKhktSelectionDecision;
  proposalBasis?: string;
  approvedSelection?: AuditKhktSelectionChoice;
  expectedSelection?: AuditKhktSelectionChoice;
  auditScope?: string;
  adhocAuditOrSupervision?: AuditKhktSelectionChoice;
  planAdjustment?: AuditKhktSelectionChoice;
  adjustmentReason?: string;
  khktgsAfterAdjustment?: AuditKhktSelectionChoice;
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
  const [selectedConfirmedIds, setSelectedConfirmedIds] = useState<string[]>([]);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditKhktBpRowItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [approving, setApproving] = useState(false);
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
    setSelectedConfirmedIds([]);
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
      proposalBasis: target.proposalBasis ?? undefined,
      approvedSelection: target.approvedSelection ?? undefined,
      expectedSelection: target.expectedSelection ?? undefined,
      auditScope: target.auditScope ?? undefined,
      adhocAuditOrSupervision: target.adhocAuditOrSupervision ?? undefined,
      planAdjustment: target.planAdjustment ?? undefined,
      adjustmentReason: target.adjustmentReason ?? undefined,
      khktgsAfterAdjustment: target.khktgsAfterAdjustment ?? undefined,
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
        proposalBasis: values.proposalBasis ?? null,
        approvedSelection: values.approvedSelection ?? null,
        expectedSelection: values.expectedSelection ?? null,
        auditScope: values.auditScope ?? null,
        adhocAuditOrSupervision: values.adhocAuditOrSupervision ?? null,
        planAdjustment: values.planAdjustment ?? null,
        adjustmentReason: values.adjustmentReason ?? null,
        khktgsAfterAdjustment: values.khktgsAfterAdjustment ?? null,
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

  const handleSetApprovalStatus = async (approved: boolean) => {
    if (selectedConfirmedIds.length === 0) return;
    setApproving(true);
    try {
      await Promise.all(selectedConfirmedIds.map((id) => setAuditKhktBpApprovalStatus(id, approved)));
      message.success(t(approved ? "auditKhktBp.messages.approveSuccess" : "auditKhktBp.messages.unapproveSuccess"));
      setSelectedConfirmedIds([]);
      await load();
    } catch {
      message.error(t("auditKhktBp.messages.saveError"));
    } finally {
      setApproving(false);
    }
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
    { title: t("auditKhktBp.columns.auditObjectCategoryCode"), dataIndex: "auditObjectCategoryCode", width: 110, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhktBp.columns.riskScore"), dataIndex: "riskScore", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("auditKhktBp.columns.rankLabel"), dataIndex: "rankLabel", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhktBp.columns.onBalanceSheetLoan"), dataIndex: "onBalanceSheetLoan", width: 130, render: (v: number | null) => v ?? "-" },
    { title: t("auditKhktBp.columns.fundingSource"), dataIndex: "fundingSource", width: 130, render: (v: number | null) => v ?? "-" },
    ...businessSegments.map((segment) => ({
      title: t("auditKhktBp.columns.segmentProposal", { segment: segment.name }),
      key: `segment_${segment.code}`,
      width: 130,
      align: "center" as const,
      render: (_: unknown, row: AuditKhktBpRowItem) => (row.businessSegmentCodes.includes(segment.code) ? "X" : ""),
    })),
    { title: t("auditKhktBp.columns.reviewResult"), dataIndex: "reviewResult", width: 220, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhktBp.columns.proposalBasis"), dataIndex: "proposalBasis", width: 220, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhktBp.columns.selectionDecision"),
      dataIndex: "selectionDecision",
      width: 130,
      render: (v: AuditKhktSelectionDecision | null) =>
        v ? <Tag color={v === "SELECTED" ? "green" : "default"}>{t(`auditKhktBp.selectionDecision.${v}`)}</Tag> : "-",
    },
    {
      title: t("auditKhktBp.columns.approvedSelection"),
      dataIndex: "approvedSelection",
      width: 110,
      render: (v: AuditKhktSelectionChoice | null) => v ?? "-",
    },
    {
      title: t("auditKhktBp.columns.expectedSelection"),
      dataIndex: "expectedSelection",
      width: 110,
      render: (v: AuditKhktSelectionChoice | null) => v ?? "-",
    },
    { title: t("auditKhktBp.columns.auditScope"), dataIndex: "auditScope", width: 140, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhktBp.columns.adhocAuditOrSupervision"),
      dataIndex: "adhocAuditOrSupervision",
      width: 110,
      render: (v: AuditKhktSelectionChoice | null) => v ?? "-",
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
    {
      title: t("auditKhktBp.columns.planAdjustment"),
      dataIndex: "planAdjustment",
      width: 110,
      render: (v: AuditKhktSelectionChoice | null) => v ?? "-",
    },
    { title: t("auditKhktBp.columns.adjustmentReason"), dataIndex: "adjustmentReason", width: 200, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhktBp.columns.khktgsAfterAdjustment"),
      dataIndex: "khktgsAfterAdjustment",
      width: 130,
      render: (v: AuditKhktSelectionChoice | null) => v ?? "-",
    },
  ];

  const confirmedColumns: TableProps<AuditKhktBpRowItem>["columns"] = [
    ...columns,
    {
      title: t("auditKhktBp.columns.approvalStatus"),
      dataIndex: "approvalStatus",
      width: 130,
      fixed: "right",
      render: (v: AuditKhktApprovalStatus | null) =>
        v ? <Tag color={v === "APPROVED" ? "green" : "gold"}>{t(`auditKhktBp.approvalStatus.${v}`)}</Tag> : "-",
    },
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
                <>
                  {canEdit && (
                    <Space style={{ marginBottom: 12 }}>
                      <Button
                        disabled={selectedConfirmedIds.length === 0}
                        loading={approving}
                        onClick={() => handleSetApprovalStatus(true)}
                      >
                        {t("auditKhktBp.approveButton")}
                      </Button>
                      <Button
                        disabled={selectedConfirmedIds.length === 0}
                        loading={approving}
                        onClick={() => handleSetApprovalStatus(false)}
                      >
                        {t("auditKhktBp.unapproveButton")}
                      </Button>
                    </Space>
                  )}
                  <Table<AuditKhktBpRowItem>
                    size="small"
                    columns={confirmedColumns}
                    dataSource={confirmed}
                    rowKey="id"
                    loading={loading}
                    pagination={false}
                    scroll={{ x: "max-content" }}
                    rowSelection={
                      canEdit
                        ? { selectedRowKeys: selectedConfirmedIds, onChange: (keys) => setSelectedConfirmedIds(keys as string[]) }
                        : undefined
                    }
                  />
                </>
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
          <Form.Item name="proposalBasis" label={t("auditKhktBp.columns.proposalBasis")}>
            <Input.TextArea rows={2} maxLength={500} />
          </Form.Item>
          <Form.Item name="selectionDecision" label={t("auditKhktBp.columns.selectionDecision")}>
            <Select allowClear options={SELECTION_DECISIONS.map((v) => ({ value: v, label: t(`auditKhktBp.selectionDecision.${v}`) }))} />
          </Form.Item>
          <Space size="large" style={{ display: "flex" }}>
            <Form.Item name="approvedSelection" label={t("auditKhktBp.columns.approvedSelection")} style={{ flex: 1 }}>
              <Select allowClear options={SELECTION_CHOICES.map((v) => ({ value: v, label: v }))} />
            </Form.Item>
            <Form.Item name="expectedSelection" label={t("auditKhktBp.columns.expectedSelection")} style={{ flex: 1 }}>
              <Select allowClear options={SELECTION_CHOICES.map((v) => ({ value: v, label: v }))} />
            </Form.Item>
          </Space>
          <Form.Item name="auditScope" label={t("auditKhktBp.columns.auditScope")}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="adhocAuditOrSupervision" label={t("auditKhktBp.columns.adhocAuditOrSupervision")}>
            <Select allowClear options={SELECTION_CHOICES.map((v) => ({ value: v, label: v }))} />
          </Form.Item>
          <Form.Item name="businessSegmentIds" label={t("auditKhktBp.columns.businessSegments")}>
            <Checkbox.Group options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))} />
          </Form.Item>
          <Space size="large" style={{ display: "flex" }}>
            <Form.Item name="planAdjustment" label={t("auditKhktBp.columns.planAdjustment")} style={{ flex: 1 }}>
              <Select allowClear options={SELECTION_CHOICES.map((v) => ({ value: v, label: v }))} />
            </Form.Item>
            <Form.Item name="khktgsAfterAdjustment" label={t("auditKhktBp.columns.khktgsAfterAdjustment")} style={{ flex: 1 }}>
              <Select allowClear options={SELECTION_CHOICES.map((v) => ({ value: v, label: v }))} />
            </Form.Item>
          </Space>
          <Form.Item name="adjustmentReason" label={t("auditKhktBp.columns.adjustmentReason")}>
            <Input.TextArea rows={2} maxLength={255} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
