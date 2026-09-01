import { useEffect, useState } from "react";
import { App, Form, Input, Button, Card, Typography, Modal, List, Space } from "antd";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { storeTokens, type ApiResponse } from "@govia/ui-kit";
import { httpClient } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { LanguageSwitcher } from "../../components/LanguageSwitcher";

interface LoginFormValues {
  tenantCode: string;
  username: string;
  password: string;
}

interface LoginResponseDto {
  accessToken: string;
  refreshToken: string;
  userId: string;
  username: string;
  employeeCode: string | null;
  tenantId: string;
  roles: string[];
  permissions: string[];
}

interface ActiveSessionInfoDto {
  deviceInfo: string | null;
  ipAddress: string | null;
  loginAt: string;
}

interface LoginOutcomeDto {
  status: "SUCCESS" | "CONFLICT";
  login: LoginResponseDto | null;
  sessions: ActiveSessionInfoDto[] | null;
  pendingToken: string | null;
}

export function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [resolving, setResolving] = useState<"KICK_OTHERS" | "ALLOW_BOTH" | null>(null);
  const [conflict, setConflict] = useState<{ sessions: ActiveSessionInfoDto[]; pendingToken: string } | null>(null);
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const { message } = App.useApp();
  const { t } = useTranslation();

  useEffect(() => {
    if (new URLSearchParams(window.location.search).get("reason") === "kicked") {
      message.warning(t("login.kickedOut"));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const applyLoginResult = (data: LoginResponseDto) => {
    storeTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
    setUser({
      userId: data.userId,
      username: data.username,
      employeeCode: data.employeeCode,
      tenantId: data.tenantId,
      roles: data.roles,
      permissions: data.permissions,
    });
    message.success(t("login.success"));
    navigate("/");
  };

  const handleSubmit = async (values: LoginFormValues) => {
    setLoading(true);
    try {
      const res = await httpClient.post<ApiResponse<LoginOutcomeDto>>("/api/auth/login", values);
      const outcome = res.data.data;
      if (outcome.status === "CONFLICT" && outcome.pendingToken) {
        setConflict({ sessions: outcome.sessions ?? [], pendingToken: outcome.pendingToken });
        return;
      }
      if (outcome.login) applyLoginResult(outcome.login);
    } catch {
      message.error(t("login.error"));
    } finally {
      setLoading(false);
    }
  };

  const handleResolve = async (action: "KICK_OTHERS" | "ALLOW_BOTH") => {
    if (!conflict) return;
    setResolving(action);
    try {
      const res = await httpClient.post<ApiResponse<LoginResponseDto>>("/api/auth/login/resolve", {
        pendingToken: conflict.pendingToken,
        action,
      });
      setConflict(null);
      applyLoginResult(res.data.data);
    } catch {
      message.error(t("login.error"));
    } finally {
      setResolving(null);
    }
  };

  return (
    <div
      style={{
        position: "relative",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        minHeight: "100vh",
        overflowY: "auto",
        background: "linear-gradient(135deg, #dbeafe 0%, #eff6ff 60%, #ffffff 100%)",
      }}
    >
      <div style={{ position: "absolute", top: 16, right: 16 }}>
        <LanguageSwitcher />
      </div>
      <Card style={{ width: 380, boxShadow: "0 8px 24px rgba(37, 99, 235, 0.12)" }}>
        <Typography.Title level={3} style={{ textAlign: "center" }}>
          {t("app.name")}
        </Typography.Title>
        <Typography.Text type="secondary" style={{ display: "block", textAlign: "center", marginBottom: 24 }}>
          {t("app.tagline")}
        </Typography.Text>
        <Form layout="vertical" initialValues={{ tenantCode: "default" }} onFinish={handleSubmit}>
          <Form.Item label={t("login.tenantLabel")} name="tenantCode" rules={[{ required: true, message: t("login.tenantRequired") }]}>
            <Input placeholder="default" />
          </Form.Item>
          <Form.Item label={t("login.usernameLabel")} name="username" rules={[{ required: true, message: t("login.usernameRequired") }]}>
            <Input placeholder="admin" />
          </Form.Item>
          <Form.Item label={t("login.passwordLabel")} name="password" rules={[{ required: true, message: t("login.passwordRequired") }]}>
            <Input.Password placeholder="Admin@123" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>
              {t("login.submit")}
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Modal
        title={t("login.conflict.title")}
        open={!!conflict}
        onCancel={() => setConflict(null)}
        footer={null}
        destroyOnClose
      >
        <Typography.Paragraph>{t("login.conflict.message")}</Typography.Paragraph>
        {conflict && conflict.sessions.length > 0 && (
          <List
            size="small"
            bordered
            dataSource={conflict.sessions}
            style={{ marginBottom: 16 }}
            renderItem={(s) => (
              <List.Item>
                {t("login.conflict.sessionItem", {
                  device: s.deviceInfo ?? "?",
                  ip: s.ipAddress ?? "?",
                  time: new Date(s.loginAt).toLocaleString(),
                })}
              </List.Item>
            )}
          />
        )}
        <Space direction="vertical" style={{ width: "100%" }}>
          <Button danger block loading={resolving === "KICK_OTHERS"} disabled={!!resolving && resolving !== "KICK_OTHERS"}
                  onClick={() => handleResolve("KICK_OTHERS")}>
            {t("login.conflict.kickOthers")}
          </Button>
          <Button block loading={resolving === "ALLOW_BOTH"} disabled={!!resolving && resolving !== "ALLOW_BOTH"}
                  onClick={() => handleResolve("ALLOW_BOTH")}>
            {t("login.conflict.allowBoth")}
          </Button>
        </Space>
      </Modal>
    </div>
  );
}
