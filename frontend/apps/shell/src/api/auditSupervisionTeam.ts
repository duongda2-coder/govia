import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { AuditEngagementMonitoringItem } from "./auditEngagementMonitoring";

export interface AuditSupervisionCandidateItem {
  employeeId: string;
  employeeCode: string | null;
  employeeName: string | null;
  username: string | null;
  selected: boolean;
}

export interface AuditSupervisionTeamMemberItem {
  id: string;
  employeeId: string;
  employeeCode: string | null;
  employeeName: string | null;
  username: string | null;
}

export interface AuditSupervisionEvaluationItem {
  id: string | null;
  engagementId: string;
  employeeId: string;
  employeeCode: string | null;
  employeeName: string | null;
  username: string | null;
  progress: boolean;
  contentAssured: boolean;
  qualityAssured: boolean;
  note: string | null;
  evaluatedAt: string | null;
}

export interface SaveSupervisionEvaluationRequest {
  progress: boolean;
  contentAssured: boolean;
  qualityAssured: boolean;
  note: string | null;
}

const BASE = "/api/audit/plan/engagement";

export async function listSupervisionCandidates(engagementId: string): Promise<AuditSupervisionCandidateItem[]> {
  const res = await httpClient.get<ApiResponse<AuditSupervisionCandidateItem[]>>(`${BASE}/${engagementId}/supervision-team/candidates`);
  return res.data.data;
}

export async function saveSupervisionTeam(engagementId: string, employeeIds: string[]): Promise<AuditSupervisionTeamMemberItem[]> {
  const res = await httpClient.put<ApiResponse<AuditSupervisionTeamMemberItem[]>>(`${BASE}/${engagementId}/supervision-team`, { employeeIds });
  return res.data.data;
}

export async function listSupervisionTeam(engagementId: string): Promise<AuditSupervisionTeamMemberItem[]> {
  const res = await httpClient.get<ApiResponse<AuditSupervisionTeamMemberItem[]>>(`${BASE}/${engagementId}/supervision-team`);
  return res.data.data;
}

export async function getMySupervisionEvaluation(engagementId: string): Promise<AuditSupervisionEvaluationItem> {
  const res = await httpClient.get<ApiResponse<AuditSupervisionEvaluationItem>>(`${BASE}/${engagementId}/supervision-team/my-evaluation`);
  return res.data.data;
}

export async function saveMySupervisionEvaluation(engagementId: string, request: SaveSupervisionEvaluationRequest): Promise<AuditSupervisionEvaluationItem> {
  const res = await httpClient.put<ApiResponse<AuditSupervisionEvaluationItem>>(`${BASE}/${engagementId}/supervision-team/my-evaluation`, request);
  return res.data.data;
}

export async function listAllSupervisionEvaluations(engagementId: string): Promise<AuditSupervisionEvaluationItem[]> {
  const res = await httpClient.get<ApiResponse<AuditSupervisionEvaluationItem[]>>(`${BASE}/${engagementId}/supervision-team/evaluations`);
  return res.data.data;
}

export async function listMySupervisionEngagements(): Promise<AuditEngagementMonitoringItem[]> {
  const res = await httpClient.get<ApiResponse<AuditEngagementMonitoringItem[]>>("/api/audit/supervision-team/my-engagements");
  return res.data.data;
}
