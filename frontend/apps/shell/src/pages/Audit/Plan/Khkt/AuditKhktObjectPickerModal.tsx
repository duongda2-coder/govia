import { useEffect, useMemo, useState } from "react";
import { App, Modal, Table, Tag } from "antd";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { riskAssessmentOtherExpertRankApi, riskBranchScoreExpertRankApi } from "../../../../api/riskScoringExec";
import type { AuditKhktSourceType } from "../../../../api/auditKhktBp";

interface PickerRow {
  key: string;
  sourceType: AuditKhktSourceType;
  auditObjectCode: string;
  auditObjectName: string;
  auditObjectCategoryCode: string | null;
  riskScore: number | null;
  rankLabel: string | null;
}

export interface AuditKhktObjectPickerModalProps {
  open: boolean;
  year: number;
  /** Ma DTKT da co san trong danh sach de xuat cua phong+nam nay - loai khoi danh sach chon. */
  excludeCodes: string[];
  onClose: () => void;
  onPick: (rows: { sourceType: AuditKhktSourceType; auditObjectCode: string; auditObjectName: string; auditObjectCategoryCode: string | null; riskScore: number | null; rankLabel: string | null }[]) => Promise<void>;
}

/** Dialog "Chon doi tuong kiem toan" cho man hinh De xuat DTKT nam theo phong (ZTC_KHKT_BP) - gop
 * ket qua danh gia rui ro chi nhanh (RiskBranchScoreExpertRank) va doi tuong khac
 * (RiskAssessmentOtherExpertRank) trong 1 danh sach, snapshot Diem rui ro/Xep hang tai thoi diem
 * chon (xem AuditKhktBpService.create o BE - khong link dong toi bang goc). */
export function AuditKhktObjectPickerModal({ open, year, excludeCodes, onClose, onPick }: AuditKhktObjectPickerModalProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [rows, setRows] = useState<PickerRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedKeys, setSelectedKeys] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) {
      setSelectedKeys([]);
      return;
    }
    setLoading(true);
    Promise.all([riskBranchScoreExpertRankApi.list(year), riskAssessmentOtherExpertRankApi.list(year)])
      .then(([branches, others]) => {
        const branchRows: PickerRow[] = branches.map((b) => ({
          key: `BRANCH:${b.branchCode}`,
          sourceType: "BRANCH",
          auditObjectCode: b.branchCode,
          auditObjectName: b.branchName ?? b.branchCode,
          auditObjectCategoryCode: null,
          riskScore: b.totalScore,
          rankLabel: b.finalRankLabel ?? b.reRankLabel ?? b.baseRankLabel,
        }));
        const otherRows: PickerRow[] = others.map((o) => ({
          key: `OTHER:${o.auditObjectCode}`,
          sourceType: "OTHER",
          auditObjectCode: o.auditObjectCode,
          auditObjectName: o.auditObjectName ?? o.auditObjectCode,
          auditObjectCategoryCode: o.auditObjectCategoryCode,
          riskScore: o.riskScore,
          rankLabel: o.finalRankLabel ?? o.reRankLabel ?? o.baseRankLabel,
        }));
        setRows([...branchRows, ...otherRows]);
      })
      .catch(() => message.error(t("auditKhktBp.messages.loadError")))
      .finally(() => setLoading(false));
  }, [open, year, message, t]);

  const excludeSet = useMemo(() => new Set(excludeCodes), [excludeCodes]);
  const availableRows = rows.filter((r) => !excludeSet.has(r.auditObjectCode));

  const handlePick = async () => {
    const selected = availableRows.filter((r) => selectedKeys.includes(r.key));
    if (selected.length === 0) return;
    setSubmitting(true);
    try {
      await onPick(selected.map((r) => ({
        sourceType: r.sourceType,
        auditObjectCode: r.auditObjectCode,
        auditObjectName: r.auditObjectName,
        auditObjectCategoryCode: r.auditObjectCategoryCode,
        riskScore: r.riskScore,
        rankLabel: r.rankLabel,
      })));
      setSelectedKeys([]);
    } finally {
      setSubmitting(false);
    }
  };

  const columns: TableProps<PickerRow>["columns"] = [
    {
      title: t("auditKhktBp.columns.sourceType"),
      dataIndex: "sourceType",
      width: 100,
      render: (v: AuditKhktSourceType) => <Tag color={v === "BRANCH" ? "blue" : "orange"}>{t(`auditKhktBp.sourceType.${v}`)}</Tag>,
    },
    { title: t("auditKhktBp.columns.auditObjectCode"), dataIndex: "auditObjectCode", width: 110 },
    { title: t("auditKhktBp.columns.auditObjectName"), dataIndex: "auditObjectName" },
    {
      title: t("auditKhktBp.columns.auditObjectCategoryCode"),
      dataIndex: "auditObjectCategoryCode",
      width: 140,
      render: (v: string | null) => v ?? "-",
    },
    { title: t("auditKhktBp.columns.riskScore"), dataIndex: "riskScore", width: 100, render: (v: number | null) => v ?? "-" },
    { title: t("auditKhktBp.columns.rankLabel"), dataIndex: "rankLabel", width: 100, render: (v: string | null) => v ?? "-" },
  ];

  return (
    <Modal
      title={t("auditKhktBp.picker.title")}
      open={open}
      onCancel={onClose}
      onOk={handlePick}
      confirmLoading={submitting}
      okButtonProps={{ disabled: selectedKeys.length === 0 }}
      okText={t("auditKhktBp.picker.addSelected", { count: selectedKeys.length })}
      width={900}
      destroyOnClose
    >
      <Table<PickerRow>
        size="small"
        rowKey="key"
        loading={loading}
        columns={columns}
        dataSource={availableRows}
        pagination={{ pageSize: 10 }}
        rowSelection={{ selectedRowKeys: selectedKeys, onChange: (keys) => setSelectedKeys(keys as string[]) }}
      />
    </Modal>
  );
}
