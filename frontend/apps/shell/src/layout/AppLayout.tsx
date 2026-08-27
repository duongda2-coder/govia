import { useEffect, useRef, useState } from "react";
import { Layout, Menu, Avatar, Dropdown, Typography, Space } from "antd";
import type { MenuProps } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { AuditOutlined, DashboardOutlined, KeyOutlined, LogoutOutlined, NodeIndexOutlined, SafetyCertificateOutlined, TeamOutlined, UserOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { useAuth } from "../auth/AuthContext";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { ChangePasswordModal } from "../components/ChangePasswordModal";

const { Header, Sider, Content } = Layout;

type MenuItemType = NonNullable<MenuProps["items"]>[number];

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
  "workflow-tasks": "/workflow/tasks",
  "workflow-instances": "/workflow/instances",
  "workflow-approval-matrix": "/workflow/approval-matrix",
  "audit-md-audit": "/audit/master-data/audit",
  "audit-md-finding": "/audit/master-data/finding",
  "audit-md-risk": "/audit/master-data/risk",
  "audit-md-control": "/audit/master-data/control",
  "audit-md-process": "/audit/master-data/process",
  "audit-md-compliance": "/audit/master-data/compliance",
  "audit-md-general": "/audit/master-data/general",
  "audit-md-document-library": "/audit/master-data/document-library",
  "audit-md-position": "/audit/master-data/position",
  "audit-md-department": "/audit/master-data/department",
  "audit-md-year": "/audit/master-data/year",
  "audit-md-business-segment": "/audit/master-data/business-segment",
  "audit-rs-groups": "/audit/risk-scoring/master-data/groups",
  "audit-rs-criteria": "/audit/risk-scoring/master-data/criteria",
  "audit-rs-weight": "/audit/risk-scoring/master-data/weight",
  "audit-rs-coefficient-matrix": "/audit/risk-scoring/master-data/coefficient-matrix",
  "audit-rs-audit-objects": "/audit/risk-scoring/master-data/audit-objects",
  "audit-rs-user-assignment": "/audit/risk-scoring/master-data/user-assignment",
  "audit-rse-scoring": "/audit/risk-scoring/scoring",
};

/** Gan title HTML len nhan menu de trinh duyet tu hien tooltip khi chu bi cat ngan (...) do sider hep. */
function menuLabel(text: string) {
  return <span title={text}>{text}</span>;
}

const SIDER_WIDTH_STORAGE_KEY = "govia.siderWidth";
const SIDER_MIN_WIDTH = 180;
const SIDER_MAX_WIDTH = 420;
const SIDER_DEFAULT_WIDTH = 220;

