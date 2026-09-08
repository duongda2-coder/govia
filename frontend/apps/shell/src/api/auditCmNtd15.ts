import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd15Item {
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
  postingUser: string;
  entryNumber: number | null;
  debitAmount: number | null;
  creditAmount: number | null;
  transactionStatus: string | null;
  currency: string | null;
  accountNumber: string | null;
  content: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

export interface AuditCmNtd15Request {
  engagementId: string;
  assignedEmployeeId: string | null;
  processStepSummaryId: string | null;
  branchCode: string;
  transactionDate: string;
  postingUser: string;
  entryNumber: number;
  debitAmount: number | null;
  creditAmount: number | null;
  transactionStatus: string | null;
  currency: string | null;
  accountNumber: string | null;
  content: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd15";

export async function listAuditCmNtd15(engagementId: string): Promise<AuditCmNtd15Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd15Item[]>>(BASE, { params: { engagementId } });
  return res.data.data;
}

export async function createAuditCmNtd15(request: AuditCmNtd15Request): Promise<AuditCmNtd15Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd15Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd15(id: string, request: AuditCmNtd15Request): Promise<AuditCmNtd15Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd15Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd15(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd15(engagementId: string, file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    params: { engagementId },
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd15(kind: "excel" | "word", engagementId: string): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { params: { engagementId }, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd15.xlsx" : "audit_cm_ntd15.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
