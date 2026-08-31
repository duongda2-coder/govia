import { useEffect, useRef, useState } from "react";
import { Alert, App, Button, Collapse, Input, List, Space, Spin, Tag, Typography } from "antd";
import { SendOutlined, RobotOutlined, UserOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { agentApi, type AgentChatResponse } from "../../../api/agent";

interface ChatEntry {
  role: "user" | "assistant" | "error";
  text?: string;
  response?: AgentChatResponse;
}

const LOADING_STEPS = [
  "Đang phân tích câu hỏi...",
  "Đang kiểm tra dữ liệu rủi ro...",
  "Đang lấy evidence...",
  "Đang tổng hợp câu trả lời...",
];

/** Man chat "Audit AI Assistant" - goi /api/audit/agent/chat (xem AgentOrchestratorService phia
 * backend). Khong hien chain-of-thought - chi hien Answer/Facts/Analysis/Recommendations/Evidence. */
export function AuditAgentChat() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [conversationId] = useState(() => crypto.randomUUID());
  const [entries, setEntries] = useState<ChatEntry[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingStepIndex, setLoadingStepIndex] = useState(0);
  const listEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    listEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [entries, loading]);

  useEffect(() => {
    if (!loading) return;
    setLoadingStepIndex(0);
    const timer = setInterval(() => {
      setLoadingStepIndex((i) => Math.min(i + 1, LOADING_STEPS.length - 1));
    }, 3000);
    return () => clearInterval(timer);
  }, [loading]);

  const send = async () => {
    const text = input.trim();
    if (!text || loading) return;
    setEntries((prev) => [...prev, { role: "user", text }]);
    setInput("");
    setLoading(true);
    try {
      const response = await agentApi.chat(conversationId, text);
      setEntries((prev) => [...prev, { role: "assistant", response }]);
    } catch {
      setEntries((prev) => [...prev, { role: "error", text: t("agent.chat.errorUnavailable") }]);
      message.error(t("agent.chat.errorUnavailable"));
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%" }}>
      <div style={{ flex: 1, overflowY: "auto", padding: "4px 4px 0" }}>
        {entries.length === 0 && (
          <Typography.Text type="secondary">{t("agent.chat.emptyHint")}</Typography.Text>
        )}
        <List
          dataSource={entries}
          locale={{ emptyText: <></> }}
          renderItem={(entry, index) => (
            <List.Item key={index} style={{ border: "none", padding: "8px 0" }}>
              {entry.role === "user" && (
                <Space align="start" style={{ width: "100%", justifyContent: "flex-end" }}>
                  <div style={{ background: "#eff6ff", borderRadius: 8, padding: "8px 12px", maxWidth: "80%" }}>
                    {entry.text}
                  </div>
                  <UserOutlined style={{ fontSize: 18, marginTop: 6 }} />
                </Space>
              )}
              {entry.role === "error" && (
                <Alert type="error" showIcon message={entry.text} style={{ width: "100%" }} />
              )}
              {entry.role === "assistant" && entry.response && (
                <Space align="start" style={{ width: "100%" }}>
                  <RobotOutlined style={{ fontSize: 18, marginTop: 6 }} />
                  <AgentAnswerCard response={entry.response} />
                </Space>
              )}
            </List.Item>
          )}
        />
        {loading && (
          <Space style={{ padding: "8px 0" }}>
            <Spin size="small" />
            <Typography.Text type="secondary">{LOADING_STEPS[loadingStepIndex]}</Typography.Text>
          </Space>
        )}
        <div ref={listEndRef} />
      </div>

      <Space.Compact style={{ width: "100%", marginTop: 16 }}>
        <Input.TextArea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={t("agent.chat.inputPlaceholder")}
          autoSize={{ minRows: 1, maxRows: 4 }}
          disabled={loading}
        />
        <Button type="primary" icon={<SendOutlined />} onClick={send} disabled={loading || !input.trim()} />
      </Space.Compact>
    </div>
  );
}

function AgentAnswerCard({ response }: { response: AgentChatResponse }) {
  const { t } = useTranslation();
  return (
    <div style={{ background: "#fafafa", borderRadius: 8, padding: "10px 14px", maxWidth: "85%" }}>
      <Typography.Paragraph style={{ marginBottom: 8 }}>{response.answer}</Typography.Paragraph>

      {response.facts.length > 0 && (
        <Section title={t("agent.chat.facts")}>
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            {response.facts.map((f, i) => (
              <li key={i}>{f}</li>
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
        <Alert
          type="warning"
          showIcon
          style={{ marginTop: 8 }}
          message={t("agent.chat.notFullyGrounded")}
        />
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
