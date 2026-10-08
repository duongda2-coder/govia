import { useEffect, useMemo, useRef, useState } from "react";
import { Alert, App, Button, Collapse, Input, List, Select, Space, Spin, Tag, Tooltip, Typography } from "antd";
import { DeleteOutlined, EyeOutlined, PlusOutlined, RobotOutlined, SendOutlined, UserOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { isAxiosError } from "axios";
import {
  agentApi,
  type AgentChatResponse,
  type AgentConversationSummary,
  type AgentPageContext,
  type AgentScreenRef,
  type AgentStoredMessage,
} from "../../../api/agent";

interface ChatEntry {
  role: "user" | "assistant" | "error";
  text?: string;
  response?: AgentChatResponse;
}

const LOADING_STEP_KEYS = ["agent.chat.loading.analyze", "agent.chat.loading.data", "agent.chat.loading.evidence", "agent.chat.loading.compose"];
const CONVERSATION_STORAGE_KEY = "govia.agent.conversationId";
/** Nhom cau goi y theo man hinh dang mo - khop voi duong dan ma AgentRouter (backend) dung de chon agent. */
const SUGGESTION_GROUPS: [prefix: string, group: string][] = [
  ["/audit/risk-scoring", "risk"],
  ["/audit/plan/execution/work-management/ttss", "finding"],
  ["/audit/phbc", "finding"],
  ["/audit/tdkp", "remediation"],
  ["/audit/plan/khkt", "plan"],
  ["/audit/plan/khns", "plan"],
  ["/audit/plan/khth", "plan"],
  ["/audit/plan/execution", "execution"],
  ["/audit/plan/engagement", "execution"],
  ["/audit/master-data", "catalog"],
  ["/audit/plan/master-data", "catalog"],
];

function suggestionGroupFor(path: string): string {
  return SUGGESTION_GROUPS.find(([prefix]) => path.startsWith(prefix))?.[1] ?? "general";
}
const SCREEN_PATH_PATTERN = /(\/(?:audit|people|admin|workflow)[\w\-/]*)/g;

function readStoredConversationId(): string | null {
  try {
    return sessionStorage.getItem(CONVERSATION_STORAGE_KEY);
  } catch {
    return null;
  }
}

function storeConversationId(id: string) {
  try {
    sessionStorage.setItem(CONVERSATION_STORAGE_KEY, id);
  } catch {
    // sessionStorage bi chan (che do rieng tu) - hoi thoai van chay, chi khong mo lai duoc sau khi tai trang
  }
}

function toEntries(messages: AgentStoredMessage[]): ChatEntry[] {
  return messages.map((m) =>
    m.role === "USER"
      ? { role: "user", text: m.content }
      : {
          role: "assistant",
          response: m.response ?? {
            answer: m.content,
            facts: [],
            analysis: [],
            recommendations: [],
            evidence: [],
            metadata: { model: "", timestamp: m.createdAt, toolsUsed: [], truncated: false, grounded: true },
            agentCode: m.agentCode,
            agentName: null,
          },
        },
  );
}

interface AuditAgentChatProps {
  pageContext?: AgentPageContext | null;
  screens?: AgentScreenRef[];
  llmReachable?: boolean;
}

/** Khung chat "Tro ly AI" - goi /api/audit/agent/chat (A0 dieu phoi chon agent chuyen trach phia backend).
 * Hoi thoai luu DB: id giu trong sessionStorage nen dong/mo lai khung chat hay tai lai trang van xem tiep
 * duoc, va chon lai duoc cac cuoc tro chuyen truoc. Khong hien chain-of-thought - chi hien
 * Answer/Facts/Analysis/Recommendations/Evidence. */
export function AuditAgentChat({ pageContext = null, screens = [], llmReachable = true }: AuditAgentChatProps) {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const [conversationId, setConversationId] = useState<string>(() => readStoredConversationId() ?? crypto.randomUUID());
  const [entries, setEntries] = useState<ChatEntry[]>([]);
  const [history, setHistory] = useState<AgentConversationSummary[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingStepIndex, setLoadingStepIndex] = useState(0);
  const [contextDismissed, setContextDismissed] = useState(false);
  const listEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    storeConversationId(conversationId);
  }, [conversationId]);

  // Mo lai hoi thoai dang do (vd sau khi tai lai trang); 404/403 = hoi thoai chua luu hoac khong con - bat dau trang
  useEffect(() => {
    const stored = readStoredConversationId();
    if (!stored) return;
    agentApi
      .messages(stored)
      .then((messages) => setEntries(toEntries(messages)))
      .catch(() => setEntries([]));
  }, []);

  useEffect(() => {
    setContextDismissed(false);
  }, [pageContext?.path]);

  useEffect(() => {
    listEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [entries, loading]);

  useEffect(() => {
    if (!loading) return;
    setLoadingStepIndex(0);
    const timer = setInterval(() => {
      setLoadingStepIndex((i) => Math.min(i + 1, LOADING_STEP_KEYS.length - 1));
    }, 3000);
    return () => clearInterval(timer);
  }, [loading]);

  const activeContext = contextDismissed ? null : pageContext;
  const knownPaths = useMemo(() => new Set(screens.map((s) => s.path)), [screens]);
  const suggestionGroup = suggestionGroupFor(pageContext?.path ?? "");
  const suggestionKeys = [1, 2, 3].map((n) => `agent.suggestions.${suggestionGroup}${n}`);

  const loadHistory = () => {
    agentApi
      .conversations()
      .then(setHistory)
      .catch(() => setHistory([]));
  };

  const send = async (raw?: string) => {
    const text = (raw ?? input).trim();
    if (!text || loading) return;
    setEntries((prev) => [...prev, { role: "user", text }]);
    setInput("");
    setLoading(true);
    try {
      const response = await agentApi.chat(conversationId, text, activeContext, screens);
      setEntries((prev) => [...prev, { role: "assistant", response }]);
    } catch (err) {
      const code = isAxiosError<{ errorCode?: string }>(err) ? err.response?.data?.errorCode : undefined;
      const key =
        code === "AGENT_DISABLED"
          ? "agent.chat.errorDisabled"
          : code === "AGENT_LLM_UNAVAILABLE"
            ? "agent.chat.errorPaused"
            : "agent.chat.errorUnavailable";
      setEntries((prev) => [...prev, { role: "error", text: t(key) }]);
    } finally {
      setLoading(false);
    }
  };

  const startNewConversation = () => {
    setConversationId(crypto.randomUUID());
    setEntries([]);
    setInput("");
  };

  const openConversation = async (id: string) => {
    try {
      const messages = await agentApi.messages(id);
      setConversationId(id);
      setEntries(toEntries(messages));
    } catch {
      message.error(t("agent.chat.errorLoadConversation"));
    }
  };

  const deleteConversation = () => {
    modal.confirm({
      title: t("agent.chat.deleteConfirmTitle"),
      content: t("agent.chat.deleteConfirmContent"),
      okButtonProps: { danger: true },
      okText: t("common.delete"),
      cancelText: t("common.cancel"),
      onOk: async () => {
        try {
          await agentApi.archive(conversationId);
        } catch {
          // Hoi thoai chua tung duoc luu (chua gui cau nao thanh cong) - chi can bat dau moi
        }
        startNewConversation();
        loadHistory();
      },
    });
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%", gap: 12 }}>
      {!llmReachable && <Alert type="warning" showIcon message={t("agent.chat.pausedBanner")} />}

      <Space.Compact style={{ width: "100%" }}>
        <Select
          style={{ flex: 1, minWidth: 0 }}
          placeholder={t("agent.chat.historyPlaceholder")}
          value={history.some((h) => h.id === conversationId) ? conversationId : undefined}
          onOpenChange={(visible) => visible && loadHistory()}
          onChange={openConversation}
          options={history.map((h) => ({ value: h.id, label: h.title }))}
          notFoundContent={t("agent.chat.historyEmpty")}
        />
        <Tooltip title={t("agent.chat.newConversation")}>
          <Button icon={<PlusOutlined />} onClick={startNewConversation} />
        </Tooltip>
        <Tooltip title={t("agent.chat.deleteConversation")}>
          <Button icon={<DeleteOutlined />} danger disabled={entries.length === 0} onClick={deleteConversation} />
        </Tooltip>
      </Space.Compact>

      {activeContext && (
        <div>
          <Tag icon={<EyeOutlined />} color="blue" closable onClose={() => setContextDismissed(true)} style={{ whiteSpace: "normal" }}>
            {t("agent.chat.viewing", {
              screen: activeContext.groupLabel ? `${activeContext.groupLabel} / ${activeContext.screenLabel}` : activeContext.screenLabel,
            })}
          </Tag>
        </div>
      )}

      <div style={{ flex: 1, overflowY: "auto", padding: "0 4px" }}>
        {entries.length === 0 && (
          <Space direction="vertical" size={8} style={{ width: "100%" }}>
            <Typography.Text type="secondary">{t("agent.chat.emptyHint")}</Typography.Text>
            {suggestionKeys.map((key) => (
              <Button key={key} block style={{ justifyContent: "flex-start", textAlign: "left", height: "auto", whiteSpace: "normal", padding: "4px 12px" }} onClick={() => send(t(key))} disabled={loading}>
                {t(key)}
              </Button>
            ))}
          </Space>
        )}
        <List
          dataSource={entries}
          locale={{ emptyText: <></> }}
          renderItem={(entry, index) => (
            <List.Item key={index} style={{ border: "none", padding: "8px 0" }}>
              {entry.role === "user" && (
                <Space align="start" style={{ width: "100%", justifyContent: "flex-end" }}>
                  <div style={{ background: "#eff6ff", borderRadius: 8, padding: "8px 12px", maxWidth: "80%", whiteSpace: "pre-wrap" }}>
                    {entry.text}
                  </div>
                  <UserOutlined style={{ fontSize: 18, marginTop: 6 }} />
                </Space>
              )}
              {entry.role === "error" && <Alert type="error" showIcon message={entry.text} style={{ width: "100%" }} />}
              {entry.role === "assistant" && entry.response && (
                <Space align="start" style={{ width: "100%" }}>
                  <RobotOutlined style={{ fontSize: 18, marginTop: 6 }} />
                  <AgentAnswerCard response={entry.response} knownPaths={knownPaths} />
                </Space>
              )}
            </List.Item>
          )}
        />
        {loading && (
          <Space style={{ padding: "8px 0" }}>
            <Spin size="small" />
            <Typography.Text type="secondary">{t(LOADING_STEP_KEYS[loadingStepIndex])}</Typography.Text>
          </Space>
        )}
        <div ref={listEndRef} />
      </div>

      <Space.Compact style={{ width: "100%" }}>
        <Input.TextArea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={t("agent.chat.inputPlaceholder")}
          autoSize={{ minRows: 1, maxRows: 4 }}
          disabled={loading}
          maxLength={4000}
        />
        <Button type="primary" icon={<SendOutlined />} onClick={() => send()} disabled={loading || !input.trim()} />
      </Space.Compact>
    </div>
  );
}

