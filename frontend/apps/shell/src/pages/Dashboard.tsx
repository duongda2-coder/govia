import { Card, Typography, Descriptions } from "antd";
import { useTranslation } from "react-i18next";
import { useAuth } from "../auth/AuthContext";

export function Dashboard() {
  const { user } = useAuth();
  const { t } = useTranslation();

  return (
    <Card>
      <Typography.Title level={4}>{t("dashboard.welcome")}</Typography.Title>
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label={t("dashboard.username")}>{user?.username}</Descriptions.Item>
        <Descriptions.Item label={t("dashboard.employeeCode")}>{user?.employeeCode ?? "-"}</Descriptions.Item>
        <Descriptions.Item label={t("dashboard.tenant")}>{user?.tenantId}</Descriptions.Item>
        <Descriptions.Item label={t("dashboard.roles")}>{user?.roles.join(", ")}</Descriptions.Item>
      </Descriptions>
    </Card>
  );
}
