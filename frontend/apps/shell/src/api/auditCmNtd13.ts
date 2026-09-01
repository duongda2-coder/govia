import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd13Item {
  id: string;
  branchCode: string;
  occurrenceDate: string;
  merchantId: string | null;
  merchantAccountNumber: string;
  businessRegistrationName: string;
  status: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

export interface AuditCmNtd13Request {
  branchCode: string;
  occurrenceDate: string;
  merchantId: string | null;
  merchantAccountNumber: string;
  businessRegistrationName: string;
  status: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd13";

export async function listAuditCmNtd13(): Promise<AuditCmNtd13Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd13Item[]>>(BASE);
  return res.data.data;
}

export async function createAuditCmNtd13(request: AuditCmNtd13Request): Promise<AuditCmNtd13Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd13Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd13(id: string, request: AuditCmNtd13Request): Promise<AuditCmNtd13Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd13Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd13(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd13(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd13(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd13.xlsx" : "audit_cm_ntd13.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
