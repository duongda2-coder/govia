import { useEffect, useMemo, useState } from "react";
import { Alert, App, Button, Collapse, Form, Input, InputNumber, List, Select, Space, Tabs, Tag, Typography } from "antd";
import { CopyOutlined, RobotOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import {
  agentDraftApi,
  type AgentInfo,
  type AgentPageContext,
  type RecommendationDraftResult,
  type ReminderDraftResult,
  type RewritePurpose,
  type RewriteResult,
  type TdkpReminderSource,
  type ImportCatalog,
} from "../../api/agent";
import { listAssignedAuditEngagements, type AuditEngagementItem } from "../../api/auditEngagement";
import { listMasterDataItems, type MasterDataItem } from "../../api/auditMasterData";
import { agentErrorKey } from "./agentErrors";
import { DgclScoreTool, ImportCheckTool } from "./AgentG4Tools";

type DraftTool = "recommendation" | "rewrite" | "reminder" | "dgcl" | "importCheck";

/** Cong cu nao mo san theo man hinh dang xem - nguoi dung van doi duoc. */
function defaultTool(path: string): DraftTool {
  if (path.startsWith("/audit/tdkp")) return "reminder";
  if (path.startsWith("/audit/dgcl")) return "dgcl";
  if (path.startsWith("/audit/plan/master-data") || path.startsWith("/audit/master-data/control-point")) return "importCheck";
  if (path.startsWith("/audit/plan/execution/work-management/ttss")) return "recommendation";
  return "rewrite";
}

/** Danh sach TDKP mac dinh theo man hinh TDKP dang mo. */
function defaultReminderSource(path: string): TdkpReminderSource {
  if (path.startsWith("/audit/tdkp/ceo-kh")) return "CEO_KH";
  if (path.startsWith("/audit/tdkp/branch")) return "BRANCH";
  if (path.startsWith("/audit/tdkp/resolution")) return "RESOLUTION";
  if (path.startsWith("/audit/tdkp/unit-recommendation")) return "UNIT";
  return "CEO_ALL";
}

/** Danh muc mo san theo man hinh danh muc dang xem. */
function defaultImportCatalog(path: string): ImportCatalog {
  const qt = path.includes("master-data-qt");
  if (path.includes("control-point")) return qt ? "control_point_qt" : "control_point";
  if (path.includes("work-item")) return qt ? "work_item_qt" : "work_item";
  if (path.includes("exception-type")) return qt ? "exception_type_qt" : "exception_type";
  if (path.includes("exception-mapping")) return qt ? "exception_mapping_qt" : "exception_mapping";
  if (path.includes("process-step")) return qt ? "process_step_qt" : "process_step";
  return "control_point";
}

function useCopy() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  return async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      message.success(t("agent.panel.copied"));
    } catch {
      message.error(t("agent.panel.copyFailed"));
    }
  };
}

interface AgentDraftPanelProps {
  pageContext: AgentPageContext | null;
  agents: AgentInfo[];
}

/**
 * Tab "Soạn nháp" trong khung Tro ly AI (G2-G3). Gom toan bo chuc nang soan nhap vao khung AI - KHONG dat nut
 * nao tren man hinh nghiep vu, khong dien vao form nghiep vu, khong luu, khong gui: AI chi tra ban nhap de
 * nguoi dung SAO CHEP roi tu nhap/gui theo cach lam hien tai.
 */
export function AgentDraftPanel({ pageContext, agents }: AgentDraftPanelProps) {
  const { t } = useTranslation();
  const path = pageContext?.path ?? "";
  const enabled = (code: string) => agents.some((a) => a.code === code && a.enabled);
  const tools = useMemo(
    () =>
      (
        [
          { value: "recommendation", label: t("agent.panel.toolRecommendation"), agent: "A4" },
          { value: "rewrite", label: t("agent.panel.toolRewrite"), agent: "A4" },
          { value: "reminder", label: t("agent.panel.toolReminder"), agent: "A5" },
          { value: "dgcl", label: t("agent.panel.toolDgcl"), agent: "A6" },
          { value: "importCheck", label: t("agent.panel.toolImportCheck"), agent: "A7" },
        ] as const
      ).filter((tool) => enabled(tool.agent)),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [agents, t],
  );
  const [tool, setTool] = useState<DraftTool>(defaultTool(path));

  useEffect(() => {
    setTool(defaultTool(path));
  }, [path]);

  if (tools.length === 0) {
    return <Typography.Text type="secondary">{t("agent.panel.noTools")}</Typography.Text>;
  }
  const active = tools.some((x) => x.value === tool) ? tool : tools[0].value;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <Form layout="vertical" style={{ marginBottom: -12 }}>
        <Form.Item label={t("agent.panel.tool")}>
          <Select<DraftTool> value={active} onChange={setTool} options={tools.map((x) => ({ value: x.value, label: x.label }))} />
        </Form.Item>
      </Form>
      <Alert type="info" showIcon message={t("agent.panel.hint")} />
      {active === "recommendation" && <RecommendationTool />}
      {active === "rewrite" && <RewriteTool defaultPurpose={path.startsWith("/audit/phbc") ? "RECOMMENDATION" : "FINDING"} />}
      {active === "reminder" && <ReminderTool defaultSource={defaultReminderSource(path)} />}
      {active === "dgcl" && <DgclScoreTool />}
      {active === "importCheck" && <ImportCheckTool defaultCatalog={defaultImportCatalog(path)} />}
    </div>
  );
}

