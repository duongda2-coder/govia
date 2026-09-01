import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd6Item {
  id: string;
  branchCode: string;
  staffCode: string | null;
  staffName: string;
  ipcasUser: string;
  adUser: string | null;
  securityDevice: string | null;
  sampleReason: string | null;
  sampleCode: string | null;
  auditResult: string | null;
  active: boolean;
}

export interface AuditCmNtd6Request {
  branchCode: string;
  staffCode: string | null;
  staffName: string;
  ipcasUser: string;
  adUser: string | null;
  securityDevice: string | null;
  sampleReason: string | null;
  sampleCode: string | null;
  auditResult: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd6";

export async function listAuditCmNtd6(): Promise<AuditCmNtd6Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd6Item[]>>(BASE);
  return res.data.data;
}

export async function createAuditCmNtd6(request: AuditCmNtd6Request): Promise<AuditCmNtd6Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd6Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd6(id: string, request: AuditCmNtd6Request): Promise<AuditCmNtd6Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd6Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd6(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd6(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd6(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd6.xlsx" : "audit_cm_ntd6.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
