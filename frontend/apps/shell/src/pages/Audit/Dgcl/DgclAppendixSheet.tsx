import { useCallback, useEffect, useMemo, useState } from "react";
import { App, Badge, Button, Checkbox, Descriptions, Drawer, Input, InputNumber, Space, Switch, Table, Tag, Typography, theme } from "antd";
import { CheckCircleOutlined, LockOutlined, PaperClipOutlined, SaveOutlined, StopOutlined, UnlockOutlined } from "@ant-design/icons";
import type { TableProps } from "antd";
import { useTranslation } from "react-i18next";
import { AttachmentPanel, fetchAttachmentCounts, getApiErrorMessage } from "@govia/ui-kit";
import {
  DGCL_ATTACHMENT_ENTITY,
  getDgclSheet,
  runDgclSheetAction,
  saveDgclSheet,
  type DgclAppendix,
  type DgclCapability,
  type DgclLine,
  type DgclLineInput,
  type DgclSheet,
  type DgclSheetAction,
} from "../../../api/auditDgcl";
import { httpClient } from "../../../api/client";
import { formatPercent, formatScore } from "./dgclShared";

interface Props {
  engagementId: string;
  subjectKey: string;
  subjectSegments: string | null;
  team: boolean;
  appendix: DgclAppendix;
  capability: DgclCapability;
  /** Tab dang mo - PL01F lay diem B1/B2 tu PL01A/PL01B nen tai lai moi lan quay lai tab (neu chua sua gi). */
  active: boolean;
}

/** Dong PL01F to mau (vung vang cua sheet PL 01F): dong diem nhom/tong. */
const HIGHLIGHT_KINDS = new Set([
  "SCORE_A",
  "SCORE_B",
  "SCORE_QUALITY",
  "DEDUCTION_GROUP",
  "WEIGHTED_TOTAL",
  "BONUS_GROUP",
  "PENALTY_GROUP",
  "GRAND_TOTAL",
  "CLASSIFICATION",
  "TOTAL_COUNT",
  "RATIO",
  "SCORE",
]);
/** Dong IV/V/VI + dong "gián đoạn" cuoi PL01A/PL01B (test 29.9). */
const FOOTER_KINDS = new Set(["TOTAL_COUNT", "RATIO", "SCORE"]);
const COMMON_SEGMENT = "CHUNG";
const ATTACHMENT_COUNT_CHUNK = 80;

/** Dong tieu de: muc La Ma cua PL01A/B, dong "A"/"B" cua PL01F. */
const isHeading = (line: DgclLine) => line.header || line.kind === "HEADER";
const isFooter = (line: DgclLine) => line.kind != null && FOOTER_KINDS.has(line.kind);

function toInput(line: DgclLine): DgclLineInput {
  return {
    key: line.key,
    required: line.required,
    compliant: line.compliant,
    nonCompliant: line.nonCompliant,
    checked: line.checked,
    violationCount: line.violationCount,
    maxScore: line.maxScore,
    detail: line.detail,
    document: line.document,
    note: line.note,
  };
}

/** 1 phieu cham diem PL01A / PL01B / PL01F + 4 nut cua sheet logic dong 76-80 (Xác nhận hoàn thành, Hủy xác nhận,
 * Kiểm soát, Hủy kiểm soát). Cot/dong theo dung sheet cung ten; diem tinh o backend sau moi lan Lưu. */
