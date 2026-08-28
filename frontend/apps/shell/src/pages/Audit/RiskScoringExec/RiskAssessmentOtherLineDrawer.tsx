import { useCallback, useEffect, useState } from "react";
import { App, Drawer, Select, Table, Typography } from "antd";
import { useTranslation } from "react-i18next";
import {
  riskAssessmentOtherApi,
  riskCriteriaOtherScaleApi,
  type RiskAssessmentOtherHeaderItem,
  type RiskAssessmentOtherLineItem,
  type RiskCriteriaOtherScaleItem,
} from "../../../api/riskScoringExec";

interface Props {
  open: boolean;
  header: RiskAssessmentOtherHeaderItem | null;
  onClose: () => void;
}

/**
 * Drawer "Cham diem cac chi tieu rui ro" cua 1 header ZTC_CDRR_KHAC - he thong tu dong sinh 1
 * dong cho moi chi tieu phu hop, NSD chi can chon Diem cho tung dong (khong them/xoa dong).
 */
export function RiskAssessmentOtherLineDrawer({ open, header, onClose }: Props) {
  const { t } = useTranslation();
  const { message } = App.useApp();

  const [lines, setLines] = useState<RiskAssessmentOtherLineItem[]>([]);
  const [scales, setScales] = useState<RiskCriteriaOtherScaleItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [savingLineId, setSavingLineId] = useState<string | null>(null);

  const load = useCallback(async (headerId: string) => {
    setLoading(true);
    try {
      const [lineList, scaleList] = await Promise.all([riskAssessmentOtherApi.lines(headerId), riskCriteriaOtherScaleApi.list()]);
      setLines(lineList);
      setScales(scaleList);
    } catch {
      message.error(t("riskScoringExec.messages.loadError"));
    } finally {
      setLoading(false);
    }
  }, [message, t]);

  useEffect(() => {
    if (open && header) load(header.id);
  }, [open, header, load]);

  const handleScoreChange = async (line: RiskAssessmentOtherLineItem, scaleId: string | null) => {
    if (!header) return;
    setSavingLineId(line.id);
    try {
      const updated = await riskAssessmentOtherApi.updateLine(header.id, line.id, { scaleId });
      setLines((prev) => prev.map((l) => (l.id === line.id ? updated : l)));
    } catch {
      message.error(t("riskScoringExec.messages.saveError"));
    } finally {
      setSavingLineId(null);
    }
  };

  return (
    <Drawer
      title={header ? `${t("riskScoringExec.assessmentOther.scoreLines")} - ${header.auditObjectCode} / ${header.year}` : t("riskScoringExec.assessmentOther.scoreLines")}
      open={open}
      onClose={onClose}
      width={720}
    >
      {header && (
        <Typography.Paragraph type="secondary">
          {header.auditObjectCategoryCode} - {header.auditObjectName ?? header.auditObjectCode}
        </Typography.Paragraph>
      )}
      <Table<RiskAssessmentOtherLineItem>
        rowKey="id"
        loading={loading}
        dataSource={lines}
        pagination={false}
        columns={[
          { title: t("riskScoringExec.columns.criteriaCode"), dataIndex: "criteriaOtherCode", width: 100, render: (v: string | null) => v ?? "-" },
          { title: t("riskScoringExec.columns.criteriaName"), dataIndex: "criteriaOtherName", render: (v: string | null) => v ?? "-" },
          {
            title: t("riskScoringExec.columns.scaleScore"),
            width: 260,
            render: (_: unknown, record: RiskAssessmentOtherLineItem) => {
              const options = scales
                .filter((s) => s.criteriaOtherId === record.criteriaOtherId)
                .map((s) => ({ value: s.id, label: `${s.scaleScore} - ${s.ratingLevel}` }));
              return (
                <Select
                  style={{ width: "100%" }}
                  allowClear
                  placeholder={t("riskScoringExec.assessmentOther.selectScore")}
                  value={record.scaleId ?? undefined}
                  options={options}
                  loading={savingLineId === record.id}
                  onChange={(value) => handleScoreChange(record, value ?? null)}
                />
              );
            },
          },
        ]}
      />
    </Drawer>
  );
}