/** Duong dan man hinh trong cau tra loi (vd "/audit/plan/khkt-thang") thanh link bam duoc - chi voi duong
 * dan co trong menu cua chinh nguoi dung, chuoi giong duong dan khac giu nguyen la chu. */
function LinkifiedText({ text, knownPaths }: { text: string; knownPaths: Set<string> }) {
  const navigate = useNavigate();
  const parts = text.split(SCREEN_PATH_PATTERN);
  return (
    <>
      {parts.map((part, i) =>
        knownPaths.has(part) ? (
          <Typography.Link key={i} onClick={() => navigate(part)}>
            {part}
          </Typography.Link>
        ) : (
          <span key={i}>{part}</span>
        ),
      )}
    </>
  );
}

function AgentAnswerCard({ response, knownPaths }: { response: AgentChatResponse; knownPaths: Set<string> }) {
  const { t } = useTranslation();
  return (
    <div style={{ background: "#fafafa", borderRadius: 8, padding: "10px 14px", maxWidth: "85%" }}>
      {response.agentName && (
        <Tag color="geekblue" style={{ marginBottom: 6 }}>
          {response.agentCode} · {response.agentName}
        </Tag>
      )}
      <Typography.Paragraph style={{ marginBottom: 8, whiteSpace: "pre-wrap" }}>
        <LinkifiedText text={response.answer} knownPaths={knownPaths} />
      </Typography.Paragraph>

      {response.facts.length > 0 && (
        <Section title={t("agent.chat.facts")}>
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            {response.facts.map((f, i) => (
              <li key={i}>
                <LinkifiedText text={f} knownPaths={knownPaths} />
              </li>
            ))}
          </ul>
        </Section>
      )}

      {response.analysis.length > 0 && (
        <Section title={t("agent.chat.analysis")}>
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            {response.analysis.map((a, i) => (
              <li key={i}>{a}</li>
            ))}
          </ul>
        </Section>
      )}

      {response.recommendations.length > 0 && (
        <Section title={t("agent.chat.recommendations")}>
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            {response.recommendations.map((r, i) => (
              <li key={i}>
                <Tag color="blue">{t("agent.chat.recommendationTag")}</Tag> {r}
              </li>
            ))}
          </ul>
        </Section>
      )}

      {response.evidence.length > 0 && (
        <Collapse
          size="small"
          ghost
          style={{ marginTop: 8 }}
          items={[
            {
              key: "evidence",
              label: t("agent.chat.evidenceCount", { count: response.evidence.length }),
              children: (
                <Space direction="vertical" size={4} style={{ width: "100%" }}>
                  {response.evidence.map((e, i) => (
                    <div key={i} style={{ fontSize: 12 }}>
                      <Tag>{e.tool}</Tag>
                      <span style={{ color: "#888" }}>{JSON.stringify(e.args)}</span>
                      <br />
                      <span>{JSON.stringify(e.keyData)}</span>
                    </div>
                  ))}
                </Space>
              ),
            },
          ]}
        />
      )}

      {!response.metadata.grounded && (
        <Alert type="warning" showIcon style={{ marginTop: 8 }} message={t("agent.chat.notFullyGrounded")} />
      )}
      {response.metadata.truncated && (
        <Alert type="info" showIcon style={{ marginTop: 8 }} message={t("agent.chat.truncated")} />
      )}
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div style={{ marginTop: 8 }}>
      <Typography.Text strong style={{ fontSize: 12, color: "#888" }}>
        {title}
      </Typography.Text>
      {children}
    </div>
  );
}
