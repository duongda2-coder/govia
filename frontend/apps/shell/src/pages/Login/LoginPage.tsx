import { useState } from "react";
import { App, Form, Input, Button, Card, Typography } from "antd";
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

export function LoginPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const { message } = App.useApp();
  const { t } = useTranslation();

  const handleSubmit = async (values: LoginFormValues) => {
    setLoading(true);
    try {
      const res = await httpClient.post<ApiResponse<LoginResponseDto>>("/api/auth/login", values);
      const data = res.data.data;
      storeTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
      setUser({
        username: data.username,
        employeeCode: data.employeeCode,
        tenantId: data.tenantId,
        roles: data.roles,
        permissions: data.permissions,
      });
      message.success(t("login.success"));
      navigate("/");
    } catch {
      message.error(t("login.error"));
    } finally {
      setLoading(false);
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
    </div>
  );
}
