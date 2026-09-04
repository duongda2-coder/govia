import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd4Item {
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
  referenceNumber: number;
  openDate: string;
  corebankCustomerCode: string | null;
  amount: number | null;
  beneficiary: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

export interface AuditCmNtd4Request {
  engagementId: string;
  assignedEmployeeId: string | null;
  processStepSummaryId: string | null;
  branchCode: string;
  referenceNumber: number;
  openDate: string;
  corebankCustomerCode: string | null;
  amount: number | null;
  beneficiary: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd4";

export async function listAuditCmNtd4(engagementId: string): Promise<AuditCmNtd4Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd4Item[]>>(BASE, { params: { engagementId } });
  return res.data.data;
}

export async function createAuditCmNtd4(request: AuditCmNtd4Request): Promise<AuditCmNtd4Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd4Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd4(id: string, request: AuditCmNtd4Request): Promise<AuditCmNtd4Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd4Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd4(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd4(engagementId: string, file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    params: { engagementId },
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd4(kind: "excel" | "word", engagementId: string): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { params: { engagementId }, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd4.xlsx" : "audit_cm_ntd4.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
