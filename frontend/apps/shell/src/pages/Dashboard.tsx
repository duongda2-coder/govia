import { Card, Typography, Descriptions, Space } from "antd";
import { useTranslation } from "react-i18next";
import { useOutletContext } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { GlobalSearch } from "../components/GlobalSearch";
import type { AppLayoutOutletContext } from "../layout/AppLayout";

export function Dashboard() {
  const { user } = useAuth();
  const { t } = useTranslation();
  const { searchableScreens } = useOutletContext<AppLayoutOutletContext>();

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Card>
        <Typography.Title level={5} style={{ marginTop: 0 }}>
          {t("dashboard.search.title")}
        </Typography.Title>
        <GlobalSearch screens={searchableScreens} />
      </Card>

      <Card>
        <Typography.Title level={4}>{t("dashboard.welcome")}</Typography.Title>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label={t("dashboard.username")}>{user?.username}</Descriptions.Item>
          <Descriptions.Item label={t("dashboard.employeeCode")}>{user?.employeeCode ?? "-"}</Descriptions.Item>
          <Descriptions.Item label={t("dashboard.tenant")}>{user?.tenantId}</Descriptions.Item>
          <Descriptions.Item label={t("dashboard.roles")}>{user?.roles.join(", ")}</Descriptions.Item>
        </Descriptions>
      </Card>
    </Space>
  );
}
