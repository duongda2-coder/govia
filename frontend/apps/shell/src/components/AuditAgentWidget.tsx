import { useEffect, useMemo, useState } from "react";
import { Drawer, FloatButton, Segmented } from "antd";
import { RobotOutlined } from "@ant-design/icons";
import { useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "../auth/AuthContext";
import { AuditAgentChat } from "../pages/Audit/Agent/AuditAgentChat";
import { AgentDraftPanel } from "./agent/AgentDraftPanel";
import { agentApi, type AgentHealth, type AgentPageContext, type AgentScreenRef } from "../api/agent";
import type { SearchableScreen } from "../layout/useAppMenu";

type WidgetView = "chat" | "draft";

/** Nut noi "Tro ly AI" - hien tren MOI man hinh (mount 1 lan trong AppLayout), mo khung AI ben phai voi 2
 * phan: "Trò chuyện" va "Soạn nháp". TOAN BO tinh nang AI nam trong khung nay - khong co nut AI nao tren man
 * hinh nghiep vu. Chi hien khi co quyen AUDIT.AGENT.VIEW VA AI dang bat (health.enabled); tat AI la nut bien
 * mat. Tu biet man hinh dang mo (pageContext) va danh sach man hinh nguoi dung duoc thay (screens). */
export function AuditAgentWidget({ screens }: { screens: SearchableScreen[] }) {
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [view, setView] = useState<WidgetView>("chat");
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
        extra={
          <Segmented<WidgetView>
            value={view}
            onChange={setView}
            options={[
              { value: "chat", label: t("agent.widget.chat") },
              { value: "draft", label: t("agent.widget.draft") },
            ]}
          />
        }
        open={open}
        onClose={() => setOpen(false)}
        width={560}
        destroyOnClose={false}
        styles={{ body: { display: "flex", flexDirection: "column", padding: 16 } }}
      >
        {/* Giu ca 2 phan luon mount de chuyen qua lai khong mat hoi thoai/ban nhap dang soan */}
        <div style={{ display: view === "chat" ? "flex" : "none", flexDirection: "column", flex: 1, minHeight: 0 }}>
          <AuditAgentChat pageContext={pageContext} screens={screenRefs} llmReachable={health.llmReachable} />
        </div>
        <div style={{ display: view === "draft" ? "block" : "none", overflowY: "auto" }}>
          <AgentDraftPanel pageContext={pageContext} agents={health.agents} />
        </div>
      </Drawer>
    </>
  );
}
