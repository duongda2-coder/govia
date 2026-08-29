import { useEffect, useRef, useState } from "react";
import { Layout, Menu, Avatar, Dropdown, Typography, Space } from "antd";
import type { MenuProps } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { KeyOutlined, LogoutOutlined, UserOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { useAuth } from "../auth/AuthContext";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { ChangePasswordModal } from "../components/ChangePasswordModal";
import { MENU_ROUTES, useAppMenu, type SearchableScreen } from "./useAppMenu";

const { Header, Sider, Content } = Layout;

export interface AppLayoutOutletContext {
  searchableScreens: SearchableScreen[];
}

const SIDER_WIDTH_STORAGE_KEY = "govia.siderWidth";
const SIDER_MIN_WIDTH = 180;
const SIDER_MAX_WIDTH = 420;
const SIDER_DEFAULT_WIDTH = 220;

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const [changePasswordOpen, setChangePasswordOpen] = useState(false);
  const { moduleMenuItems, searchableScreens } = useAppMenu();

  const [siderWidth, setSiderWidth] = useState(() => {
    const stored = Number(localStorage.getItem(SIDER_WIDTH_STORAGE_KEY));
    return stored >= SIDER_MIN_WIDTH && stored <= SIDER_MAX_WIDTH ? stored : SIDER_DEFAULT_WIDTH;
  });
  const resizingRef = useRef(false);
  const siderRef = useRef<HTMLDivElement>(null);

  // Sider voi prop "breakpoint" tu chen CSS !important co dinh width luc mount (de responsive) -
  // ghi de qua prop "width" binh thuong khong thang duoc !important do, nen phai set truc tiep len
  // DOM voi priority "important" (chi co inline style + !important moi thang duoc !important khac).
  useEffect(() => {
    const node = siderRef.current;
    if (!node) return;
    const px = `${siderWidth}px`;
    node.style.setProperty("width", px, "important");
    node.style.setProperty("min-width", px, "important");
    node.style.setProperty("max-width", px, "important");
    node.style.setProperty("flex", `0 0 ${px}`, "important");
  }, [siderWidth]);

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!resizingRef.current) return;
      setSiderWidth(Math.min(Math.max(e.clientX, SIDER_MIN_WIDTH), SIDER_MAX_WIDTH));
    };
    const handleMouseUp = () => {
      if (!resizingRef.current) return;
      resizingRef.current = false;
      document.body.style.cursor = "";
      document.body.style.userSelect = "";
      setSiderWidth((current) => {
        localStorage.setItem(SIDER_WIDTH_STORAGE_KEY, String(current));
        return current;
      });
    };
    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);
    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
    };
  }, []);

  const startResizing = () => {
    resizingRef.current = true;
    document.body.style.cursor = "col-resize";
    document.body.style.userSelect = "none";
  };

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
    <Layout style={{ height: "100vh", overflow: "hidden" }}>
      <Sider
        ref={siderRef}
        breakpoint="lg"
        collapsedWidth="0"
        theme="light"
        width={siderWidth}
        style={{ background: "#dbeafe", borderRight: "1px solid #bfdbfe", position: "relative", overflowY: "auto" }}
      >
        <div
          onMouseDown={startResizing}
          title={t("common.resizeSidebar")}
          style={{ position: "absolute", top: 0, right: -3, width: 6, height: "100%", cursor: "col-resize", zIndex: 10 }}
        />
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
      <Layout style={{ overflow: "hidden" }}>
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
        <Content style={{ margin: 24, overflowY: "auto" }}>
          <Outlet context={{ searchableScreens } satisfies AppLayoutOutletContext} />
        </Content>
      </Layout>

      <ChangePasswordModal open={changePasswordOpen} onClose={() => setChangePasswordOpen(false)} />
    </Layout>
  );
}
