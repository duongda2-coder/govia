import type { ApiResponse, ImportResult } from "@govia/ui-kit";
import { httpClient } from "./client";

export type AuditWorkPhase = "PREPARATION" | "EXECUTION" | "CLOSING";

export interface AuditWorkItemItem {
  id: string;
  phase: AuditWorkPhase | null;
  businessSegmentId: string | null;
  businessSegmentCode: string | null;
  businessSegmentName: string | null;
  code: string;
  name: string;
  applicableYear: number | null;
  workSetCode: string | null;
  workType: string | null;
  active: boolean;
}

export interface AuditWorkItemRequest {
  phase: AuditWorkPhase | null;
  businessSegmentId: string | null;
  code: string;
  name: string;
  applicableYear: number | null;
  workSetCode: string | null;
  workType: string | null;
  active: boolean;
}

const BASE = "/api/audit/plan/master-data/work-item";

export async function listAuditWorkItems(): Promise<AuditWorkItemItem[]> {
  const res = await httpClient.get<ApiResponse<AuditWorkItemItem[]>>(BASE);
  return res.data.data;
}

export async function createAuditWorkItem(request: AuditWorkItemRequest): Promise<AuditWorkItemItem> {
  const res = await httpClient.post<ApiResponse<AuditWorkItemItem>>(BASE, request);
  return res.data.data;
}

export async function updateAuditWorkItem(id: string, request: AuditWorkItemRequest): Promise<AuditWorkItemItem> {
  const res = await httpClient.put<ApiResponse<AuditWorkItemItem>>(`${BASE}/${id}`, request);
  return res.data.data;
}

export async function deleteAuditWorkItem(id: string): Promise<void> {
  await httpClient.delete(`${BASE}/${id}`);
}

export async function importAuditWorkItems(file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await httpClient.post<ApiResponse<ImportResult>>(`${BASE}/import`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.data;
}

export async function exportAuditWorkItems(kind: "excel" | "word"): Promise<void> {
  const res = await httpClient.get(`${BASE}/export/${kind}`, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(res.data as Blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = kind === "excel" ? "audit_work_item.xlsx" : "audit_work_item.docx";
  link.click();
  window.URL.revokeObjectURL(blobUrl);
}
