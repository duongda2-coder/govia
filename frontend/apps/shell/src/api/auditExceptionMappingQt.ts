import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditExceptionMappingQtItem {
  id: string;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  processStepDetailId: string;
  processStepDetailCode: string | null;
  exceptionTypeId: string;
  exceptionTypeCode: string | null;
  exceptionTypeName: string | null;
  applicableYear: number | null;
  active: boolean;
}

export interface AuditExceptionMappingQtRequest {
  businessSegmentId: string | null;
  processStepDetailId: string;
  exceptionTypeId: string;
  applicableYear: number | null;
  active: boolean;
}

const BASE = "/api/audit/plan/master-data-qt/exception-mapping";

export async function listAuditExceptionMappingsQt(): Promise<AuditExceptionMappingQtItem[]> {
  const res = await httpClient.get<ApiResponse<AuditExceptionMappingQtItem[]>>(BASE);
  return res.data.data;
}

export async function createAuditExceptionMappingQt(request: AuditExceptionMappingQtRequest): Promise<AuditExceptionMappingQtItem> {
  const res = await httpClient.post<ApiResponse<AuditExceptionMappingQtItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditExceptionMappingQt(id: string, request: AuditExceptionMappingQtRequest): Promise<AuditExceptionMappingQtItem> {
  const res = await httpClient.put<ApiResponse<AuditExceptionMappingQtItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditExceptionMappingQt(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditExceptionMappingsQt(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditExceptionMappingsQt(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_exception_mapping_qt.xlsx" : "audit_exception_mapping_qt.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
