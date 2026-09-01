import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd1Item {
  id: string;
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
  workType: string | null;
  active: boolean;
}

export interface AuditCmNtd1Request {
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
  workType: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd1";

export async function listAuditCmNtd1(): Promise<AuditCmNtd1Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd1Item[]>>(BASE);
  return res.data.data;
}

export async function createAuditCmNtd1(request: AuditCmNtd1Request): Promise<AuditCmNtd1Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd1Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd1(id: string, request: AuditCmNtd1Request): Promise<AuditCmNtd1Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd1Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd1(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd1(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd1(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd1.xlsx" : "audit_cm_ntd1.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
