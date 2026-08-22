import { useState } from "react";
import { Layout, Menu, Avatar, Dropdown, Typography, Space } from "antd";
import type { MenuProps } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { DashboardOutlined, KeyOutlined, LogoutOutlined, SafetyCertificateOutlined, TeamOutlined, UserOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { useAuth } from "../auth/AuthContext";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { ChangePasswordModal } from "../components/ChangePasswordModal";

const { Header, Sider, Content } = Layout;

/**
 * Route ung voi key cua tung mo-dun trong menu - dung de dieu huong khi bam menu
 * va de xac dinh menu nao dang duoc chon dua theo URL hien tai.
 */
const MENU_ROUTES: Record<string, string> = {
  dashboard: "/",
  "people-employees": "/people/employees",
  "people-positions": "/people/positions",
  "people-org-units": "/people/org-units",
  "admin-roles": "/admin/roles",
  "admin-accounts": "/admin/accounts",
};

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const [changePasswordOpen, setChangePasswordOpen] = useState(false);

  /**
   * Menu chi liet ke module da co man hinh that. Cac module khac trong NOTE.txt / yeu cau 1
   * (Access, Audit, Risk, Compliance, Asset, Vendor, Policy, Analytics, AI)
   * se duoc them lai vao day khi tung module thuc su co UI, thay vi de placeholder disabled.
   */
  const isSuperAdmin = user?.roles.includes("SUPER_ADMIN") ?? false;

  const moduleMenuItems: MenuProps["items"] = [
    { key: "dashboard", icon: <DashboardOutlined />, label: t("menu.dashboard") },
    {
      key: "people",
      icon: <TeamOutlined />,
      label: t("menu.people"),
      children: [
        { key: "people-employees", label: t("menu.employees") },
        { key: "people-positions", label: t("menu.positions") },
        { key: "people-org-units", label: t("menu.orgUnits") },
      ],
    },
    ...(isSuperAdmin
      ? [
          {
            key: "admin",
            icon: <SafetyCertificateOutlined />,
            label: t("menu.admin"),
            children: [
              { key: "admin-roles", label: t("menu.roles") },
              { key: "admin-accounts", label: t("menu.accounts") },
            ],
          },
        ]
      : []),
  ];

  const selectedKey =
    Object.entries(MENU_ROUTES)
      .filter(([, path]) => path !== "/" && location.pathname.startsWith(path))
      .map(([key]) => key)[0] ?? "dashboard";

  const userMenu: MenuProps["items"] = [
    { key: "changePassword", icon: <KeyOutlined />, label: t("account.changePassword.menuLabel"), onClick: () => setChangePasswordOpen(true) },
    { type: "divider" },
    { key: "logout", icon: <LogoutOutlined />, label: t("header.logout"), onClick: () => { logout(); navigate("/login"); } },
  ];

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider breakpoint="lg" collapsedWidth="0" theme="light" style={{ background: "#dbeafe", borderRight: "1px solid #bfdbfe" }}>
        <div style={{ color: "#1d4ed8", textAlign: "center", padding: 16, fontWeight: 700, fontSize: 18 }}>
          GOVIA
        </div>
        <Menu
          theme="light"
          mode="inline"
          selectedKeys={[selectedKey]}
          defaultOpenKeys={["people"]}
          items={moduleMenuItems}
          onClick={({ key }) => {
            const path = MENU_ROUTES[key];
            if (path) {
              navigate(path);
            }
          }}
          style={{ background: "transparent", borderInlineEnd: "none" }}
        />
      </Sider>
      <Layout>
        <Header style={{ background: "#fff", borderBottom: "1px solid #dbeafe", display: "flex", justifyContent: "flex-end", alignItems: "center", paddingInline: 24 }}>
          <Space size={16}>
            <LanguageSwitcher />
            <Dropdown menu={{ items: userMenu }} placement="bottomRight">
              <div style={{ display: "flex", alignItems: "center", gap: 8, cursor: "pointer" }}>
                <Avatar icon={<UserOutlined />} style={{ background: "#2563eb" }} />
                <Typography.Text>{user?.username}</Typography.Text>
              </div>
            </Dropdown>
          </Space>
        </Header>
        <Content style={{ margin: 24 }}>
          <Outlet />
        </Content>
      </Layout>

      <ChangePasswordModal open={changePasswordOpen} onClose={() => setChangePasswordOpen(false)} />
    </Layout>
  );
}
