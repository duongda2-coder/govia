import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

const BASE = "/api/audit/agent";

export interface AgentEvidenceRef {
  tool: string;
  args: Record<string, unknown>;
  keyData: Record<string, unknown>;
}

export interface AgentMetadata {
  model: string;
  timestamp: string;
  toolsUsed: string[];
  truncated: boolean;
  grounded: boolean;
}

export interface AgentChatResponse {
  answer: string;
  facts: string[];
  analysis: string[];
  recommendations: string[];
  evidence: AgentEvidenceRef[];
  metadata: AgentMetadata;
  agentCode: string | null;
  agentName: string | null;
}

export interface AgentInfo {
  code: string;
  name: string;
  description: string;
  enabled: boolean;
}

export interface AgentHealth {
  /** false = AI dang bi tat bang cau hinh (cong tac tat) - an han nut AI. */
  enabled: boolean;
  /** false = may chay model khong phan hoi - van mo duoc khung chat nhung bao "tam ngung". */
  llmReachable: boolean;
  model: string;
  provider: string;
  semanticSearch: boolean;
  agents: AgentInfo[];
}

/** Man hinh dang mo - nguoi dung bo duoc tren giao dien (the "Dang xem"). */
export interface AgentPageContext {
  path: string;
  screenLabel: string;
  groupLabel: string;
}

/** 1 muc menu nguoi dung duoc thay - gui kem moi luot de tro ly chi chi toi man hinh nguoi dung co quyen. */
export interface AgentScreenRef {
  label: string;
  group: string;
  path: string;
}

export interface AgentConversationSummary {
  id: string;
  title: string;
  lastAgentCode: string | null;
  lastMessageAt: string;
  messageCount: number;
}

export interface AgentStoredMessage {
  id: string;
  seq: number;
  role: "USER" | "ASSISTANT";
  content: string;
  agentCode: string | null;
  response: AgentChatResponse | null;
  pageLabel: string | null;
  createdAt: string;
}

export const agentApi = {
  async chat(
    conversationId: string,
    message: string,
    pageContext?: AgentPageContext | null,
    screens?: AgentScreenRef[],
  ): Promise<AgentChatResponse> {
    const res = await httpClient.post<ApiResponse<AgentChatResponse>>(`${BASE}/chat`, {
      conversationId,
      message,
      pageContext: pageContext ?? null,
      screens: screens ?? [],
    });
    return res.data.data;
  },
  async health(): Promise<AgentHealth> {
    const res = await httpClient.get<ApiResponse<AgentHealth>>(`${BASE}/health`);
    return res.data.data;
  },
  async conversations(): Promise<AgentConversationSummary[]> {
    const res = await httpClient.get<ApiResponse<AgentConversationSummary[]>>(`${BASE}/conversations`);
    return res.data.data;
  },
  async messages(conversationId: string): Promise<AgentStoredMessage[]> {
    const res = await httpClient.get<ApiResponse<AgentStoredMessage[]>>(`${BASE}/conversations/${conversationId}/messages`);
    return res.data.data;
  },
  async archive(conversationId: string): Promise<void> {
    await httpClient.delete(`${BASE}/conversations/${conversationId}`);
  },
};
