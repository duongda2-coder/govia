import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd2Item {
  id: string;
  branchCode: string;
  transactionDate: string;
  valueDate: string | null;
  postingUser: string;
  entryNumber: number | null;
  currency: string | null;
  amount: number | null;
  accountNumber: string | null;
  bookNumber: string | null;
  transactionType: string | null;
  transactionStatus: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

export interface AuditCmNtd2Request {
  branchCode: string;
  transactionDate: string;
  valueDate: string | null;
  postingUser: string;
  entryNumber: number | null;
  currency: string | null;
  amount: number | null;
  accountNumber: string | null;
  bookNumber: string | null;
  transactionType: string | null;
  transactionStatus: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd2";

export async function listAuditCmNtd2(): Promise<AuditCmNtd2Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd2Item[]>>(BASE);
  return res.data.data;
}

export async function createAuditCmNtd2(request: AuditCmNtd2Request): Promise<AuditCmNtd2Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd2Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd2(id: string, request: AuditCmNtd2Request): Promise<AuditCmNtd2Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd2Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd2(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd2(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd2(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd2.xlsx" : "audit_cm_ntd2.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
