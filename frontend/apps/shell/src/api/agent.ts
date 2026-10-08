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
  /** Cong 3: AI duoc doc noi dung file dinh kem (mac dinh tat, bat sau khi ATTT duyet). */
  fileReading: boolean;
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

export interface RecommendationDraft {
  content: string;
  findingCodes: string[];
  rationale: string | null;
  /** false = ban nhap nhac ma phat hien/so lieu khong co trong du lieu TTSS - can kiem tra ky. */
  grounded: boolean;
}

export interface RecommendationDraftResult {
  engagementCode: string;
  businessSegmentCode: string | null;
  sourceTtssCount: number;
  drafts: RecommendationDraft[];
  similarRecommendations: { engagementCode: string; code: string; content: string }[];
  model: string;
}

export type RewritePurpose = "RECOMMENDATION" | "FINDING" | "SUMMARY";

export type TdkpReminderSource = "CEO_ALL" | "CEO_KH" | "BRANCH" | "RESOLUTION" | "UNIT";

/** Thu don doc - danh sach kien nghi trong `body` do he thong dung tu du lieu that; AI chi viet loi mo/ket. */
export interface ReminderLetter {
  unit: string;
  subject: string;
  body: string;
  items: { code: string | null; content: string | null; deadline: string | null; daysOverdue: number | null; status: string | null }[];
}

export interface ReminderDraftResult {
  source: TdkpReminderSource;
  sourceLabel: string;
  letters: ReminderLetter[];
  /** false = AI khong tra dung mau, he thong dung mau co dinh (so lieu van dung). */
  grounded: boolean;
  model: string;
}

export interface RewriteResult {
  text: string;
  changes: string[];
  grounded: boolean;
  model: string;
}

/** Soan nhap bang AI - chi tra ve ban nhap, KHONG luu gi; nguoi dung tu bam Them/Luu nhu binh thuong. */
export const agentDraftApi = {
  async recommendations(engagementId: string, businessSegmentId?: string | null, instruction?: string): Promise<RecommendationDraftResult> {
    const res = await httpClient.post<ApiResponse<RecommendationDraftResult>>(`${BASE}/drafts/recommendations`, {
      engagementId,
      businessSegmentId: businessSegmentId ?? null,
      instruction: instruction ?? null,
    });
    return res.data.data;
  },
  async reminder(source: TdkpReminderSource, itemIds: string[], dueWithinDays?: number): Promise<ReminderDraftResult> {
    const res = await httpClient.post<ApiResponse<ReminderDraftResult>>(`${BASE}/drafts/reminder`, { source, itemIds, dueWithinDays: dueWithinDays ?? null });
    return res.data.data;
  },
  async rewrite(text: string, purpose: RewritePurpose): Promise<RewriteResult> {
    const res = await httpClient.post<ApiResponse<RewriteResult>>(`${BASE}/drafts/rewrite`, { text, purpose });
    return res.data.data;
  },
};