function RecommendationTool() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const copy = useCopy();
  const [engagements, setEngagements] = useState<AuditEngagementItem[]>([]);
  const [segments, setSegments] = useState<MasterDataItem[]>([]);
  const [engagementId, setEngagementId] = useState<string>();
  const [segmentId, setSegmentId] = useState<string>();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<RecommendationDraftResult | null>(null);

  useEffect(() => {
    listAssignedAuditEngagements().then(setEngagements).catch(() => setEngagements([]));
    listMasterDataItems("BUSINESS_SEGMENT").then(setSegments).catch(() => setSegments([]));
  }, []);

  const run = async () => {
    if (!engagementId) return;
    setLoading(true);
    setResult(null);
    try {
      setResult(await agentDraftApi.recommendations(engagementId, segmentId));
    } catch (err) {
      message.error(t(agentErrorKey(err)));
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Form layout="vertical">
        <Form.Item label={t("agent.panel.engagement")} required>
          <Select
            showSearch
            optionFilterProp="label"
            value={engagementId}
            onChange={setEngagementId}
            placeholder={t("agent.panel.engagementPlaceholder")}
            options={engagements.map((e) => ({ value: e.id, label: `${e.code} - ${e.auditObjectUnitName ?? ""}` }))}
          />
        </Form.Item>
        <Form.Item label={t("agent.panel.businessSegment")}>
          <Select allowClear value={segmentId} onChange={setSegmentId} options={segments.map((s) => ({ value: s.id, label: `${s.code} - ${s.name}` }))} />
        </Form.Item>
        <Button type="primary" icon={<RobotOutlined />} loading={loading} disabled={!engagementId} onClick={run}>
          {t("agent.panel.runRecommendation")}
        </Button>
      </Form>
      {result && (
        <>
          <Alert type="info" showIcon message={t("agent.draft.recommendationHint", { count: result.sourceTtssCount })} />
          {result.drafts.length === 0 && <Typography.Text type="secondary">{t("agent.draft.noDrafts")}</Typography.Text>}
          <List
            dataSource={result.drafts}
            locale={{ emptyText: <></> }}
            renderItem={(draft, index) => (
              <List.Item key={index} style={{ display: "block" }}>
                <Typography.Paragraph style={{ whiteSpace: "pre-wrap", marginBottom: 6 }}>{draft.content}</Typography.Paragraph>
                <Space wrap size={4} style={{ marginBottom: 6 }}>
                  {draft.findingCodes.map((code) => (
                    <Tag key={code}>{code}</Tag>
                  ))}
                  {!draft.grounded && <Tag color="warning">{t("agent.draft.notGrounded")}</Tag>}
                </Space>
                {draft.rationale && (
                  <Typography.Paragraph type="secondary" style={{ fontSize: 12, marginBottom: 6 }}>
                    {draft.rationale}
                  </Typography.Paragraph>
                )}
                <Button size="small" icon={<CopyOutlined />} onClick={() => copy(draft.content)}>
                  {t("agent.panel.copy")}
                </Button>
              </List.Item>
            )}
          />
          {result.similarRecommendations.length > 0 && (
            <Collapse
              size="small"
              ghost
              items={[
                {
                  key: "similar",
                  label: t("agent.draft.similarTitle", { count: result.similarRecommendations.length }),
                  children: (
                    <List
                      size="small"
                      dataSource={result.similarRecommendations}
                      renderItem={(s) => (
                        <List.Item>
                          <span>
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                              {`${s.engagementCode} · ${s.code}: `}
                            </Typography.Text>
                            {s.content}
                          </span>
                        </List.Item>
                      )}
                    />
                  ),
                },
              ]}
            />
          )}
        </>
      )}
    </>
  );
}

