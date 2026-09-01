import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd12Item {
  id: string;
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

export interface AuditCmNtd12Request {
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

const BASE = "/api/audit/plan/execution/cm-ntd12";

export async function listAuditCmNtd12(): Promise<AuditCmNtd12Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd12Item[]>>(BASE);
  return res.data.data;
}

export async function createAuditCmNtd12(request: AuditCmNtd12Request): Promise<AuditCmNtd12Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd12Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd12(id: string, request: AuditCmNtd12Request): Promise<AuditCmNtd12Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd12Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd12(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd12(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd12(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd12.xlsx" : "audit_cm_ntd12.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
