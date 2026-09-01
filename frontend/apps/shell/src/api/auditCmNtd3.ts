import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd3Item {
  id: string;
  branchCode: string;
  transactionDate: string;
  customerCode: string | null;
  customerName: string;
  customerAddress: string | null;
  accountNumber: string;
  currency: string | null;
  originalCurrencyBalance: number | null;
  convertedBalance: number | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

export interface AuditCmNtd3Request {
  branchCode: string;
  transactionDate: string;
  customerCode: string | null;
  customerName: string;
  customerAddress: string | null;
  accountNumber: string;
  currency: string | null;
  originalCurrencyBalance: number | null;
  convertedBalance: number | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd3";

export async function listAuditCmNtd3(): Promise<AuditCmNtd3Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd3Item[]>>(BASE);
  return res.data.data;
}

export async function createAuditCmNtd3(request: AuditCmNtd3Request): Promise<AuditCmNtd3Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd3Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd3(id: string, request: AuditCmNtd3Request): Promise<AuditCmNtd3Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd3Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd3(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd3(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd3(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd3.xlsx" : "audit_cm_ntd3.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
