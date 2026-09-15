import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Button, Checkbox, Form, Input, Modal, Result, Select, Space, Switch, Table, Tabs, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { FileExcelOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import {
  confirmAuditKhktTh,
  deleteAuditKhktTh,
  exportAuditKhktThReport,
  listAuditKhktTh,
  listAuditKhktThConfirmed,
  setAuditKhktThApprovalStatus,
  syncAuditKhktTh,
  updateAuditKhktTh,
  type AuditKhktThRowItem,
} from "../../../../api/auditKhktTh";
import type { AuditKhktApprovalStatus, AuditKhktSelectionChoice, AuditKhktSourceType } from "../../../../api/auditKhktBp";
import { listMasterDataItems, type MasterDataItem } from "../../../../api/auditMasterData";
import { useAuth } from "../../../../auth/AuthContext";

const SELECTION_CHOICES: AuditKhktSelectionChoice[] = ["KT", "GS"];

interface EditFormValues {
  proposalBasisTh?: string;
  expertOpinion?: string;
  selection1: boolean;
  selection2: boolean;
  selection3: boolean;
  auditScope?: string;
  adhocAuditOrSupervision?: AuditKhktSelectionChoice;
  planAdjustment?: AuditKhktSelectionChoice;
  adjustmentReason?: string;
  khktgsAfterAdjustment?: AuditKhktSelectionChoice;
  thBusinessSegmentIds: string[];
}

/** "Danh sach DTKT nam cua Phong ke hoach" (sheet ZTC_KHKT_TH) - Phan 1 (TH, tong hop tu BP2 +
 * sua duoc) + Phan 2 (TH2, chi xem) trong 2 tab. Khac AuditKhktBpPage: khong loc theo phong (1
 * dong / 1 doi tuong / 1 nam, gop tu TAT CA phong qua nut "Tong hop tu BP2"). */
export function AuditKhktThPage() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuth();
  const canView = hasPermission("AUDIT.KHKT_TH.VIEW");
  const canCreate = hasPermission("AUDIT.KHKT_TH.CREATE");
  const canEdit = hasPermission("AUDIT.KHKT_TH.EDIT");
  const canDelete = hasPermission("AUDIT.KHKT_TH.DELETE");

  const [years, setYears] = useState<MasterDataItem[]>([]);
  const [businessSegments, setBusinessSegments] = useState<MasterDataItem[]>([]);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [activeTab, setActiveTab] = useState<"candidate" | "confirmed">("candidate");

  const [candidates, setCandidates] = useState<AuditKhktThRowItem[]>([]);
  const [confirmed, setConfirmed] = useState<AuditKhktThRowItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [selectedConfirmedIds, setSelectedConfirmedIds] = useState<string[]>([]);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editing, setEditing] = useState<AuditKhktThRowItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [approving, setApproving] = useState(false);
  const [form] = Form.useForm<EditFormValues>();

  useEffect(() => {
    Promise.all([listMasterDataItems("YEAR"), listMasterDataItems("BUSINESS_SEGMENT")])
      .then(([yearList, segmentList]) => {
        setYears(yearList);
        setBusinessSegments(segmentList);
      })
      .catch(() => message.error(t("auditKhktTh.messages.loadError")));
  }, [message, t]);

  const load = useCallback(async () => {
    if (!year) return;
    setLoading(true);
    try {
      const [candidateList, confirmedList] = await Promise.all([listAuditKhktTh(year), listAuditKhktThConfirmed(year)]);
      setCandidates(candidateList);
      setConfirmed(confirmedList);
    } catch {
      message.error(t("auditKhktTh.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [year, message, t]);

  useEffect(() => {
    if (canView) load();
  }, [canView, load]);

  useEffect(() => {
    setSelectedIds([]);
    setSelectedConfirmedIds([]);
  }, [activeTab, year]);

  const segmentIdByCode = useMemo(() => new Map(businessSegments.map((s) => [s.code, s.id])), [businessSegments]);
  const selectedCandidates = candidates.filter((c) => selectedIds.includes(c.id));

  const handleSync = async () => {
    if (!year) return;
    setSyncing(true);
    try {
      await syncAuditKhktTh(year);
      message.success(t("auditKhktTh.messages.syncSuccess"));
      await load();
    } catch {
      message.error(t("auditKhktTh.messages.saveError"));
    } finally {
      setSyncing(false);
    }
  };

  const openEdit = () => {
    const target = selectedCandidates[0];
    if (!target) return;
    setEditing(target);
    form.setFieldsValue({
      proposalBasisTh: target.proposalBasisTh ?? undefined,
      expertOpinion: target.expertOpinion ?? undefined,
      selection1: target.selection1,
      selection2: target.selection2,
      selection3: target.selection3,
      auditScope: target.auditScope ?? undefined,
      adhocAuditOrSupervision: target.adhocAuditOrSupervision ?? undefined,
      planAdjustment: target.planAdjustment ?? undefined,
      adjustmentReason: target.adjustmentReason ?? undefined,
      khktgsAfterAdjustment: target.khktgsAfterAdjustment ?? undefined,
      thBusinessSegmentIds: target.thBusinessSegmentCodes.map((c) => segmentIdByCode.get(c)).filter((v): v is string => !!v),
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
      await updateAuditKhktTh(editing.id, {
        proposalBasisTh: values.proposalBasisTh ?? null,
        expertOpinion: values.expertOpinion ?? null,
        selection1: values.selection1 ?? false,
        selection2: values.selection2 ?? false,
        selection3: values.selection3 ?? false,
        auditScope: values.auditScope ?? null,
        adhocAuditOrSupervision: values.adhocAuditOrSupervision ?? null,
        planAdjustment: values.planAdjustment ?? null,
        adjustmentReason: values.adjustmentReason ?? null,
        khktgsAfterAdjustment: values.khktgsAfterAdjustment ?? null,
        thBusinessSegmentIds: values.thBusinessSegmentIds ?? [],
      });
      message.success(t("auditKhktTh.messages.updateSuccess"));
      setEditModalOpen(false);
      setSelectedIds([]);
      await load();
    } catch {
      message.error(t("auditKhktTh.messages.saveError"));
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
          : t("auditKhktTh.deleteConfirmTitle"),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await Promise.all(selectedCandidates.map((item) => deleteAuditKhktTh(item.id)));
          message.success(t("auditKhktTh.messages.deleteSuccess"));
          setSelectedIds([]);
          await load();
        } catch {
          message.error(t("auditKhktTh.messages.deleteError"));
        }
      },
    });
  };

  const handleConfirm = () => {
    if (!year) return;
    modal.confirm({
      title: t("auditKhktTh.confirmListTitle"),
      content: t("auditKhktTh.confirmListContent", { count: candidates.length }),
      okText: t("common.yes"),
      cancelText: t("common.no"),
      onOk: async () => {
        setConfirming(true);
        try {
          await confirmAuditKhktTh(year);
          message.success(t("auditKhktTh.messages.confirmSuccess"));
          await load();
        } catch {
          message.error(t("auditKhktTh.messages.saveError"));
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
      await Promise.all(selectedConfirmedIds.map((id) => setAuditKhktThApprovalStatus(id, approved)));
      message.success(t(approved ? "auditKhktTh.messages.approveSuccess" : "auditKhktTh.messages.unapproveSuccess"));
      setSelectedConfirmedIds([]);
      await load();
    } catch {
      message.error(t("auditKhktTh.messages.saveError"));
    } finally {
      setApproving(false);
    }
  };

  const columns: TableProps<AuditKhktThRowItem>["columns"] = [
    {
      title: t("auditKhktTh.columns.sourceType"),
      dataIndex: "sourceType",
      width: 90,
      fixed: "left",
      render: (v: AuditKhktSourceType) => <Tag color={v === "BRANCH" ? "blue" : "orange"}>{t(`auditKhktBp.sourceType.${v}`)}</Tag>,
    },
    { title: t("auditKhktTh.columns.auditObjectCode"), dataIndex: "auditObjectCode", width: 100, fixed: "left" },
    { title: t("auditKhktTh.columns.auditObjectName"), dataIndex: "auditObjectName", width: 180, fixed: "left" },
    { title: t("auditKhktTh.columns.auditObjectCategoryCode"), dataIndex: "auditObjectCategoryCode", width: 110, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhktTh.columns.riskScore"), dataIndex: "riskScore", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("auditKhktTh.columns.rankLabel"), dataIndex: "rankLabel", width: 100, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhktTh.columns.onBalanceSheetLoan"), dataIndex: "onBalanceSheetLoan", width: 130, render: (v: number | null) => v ?? "-" },
    { title: t("auditKhktTh.columns.fundingSource"), dataIndex: "fundingSource", width: 130, render: (v: number | null) => v ?? "-" },
    {
      title: t("auditKhktTh.columns.proposingDepartments"),
      dataIndex: "proposingDepartmentCodes",
      width: 160,
      render: (codes: string[]) => (codes.length === 0 ? "-" : codes.join(", ")),
    },
    {
      title: t("auditKhktTh.columns.bpProposedSegments"),
      dataIndex: "bpProposedSegmentCodes",
      width: 180,
      render: (codes: string[]) => (codes.length === 0 ? "-" : codes.map((c) => <Tag key={c}>{c}</Tag>)),
    },
    {
      title: t("auditKhktTh.columns.thBusinessSegments"),
      dataIndex: "thBusinessSegmentCodes",
      width: 180,
      render: (codes: string[]) => (codes.length === 0 ? "-" : codes.map((c) => <Tag key={c} color="green">{c}</Tag>)),
    },
    { title: t("auditKhktTh.columns.bpReviewResult"), dataIndex: "bpReviewResult", width: 200, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhktTh.columns.proposalBasisTh"), dataIndex: "proposalBasisTh", width: 160, render: (v: string | null) => v ?? "-" },
    { title: t("auditKhktTh.columns.expertOpinion"), dataIndex: "expertOpinion", width: 200, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhktTh.columns.selection1"),
      dataIndex: "selection1",
      width: 90,
      align: "center",
      render: (v: boolean) => (v ? "X" : ""),
    },
    {
      title: t("auditKhktTh.columns.selection2"),
      dataIndex: "selection2",
      width: 90,
      align: "center",
      render: (v: boolean) => (v ? "X" : ""),
    },
    {
      title: t("auditKhktTh.columns.selection3"),
      dataIndex: "selection3",
      width: 90,
      align: "center",
      render: (v: boolean) => (v ? "X" : ""),
    },
    { title: t("auditKhktTh.columns.auditScope"), dataIndex: "auditScope", width: 140, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhktTh.columns.adhocAuditOrSupervision"),
      dataIndex: "adhocAuditOrSupervision",
      width: 110,
      render: (v: AuditKhktSelectionChoice | null) => v ?? "-",
    },
    {
      title: t("auditKhktTh.columns.planAdjustment"),
      dataIndex: "planAdjustment",
      width: 110,
      render: (v: AuditKhktSelectionChoice | null) => v ?? "-",
    },
    { title: t("auditKhktTh.columns.adjustmentReason"), dataIndex: "adjustmentReason", width: 200, render: (v: string | null) => v ?? "-" },
    {
      title: t("auditKhktTh.columns.khktgsAfterAdjustment"),
      dataIndex: "khktgsAfterAdjustment",
      width: 130,
      render: (v: AuditKhktSelectionChoice | null) => v ?? "-",
    },
    {
      title: t("auditKhktTh.columns.approvalStatus"),
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
        {t("auditKhktTh.title")}
      </Typography.Title>
      <Space style={{ marginBottom: 16 }}>
        <Select
          placeholder={t("common.selectYear")}
          style={{ width: 120 }}
          value={year}
          onChange={setYear}
          options={years.map((y) => ({ value: Number(y.code), label: y.code }))}
        />
        <Button icon={<FileExcelOutlined />} disabled={!year} onClick={() => year && exportAuditKhktThReport(year)}>
          {t("auditKhktTh.exportReportButton")}
        </Button>
      </Space>

      {!year ? (
        <Typography.Text type="secondary">{t("auditKhktTh.selectYearHint")}</Typography.Text>
      ) : (
        <Tabs
          activeKey={activeTab}
          onChange={(k) => setActiveTab(k as "candidate" | "confirmed")}
          items={[
            {
              key: "candidate",
              label: t("auditKhktTh.tabCandidate"),
              children: (
                <>
                  <Space style={{ marginBottom: 12 }}>
                    {canCreate && (
                      <Button type="primary" onClick={handleSync} loading={syncing}>
                        {t("auditKhktTh.syncButton")}
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
                        {t("auditKhktTh.confirmListButton")}
                      </Button>
                    )}
                  </Space>
                  <Table<AuditKhktThRowItem>
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
              label: t("auditKhktTh.tabConfirmed"),
              children: (
                <>
                  {canEdit && (
                    <Space style={{ marginBottom: 12 }}>
                      <Button
                        disabled={selectedConfirmedIds.length === 0}
                        loading={approving}
                        onClick={() => handleSetApprovalStatus(true)}
                      >
                        {t("auditKhktTh.approveButton")}
                      </Button>
                      <Button
                        disabled={selectedConfirmedIds.length === 0}
                        loading={approving}
                        onClick={() => handleSetApprovalStatus(false)}
                      >
                        {t("auditKhktTh.unapproveButton")}
                      </Button>
                    </Space>
                  )}
                  <Table<AuditKhktThRowItem>
                    size="small"
                    columns={columns}
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

      <Modal
        title={t("auditKhktTh.form.editTitle")}
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onOk={handleSubmitEdit}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
      >
        <Form<EditFormValues> form={form} layout="vertical">
          <Form.Item name="proposalBasisTh" label={t("auditKhktTh.columns.proposalBasisTh")}>
            <Input maxLength={120} />
          </Form.Item>
          <Form.Item name="expertOpinion" label={t("auditKhktTh.columns.expertOpinion")}>
            <Input.TextArea rows={2} maxLength={250} />
          </Form.Item>
          <Form.Item name="thBusinessSegmentIds" label={t("auditKhktTh.columns.thBusinessSegments")}>
            <Checkbox.Group options={businessSegments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))} />
          </Form.Item>
          <Space size="large">
            <Form.Item name="selection1" label={t("auditKhktTh.columns.selection1")} valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="selection2" label={t("auditKhktTh.columns.selection2")} valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="selection3" label={t("auditKhktTh.columns.selection3")} valuePropName="checked">
              <Switch />
            </Form.Item>
          </Space>
          <Form.Item name="auditScope" label={t("auditKhktTh.columns.auditScope")}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="adhocAuditOrSupervision" label={t("auditKhktTh.columns.adhocAuditOrSupervision")}>
            <Select allowClear options={SELECTION_CHOICES.map((v) => ({ value: v, label: v }))} />
          </Form.Item>
          <Space size="large" style={{ display: "flex" }}>
            <Form.Item name="planAdjustment" label={t("auditKhktTh.columns.planAdjustment")} style={{ flex: 1 }}>
              <Select allowClear options={SELECTION_CHOICES.map((v) => ({ value: v, label: v }))} />
            </Form.Item>
            <Form.Item name="khktgsAfterAdjustment" label={t("auditKhktTh.columns.khktgsAfterAdjustment")} style={{ flex: 1 }}>
              <Select allowClear options={SELECTION_CHOICES.map((v) => ({ value: v, label: v }))} />
            </Form.Item>
          </Space>
          <Form.Item name="adjustmentReason" label={t("auditKhktTh.columns.adjustmentReason")}>
            <Input.TextArea rows={2} maxLength={255} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
