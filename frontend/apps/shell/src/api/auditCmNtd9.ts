import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd9Item {
  id: string;
  branchCode: string;
  transactionDate: string;
  postingUser: string;
  customerCode: string | null;
  customerName: string;
  idNumber: string | null;
  customerType: string | null;
  transactionContent: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

export interface AuditCmNtd9Request {
  branchCode: string;
  transactionDate: string;
  postingUser: string;
  customerCode: string | null;
  customerName: string;
  idNumber: string | null;
  customerType: string | null;
  transactionContent: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  recommendationType: string | null;
  transactionStaff: string | null;
  controlUser: string | null;
  controlStaff: string | null;
  controlStaffTitle: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd9";

export async function listAuditCmNtd9(): Promise<AuditCmNtd9Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd9Item[]>>(BASE);
  return res.data.data;
}

export async function createAuditCmNtd9(request: AuditCmNtd9Request): Promise<AuditCmNtd9Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd9Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd9(id: string, request: AuditCmNtd9Request): Promise<AuditCmNtd9Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd9Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd9(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd9(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd9(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd9.xlsx" : "audit_cm_ntd9.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
