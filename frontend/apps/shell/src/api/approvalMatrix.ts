import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { EmployeeRankLevel } from "./employees";

/** orgUnitId null = quy tac mac dinh (fallback) cho toan tenant. */
export interface ApprovalMatrixRule {
  id: string;
  orgUnitId: string | null;
  orgUnitCode: string | null;
  orgUnitName: string | null;
  finalApprovalLevel: EmployeeRankLevel;
  requireFinalSuperAdminStep: boolean;
  active: boolean;
}

export interface ApprovalMatrixRuleRequest {
  orgUnitId?: string | null;
  finalApprovalLevel: EmployeeRankLevel;
  requireFinalSuperAdminStep: boolean;
  active: boolean;
}

export async function listApprovalMatrixRules(): Promise<ApprovalMatrixRule[]> {
  const res = await httpClient.get<ApiResponse<ApprovalMatrixRule[]>>("/api/workflow/approval-matrix");
  return res.data.data;
}

export async function createApprovalMatrixRule(request: ApprovalMatrixRuleRequest): Promise<ApprovalMatrixRule> {
  const res = await httpClient.post<ApiResponse<ApprovalMatrixRule>>("/api/workflow/approval-matrix", request);
  return res.data.data;
}

export async function updateApprovalMatrixRule(id: string, request: ApprovalMatrixRuleRequest): Promise<ApprovalMatrixRule> {
  const res = await httpClient.put<ApiResponse<ApprovalMatrixRule>>(`/api/workflow/approval-matrix/${id}`, request);
  return res.data.data;
}

export async function deleteApprovalMatrixRule(id: string): Promise<void> {
  await httpClient.delete(`/api/workflow/approval-matrix/${id}`);
}