export function AppLayout() {
  const { user, logout, hasPermission } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const [changePasswordOpen, setChangePasswordOpen] = useState(false);

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

  /**
   * Menu chi liet ke module da co man hinh that. Cac module khac trong NOTE.txt / yeu cau 1
   * (Access, Audit, Risk, Compliance, Asset, Vendor, Policy, Analytics, AI)
   * se duoc them lai vao day khi tung module thuc su co UI, thay vi de placeholder disabled.
   */
  const isSuperAdmin = user?.roles.includes("SUPER_ADMIN") ?? false;

  /**
   * Loc menu theo dung quyen VIEW cua tung man hinh (cung ma quyen ma chinh trang do dang tu kiem
   * tra qua hasPermission, xem vd MasterDataGroupPage/RiskScoring tables) - user khong co quyen thi
   * KHONG thay module/man hinh do trong sidebar, khong chi bi chan luc bam vao. "false" bi loai boi
   * dropNulls, nen 1 dong co the vua la dieu kien vua la item.
   */
  const dropNulls = (items: (MenuItemType | false | null | undefined)[]): MenuItemType[] =>
    items.filter((item): item is MenuItemType => Boolean(item));

  const peopleChildren = dropNulls([
    hasPermission("PEOPLE.EMPLOYEE.VIEW") && { key: "people-employees", label: menuLabel(t("menu.employees")) },
    hasPermission("PEOPLE.POSITION.VIEW") && { key: "people-positions", label: menuLabel(t("menu.positions")) },
    hasPermission("PEOPLE.ORGUNIT.VIEW") && { key: "people-org-units", label: menuLabel(t("menu.orgUnits")) },
  ]);

  const workflowChildren = dropNulls([
    hasPermission("WORKFLOW.TASK.VIEW") && { key: "workflow-tasks", label: menuLabel(t("menu.workflowTasks")) },
    hasPermission("WORKFLOW.INSTANCE.VIEW") && { key: "workflow-instances", label: menuLabel(t("menu.workflowInstances")) },
    hasPermission("WORKFLOW.APPROVAL_MATRIX.VIEW") && { key: "workflow-approval-matrix", label: menuLabel(t("menu.workflowApprovalMatrix")) },
  ]);

  const canViewAuditMasterData = hasPermission("AUDIT.MASTER_DATA.VIEW");
  const canViewRiskScoring = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canViewRiskScoringExec = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");

  const auditChildren = dropNulls([
    canViewAuditMasterData && {
      key: "audit-master-data",
      label: menuLabel(t("menu.auditMasterData")),
      children: [
        { key: "audit-md-audit", label: menuLabel(t("menu.auditMdAudit")) },
        { key: "audit-md-finding", label: menuLabel(t("menu.auditMdFinding")) },
        { key: "audit-md-risk", label: menuLabel(t("menu.auditMdRisk")) },
        { key: "audit-md-control", label: menuLabel(t("menu.auditMdControl")) },
        { key: "audit-md-process", label: menuLabel(t("menu.auditMdProcess")) },
        { key: "audit-md-compliance", label: menuLabel(t("menu.auditMdCompliance")) },
        { key: "audit-md-general", label: menuLabel(t("menu.auditMdGeneral")) },
        { key: "audit-md-document-library", label: menuLabel(t("menu.auditMdDocumentLibrary")) },
        { key: "audit-md-position", label: menuLabel(t("menu.auditMdPosition")) },
        { key: "audit-md-department", label: menuLabel(t("menu.auditMdDepartment")) },
        { key: "audit-md-year", label: menuLabel(t("menu.auditMdYear")) },
        { key: "audit-md-business-segment", label: menuLabel(t("menu.auditMdBusinessSegment")) },
      ],
    },
    (canViewRiskScoring || canViewRiskScoringExec) && {
      key: "audit-risk-scoring",
      label: menuLabel(t("menu.riskScoring")),
      children: dropNulls([
        canViewRiskScoring && {
          key: "audit-rs-master-data",
          label: menuLabel(t("menu.riskScoringMasterData")),
          children: [
            { key: "audit-rs-groups", label: menuLabel(t("menu.riskScoringGroups")) },
            { key: "audit-rs-criteria", label: menuLabel(t("menu.riskScoringCriteria")) },
            { key: "audit-rs-weight", label: menuLabel(t("menu.riskScoringWeight")) },
            { key: "audit-rs-coefficient-matrix", label: menuLabel(t("menu.riskScoringCoefficientMatrix")) },
            { key: "audit-rs-user-assignment", label: menuLabel(t("menu.riskScoringUserAssignment")) },
            { key: "audit-rs-audit-objects", label: menuLabel(t("menu.riskScoringAuditObjects")) },
          ],
        },
        canViewRiskScoringExec && { key: "audit-rse-scoring", label: menuLabel(t("menu.riskScoringExec")) },
      ]),
    },
    // Ke hoach (Audit Plan/Universe) va Thuc hien (Work Program/Finding...) se them vao day
    // khi tung phan thuc su co man hinh, cung cap voi "audit-master-data".
  ]);

  const moduleMenuItems: MenuProps["items"] = dropNulls([
    { key: "dashboard", icon: <DashboardOutlined />, label: menuLabel(t("menu.dashboard")) },
    peopleChildren.length > 0 && { key: "people", icon: <TeamOutlined />, label: menuLabel(t("menu.people")), children: peopleChildren },
    workflowChildren.length > 0 && { key: "workflow", icon: <NodeIndexOutlined />, label: menuLabel(t("menu.workflow")), children: workflowChildren },
    auditChildren.length > 0 && { key: "audit", icon: <AuditOutlined />, label: menuLabel(t("menu.audit")), children: auditChildren },
    isSuperAdmin && {
      key: "admin",
      icon: <SafetyCertificateOutlined />,
      label: menuLabel(t("menu.admin")),
      children: [
        { key: "admin-roles", label: menuLabel(t("menu.roles")) },
        { key: "admin-accounts", label: menuLabel(t("menu.accounts")) },
      ],
    },
  ]);

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
      <Sider
        ref={siderRef}
        breakpoint="lg"
        collapsedWidth="0"
        theme="light"
        width={siderWidth}
        style={{ background: "#dbeafe", borderRight: "1px solid #bfdbfe", position: "relative" }}
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
