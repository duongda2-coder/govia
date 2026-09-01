import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd14Item {
  id: string;
  branchCode: string;
  attendanceDate: string;
  staffCode: string;
  staffName: string | null;
  attendanceCode: string | null;
  description: string | null;
  matchedTransactionCount: number | null;
  unmatchedTransactionCount: number | null;
  adjustedTransactionCount: number | null;
  userCode: string | null;
  note: string | null;
  sampleCode: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  active: boolean;
}

export interface AuditCmNtd14Request {
  branchCode: string;
  attendanceDate: string;
  staffCode: string;
  staffName: string | null;
  attendanceCode: string | null;
  description: string | null;
  matchedTransactionCount: number | null;
  unmatchedTransactionCount: number | null;
  adjustedTransactionCount: number | null;
  userCode: string | null;
  note: string | null;
  sampleCode: string | null;
  sampleReason: string | null;
  auditResult: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd14";

export async function listAuditCmNtd14(): Promise<AuditCmNtd14Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd14Item[]>>(BASE);
  return res.data.data;
}

export async function createAuditCmNtd14(request: AuditCmNtd14Request): Promise<AuditCmNtd14Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd14Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd14(id: string, request: AuditCmNtd14Request): Promise<AuditCmNtd14Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd14Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd14(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd14(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd14(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd14.xlsx" : "audit_cm_ntd14.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
