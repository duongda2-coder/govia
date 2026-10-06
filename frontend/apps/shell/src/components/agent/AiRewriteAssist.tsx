import { useState } from "react";
import { Alert, App, Button, Card, Space, Typography } from "antd";
import type { FormInstance } from "antd";
import { RobotOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { agentDraftApi, type RewritePurpose, type RewriteResult } from "../../api/agent";
import { useAgentAvailability } from "./useAgentAvailability";
import { agentErrorKey } from "./agentErrors";

interface AiRewriteAssistProps {
  form: FormInstance;
  field: string;
  purpose: RewritePurpose;
  maxLength?: number;
}

/** "AI chuẩn hoá câu chữ" - dat vao prop `extra` cua Form.Item. Chi DE XUAT ban viet lai; nguoi dung bam
 * "Thay thế" thi moi doi noi dung o o nhap, va van tu bam Luu cua form nhu binh thuong. An han khi khong
 * co quyen dung AI hoac AI/agent A4 dang tat. */
export function AiRewriteAssist({ form, field, purpose, maxLength }: AiRewriteAssistProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { available } = useAgentAvailability("A4");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<RewriteResult | null>(null);

  if (!available) {
    return null;
  }

  const run = async () => {
    const text = String(form.getFieldValue(field) ?? "").trim();
    if (!text) {
      message.info(t("agent.draft.rewriteEmpty"));
      return;
    }
    setLoading(true);
    setResult(null);
    try {
      setResult(await agentDraftApi.rewrite(text, purpose));
    } catch (err) {
      message.error(t(agentErrorKey(err)));
    } finally {
      setLoading(false);
    }
  };

  const apply = () => {
    if (!result) return;
    form.setFieldValue(field, maxLength ? result.text.slice(0, maxLength) : result.text);
    setResult(null);
  };

  return (
    <div style={{ marginTop: 4 }}>
      <Button size="small" icon={<RobotOutlined />} loading={loading} onClick={run}>
        {t("agent.draft.rewriteButton")}
      </Button>
      {result && (
        <Card size="small" style={{ marginTop: 8, background: "#f8fafc" }}>
          <Typography.Paragraph style={{ whiteSpace: "pre-wrap", marginBottom: 8 }}>{result.text}</Typography.Paragraph>
          {result.changes.length > 0 && (
            <ul style={{ margin: "0 0 8px", paddingLeft: 18, color: "rgba(0,0,0,0.45)", fontSize: 12 }}>
              {result.changes.map((c, i) => (
                <li key={i}>{c}</li>
              ))}
            </ul>
          )}
          {!result.grounded && <Alert type="warning" showIcon style={{ marginBottom: 8 }} message={t("agent.draft.rewriteNotGrounded")} />}
          <Space>
            <Button size="small" type="primary" onClick={apply}>
              {t("agent.draft.replace")}
            </Button>
            <Button size="small" onClick={() => setResult(null)}>
              {t("agent.draft.dismiss")}
            </Button>
          </Space>
        </Card>
      )}
    </div>
  );
}
