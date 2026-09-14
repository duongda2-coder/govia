import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";

export type AuditKhktSourceType = "BRANCH" | "OTHER";
export type AuditKhktSelectionDecision = "SELECTED" | "NOT_SELECTED";

export interface AuditKhktBpRowItem {
  id: string;
  departmentId: string;
  departmentCode: string | null;
  departmentName: string | null;
  year: number;
  sourceType: AuditKhktSourceType;
  auditObjectCode: string;
  auditObjectName: string;
  auditObjectCategoryCode: string | null;
  riskScore: number | null;
  rankLabel: string | null;
  onBalanceSheetLoan: number | null;
  fundingSource: number | null;
  reviewResult: string | null;
  selectionDecision: AuditKhktSelectionDecision | null;
  businessSegmentCodes: string[];
  /** 30 khoa dang "{LOAI}_T{1..5}" (vd "TTCP_T5") -> co/khong co lich su thanh tra/giam sat/kiem
   * toan nam tuong ung - xem AuditKhktBpService.buildInspectionHistory o BE. */
  inspectionHistory: Record<string, boolean>;
}

export interface AuditKhktBpCandidateRequest {
  departmentId: string;
  year: number;
  sourceType: AuditKhktSourceType;
  auditObjectCode: string;
  auditObjectName: string;
  auditObjectCategoryCode: string | null;
  riskScore: number | null;
  rankLabel: string | null;
  businessSegmentIds: string[];
}

export interface AuditKhktBpCandidateUpdateRequest {
  reviewResult: string | null;
  selectionDecision: AuditKhktSelectionDecision | null;
  businessSegmentIds: string[];
}

const BASE = "/api/audit/plan/khkt-bp";
const CONFIRMED_BASE = "/api/audit/plan/khkt-bp-confirmed";

export async function listAuditKhktBp(departmentId: string, year: number): Promise<AuditKhktBpRowItem[]> {
  const res = await httpClient.get<ApiResponse<AuditKhktBpRowItem[]>>(BASE, { params: { departmentId, year } });
  return res.data.data;
}

export async function listAuditKhktBpConfirmed(departmentId: string, year: number): Promise<AuditKhktBpRowItem[]> {
  const res = await httpClient.get<ApiResponse<AuditKhktBpRowItem[]>>(CONFIRMED_BASE, { params: { departmentId, year } });
  return res.data.data;
}

export async function createAuditKhktBp(request: AuditKhktBpCandidateRequest): Promise<AuditKhktBpRowItem> {
  const res = await httpClient.post<ApiResponse<AuditKhktBpRowItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditKhktBp(id: string, request: AuditKhktBpCandidateUpdateRequest): Promise<AuditKhktBpRowItem> {
  const res = await httpClient.put<ApiResponse<AuditKhktBpRowItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditKhktBp(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function confirmAuditKhktBp(departmentId: string, year: number): Promise<void> {
  await httpClient.post(`${BASE}/confirm`, null, { params: { departmentId, year } });
}

export async function exportAuditKhktBpReport(year: number): Promise<void> {
  const res = await httpClient.get("/api/audit/plan/khkt-report/bp/export", { params: { year }, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = `bao_cao_khkt_bp_${year}.xlsx`;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
