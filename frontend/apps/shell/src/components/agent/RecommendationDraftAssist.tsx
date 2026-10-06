import { useState } from "react";
import { Alert, App, Button, Card, Collapse, List, Space, Tag, Typography } from "antd";
import { RobotOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { agentDraftApi, type RecommendationDraftResult } from "../../api/agent";
import { useAgentAvailability } from "./useAgentAvailability";
import { agentErrorKey } from "./agentErrors";

interface RecommendationDraftAssistProps {
  engagementId: string | null;
  businessSegmentId?: string | null;
  /** Dua noi dung ban nhap vao o "Nội dung" cua form Them kien nghi - KHONG luu, nguoi dung tu bam Them. */
  onUse: (content: string) => void;
}

/** "AI soạn kiến nghị" (A4) cho man hinh "Thêm kiến nghị": AI doc TTSS cua cuoc kiem toan (trong pham vi
 * nguoi dung duoc xem), uu tien TTSS chua gan kien nghi, roi de xuat ban nhap. Moi ban nhap kem ma phat
 * hien lam can cu va canh bao neu co thong tin khong khop du lieu. */
export function RecommendationDraftAssist({ engagementId, businessSegmentId, onUse }: RecommendationDraftAssistProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { available } = useAgentAvailability("A4");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<RecommendationDraftResult | null>(null);

  if (!available || !engagementId) {
    return null;
  }

  const run = async () => {
    setLoading(true);
    setResult(null);
    try {
      setResult(await agentDraftApi.recommendations(engagementId, businessSegmentId));
    } catch (err) {
      message.error(t(agentErrorKey(err)));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ marginBottom: 16 }}>
      <Button icon={<RobotOutlined />} loading={loading} onClick={run}>
        {t("agent.draft.recommendationButton")}
      </Button>
      {result && (
        <Card size="small" style={{ marginTop: 8, background: "#f8fafc" }}>
          <Alert type="info" showIcon style={{ marginBottom: 8 }} message={t("agent.draft.recommendationHint", { count: result.sourceTtssCount })} />
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
                <Button size="small" type="primary" onClick={() => onUse(draft.content)}>
                  {t("agent.draft.use")}
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
        </Card>
      )}
    </div>
  );
}
