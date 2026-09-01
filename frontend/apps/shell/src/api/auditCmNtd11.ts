import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd11Item {
  id: string;
  branchCode: string;
  referenceNumber: number;
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
  branchCode: string;
  referenceNumber: number;
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

export async function listAuditCmNtd11(): Promise<AuditCmNtd11Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd11Item[]>>(BASE);
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

export async function importAuditCmNtd11(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd11(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd11.xlsx" : "audit_cm_ntd11.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
