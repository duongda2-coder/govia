import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export interface AuditAppendixItem {
  id: string;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  sampleType: string;
  appendixCode: string;
  note: string | null;
  active: boolean;
}

export interface AuditAppendixRequest {
  businessSegmentId: string | null;
  sampleType: string;
  appendixCode: string;
  note: string | null;
  active: boolean;
}

const BASE = "/api/audit/master-data/appendix";

export async function listAuditAppendices(): Promise<AuditAppendixItem[]> {
  const res = await httpClient.get<ApiResponse<AuditAppendixItem[]>>(BASE);
  return res.data.data;
}

export async function createAuditAppendix(request: AuditAppendixRequest): Promise<AuditAppendixItem> {
  const res = await httpClient.post<ApiResponse<AuditAppendixItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditAppendix(id: string, request: AuditAppendixRequest): Promise<AuditAppendixItem> {
  const res = await httpClient.put<ApiResponse<AuditAppendixItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditAppendix(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditAppendices(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

/** Tai file Excel/Word ve may - dung axios (khong phai <a href>) de header Authorization duoc dinh kem tu dong. */
export async function exportAuditAppendices(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_appendix.xlsx" : "audit_appendix.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
