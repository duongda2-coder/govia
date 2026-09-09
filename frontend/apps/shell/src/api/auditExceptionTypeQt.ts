import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";
import type { AuditExceptionCategory, AuditLevel } from "./auditExceptionType";

export interface AuditExceptionTypeQtItem {
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

export interface AuditExceptionTypeQtRequest {
  businessSegmentId: string | null;
  code: string;
  name: string;
  category: AuditExceptionCategory | null;
  impactLevel: AuditLevel | null;
  classificationBasis: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/master-data-qt/exception-type";

export async function listAuditExceptionTypesQt(): Promise<AuditExceptionTypeQtItem[]> {
  const res = await httpClient.get<ApiResponse<AuditExceptionTypeQtItem[]>>(BASE);
  return res.data.data;
}

export async function createAuditExceptionTypeQt(request: AuditExceptionTypeQtRequest): Promise<AuditExceptionTypeQtItem> {
  const res = await httpClient.post<ApiResponse<AuditExceptionTypeQtItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditExceptionTypeQt(id: string, request: AuditExceptionTypeQtRequest): Promise<AuditExceptionTypeQtItem> {
  const res = await httpClient.put<ApiResponse<AuditExceptionTypeQtItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditExceptionTypeQt(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditExceptionTypesQt(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditExceptionTypesQt(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_exception_type_qt.xlsx" : "audit_exception_type_qt.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
