import { useEffect, useMemo, useState } from "react";
import { Drawer, FloatButton } from "antd";
import { RobotOutlined } from "@ant-design/icons";
import { useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "../auth/AuthContext";
import { AuditAgentChat } from "../pages/Audit/Agent/AuditAgentChat";
import { agentApi, type AgentHealth, type AgentPageContext, type AgentScreenRef } from "../api/agent";
import type { SearchableScreen } from "../layout/useAppMenu";

/** Nut noi "Tro ly AI" - hien tren MOI man hinh (mount 1 lan trong AppLayout), mo khung chat ben phai.
 * Chi hien khi co quyen AUDIT.AGENT.VIEW VA AI dang bat (health.enabled) - tat AI bang cau hinh la nut
 * bien mat, khong man hinh nghiep vu nao bi anh huong. Tu biet man hinh dang mo (pageContext) va danh
 * sach man hinh nguoi dung duoc thay tren menu (screens, da loc theo quyen) de gui kem cau hoi. */
export function AuditAgentWidget({ screens }: { screens: SearchableScreen[] }) {
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [health, setHealth] = useState<AgentHealth | null>(null);
  const canUse = hasPermission("AUDIT.AGENT.VIEW");

  useEffect(() => {
    if (!canUse) return;
    let cancelled = false;
    agentApi
      .health()
      .then((h) => !cancelled && setHealth(h))
      .catch(() => !cancelled && setHealth(null));
    return () => {
      cancelled = true;
    };
  }, [canUse, open]);

  const screenRefs = useMemo<AgentScreenRef[]>(
    () => screens.map((s) => ({ label: s.label, group: s.groupLabel, path: s.path })),
    [screens],
  );

  const pageContext = useMemo<AgentPageContext | null>(() => {
    const current = screens
      .filter((s) => s.path !== "/" && location.pathname.startsWith(s.path))
      .sort((a, b) => b.path.length - a.path.length)[0];
    return current ? { path: location.pathname, screenLabel: current.label, groupLabel: current.groupLabel } : null;
  }, [screens, location.pathname]);

  if (!canUse || !health?.enabled) {
    return null;
  }

  return (
    <>
      <FloatButton icon={<RobotOutlined />} type="primary" tooltip={t("agent.widget.title")} onClick={() => setOpen(true)} />
      <Drawer
        title={t("agent.widget.title")}
        open={open}
        onClose={() => setOpen(false)}
        width={520}
        destroyOnClose={false}
        styles={{ body: { display: "flex", flexDirection: "column", padding: 16 } }}
      >
        <AuditAgentChat pageContext={pageContext} screens={screenRefs} llmReachable={health.llmReachable} />
      </Drawer>
    </>
  );
}
