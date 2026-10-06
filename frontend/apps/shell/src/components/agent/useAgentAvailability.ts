import { useEffect, useState } from "react";
import { useAuth } from "../../auth/AuthContext";
import { agentApi, type AgentHealth } from "../../api/agent";

const CACHE_MS = 60_000;
let cached: { at: number; promise: Promise<AgentHealth | null> } | null = null;

function loadHealth(): Promise<AgentHealth | null> {
  if (!cached || Date.now() - cached.at > CACHE_MS) {
    cached = { at: Date.now(), promise: agentApi.health().catch(() => null) };
  }
  return cached.promise;
}

/** Nut AI tren cac man hinh nghiep vu chi hien khi: co quyen AUDIT.AGENT.VIEW + AI dang bat + agent
 * chuyen trach do dang bat (cong tac tat cua quan tri). Ket qua /health duoc dung chung 60 giay giua
 * cac man hinh de khong goi lap lai. */
export function useAgentAvailability(agentCode: string): { available: boolean } {
  const { hasPermission } = useAuth();
  const canUse = hasPermission("AUDIT.AGENT.VIEW");
  const [health, setHealth] = useState<AgentHealth | null>(null);

  useEffect(() => {
    if (!canUse) return;
    let cancelled = false;
    loadHealth().then((h) => !cancelled && setHealth(h));
    return () => {
      cancelled = true;
    };
  }, [canUse]);

  const agentOn = health?.agents.some((a) => a.code === agentCode && a.enabled) ?? false;
  return { available: canUse && !!health?.enabled && agentOn };
}
