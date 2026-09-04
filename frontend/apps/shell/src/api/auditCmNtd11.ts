import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd11Item {
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
  referenceNumber: string;
  customerCode: string;
  customerName: string;
  transactionDate: string;
  currency: string | null;
  amount: number | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

export interface AuditCmNtd11Request {
  engagementId: string;
  assignedEmployeeId: string | null;
  processStepSummaryId: string | null;
  branchCode: string;
  referenceNumber: string;
  customerCode: string;
  customerName: string;
  transactionDate: string;
  currency: string | null;
  amount: number | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd11";

export async function listAuditCmNtd11(engagementId: string): Promise<AuditCmNtd11Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd11Item[]>>(BASE, { params: { engagementId } });
  return res.data.data;
}

export async function createAuditCmNtd11(request: AuditCmNtd11Request): Promise<AuditCmNtd11Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd11Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd11(id: string, request: AuditCmNtd11Request): Promise<AuditCmNtd11Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd11Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd11(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd11(engagementId: string, file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    params: { engagementId },
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd11(kind: "excel" | "word", engagementId: string): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { params: { engagementId }, responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd11.xlsx" : "audit_cm_ntd11.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
