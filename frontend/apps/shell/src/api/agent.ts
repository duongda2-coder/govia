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
}

export interface AgentHealth {
  ollamaReachable: boolean;
  model: string;
  configured: boolean;
}

export const agentApi = {
  async chat(conversationId: string, message: string): Promise<AgentChatResponse> {
    const res = await httpClient.post<ApiResponse<AgentChatResponse>>(`${BASE}/chat`, { conversationId, message });
    return res.data.data;
  },
  async health(): Promise<AgentHealth> {
    const res = await httpClient.get<ApiResponse<AgentHealth>>(`${BASE}/health`);
    return res.data.data;
  },
};
