import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditCmNtd7Item {
  id: string;
  branchCode: string;
  constructionCode: string;
  constructionName: string | null;
  content: string | null;
  documentType: string | null;
  completenessAssessment: string | null;
  assessment: string | null;
  auditResult: string | null;
  active: boolean;
}

export interface AuditCmNtd7Request {
  branchCode: string;
  constructionCode: string;
  constructionName: string | null;
  content: string | null;
  documentType: string | null;
  completenessAssessment: string | null;
  assessment: string | null;
  auditResult: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/execution/cm-ntd7";

export async function listAuditCmNtd7(): Promise<AuditCmNtd7Item[]> {
  const res = await httpClient.get<ApiResponse<AuditCmNtd7Item[]>>(BASE);
  return res.data.data;
}

export async function createAuditCmNtd7(request: AuditCmNtd7Request): Promise<AuditCmNtd7Item> {
  const res = await httpClient.post<ApiResponse<AuditCmNtd7Item>>(BASE, request);
  return res.data.data;
}

export async function updateAuditCmNtd7(id: string, request: AuditCmNtd7Request): Promise<AuditCmNtd7Item> {
  const res = await httpClient.put<ApiResponse<AuditCmNtd7Item>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditCmNtd7(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditCmNtd7(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditCmNtd7(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_cm_ntd7.xlsx" : "audit_cm_ntd7.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
