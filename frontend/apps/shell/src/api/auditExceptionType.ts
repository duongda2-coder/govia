import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export type AuditExceptionCategory = "RISK_MANAGEMENT" | "INTERNAL_CONTROL";
export type AuditLevel = "HIGH" | "MEDIUM" | "LOW";

export interface AuditExceptionTypeItem {
  id: string;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  code: string;
  name: string;
  category: AuditExceptionCategory | null;
  impactLevel: AuditLevel | null;
  classificationBasis: string | null;
  active: boolean;
}

export interface AuditExceptionTypeRequest {
  businessSegmentId: string | null;
  code: string;
  name: string;
  category: AuditExceptionCategory | null;
  impactLevel: AuditLevel | null;
  classificationBasis: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/master-data/exception-type";

export async function listAuditExceptionTypes(): Promise<AuditExceptionTypeItem[]> {
  const res = await httpClient.get<ApiResponse<AuditExceptionTypeItem[]>>(BASE);
  return res.data.data;
}

export async function createAuditExceptionType(request: AuditExceptionTypeRequest): Promise<AuditExceptionTypeItem> {
  const res = await httpClient.post<ApiResponse<AuditExceptionTypeItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditExceptionType(id: string, request: AuditExceptionTypeRequest): Promise<AuditExceptionTypeItem> {
  const res = await httpClient.put<ApiResponse<AuditExceptionTypeItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditExceptionType(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditExceptionTypes(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditExceptionTypes(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_exception_type.xlsx" : "audit_exception_type.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
