import { useCallback, useEffect, useState } from "react";
import { Alert, App, Button, Empty, List, Space, Spin, Tag, Typography } from "antd";
import { EyeInvisibleOutlined, LinkOutlined, ReloadOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { agentSuggestionApi, type AgentSuggestion } from "../../api/agent";
import { agentErrorKey } from "./agentErrors";

const SEVERITY_COLOR: Record<AgentSuggestion["severity"], string> = { HIGH: "error", WARN: "warning", INFO: "processing" };

interface AgentSuggestionPanelProps {
  /** Tab dang mo - mo tab thi danh dau da doc. */
  active: boolean;
  scheduled: boolean;
  onUnreadChange: (count: number) => void;
}

/**
 * Tab "Gợi ý AI" (G4, muc M3): viec can chu y do job theo lich (06:00 ngay lam viec) hoac nut "Làm mới" rut ra tu du
 * lieu nguoi dung duoc xem. Chi la nhac viec - khong tao task, khong gui thu; nguoi dung bam "Mở màn hình" de tu xu ly
 * tren man hinh nghiep vu nhu hien nay.
 */
export function AgentSuggestionPanel({ active, scheduled, onUnreadChange }: AgentSuggestionPanelProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [items, setItems] = useState<AgentSuggestion[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  const apply = useCallback(
    (list: AgentSuggestion[]) => {
      setItems(list);
      onUnreadChange(list.filter((s) => !s.read).length);
    },
    [onUnreadChange],
  );

  useEffect(() => {
    setLoading(true);
    agentSuggestionApi
      .list()
      .then(apply)
      .catch(() => apply([]))
      .finally(() => setLoading(false));
  }, [apply]);

  useEffect(() => {
    if (!active || !items.some((s) => !s.read)) return;
    agentSuggestionApi
      .markRead()
      .then(() => apply(items.map((s) => ({ ...s, read: true }))))
      .catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active]);

  const refresh = async () => {
    setRefreshing(true);
    try {
      apply(await agentSuggestionApi.refresh());
      message.success(t("agent.suggestion.refreshed"));
    } catch (err) {
      message.error(t(agentErrorKey(err)));
    } finally {
      setRefreshing(false);
    }
  };

  const dismiss = async (id: string) => {
    try {
      await agentSuggestionApi.dismiss(id);
      apply(items.filter((s) => s.id !== id));
    } catch (err) {
      message.error(t(agentErrorKey(err)));
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <Space style={{ justifyContent: "space-between", width: "100%" }}>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          {scheduled ? t("agent.suggestion.scheduleHint") : t("agent.suggestion.scheduleOff")}
        </Typography.Text>
        <Button icon={<ReloadOutlined />} loading={refreshing} onClick={refresh}>
          {t("agent.suggestion.refresh")}
        </Button>
      </Space>
      <Alert type="info" showIcon message={t("agent.suggestion.hint")} />
      <Spin spinning={loading}>
        {items.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("agent.suggestion.empty")} />
        ) : (
          <List
            dataSource={items}
            renderItem={(s) => (
              <List.Item key={s.id} style={{ display: "block" }}>
                <Space size={6} wrap style={{ marginBottom: 4 }}>
                  <Tag color={SEVERITY_COLOR[s.severity]}>{t(`agent.suggestion.severity${s.severity}`)}</Tag>
                  <Tag>{s.agentCode}</Tag>
                  <Typography.Text strong>{s.title}</Typography.Text>
                </Space>
                {s.detail && (
                  <Typography.Paragraph type="secondary" style={{ whiteSpace: "pre-wrap", fontSize: 12, marginBottom: 6 }}>
                    {s.detail}
                  </Typography.Paragraph>
                )}
                <Space size={8}>
                  {s.linkPath && (
                    <Button size="small" icon={<LinkOutlined />} onClick={() => navigate(s.linkPath!)}>
                      {t("agent.suggestion.open")}
                    </Button>
                  )}
                  <Button size="small" type="text" icon={<EyeInvisibleOutlined />} onClick={() => dismiss(s.id)}>
                    {t("agent.suggestion.dismiss")}
                  </Button>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {t("agent.suggestion.runDate", { date: s.runDate })}
                  </Typography.Text>
                </Space>
              </List.Item>
            )}
          />
        )}
      </Spin>
    </div>
  );
}