export function DgclAppendixSheet({ engagementId, subjectKey, subjectSegments, team, appendix, capability, active }: Props) {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { token } = theme.useToken();

  const [sheet, setSheet] = useState<DgclSheet | null>(null);
  const [inputs, setInputs] = useState<Record<string, DgclLineInput>>({});
  const [dirty, setDirty] = useState(false);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState<DgclSheetAction | "save" | null>(null);
  const [ownSegmentsOnly, setOwnSegmentsOnly] = useState(false);
  const [attachmentLine, setAttachmentLine] = useState<DgclLine | null>(null);
  const [attachmentCounts, setAttachmentCounts] = useState<Record<string, number>>({});

  const applySheet = useCallback((s: DgclSheet) => {
    setSheet(s);
    setInputs(Object.fromEntries(s.lines.map((l) => [l.key, toInput(l)])));
    setDirty(false);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const s = await getDgclSheet(engagementId, subjectKey, appendix);
      applySheet(s);
      const ids = s.lines.filter((l) => !l.header).map((l) => l.attachmentEntityId);
      const counts: Record<string, number> = {};
      for (let i = 0; i < ids.length; i += ATTACHMENT_COUNT_CHUNK) {
        Object.assign(counts, await fetchAttachmentCounts(httpClient, DGCL_ATTACHMENT_ENTITY, ids.slice(i, i + ATTACHMENT_COUNT_CHUNK)));
      }
      setAttachmentCounts(counts);
    } catch (err) {
      message.error(getApiErrorMessage(err, t("auditDgcl.messages.loadError")));
    } finally {
      setLoading(false);
    }
  }, [engagementId, subjectKey, appendix, applySheet, message, t]);

  useEffect(() => {
    if (active && !dirty) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps -- chi tai lai khi chuyen tab, khong tai lai khi dang sua
  }, [active, load]);

  const update = (key: string, patch: Partial<DgclLineInput>) => {
    setInputs((prev) => {
      const next = { ...prev[key], ...patch };
      // Tuan thu / Khong tuan thu loai tru nhau va chi co nghia tren dong NDTH.
      if (patch.compliant) next.nonCompliant = false;
      if (patch.nonCompliant) next.compliant = false;
      if (patch.compliant || patch.nonCompliant) next.required = true;
      if (patch.required === false) {
        next.compliant = false;
        next.nonCompliant = false;
      }
      return { ...prev, [key]: next };
    });
    setDirty(true);
  };

  const save = async () => {
    setBusy("save");
    try {
      applySheet(await saveDgclSheet(engagementId, subjectKey, appendix, Object.values(inputs)));
      message.success(t("auditDgcl.messages.saveSuccess"));
    } catch (err) {
      message.error(getApiErrorMessage(err, t("auditDgcl.messages.saveError")));
    } finally {
      setBusy(null);
    }
  };

  const runAction = (action: DgclSheetAction) => {
    const execute = async () => {
      setBusy(action);
      try {
        applySheet(await runDgclSheetAction(engagementId, subjectKey, appendix, action));
        message.success(t(`auditDgcl.messages.${action}Success`));
      } catch (err) {
        message.error(getApiErrorMessage(err, t("auditDgcl.messages.actionError")));
      } finally {
        setBusy(null);
      }
    };
    modal.confirm({ title: t(`auditDgcl.confirmAction.${action}`), onOk: execute });
  };

  const attachmentEntityId = attachmentLine?.attachmentEntityId;
  const onAttachmentCount = useCallback(
    (count: number) => {
      if (attachmentEntityId) setAttachmentCounts((prev) => (prev[attachmentEntityId] === count ? prev : { ...prev, [attachmentEntityId]: count }));
    },
    [attachmentEntityId],
  );

  const segments = useMemo(() => new Set((subjectSegments ?? "").split(",").map((c) => c.trim().toUpperCase()).filter(Boolean)), [subjectSegments]);
  const canFilterSegments = appendix === "PL01B" && !team && segments.size > 0;
  const lines = useMemo(() => {
    const all = sheet?.lines ?? [];
    if (!canFilterSegments || !ownSegmentsOnly) return all;
    return all.filter((l) => !l.segment || l.segment === COMMON_SEGMENT || segments.has(l.segment.toUpperCase()));
  }, [sheet, canFilterSegments, ownSegmentsOnly, segments]);

  const editable = !!sheet?.canEdit;
  const summary = sheet?.summary;
  // Phieu chua luu: cac o tich san chua duoc ghi, cho Lưu ngay ca khi chua sua gi.
  const unsaved = !!sheet && !sheet.saved;
  const dash = (v: string | null | undefined) => v ?? "-";
  const value = (key: string) => inputs[key];

  const attachmentCell = (line: DgclLine) => {
    if (line.header || isFooter(line)) return null;
    const count = attachmentCounts[line.attachmentEntityId] ?? 0;
    return (
      <Badge count={count} size="small">
        <Button size="small" type="text" icon={<PaperClipOutlined />} onClick={() => setAttachmentLine(line)} aria-label={t("common.attachment")} />
      </Badge>
    );
  };

  const textCell = (field: "detail" | "document" | "note", maxLength: number) => (_: unknown, line: DgclLine) => {
    if (line.header || line.kind === "NOTE" || line.kind === "HEADER" || isFooter(line)) return null;
    const v = value(line.key)?.[field];
    return editable ? (
      <Input size="small" value={v ?? ""} maxLength={maxLength} onChange={(e) => update(line.key, { [field]: e.target.value })} />
    ) : (
      dash(v)
    );
  };

  const sttColumn = { title: t("auditDgcl.columns.stt"), dataIndex: "stt", width: 60, render: (v: string | null) => v || "" };
  const contentColumn = {
    title: appendix === "PL01A" ? t("auditDgcl.columns.controlContent") : t("auditDgcl.columns.criteria"),
    dataIndex: "content",
    width: 520,
    render: (v: string, line: DgclLine) => (
      <Typography.Text strong={isHeading(line) || (line.kind != null && HIGHLIGHT_KINDS.has(line.kind))} style={{ whiteSpace: "pre-line" }}>
        {v}
      </Typography.Text>
    ),
  };
  const tailColumns: TableProps<DgclLine>["columns"] = [
    { title: t("auditDgcl.columns.detail"), width: 220, render: textCell("detail", 2000) },
    { title: t("auditDgcl.columns.document"), width: 220, render: textCell("document", 1000) },
    { title: t("common.attachment"), width: 70, align: "center", render: (_: unknown, line) => attachmentCell(line) },
    { title: t("auditDgcl.columns.lineEvaluator"), dataIndex: "evaluatorName", width: 160, render: (v: string | null, line) => (line.header || isFooter(line) ? null : dash(v)) },
    { title: t("auditDgcl.columns.note"), width: 200, render: textCell("note", 1000) },
  ];

  const summaryCount = (field: "required" | "compliant" | "nonCompliant") =>
    field === "required" ? summary?.requiredCount : field === "compliant" ? summary?.compliantCount : summary?.nonCompliantCount;

  /** Dong IV: dem so dong tich o tung cot; V: ty le; VI: diem cham (sau quy tac "gián đoạn") - dat o cot Tuân thủ nhu sheet PL 01A. */
  const footerCell = (field: "required" | "compliant" | "nonCompliant", line: DgclLine) => {
    const text = (v: string | number | null | undefined) => <Typography.Text strong>{v ?? "-"}</Typography.Text>;
    if (line.kind === "TOTAL_COUNT") return text(summaryCount(field) ?? 0);
    if (field !== "compliant") return null;
    return line.kind === "RATIO" ? text(formatPercent(summary?.ratio)) : text(formatScore(summary?.score));
  };

  const checkboxCell = (field: "required" | "compliant" | "nonCompliant") => (_: unknown, line: DgclLine) => {
    if (line.header) return null;
    if (isFooter(line)) return footerCell(field, line);
    if (line.kind === "DISRUPTION") {
      return field === "required" ? (
        <Checkbox checked={!!value(line.key)?.checked} disabled={!editable} onChange={(e) => update(line.key, { checked: e.target.checked })} />
      ) : null;
    }
    return <Checkbox checked={!!value(line.key)?.[field]} disabled={!editable} onChange={(e) => update(line.key, { [field]: e.target.checked })} />;
  };

  const complianceColumns: TableProps<DgclLine>["columns"] = [
    sttColumn,
    contentColumn,
    ...(appendix === "PL01B"
      ? [{ title: t("auditDgcl.columns.lineSegment"), dataIndex: "segment", width: 90, render: (v: string | null, line: DgclLine) => (line.header || line.kind ? null : dash(v)) }]
      : []),
    { title: t("auditDgcl.columns.required"), width: 70, align: "center" as const, render: checkboxCell("required") },
    { title: t("auditDgcl.columns.compliant"), width: 80, align: "center" as const, render: checkboxCell("compliant") },
    { title: t("auditDgcl.columns.nonCompliant"), width: 100, align: "center" as const, render: checkboxCell("nonCompliant") },
    ...tailColumns,
  ];

  const qualityColumns: TableProps<DgclLine>["columns"] = [
    sttColumn,
    contentColumn,
    {
      title: t("auditDgcl.columns.maxScore"),
      width: 110,
      align: "right",
      // test 29.9: diem toi da I/II/III co dinh 100, khong cho nhap.
      render: (_: unknown, line) => formatScore(line.calcMax),
    },
    {
      title: t("auditDgcl.columns.violationCount"),
      width: 120,
      align: "center",
      render: (_: unknown, line) => {
        if (line.kind === "DEDUCTION") {
          return editable ? (
            <InputNumber size="small" min={0} precision={0} style={{ width: 80 }} value={value(line.key)?.violationCount ?? null} onChange={(v) => update(line.key, { violationCount: v })} />
          ) : (
            (value(line.key)?.violationCount ?? "")
          );
        }
        if (line.kind === "BONUS" || line.kind === "PENALTY") {
          return <Checkbox checked={!!value(line.key)?.checked} disabled={!editable} onChange={(e) => update(line.key, { checked: e.target.checked })} />;
        }
        return null;
      },
    },
    { title: t("auditDgcl.columns.deduction"), dataIndex: "calcDeduction", width: 90, align: "right", render: (v: number | null) => formatScore(v) },
    { title: t("auditDgcl.columns.points"), dataIndex: "calcPoints", width: 90, align: "right", render: (v: number | null) => formatScore(v) },
    {
      title: t("auditDgcl.columns.ratio"),
      width: 170,
      align: "right",
      render: (_: unknown, line) => (line.kind === "CLASSIFICATION" ? <Typography.Text strong>{sheet?.summary.classification ?? "-"}</Typography.Text> : formatPercent(line.calcRatio)),
    },
    ...tailColumns,
  ];

  const statusTag = sheet?.controlled ? (
    <Tag icon={<LockOutlined />} color="red">
      {t("auditDgcl.status.controlled", { name: sheet.controlledBy ?? "" })}
    </Tag>
  ) : sheet?.confirmed ? (
    <Tag icon={<CheckCircleOutlined />} color="green">
      {t("auditDgcl.status.confirmed", { name: sheet.confirmedBy ?? "" })}
    </Tag>
  ) : (
    <Tag>{t("auditDgcl.status.draft")}</Tag>
  );

  return (
    <div>
      <Space wrap style={{ marginBottom: 12 }}>
        {capability.canEvaluate && (
          <>
            <Button type="primary" icon={<SaveOutlined />} disabled={!editable || (!dirty && !unsaved)} loading={busy === "save"} onClick={save}>
              {t("common.save")}
            </Button>
            <Button icon={<CheckCircleOutlined />} disabled={!sheet?.canConfirm || dirty} loading={busy === "confirm"} onClick={() => runAction("confirm")}>
              {t("auditDgcl.actions.confirm")}
            </Button>
            <Button icon={<StopOutlined />} disabled={!sheet?.canUnconfirm} loading={busy === "unconfirm"} onClick={() => runAction("unconfirm")}>
              {t("auditDgcl.actions.unconfirm")}
            </Button>
          </>
        )}
        {capability.canControl && (
          <>
            <Button icon={<LockOutlined />} disabled={!sheet?.canControl} loading={busy === "control"} onClick={() => runAction("control")}>
              {t("auditDgcl.actions.control")}
            </Button>
            <Button icon={<UnlockOutlined />} disabled={!sheet?.canUncontrol} loading={busy === "uncontrol"} onClick={() => runAction("uncontrol")}>
              {t("auditDgcl.actions.uncontrol")}
            </Button>
          </>
        )}
        {statusTag}
        {dirty && <Typography.Text type="warning">{t("auditDgcl.unsavedHint")}</Typography.Text>}
      </Space>

      {!capability.canEvaluate && !capability.canControl && (
        <Typography.Paragraph type="secondary">{t("auditDgcl.readOnlyHint")}</Typography.Paragraph>
      )}

      {summary && (
        <Descriptions size="small" bordered column={{ xs: 1, sm: 1, md: 2, xl: 4 }} style={{ marginBottom: 12 }}>
          {appendix === "PL01F" ? (
            <>
              <Descriptions.Item label={t("auditDgcl.summary.grandTotal")}>{formatScore(summary.score) ?? "-"}</Descriptions.Item>
              <Descriptions.Item label={t("auditDgcl.summary.ratio")}>{formatPercent(summary.ratio) ?? "-"}</Descriptions.Item>
              <Descriptions.Item label={t("auditDgcl.summary.bonusPenalty")}>
                +{formatScore(summary.bonusPoints) ?? 0} / -{formatScore(summary.penaltyPoints) ?? 0}
              </Descriptions.Item>
              <Descriptions.Item label={t("auditDgcl.columns.classification")}>{summary.classification ?? "-"}</Descriptions.Item>
            </>
          ) : (
            <>
              <Descriptions.Item label={t("auditDgcl.summary.total")}>
                {t("auditDgcl.summary.counts", { required: summary.requiredCount ?? 0, compliant: summary.compliantCount ?? 0, nonCompliant: summary.nonCompliantCount ?? 0 })}
              </Descriptions.Item>
              <Descriptions.Item label={t("auditDgcl.summary.ratio")}>{formatPercent(summary.ratio) ?? "-"}</Descriptions.Item>
              <Descriptions.Item label={t("auditDgcl.summary.score")}>{formatScore(summary.score) ?? "-"}</Descriptions.Item>
              <Descriptions.Item label={t("auditDgcl.summary.evaluator")}>{sheet?.evaluatorName ?? "-"}</Descriptions.Item>
            </>
          )}
        </Descriptions>
      )}

      {unsaved && editable && appendix !== "PL01F" && <Typography.Paragraph type="warning">{t("auditDgcl.preTickedHint")}</Typography.Paragraph>}
      {canFilterSegments && (
        <Space style={{ marginBottom: 8 }}>
          <Switch size="small" checked={ownSegmentsOnly} onChange={setOwnSegmentsOnly} />
          <Typography.Text>{t("auditDgcl.ownSegmentsOnly", { segments: subjectSegments })}</Typography.Text>
        </Space>
      )}
      {appendix === "PL01F" && <Typography.Paragraph type="secondary">{t("auditDgcl.pl01fHint")}</Typography.Paragraph>}

      <Table<DgclLine>
        size="small"
        bordered
        rowKey="key"
        loading={loading}
        columns={appendix === "PL01F" ? qualityColumns : complianceColumns}
        dataSource={lines}
        pagination={false}
        scroll={{ x: "max-content", y: "calc(100vh - 420px)" }}
        onRow={(line) => ({
          style:
            line.kind && HIGHLIGHT_KINDS.has(line.kind) ? { background: token.colorWarningBg } : isHeading(line) ? { background: token.colorFillQuaternary } : undefined,
        })}
      />

      <Drawer title={t("common.attachment")} open={!!attachmentLine} onClose={() => setAttachmentLine(null)} width={480} destroyOnClose>
        {attachmentLine && (
          <>
            <Typography.Paragraph type="secondary">
              {attachmentLine.stt ? `${attachmentLine.stt}. ` : ""}
              {attachmentLine.content}
            </Typography.Paragraph>
            <AttachmentPanel
              http={httpClient}
              entityName={DGCL_ATTACHMENT_ENTITY}
              entityId={attachmentLine.attachmentEntityId}
              readOnly={!editable}
              onCountChange={onAttachmentCount}
            />
          </>
        )}
      </Drawer>
    </div>
  );
}
