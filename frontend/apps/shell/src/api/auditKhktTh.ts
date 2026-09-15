import type { ApiResponse } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { AuditKhktApprovalStatus, AuditKhktSelectionChoice, AuditKhktSourceType } from "./auditKhktBp";

export interface AuditKhktThRowItem {
  id: string;
  year: number;
  sourceType: AuditKhktSourceType;
  auditObjectCode: string;
  auditObjectName: string;
  auditObjectCategoryCode: string | null;
  riskScore: number | null;
  rankLabel: string | null;
  onBalanceSheetLoan: number | null;
  fundingSource: number | null;
  bpReviewResult: string | null;
  proposalBasisTh: string | null;
  expertOpinion: string | null;
  selection1: boolean;
  selection2: boolean;
  selection3: boolean;
  auditScope: string | null;
  adhocAuditOrSupervision: AuditKhktSelectionChoice | null;
  planAdjustment: AuditKhktSelectionChoice | null;
  adjustmentReason: string | null;
  khktgsAfterAdjustment: AuditKhktSelectionChoice | null;
  approvalStatus: AuditKhktApprovalStatus | null;
  /** Danh sach ma phong da de xuat doi tuong nay (tinh dong tu BP2, chi de tham khao). */
  proposingDepartmentCodes: string[];
  /** Hop cac ma linh vuc CAC PHONG da de xuat cho doi tuong nay (tinh dong tu BP2, chi de tham khao). */
  bpProposedSegmentCodes: string[];
  /** Linh vuc Phong Ke hoach THUC SU quyet dinh theo doi (TH tu chon, luu rieng). */
  thBusinessSegmentCodes: string[];
}

export interface AuditKhktThCandidateUpdateRequest {
  proposalBasisTh: string | null;
  expertOpinion: string | null;
  selection1: boolean;
  selection2: boolean;
  selection3: boolean;
  auditScope: string | null;
  adhocAuditOrSupervision: AuditKhktSelectionChoice | null;
  planAdjustment: AuditKhktSelectionChoice | null;
  adjustmentReason: string | null;
  khktgsAfterAdjustment: AuditKhktSelectionChoice | null;
  thBusinessSegmentIds: string[];
}

const BASE = "/api/audit/plan/khkt-th";
const CONFIRMED_BASE = "/api/audit/plan/khkt-th-confirmed";

export async function listAuditKhktTh(year: number): Promise<AuditKhktThRowItem[]> {
  const res = await httpClient.get<ApiResponse<AuditKhktThRowItem[]>>(BASE, { params: { year } });
  return res.data.data;
}

export async function listAuditKhktThConfirmed(year: number): Promise<AuditKhktThRowItem[]> {
  const res = await httpClient.get<ApiResponse<AuditKhktThRowItem[]>>(CONFIRMED_BASE, { params: { year } });
  return res.data.data;
}

export async function syncAuditKhktTh(year: number): Promise<void> {
  await httpClient.post(`${BASE}/sync`, null, { params: { year } });
}

export async function updateAuditKhktTh(id: string, request: AuditKhktThCandidateUpdateRequest): Promise<AuditKhktThRowItem> {
  const res = await httpClient.put<ApiResponse<AuditKhktThRowItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditKhktTh(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function confirmAuditKhktTh(year: number): Promise<void> {
  await httpClient.post(`${BASE}/confirm`, null, { params: { year } });
}

export async function setAuditKhktThApprovalStatus(id: string, approved: boolean): Promise<AuditKhktThRowItem> {
  const res = await httpClient.patch<ApiResponse<AuditKhktThRowItem>>(`${CONFIRMED_BASE}/${id}/approval-status`, null, {
    params: { approved },
  });
  return res.data.data;
}

export async function exportAuditKhktThReport(year: number): Promise<void> {
  const res = await httpClient.get("/api/audit/plan/khkt-report/th/export", { params: { year }, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = `bao_cao_khkt_th_${year}.xlsx`;
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
