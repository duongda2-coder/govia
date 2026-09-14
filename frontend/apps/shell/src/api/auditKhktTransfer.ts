import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditKhnsTransferCandidateItem {
  auditObjectCode: string;
  auditObjectName: string;
  teamLeadEmployeeCode: string | null;
  teamLeadEmployeeName: string | null;
  memberCount: number;
  expectedMonth: number | null;
  decisionNumber: string | null;
  decisionDate: string | null;
  expectedBatch: string | null;
  existingEngagementCode: string | null;
  transferable: boolean;
  blockReason: string | null;
}

export interface AuditKhnsTransferResultItem {
  auditObjectCode: string;
  success: boolean;
  engagementCode: string | null;
  message: string | null;
}

const BASE = "/api/audit/plan/khns-nam";

export async function listAuditKhktTransferCandidates(year: number): Promise<AuditKhnsTransferCandidateItem[]> {
  const res = await httpClient.get<ApiResponse<AuditKhnsTransferCandidateItem[]>>(`${BASE}/transfer-candidates`, { params: { year } });
  return res.data.data;
}

export async function transferAuditKhktObjects(year: number, auditObjectCodes: string[]): Promise<AuditKhnsTransferResultItem[]> {
  const res = await httpClient.post<ApiResponse<AuditKhnsTransferResultItem[]>>(
    `${BASE}/transfer`,
    { auditObjectCodes },
    { params: { year } },
  );
  return res.data.data;
}
