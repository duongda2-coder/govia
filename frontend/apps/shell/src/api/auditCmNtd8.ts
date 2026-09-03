import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd8Item {
  id: string;
  engagementId: string | null;
  engagementCode: string | null;
  assignedEmployeeId: string | null;
  assignedEmployeeCode: string | null;
  assignedUsername: string | null;
  processStepSummaryId: string | null;
  processStepSummaryCode: string | null;
  processStepSummaryName: string | null;
  branchCode: string;
  transactionDate: string;
  referenceNumber: number | null;
  postingUser: string;
  entryNumber: number;
  amount: number | null;
  currency: string | null;
  orderingParty: string | null;
  beneficiaryParty: string | null;
  beneficiaryAccount: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

export interface AuditCmNtd8Request {
  engagementId: string;
  assignedEmployeeId: string | null;
  processStepSummaryId: string | null;
  branchCode: string;
  transactionDate: string;
  referenceNumber: number | null;
  postingUser: string;
  entryNumber: number;
  amount: number | null;
  currency: string | null;
  orderingParty: string | null;
  beneficiaryParty: string | null;
  beneficiaryAccount: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd8";

export async function listAuditCmNtd8(engagementId: string): Promise<AuditCmNtd8Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd8Item[]>>(BASE, { params: { engagementId } });
  return res.data.data;
}

export async function createAuditCmNtd8(request: AuditCmNtd8Request): Promise<AuditCmNtd8Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd8Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd8(id: string, request: AuditCmNtd8Request): Promise<AuditCmNtd8Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd8Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd8(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd8(engagementId: string, file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    params: { engagementId },
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd8(kind: "excel" | "word", engagementId: string): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { params: { engagementId }, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd8.xlsx" : "audit_cm_ntd8.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
