import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { AssignmentApprovalStatus } from "./auditWorkManagement";

export interface AuditProgressReportItem {
  id: string;
  engagementId: string;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  reportedEmployeeId: string | null;
  reportedEmployeeCode: string | null;
  reportedEmployeeName: string | null;
  totalFindings: number;
  totalTtss: number;
  totalMaterialFindings: number;
  totalMaterialTtss: number;
  totalSamples: number;
  completedSamples: number;
  reportDate: string;
  reportRound: number;
  reportedByUsername: string | null;
  note: string | null;
  approvalStatus: AssignmentApprovalStatus | null;
  approvedBy: string | null;
  approvedAt: string | null;
}

function base(engagementId: string): string {
  return `/api/audit/plan/engagement/${engagementId}/work-management/progress-reports`;
}

export async function listAuditProgressReports(engagementId: string): Promise<AuditProgressReportItem[]> {
  const res = await httpClient.get<ApiResponse<AuditProgressReportItem[]>>(base(engagementId));
  return res.data.data;
}

export async function approveAuditProgressReports(engagementId: string, reportIds: string[]): Promise<string[]> {
  const res = await httpClient.post<ApiResponse<string[]>>(`${base(engagementId)}/approve`, { reportIds });
  return res.data.data;
}
