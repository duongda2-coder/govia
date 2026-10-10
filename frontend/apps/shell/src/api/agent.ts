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
  /** G4: job "Gợi ý AI" theo lich dang bat. */
  scheduledSuggestions: boolean;
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

// ---------------------------------------------------------------- G4

export type DgclSuggestionValue = "COMPLIANT" | "NON_COMPLIANT" | "APPLIES" | "NOT_APPLIES" | "NEED_REVIEW";

/** Goi y cham DGCL - chi de nguoi cham tham khao, KHONG luu vao phieu. */
export interface DgclScoreDraftItem {
  key: string;
  stt: string | null;
  content: string;
  current: DgclSuggestionValue | "NOT_SET";
  suggestion: DgclSuggestionValue;
  violationCount: number | null;
  reason: string | null;
  evidence: string | null;
  differsFromCurrent: boolean;
}

export interface DgclScoreDraftResult {
  engagementCode: string;
  subject: string;
  appendix: "PL01A" | "PL01B" | "PL01F";
  sheetSaved: boolean;
  sheetConfirmed: boolean;
  currentScore: number | null;
  criteriaCount: number;
  truncated: boolean;
  items: DgclScoreDraftItem[];
  suggestedPositive: number;
  suggestedNegative: number;
  needReview: number;
  /** Goi y AI bi ha ve "can xem ho so" vi khong chi ra duoc can cu co that. */
  downgraded: number;
  suggestedRatio: number | null;
  dossierFacts: string[];
  overall: string | null;
  grounded: boolean;
  model: string;
}

export type ImportCatalog =
  | "control_point"
  | "control_point_qt"
  | "work_item"
  | "work_item_qt"
  | "exception_type"
  | "exception_type_qt"
  | "exception_mapping"
  | "exception_mapping_qt"
  | "process_step"
  | "process_step_qt";

export interface ImportCheckIssue {
  row: number;
  message: string;
}

export interface ImportCheckResult {
  catalog: ImportCatalog;
  catalogLabel: string;
  fileName: string;
  totalRows: number;
  okRows: number;
  errorRows: number;
  warningRows: number;
  expectedHeaders: string[];
  missingHeaders: string[];
  unknownHeaders: string[];
  headerHints: string[];
  codeColumn: string | null;
  yearColumn: string | null;
  nameColumn: string | null;
  errors: ImportCheckIssue[];
  warnings: ImportCheckIssue[];
  truncated: boolean;
  readyToImport: boolean;
}

export const agentG4DraftApi = {
  async dgclScore(engagementId: string, subjectKey: string, appendix: string, instruction?: string): Promise<DgclScoreDraftResult> {
    const res = await httpClient.post<ApiResponse<DgclScoreDraftResult>>(`${BASE}/drafts/dgcl-score`, {
      engagementId,
      subjectKey,
      appendix,
      instruction: instruction ?? null,
    });
    return res.data.data;
  },
  async importCheck(catalog: ImportCatalog, file: File): Promise<ImportCheckResult> {
    const form = new FormData();
    form.append("catalog", catalog);
    form.append("file", file);
    const res = await httpClient.post<ApiResponse<ImportCheckResult>>(`${BASE}/drafts/import-check`, form);
    return res.data.data;
  },
};

/** 1 "Gợi ý AI" do job theo lich (hoac nut Lam moi) sinh tu du lieu nguoi dung duoc xem. */
export interface AgentSuggestion {
  id: string;
  agentCode: string;
  category: string;
  severity: "INFO" | "WARN" | "HIGH";
  title: string;
  detail: string | null;
  linkPath: string | null;
  itemCount: number;
  runDate: string;
  createdAt: string;
  read: boolean;
}

export interface AgentKpiTarget {
  name: string;
  goal: string;
  unit: "PERCENT" | "COUNT";
  value: number | null;
  met: boolean | null;
}

export interface AgentKpi {
  periodDays: number;
  from: string;
  questions: number;
  activeUsers: number;
  answersByAgent: Record<string, number>;
  groundedRate: number | null;
  groundedRateByAgent: Record<string, number>;
  latencyP50Ms: number | null;
  latencyP90Ms: number | null;
  answersUnder15sRate: number | null;
  llmErrors: number;
  toolCalls: number;
  toolCallsDenied: number;
  drafts: Record<string, number>;
  suggestionsGenerated: number;
  targets: AgentKpiTarget[];
}

export const agentSuggestionApi = {
  async list(): Promise<AgentSuggestion[]> {
    const res = await httpClient.get<ApiResponse<AgentSuggestion[]>>(`${BASE}/suggestions`);
    return res.data.data;
  },
  async refresh(): Promise<AgentSuggestion[]> {
    const res = await httpClient.post<ApiResponse<AgentSuggestion[]>>(`${BASE}/suggestions/refresh`);
    return res.data.data;
  },
  async dismiss(id: string): Promise<void> {
    await httpClient.post(`${BASE}/suggestions/${id}/dismiss`);
  },
  async markRead(): Promise<void> {
    await httpClient.post(`${BASE}/suggestions/read`);
  },
  async kpi(days: number): Promise<AgentKpi> {
    const res = await httpClient.get<ApiResponse<AgentKpi>>(`${BASE}/kpi`, { params: { days } });
    return res.data.data;
  },
};