function RewriteTool({ defaultPurpose }: { defaultPurpose: RewritePurpose }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const copy = useCopy();
  const [purpose, setPurpose] = useState<RewritePurpose>(defaultPurpose);
  const [text, setText] = useState("");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<RewriteResult | null>(null);

  const run = async () => {
    if (!text.trim()) {
      message.info(t("agent.draft.rewriteEmpty"));
      return;
    }
    setLoading(true);
    setResult(null);
    try {
      setResult(await agentDraftApi.rewrite(text.trim(), purpose));
    } catch (err) {
      message.error(t(agentErrorKey(err)));
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Form layout="vertical">
        <Form.Item label={t("agent.panel.purpose")}>
          <Select<RewritePurpose>
            value={purpose}
            onChange={setPurpose}
            options={(["RECOMMENDATION", "FINDING", "SUMMARY"] as const).map((p) => ({ value: p, label: t(`agent.panel.purpose${p}`) }))}
          />
        </Form.Item>
        <Form.Item label={t("agent.panel.originalText")}>
          <Input.TextArea value={text} onChange={(e) => setText(e.target.value)} autoSize={{ minRows: 4, maxRows: 10 }} maxLength={4000} showCount />
        </Form.Item>
        <Button type="primary" icon={<RobotOutlined />} loading={loading} onClick={run}>
          {t("agent.draft.rewriteButton")}
        </Button>
      </Form>
      {result && (
        <>
          <Typography.Paragraph style={{ whiteSpace: "pre-wrap", marginBottom: 0 }}>{result.text}</Typography.Paragraph>
          {result.changes.length > 0 && (
            <ul style={{ margin: 0, paddingLeft: 18, color: "rgba(0,0,0,0.45)", fontSize: 12 }}>
              {result.changes.map((c, i) => (
                <li key={i}>{c}</li>
              ))}
            </ul>
          )}
          {!result.grounded && <Alert type="warning" showIcon message={t("agent.draft.rewriteNotGrounded")} />}
          <Button icon={<CopyOutlined />} onClick={() => copy(result.text)} style={{ alignSelf: "flex-start" }}>
            {t("agent.panel.copy")}
          </Button>
        </>
      )}
    </>
  );
}

const REMINDER_SOURCES: TdkpReminderSource[] = ["CEO_ALL", "CEO_KH", "BRANCH", "RESOLUTION", "UNIT"];

function ReminderTool({ defaultSource }: { defaultSource: TdkpReminderSource }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const copy = useCopy();
  const [source, setSource] = useState<TdkpReminderSource>(defaultSource);
  const [days, setDays] = useState<number>(30);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ReminderDraftResult | null>(null);

  useEffect(() => {
    setSource(defaultSource);
  }, [defaultSource]);

  const run = async () => {
    setLoading(true);
    setResult(null);
    try {
      setResult(await agentDraftApi.reminder(source, [], days));
    } catch (err) {
      message.error(t(agentErrorKey(err)));
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Form layout="vertical">
        <Form.Item label={t("agent.panel.reminderSource")}>
          <Select<TdkpReminderSource>
            value={source}
            onChange={setSource}
            options={REMINDER_SOURCES.map((s) => ({ value: s, label: t(`agent.panel.source${s}`) }))}
          />
        </Form.Item>
        <Form.Item label={t("agent.panel.dueWithinDays")} extra={t("agent.panel.reminderScope")}>
          <InputNumber min={1} max={365} value={days} onChange={(v) => setDays(v ?? 30)} />
        </Form.Item>
        <Button type="primary" icon={<RobotOutlined />} loading={loading} onClick={run}>
          {t("agent.panel.runReminder")}
        </Button>
      </Form>
      {result && (
        <>
          <Alert type="info" showIcon message={t("agent.reminder.hint", { count: result.letters.length })} />
          {!result.grounded && <Alert type="warning" showIcon message={t("agent.reminder.fallback")} />}
          <Tabs
            size="small"
            items={result.letters.map((letter, index) => ({
              key: String(index),
              label: `${letter.unit} (${letter.items.length})`,
              children: (
                <Space direction="vertical" size={8} style={{ width: "100%" }}>
                  <Typography.Text strong>{letter.subject}</Typography.Text>
                  <Input.TextArea value={letter.body} readOnly autoSize={{ minRows: 6, maxRows: 16 }} />
                  <Button icon={<CopyOutlined />} onClick={() => copy(`${letter.subject}\n\n${letter.body}`)}>
                    {t("agent.reminder.copy")}
                  </Button>
                </Space>
              ),
            }))}
          />
        </>
      )}
    </>
  );
}
