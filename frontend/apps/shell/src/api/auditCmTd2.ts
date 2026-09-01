import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmTd2Item {
  id: string;
  branchCode: string;
  transactionDate: string;
  valueDate: string;
  postingUser: string;
  entryNumber: number | null;
  customerCode: string | null;
  customerName: string;
  disbursementNumber: string | null;
  businessCode: string | null;
  transactionStatus: string | null;
  currency: string | null;
  debitAmount: number | null;
  creditAmount: number | null;
  accountNumber: string | null;
  postingDateDiff: number | null;
  ipcasReviewResult: string | null;
  documentCheckResult: string | null;
  active: boolean;
}

export interface AuditCmTd2Request {
  branchCode: string;
  transactionDate: string;
  valueDate: string;
  postingUser: string;
  entryNumber: number | null;
  customerCode: string | null;
  customerName: string;
  disbursementNumber: string | null;
  businessCode: string | null;
  transactionStatus: string | null;
  currency: string | null;
  debitAmount: number | null;
  creditAmount: number | null;
  accountNumber: string | null;
  ipcasReviewResult: string | null;
  documentCheckResult: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-td2";

export async function listAuditCmTd2(): Promise<AuditCmTd2Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmTd2Item[]>>(BASE);
  return res.data.data;
}

export async function createAuditCmTd2(request: AuditCmTd2Request): Promise<AuditCmTd2Item> {
  const res = await httpClient.post<ApiResponse<AuditCmTd2Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmTd2(id: string, request: AuditCmTd2Request): Promise<AuditCmTd2Item> {
  const res = await httpClient.put<ApiResponse<AuditCmTd2Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmTd2(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmTd2(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmTd2(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_td2.xlsx" : "audit_cm_td2.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
