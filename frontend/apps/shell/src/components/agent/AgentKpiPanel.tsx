import { useEffect, useState } from "react";
import { Alert, Descriptions, Segmented, Spin, Table, Tag } from "antd";
import { useTranslation } from "react-i18next";
import { agentSuggestionApi, type AgentKpi, type AgentKpiTarget } from "../../api/agent";

const percent = (v: number | null | undefined) => (v == null ? "-" : `${Math.round(v * 1000) / 10}%`);
const seconds = (ms: number | null | undefined) => (ms == null ? "-" : `${Math.round(ms / 100) / 10} s`);

/** Tab "Thống kê" (G4, chi quyen AUDIT.AGENT.ADMIN): do KPI cua phuong an tu nhat ky rieng cua Tro ly AI. */
export function AgentKpiPanel() {
  const { t } = useTranslation();
  const [days, setDays] = useState(30);
  const [kpi, setKpi] = useState<AgentKpi | null>(null);
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    setLoading(true);
    setFailed(false);
    agentSuggestionApi
      .kpi(days)
      .then(setKpi)
      .catch(() => setFailed(true))
      .finally(() => setLoading(false));
  }, [days]);

  const targetValue = (row: AgentKpiTarget) => (row.unit === "PERCENT" ? percent(row.value) : (row.value ?? "-"));

  return (
    <Spin spinning={loading}>
      <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
        <Segmented<number>
          value={days}
          onChange={setDays}
          options={[7, 30, 90].map((d) => ({ value: d, label: t("agent.kpi.days", { count: d }) }))}
        />
        {failed && <Alert type="error" showIcon message={t("agent.kpi.error")} />}
        {kpi && (
          <>
            <Table<AgentKpiTarget>
              size="small"
              pagination={false}
              rowKey="name"
              dataSource={kpi.targets}
              columns={[
                { title: t("agent.kpi.target"), dataIndex: "name" },
                { title: t("agent.kpi.goal"), dataIndex: "goal", width: 120 },
                { title: t("agent.kpi.value"), width: 80, render: (_, row) => targetValue(row) },
                {
                  title: "",
                  width: 90,
                  render: (_, row) =>
                    row.met == null ? "-" : <Tag color={row.met ? "success" : "warning"}>{t(row.met ? "agent.kpi.met" : "agent.kpi.notMet")}</Tag>,
                },
              ]}
            />
            <Descriptions size="small" column={2} bordered>
              <Descriptions.Item label={t("agent.kpi.questions")}>{kpi.questions}</Descriptions.Item>
              <Descriptions.Item label={t("agent.kpi.activeUsers")}>{kpi.activeUsers}</Descriptions.Item>
              <Descriptions.Item label={t("agent.kpi.grounded")}>{percent(kpi.groundedRate)}</Descriptions.Item>
              <Descriptions.Item label={t("agent.kpi.latency")}>
                {seconds(kpi.latencyP50Ms)} / {seconds(kpi.latencyP90Ms)}
              </Descriptions.Item>
              <Descriptions.Item label={t("agent.kpi.toolCalls")}>{kpi.toolCalls}</Descriptions.Item>
              <Descriptions.Item label={t("agent.kpi.denied")}>{kpi.toolCallsDenied}</Descriptions.Item>
              <Descriptions.Item label={t("agent.kpi.llmErrors")}>{kpi.llmErrors}</Descriptions.Item>
              <Descriptions.Item label={t("agent.kpi.suggestions")}>{kpi.suggestionsGenerated}</Descriptions.Item>
            </Descriptions>
            <Descriptions size="small" column={1} bordered title={t("agent.kpi.byAgent")}>
              {Object.entries(kpi.answersByAgent).map(([code, count]) => (
                <Descriptions.Item key={code} label={code}>
                  {t("agent.kpi.byAgentValue", { count, grounded: percent(kpi.groundedRateByAgent[code]) })}
                </Descriptions.Item>
              ))}
            </Descriptions>
            <Descriptions size="small" column={1} bordered title={t("agent.kpi.drafts")}>
              {Object.entries(kpi.drafts).map(([tool, count]) => (
                <Descriptions.Item key={tool} label={t(`agent.kpi.draft_${tool}`)}>
                  {count}
                </Descriptions.Item>
              ))}
            </Descriptions>
          </>
        )}
      </div>
    </Spin>
  );
}
