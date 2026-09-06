import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { AuditEngagementItem } from "./auditEngagement";

/** Man hinh "Quản lý đợt kiểm toán" (Tao CKT (2).xlsx) - tach rieng API voi auditEngagement.ts
 * (man hinh CRUD "Khoi tao va quan ly cuoc kiem toan") vi day la man hinh giam sat/tong hop. */
export interface AuditEngagementMonitoringItem extends AuditEngagementItem {
  memberCount: number;
  businessSegmentCount: number;
  totalFindings: number;
  totalMaterialFindings: number;
  recommendationCount: number;
}

export interface ProgressStat {
  completed: number;
  total: number;
}

export interface AuditEngagementTeamMemberDetailItem {
  stt: number;
  memberId: string;
  employeeId: string;
  employeeCode: string | null;
  employeeName: string | null;
  roleTitle: string;
  businessSegmentNames: string;
  totalFindings: number;
  ttssTypeCount: number;
  totalMaterialFindings: number;
  materialTtssTypeCount: number;
  recommendationCount: number;
  cbktProgress: ProgressStat;
  thktSampleProgress: ProgressStat;
  thktNoSampleProgress: ProgressStat;
  score: number | null;
  ranking: string | null;
  note: string | null;
}

const BASE = "/api/audit/plan/engagement/monitoring";

export async function listAuditEngagementMonitoring(): Promise<AuditEngagementMonitoringItem[]> {
  const res = await httpClient.get<ApiResponse<AuditEngagementMonitoringItem[]>>(BASE);
  return res.data.data;
}

export async function getAuditEngagementTeamDetail(engagementId: string): Promise<AuditEngagementTeamMemberDetailItem[]> {
  const res = await httpClient.get<ApiResponse<AuditEngagementTeamMemberDetailItem[]>>(`${BASE}/${engagementId}/team-detail`);
  return res.data.data;
}

export async function updateAuditEngagementTeamRanking(engagementId: string, teamRanking: string | null): Promise<AuditEngagementMonitoringItem> {
  const res = await httpClient.patch<ApiResponse<AuditEngagementMonitoringItem>>(`${BASE}/${engagementId}/team-ranking`, { teamRanking });
  return res.data.data;
}

export async function updateAuditEngagementTeamMemberScoring(
  engagementId: string,
  memberId: string,
  request: { score: number | null; ranking: string | null; note: string | null },
): Promise<AuditEngagementTeamMemberDetailItem> {
  const res = await httpClient.patch<ApiResponse<AuditEngagementTeamMemberDetailItem>>(`${BASE}/${engagementId}/team-members/${memberId}`, request);
  return res.data.data;
}
