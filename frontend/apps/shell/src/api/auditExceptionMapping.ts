import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditExceptionMappingItem {
  id: string;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  processStepDetailId: string;
  processStepDetailCode: string | null;
  exceptionTypeId: string;
  exceptionTypeCode: string | null;
  exceptionTypeName: string | null;
  active: boolean;
}

export interface AuditExceptionMappingRequest {
  businessSegmentId: string | null;
  processStepDetailId: string;
  exceptionTypeId: string;
  active: boolean;
}

const BASE = "/api/audit/plan/master-data/exception-mapping";

export async function listAuditExceptionMappings(): Promise<AuditExceptionMappingItem[]> {
  const res = await httpClient.get<ApiResponse<AuditExceptionMappingItem[]>>(BASE);
  return res.data.data;
}

export async function createAuditExceptionMapping(request: AuditExceptionMappingRequest): Promise<AuditExceptionMappingItem> {
  const res = await httpClient.post<ApiResponse<AuditExceptionMappingItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditExceptionMapping(id: string, request: AuditExceptionMappingRequest): Promise<AuditExceptionMappingItem> {
  const res = await httpClient.put<ApiResponse<AuditExceptionMappingItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditExceptionMapping(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditExceptionMappings(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditExceptionMappings(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_exception_mapping.xlsx" : "audit_exception_mapping.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
